//2071141 홍민혁
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.Socket;
import java.rmi.UnknownHostException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class WithTalk extends JFrame {
    private JTextField t_input;
    //private JTextArea t_display;
    private JTextPane t_display;
    private DefaultStyledDocument document;
    private JTextField t_userID, t_hostAddr, t_portNum;

    private JButton b_connect = new JButton("접속하기");
    private JButton b_disconnect = new JButton("접속 끊기");
    private JButton b_send = new JButton("보내기");
    private JButton b_exit = new JButton("종료하기");
    private JButton b_select;

    private String serverAddress;
    private int serverPort;
    private String uid;

    private Socket socket;
    private ObjectOutputStream out;

    private Thread receiveThread = null;

    public WithTalk(String serverAddress, int serverPort) {
        super("2071141 With Talk");

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        buildGUI();

        setBounds(200, 80, 500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        JPanel panel = new JPanel(new GridLayout(3,0));
        panel.add(createInputPanel());
        panel.add(createInfoPanel());
        panel.add(createControlPanel());
        add(createDisplayPanel(), BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        t_userID = new JTextField(7);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);

        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
        t_hostAddr.setText(this.serverAddress);
        t_portNum.setText(String.valueOf(this.serverPort));

        t_portNum.setHorizontalAlignment(JTextField.CENTER);

        p.add(new JLabel("아이디: "));
        p.add(t_userID);

        p.add(new JLabel("서버주소: "));
        p.add(t_hostAddr);

        p.add(new JLabel("포트번호: "));
        p.add(t_portNum);

        return p;
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());

        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);

        t_display.setEditable(false);

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return p;
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

        b_select = new JButton("선택하기");
        b_select.addActionListener(new ActionListener() {

            JFileChooser chooser = new JFileChooser();

            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images",
                        "jpg", "gif", "png");

                chooser.setFileFilter(filter);

                int ret = chooser.showOpenDialog(WithTalk.this);
                if(ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(WithTalk.this, "파일을 선택하지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });

        panel.add(t_input, BorderLayout.CENTER);
        JPanel p_button = new JPanel(new GridLayout(1,0));
        p_button.add(b_send);
        p_button.add(b_select);
        panel.add(p_button, BorderLayout.EAST);
        b_select.setEnabled(false);
        t_input.setEnabled(false);
        b_send.setEnabled(false);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0,3));
        panel.add(b_connect);
        panel.add(b_disconnect);
        panel.add(b_exit);
        b_disconnect.setEnabled(false);
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WithTalk.this.serverAddress = t_hostAddr.getText();
                WithTalk.this.serverPort = Integer.parseInt(t_portNum.getText());
                try {
                    connectToServer();
                    sendUserID();
                } catch (UnknownHostException e1){
                    printDisplay("서버 주소와 포트번호를 확인하세요: " + e1.getMessage());
                    return;
                } catch (IOException e1) {
                    printDisplay("서버와의 연결 오류 : " + e1.getMessage());
                    return;
                }
                b_select.setEnabled(true);
                b_connect.setEnabled(false);
                b_disconnect.setEnabled(true);
                t_input.setEnabled(true);
                b_exit.setEnabled(false);
                b_send.setEnabled(true);

            }
        });
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
                b_select.setEnabled(false);
                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                b_send.setEnabled(false);
                t_input.setText("");
                t_input.setEnabled(false);
                b_exit.setEnabled(true);
            }
        });
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(-1);
            }
        });
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

    private void printDisplay(ImageIcon icon) {
        t_display.setCaretPosition(t_display.getDocument().getLength());

        if(icon.getIconWidth() > 400) {
            Image img = icon.getImage();
            Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }

        t_display.insertIcon(icon);

        printDisplay("");
        t_input.setText("");
    }

    private String getLocalAddr() {
        InetAddress local = null;
        String addr = "";
        try {
            local = InetAddress.getLocalHost();
            addr = local.getHostAddress();
            System.out.println(addr);
        } catch(java.net.UnknownHostException e) {
            e.printStackTrace();
        }

        return addr;
    }


    private void connectToServer() throws UnknownHostException, IOException{
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
                        printDisplay("서버 연결 끊김");
                        return;
                    }
                    //printDisplay(inMsg);
                    switch (inMsg.mode) {
                        case ChatMsg.MODE_TX_STRING :
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;

                        case ChatMsg.MODE_TX_IMAGE:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            printDisplay(inMsg.image);
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

    private void sendMessage() {
        String message = t_input.getText();
        if(message.isEmpty()) return;

        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));

        t_input.setText("");
    }

    private void sendUserID() {
        uid = t_userID.getText();
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
    }

    private void sendImage() {
        String filename = t_input.getText().strip();
        if(filename.isEmpty()) return;

        File file = new File(filename);
        if(!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다: " + filename);
            return;
        }

        ImageIcon icon = new ImageIcon(filename);
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon));

        t_input.setText("");
    }



    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        new WithTalk(serverAddress, serverPort);
    }
}
