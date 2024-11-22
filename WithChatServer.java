//2071141 홍민혁
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class WithChatServer extends JFrame{
    private int port;
    private ServerSocket serverSocket = null;

    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();

    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;

    public WithChatServer(int port) {
        super("2071141 With ChatServer");

        buildGUI();

        setBounds(600, 80, 400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        this.port = port;

    }

    private void buildGUI() {
        add(createDisplayPanel(),BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDisplayPanel() {
        t_display = new JTextArea();
        t_display.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(t_display);
        scrollPane.setSize(t_display.getWidth(), t_display.getHeight());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }


    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(1,0));
        b_disconnect = new JButton("서버 종료");
        b_connect = new JButton("서버 시작");
        b_exit = new JButton("종료");

        b_disconnect.setEnabled(false);
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        startServer();
                    }
                });
                acceptThread.start();

                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                b_exit.setEnabled(false);
            }
        });


        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();

                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                b_exit.setEnabled(true);
            }
        });

        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(-1);
            }
        });

        panel.add(b_connect);
        panel.add(b_disconnect);
        panel.add(b_exit);

        b_disconnect.setEnabled(false);

        return panel;
    }

    private void startServer() {
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다.");

            while(acceptThread == Thread.currentThread()) {
                clientSocket = serverSocket.accept();

                String cAddr = clientSocket.getInetAddress().getHostAddress();
                printDisplay("클라이언트가 연결되었습니다: " + cAddr);

                ClientHandler cHandler= new ClientHandler(clientSocket);
                users.add(cHandler);
                cHandler.start();
            }
        } catch (SocketException e) {
            printDisplay("서버 소켓 종료");
        } catch (IOException e){
            e.printStackTrace();
        }
        finally {
            try {
                if(clientSocket != null) clientSocket.close();
                if(serverSocket != null) serverSocket.close();
            }
            catch (IOException e) {
                System.err.println("서버 닫기 오류> " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    private void disconnect() {
        try {
            acceptThread = null;
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 소켓 닫기 오류> " + e.getMessage());
            System.exit(-1);
        }
    }

    private void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }


    private class ClientHandler extends Thread{
        private Socket clientSocket;
        //private BufferedWriter out;
        private ObjectOutputStream out;

        private String uid;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private void receiveMessages(Socket socket) {
            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                String message;
                //while((message = in.readLine()) != null) {
                ChatMsg msg;
                while((msg = (ChatMsg)in.readObject()) != null) {
                    if(msg.mode == ChatMsg.MODE_LOGIN) {
                        uid = msg.userID;

                        printDisplay("새 참가자: " + uid);
                        printDisplay("현재 참가자 수: " + users.size());
                        continue;
                    }
                    else if (msg.mode == ChatMsg.MODE_LOGOUT) {
                        break;
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_STRING) {
                        message = uid + ": " + msg.message;

                        printDisplay(message);
                        broadcasting(msg);
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_IMAGE) {
                        printDisplay(uid + ": " + msg.message);
                        broadcasting(msg);
                    }
                }

                users.removeElement(this);
                printDisplay(uid + " 퇴장. 현재 참가자 수: " + users.size());
            }
            catch (IOException e) {
                users.removeElement(this);
                printDisplay(uid + " 연결 끊김. 현재 참가자 수: " + users.size());
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("서버 닫기 오류> " + e.getMessage());
                    System.exit(-1);
                }
            }
        }

        private void send(ChatMsg msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
            }
        }

        private void sendMessage(String msg) {
            send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, msg));
        }

        private void broadcasting(ChatMsg msg) {
            for(ClientHandler c : users) {
                c.send(msg);
            }
        }

        @Override
        public void run() {
            receiveMessages(clientSocket);
        }
    }

    public static void main(String[] args) {
        int port = 54321;
        WithChatServer server  = new WithChatServer(port);
    }

}
