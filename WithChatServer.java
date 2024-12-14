import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class WithChatServer extends JFrame{
    private int port;
    private ServerSocket serverSocket = null;

    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private ArrayList<String> userIDs = new ArrayList<>();

    private JTextArea t_display;
    private JButton b_connect, b_disconnect, b_exit;

    private int orderIndex;
    private String selectedWord;

    public WithChatServer(int port) {
        super("Server");

        this.port = port;
        buildGUI();

        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startServer();
            }
        });
        acceptThread.start();

        setBounds(600, 80, 900, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
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
        b_exit = new JButton("종료");
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
                System.exit(-1);
            }
        });
        panel.add(b_exit);
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

    private boolean correctAnswer(String msg) {
        if(selectedWord.equals(msg))
            return true;
        else
            return false;
    }


    private class ClientHandler extends Thread{
        private Socket clientSocket;
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
                ChatMsg msg;
                while((msg = (ChatMsg)in.readObject()) != null) {
                    if(msg.mode == ChatMsg.MODE_LOGIN) {
                        uid = msg.userID;
                        userIDs.add(uid);
                        printDisplay("새 참가자: " + uid);
                        printDisplay("현재 참가자 수: " + users.size());

                        // 모든 클라이언트에게 새 참가자 메시지를 방송
                        ChatMsg joinMessage = new ChatMsg("", ChatMsg.MODE_ENTER, uid + "가 접속했습니다");
                        broadcasting(joinMessage);

                        continue;
                    }
                    else if (msg.mode == ChatMsg.MODE_LOGOUT) {
                        userIDs.remove(uid);
                        ChatMsg logoutMessage = new ChatMsg("", ChatMsg.MODE_ENTER, uid + "가 나갔습니다.");
                        broadcasting(logoutMessage);

                        ChatMsg userUpdateMessage = new ChatMsg("", ChatMsg.MODE_TX_USER, new ArrayList<>(userIDs));
                        broadcasting(userUpdateMessage);
                        break;
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_STRING) {
                        message = uid + ": " + msg.message;
                        printDisplay(message);
                        broadcasting(msg);
                        if(correctAnswer(msg.message)) {
                            ChatMsg addScore = new ChatMsg(uid, ChatMsg.MODE_TX_CORRECT);
                            printDisplay(uid + "에게 점수 1점");
                            printDisplay(userIDs.get(orderIndex % users.size()) +"에게 점수 1점");
                            broadcasting(addScore);
                        }
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_IMAGE) {
                        broadcasting(msg);
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_DRAW) {
                        broadcasting(msg);
                        printDisplay("Server x1: " +Integer.toString(msg.x1) + " x2: " + Integer.toString(msg.x2) + " y1: " + Integer.toString(msg.y1) + " y2: " + Integer.toString(msg.y2) +
                                " stroke: " + Float.toString(msg.stroke) +  " shape: " + msg.shapeString);
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_USER) {

                        for(String user : userIDs){
                            printDisplay("userList : " + user);
                        }
                        ChatMsg userID = new ChatMsg("", ChatMsg.MODE_TX_USER, new ArrayList<>(userIDs));
                        broadcasting(userID);
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_ORDER) {
                        orderIndex = msg.order + 1;
                        ChatMsg newMsg = new ChatMsg(uid , ChatMsg.MODE_TX_ORDER, orderIndex);
                        broadcasting(newMsg);
                    }
                    else if(msg.mode == ChatMsg.MODE_TX_START) {
                        selectedWord = msg.message;
                        broadcasting(msg);
                    }
                }
                userIDs.remove(uid);
                users.removeElement(this);
                printDisplay(uid + " 퇴장. 현재 참가자 수: " + users.size());
            }
            catch (IOException e) {
                ChatMsg logoutMessage = new ChatMsg(uid, ChatMsg.MODE_ENTER, uid + "가 나갔습니다.");
                broadcasting(logoutMessage);
                userIDs.remove(uid);
                users.removeElement(this);
                ChatMsg userUpdateMessage = new ChatMsg("", ChatMsg.MODE_TX_USER, new ArrayList<>(userIDs));
                broadcasting(userUpdateMessage);
                printDisplay(uid + " 연결 끊김. 현재 참가자 수: " + users.size());
                for(String user : userIDs){
                    printDisplay("userList : " + user);
                }
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
        new WithChatServer(port);
    }

}
