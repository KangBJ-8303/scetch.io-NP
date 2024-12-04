import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.UnknownHostException;
import java.util.ArrayList;

public class MainDisplay extends JFrame {

    private String uid;
    private Socket socket;
    private ObjectOutputStream out;

    private Thread receiveThread = null;

    private DefaultStyledDocument document;
    private JTextPane t_display;
    private JTextField t_input;

    private JButton b_send = new JButton("보내기");

    String serverAddress;
    int serverPort;

    private Canvas canvas;

    private JPanel userInfoPanel; // 사용자 정보를 보여줄 패널
    private ArrayList<String> userList;

    public MainDisplay(String serverAddress, int serverPort, String uid) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;

        buildGUI();

        setBounds(200, 80, 1200, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);

        try {
            connectToServer();
            sendUserID();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void buildGUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel chatPanel = new JPanel(new BorderLayout());
        JPanel paintPanel = new JPanel(new BorderLayout());
        JPanel userPanel = new JPanel(new BorderLayout());

        userPanel.setPreferredSize(new Dimension(200, 600));
        userInfoPanel = createUserInfoPanel();
        userPanel.add(userInfoPanel, BorderLayout.CENTER);

        chatPanel.setPreferredSize(new Dimension(500, 600));
        chatPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        chatPanel.add(createInputPanel(), BorderLayout.SOUTH);

        canvas = new Canvas(uid, this);
        paintPanel.add(canvas, BorderLayout.CENTER);
        canvas.setOnPaintListener((firstX, firstY, secondX, secondY, color, stroke, shapeString) -> {
            send(new ChatMsg(uid, ChatMsg.MODE_TX_DRAW, firstX, firstY, secondX, secondY, color, stroke, shapeString));
        });

        mainPanel.add(chatPanel, BorderLayout.EAST);
        mainPanel.add(paintPanel, BorderLayout.CENTER);
        mainPanel.add(userPanel, BorderLayout.WEST);

        add(mainPanel);
    }

    private JPanel createUserInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    public void addUserInfo(String userName) {
        JLabel userLabel = new JLabel(userName); // 사용자 이름을 JLabel로 생성
        userInfoPanel.add(userLabel); // 패널에 추가
        userInfoPanel.revalidate(); // 레이아웃 갱신
        userInfoPanel.repaint(); // 화면 갱신
    }


    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());
        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);
        t_display.setEditable(false);
        p.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return p;
    }

    private void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa,3000);

        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        //in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));

        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;

            private void receiveMessage() {
                try {
                    ChatMsg inMsg = (ChatMsg)in.readObject();
                    if(inMsg == null) {
                        disconnect();
                        return;
                    }
                    switch (inMsg.mode) {
                        case ChatMsg.MODE_TX_STRING :
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;
                        case ChatMsg.MODE_ENTER :
                            printDisplay(inMsg.message);
                            break;
                        case ChatMsg.MODE_TX_IMAGE:
                            printDisplay(inMsg.image);
                            break;
                        case ChatMsg.MODE_TX_DRAW:
                            canvas.drawing(inMsg.x1, inMsg.y1,inMsg.x2,inMsg.y2,inMsg.color, inMsg.stroke, inMsg.shapeString);
                            printDisplay("Client x1: " +Integer.toString(inMsg.x1) + " x2: " + Integer.toString(inMsg.x2) + " y1: " + Integer.toString(inMsg.y1) + " y2: " + Integer.toString(inMsg.y2) +
                                    " stroke: " + Float.toString(inMsg.stroke) +  " shape: " + inMsg.shapeString);
                            break;
                    }

                } catch (IOException e) {
                    printDisplay("연결을 종료했습니다.");
                } catch (ClassNotFoundException e) {
                    printDisplay("잘못된 객체가 전달되었습니다.");
                }

            }

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }

                while (receiveThread == Thread.currentThread()) {
                    receiveMessage();
                }
            }
        });
        receiveThread.start();
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        t_input = new JTextField(30);
        t_input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        b_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        panel.add(t_input, BorderLayout.CENTER);
        JPanel p_button = new JPanel(new GridLayout(1,0));
        p_button.add(b_send);
        panel.add(p_button, BorderLayout.EAST);
        t_input.setEnabled(true);
        b_send.setEnabled(true);

        return panel;
    }

    private void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();

        try {
            document.insertString(len, msg + "\n" , null);
        } catch (BadLocationException e){
            e.printStackTrace();
        }

        t_display.setCaretPosition(len);
    }

    private void printDisplay(BufferedImage msg) {
        int len = t_display.getDocument().getLength();

        try {
            document.insertString(len, msg + "\n" , null);
        } catch (BadLocationException e){
            e.printStackTrace();
        }

        t_display.setCaretPosition(len);
    }

    private void disconnect() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));

        try {
            receiveThread = null;
            socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류> " + e.getMessage());
            System.exit(-1);
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

    public void sendDrawing(String uid, int x1, int y1, int x2, int y2, Color color, float stroke, String shapeString) {
        send(new ChatMsg(uid, ChatMsg.MODE_TX_DRAW, x1, y1, x2, y2, color, stroke, shapeString));
    }

    private void sendMessage() {
        String message = t_input.getText();
        if(message.isEmpty()) return;

        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));

        t_input.setText("");
    }

    private void sendUserID() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
    }
}
