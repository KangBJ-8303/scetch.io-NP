//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class WithChatServer extends JFrame {
    private int port;
    private ServerSocket serverSocket = null;
    private Thread acceptThread = null;
    private Vector<ClientHandler> users = new Vector();
    private ArrayList<String> userIDs = new ArrayList();
    private Map<String, Integer> userScores = new HashMap();
    private JTextArea t_display;
    private JButton b_connect;
    private JButton b_disconnect;
    private JButton b_exit;
    private int orderIndex;
    private String selectedWord;

    public WithChatServer(int port) {
        super("Server");
        this.port = port;
        this.buildGUI();
        this.acceptThread = new Thread(new Runnable() {
            public void run() {
                WithChatServer.this.startServer();
            }
        });
        this.acceptThread.start();
        this.setBounds(600, 80, 900, 900);
        this.setDefaultCloseOperation(3);
        this.setVisible(true);
    }

    private void buildGUI() {
        this.add(this.createDisplayPanel(), "Center");
        this.add(this.createControlPanel(), "South");
    }

    private JPanel createDisplayPanel() {
        this.t_display = new JTextArea();
        this.t_display.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(this.t_display);
        scrollPane.setSize(this.t_display.getWidth(), this.t_display.getHeight());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, "Center");
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 0));
        this.b_exit = new JButton("종료");
        this.b_exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                WithChatServer.this.disconnect();
                System.exit(-1);
            }
        });
        panel.add(this.b_exit);
        return panel;
    }

    private void startServer() {
        Socket clientSocket = null;

        try {
            this.serverSocket = new ServerSocket(this.port);
            this.printDisplay("서버가 시작되었습니다.");

            while(this.acceptThread == Thread.currentThread()) {
                clientSocket = this.serverSocket.accept();
                String cAddr = clientSocket.getInetAddress().getHostAddress();
                this.printDisplay("클라이언트가 연결되었습니다: " + cAddr);
                ClientHandler cHandler = new ClientHandler(clientSocket);
                this.users.add(cHandler);
                cHandler.start();
            }
        } catch (SocketException var14) {
            this.printDisplay("서버 소켓 종료");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }

                if (this.serverSocket != null) {
                    this.serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("서버 닫기 오류> " + e.getMessage());
                System.exit(-1);
            }

        }

    }

    private void disconnect() {
        try {
            this.acceptThread = null;
            this.serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 소켓 닫기 오류> " + e.getMessage());
            System.exit(-1);
        }

    }

    private void printDisplay(String msg) {
        this.t_display.append(msg + "\n");
        this.t_display.setCaretPosition(this.t_display.getDocument().getLength());
    }

    private boolean correctAnswer(String msg) {
        return this.selectedWord.equals(msg);
    }

    public static void main(String[] args) {
        int port = 54321;
        new WithChatServer(port);
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private String uid;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private void receiveMessages(Socket socket) {
            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                ChatMsg msg;
                while((msg = (ChatMsg)in.readObject()) != null) {
                    if (msg.mode == 1) {
                        this.uid = msg.userID;
                        WithChatServer.this.userIDs.add(this.uid);
                        WithChatServer.this.printDisplay("새 참가자: " + this.uid);
                        WithChatServer.this.printDisplay("현재 참가자 수: " + WithChatServer.this.users.size());
                        ChatMsg joinMessage = new ChatMsg("", 4, this.uid + "가 접속했습니다");
                        this.broadcasting(joinMessage);
                    } else {
                        if (msg.mode == 2) {
                            WithChatServer.this.userScores.remove(this.uid);
                            WithChatServer.this.userIDs.remove(this.uid);
                            WithChatServer.this.users.removeElement(this);
                            ChatMsg logoutMessage = new ChatMsg("", 4, this.uid + "가 나갔습니다.");
                            this.broadcasting(logoutMessage);
                            ChatMsg userUpdateMessage = new ChatMsg("", 153, new ArrayList(WithChatServer.this.userIDs));
                            this.broadcasting(userUpdateMessage);
                            break;
                        }

                        if (msg.mode == 16) {
                            String message = this.uid + ": " + msg.message;
                            WithChatServer.this.printDisplay(message);
                            this.broadcasting(msg);
                            if (WithChatServer.this.selectedWord != null && WithChatServer.this.correctAnswer(msg.message)) {
                                int uidScore = (Integer)WithChatServer.this.userScores.get(this.uid) + 1;
                                WithChatServer.this.userScores.put(this.uid, uidScore);
                                String currentDrawerUid = (String)WithChatServer.this.userIDs.get(WithChatServer.this.orderIndex % WithChatServer.this.users.size());
                                int drawerScore = (Integer)WithChatServer.this.userScores.get(currentDrawerUid) + 1;
                                WithChatServer.this.userScores.put(currentDrawerUid, drawerScore);

                                for(String user : WithChatServer.this.userIDs) {
                                    WithChatServer.this.printDisplay(user + ": " + String.valueOf(WithChatServer.this.userScores.get(user)));
                                }

                                ChatMsg addScore = new ChatMsg(this.uid, 48, new HashMap(WithChatServer.this.userScores));
                                WithChatServer.this.printDisplay(this.uid + "에게 점수 1점");
                                WithChatServer.this.printDisplay(currentDrawerUid + "에게 점수 1점");
                                this.broadcasting(addScore);
                                ++WithChatServer.this.orderIndex;
                                ChatMsg newOrderMsg = new ChatMsg(this.uid, 256, WithChatServer.this.orderIndex);
                                this.broadcasting(newOrderMsg);
                            }
                        } else if (msg.mode == 64) {
                            this.broadcasting(msg);
                        } else if (msg.mode == 128) {
                            this.broadcasting(msg);
                            WithChatServer var10000 = WithChatServer.this;
                            String var48 = Integer.toString(msg.x1);
                            var10000.printDisplay("Server x1: " + var48 + " x2: " + Integer.toString(msg.x2) + " y1: " + Integer.toString(msg.y1) + " y2: " + Integer.toString(msg.y2) + " stroke: " + Float.toString(msg.stroke) + " shape: " + msg.shapeString);
                        } else if (msg.mode == 153) {
                            WithChatServer.this.userScores.put(msg.userID, 0);

                            for(String user : WithChatServer.this.userIDs) {
                                WithChatServer.this.printDisplay("userList : " + user);
                            }

                            ChatMsg userScore = new ChatMsg("", 32, new HashMap(WithChatServer.this.userScores));
                            this.broadcasting(userScore);
                            ChatMsg userID = new ChatMsg("", 153, new ArrayList(WithChatServer.this.userIDs));
                            this.broadcasting(userID);
                        } else if (msg.mode == 256) {
                            WithChatServer.this.orderIndex = msg.order + 1;
                            ChatMsg newMsg = new ChatMsg(this.uid, 256, WithChatServer.this.orderIndex);
                            this.broadcasting(newMsg);
                        } else if (msg.mode == 512) {
                            WithChatServer.this.selectedWord = msg.message;

                            for(ClientHandler client : WithChatServer.this.users) {
                                ChatMsg startMsg;
                                if (client.uid.equals(WithChatServer.this.userIDs.get(WithChatServer.this.orderIndex % WithChatServer.this.users.size()))) {
                                    startMsg = new ChatMsg(this.uid, 512, WithChatServer.this.selectedWord);
                                    System.out.println("Sending to drawer: " + WithChatServer.this.selectedWord);
                                } else {
                                    StringBuilder hiddenWord = new StringBuilder();

                                    for(int i = 0; i < WithChatServer.this.selectedWord.length(); ++i) {
                                        hiddenWord.append("_ ");
                                    }

                                    startMsg = new ChatMsg(this.uid, 512, hiddenWord.toString().trim());
                                    System.out.println("Sending to others: " + hiddenWord.toString().trim());
                                }

                                client.send(startMsg);
                            }
                        } else if (msg.mode == 96) {
                            for(String user : WithChatServer.this.userIDs) {
                                WithChatServer.this.userScores.put(user, 0);
                            }

                            ChatMsg reset = new ChatMsg(this.uid, 80, new HashMap(WithChatServer.this.userScores));
                            this.broadcasting(reset);
                        }
                    }
                }

                String var49 = this.uid;
                WithChatServer.this.printDisplay(var49 + " 퇴장. 현재 참가자 수: " + WithChatServer.this.users.size());
            } catch (IOException var20) {
                ChatMsg logoutMessage = new ChatMsg(this.uid, 4, this.uid + "가 나갔습니다.");
                this.broadcasting(logoutMessage);
                WithChatServer.this.userScores.remove(this.uid);
                WithChatServer.this.userIDs.remove(this.uid);
                WithChatServer.this.users.removeElement(this);
                if (WithChatServer.this.userIDs.size() < 2) {
                    for(String user : WithChatServer.this.userIDs) {
                        WithChatServer.this.userScores.put(user, 0);
                    }

                    ChatMsg reset = new ChatMsg(this.uid, 80, new HashMap(WithChatServer.this.userScores));
                    this.broadcasting(reset);
                }

                ChatMsg userUpdateMessage = new ChatMsg("", 153, new ArrayList(WithChatServer.this.userIDs));
                this.broadcasting(userUpdateMessage);
                String var10001 = this.uid;
                WithChatServer.this.printDisplay(var10001 + " 연결 끊김. 현재 참가자 수: " + WithChatServer.this.users.size());

                for(String user : WithChatServer.this.userIDs) {
                    WithChatServer.this.printDisplay("userList : " + user);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
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
                this.out.writeObject(msg);
                this.out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
            }

        }

        private void broadcasting(ChatMsg msg) {
            for(ClientHandler c : WithChatServer.this.users) {
                c.send(msg);
            }

        }

        public void run() {
            this.receiveMessages(this.clientSocket);
        }
    }
}
