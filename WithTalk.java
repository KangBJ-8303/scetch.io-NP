import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

public class WithTalk extends JFrame {
    private JTextField t_input;
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

    private RectPanel rectPanel;

    public WithTalk(String serverAddress, int serverPort) {
        super("With Talk");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        buildGUI();
        setBounds(200, 80, 800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void buildGUI() {
        JPanel panel = new JPanel(new GridLayout(3, 0));
        panel.add(createInputPanel());
        panel.add(createInfoPanel());
        panel.add(createControlPanel());

        add(createDisplayPanel(), BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        rectPanel = new RectPanel();
        add(rectPanel, BorderLayout.EAST);

        rectPanel.setOnPaintListener(image -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                send(new ChatMsg(uid, ChatMsg.MODE_TX_CANVAS, imageBytes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        t_userID = new JTextField(7);
        t_hostAddr = new JTextField(12);
        t_portNum = new JTextField(5);
        t_userID.setText("guest" + getLastSegmentOfLocalAddr());
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

    private String getLastSegmentOfLocalAddr() {
        try {
            String localAddr = InetAddress.getLocalHost().getHostAddress();
            return localAddr.split("\\.")[3];
        } catch (IOException e) {
            e.printStackTrace();
            return "unknown";
        }
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
                FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif", "png");
                chooser.setFileFilter(filter);
                int ret = chooser.showOpenDialog(WithTalk.this);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(WithTalk.this, "파일을 선택하지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                t_input.setText(chooser.getSelectedFile().getAbsolutePath());
                sendImage();
            }
        });
        panel.add(t_input, BorderLayout.CENTER);
        JPanel p_button = new JPanel(new GridLayout(1, 0));
        p_button.add(b_send);
        p_button.add(b_select);
        panel.add(p_button, BorderLayout.EAST);
        b_select.setEnabled(false);
        t_input.setEnabled(false);
        b_send.setEnabled(false);
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 3));
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
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);
    }

    private void printDisplay(ImageIcon icon) {
        t_display.setCaretPosition(t_display.getDocument().getLength());
        if (icon.getIconWidth() > 400) {
            Image img = icon.getImage();
            Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }
        t_display.insertIcon(icon);
        printDisplay("");
        t_input.setText("");
    }

    private void connectToServer() throws IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;
            private void receiveMessage() {
                try {
                    ChatMsg inMsg = (ChatMsg) in.readObject();
                    if (inMsg == null) {
                        disconnect();
                        printDisplay("서버 연결 끊김");
                        return;
                    }
                    switch (inMsg.mode) {
                        case ChatMsg.MODE_TX_STRING:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;
                        case ChatMsg.MODE_TX_IMAGE:
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            printDisplay(inMsg.image);
                            break;
                        case ChatMsg.MODE_TX_CANVAS:
                            displayCanvasOnPanel(inMsg.canvasImageBytes);
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
        if (message.isEmpty()) return;
        send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));
        t_input.setText("");
    }

    private void sendUserID() {
        uid = t_userID.getText();
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
    }

    private void sendImage() {
        String filename = t_input.getText().strip();
        if (filename.isEmpty()) return;
        File file = new File(filename);
        if (!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다: " + filename);
            return;
        }
        ImageIcon icon = new ImageIcon(filename);
        send(new ChatMsg(uid, ChatMsg.MODE_TX_IMAGE, file.getName(), icon));
        t_input.setText("");
    }

    private void displayCanvasOnPanel(byte[] canvasImageBytes) {
        if (canvasImageBytes != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(canvasImageBytes);
                BufferedImage canvasImage = ImageIO.read(bais);
                if (canvasImage != null) {
                    rectPanel.setBufferedImage(canvasImage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;
        new WithTalk(serverAddress, serverPort);
    }
}

class RectPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    private OnPaintListener onPaintListener;

    public interface OnPaintListener {
        void onPaint(BufferedImage image);
    }

    public void setOnPaintListener(OnPaintListener listener) {
        this.onPaintListener = listener;
    }

    String shapeString = "";
    Point firstPointer = new Point(0, 0);
    Point secondPointer = new Point(0, 0);
    BufferedImage bufferedImage;
    Color colors = Color.black;
    Float stroke = (float) 5;
    JComboBox<String> colorComboBox;
    JComboBox<Float> strokeComboBox;

    String[] colorNames = {"검정", "빨강", "파랑", "초록", "노랑", "핑크", "마젠타"};
    Color[] colorsArray = {Color.black, Color.red, Color.blue, Color.green, Color.yellow, Color.pink, Color.magenta};

    int width;
    int height;
    int minPointx;
    int minPointy;

    public RectPanel() {
        colorComboBox = new JComboBox<>(colorNames);
        strokeComboBox = new JComboBox<Float>();
        JPanel toolPanel = new JPanel(new GridLayout(2, 6));

        JButton eraseAllButton = new JButton("전체지우기");
        JButton rectButton = new JButton("네모");
        JButton lineButton = new JButton("선");
        JButton circleButton = new JButton("원");
        JButton penButton = new JButton("펜");
        JButton eraseButton = new JButton("지우개");

        strokeComboBox.setModel(new DefaultComboBoxModel<Float>(
                new Float[]{(float) 5, (float) 10, (float) 15, (float) 20, (float) 25}));

        add(eraseAllButton);
        add(penButton);
        add(lineButton);
        add(rectButton);
        add(circleButton);
        add(colorComboBox);
        add(strokeComboBox);
        add(eraseButton);

        toolPanel.add(eraseButton);
        toolPanel.add(eraseAllButton);
        toolPanel.add(rectButton);
        toolPanel.add(lineButton);
        toolPanel.add(circleButton);
        toolPanel.add(penButton);
        toolPanel.add(colorComboBox);
        toolPanel.add(strokeComboBox);

        eraseAllButton.addActionListener(this);
        rectButton.addActionListener(this);
        lineButton.addActionListener(this);
        circleButton.addActionListener(this);
        penButton.addActionListener(this);
        eraseButton.addActionListener(this);
        colorComboBox.addActionListener(this);
        strokeComboBox.addActionListener(this);

        setLayout(new BorderLayout());
        bufferedImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        setImageBackground(bufferedImage);
        add(toolPanel, BorderLayout.SOUTH);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void mousePressed(MouseEvent e) {
        firstPointer.setLocation(0, 0);
        secondPointer.setLocation(0, 0);
        firstPointer.setLocation(e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
        if (!shapeString.equals("펜")) {
            secondPointer.setLocation(e.getX(), e.getY());
            updatePaint();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().getClass().toString().contains("JButton")) {
            shapeString = e.getActionCommand();
        } else if (e.getSource().equals(colorComboBox)) {
            int selectedIndex = colorComboBox.getSelectedIndex();
            colors = colorsArray[selectedIndex];
        } else if (e.getSource().equals(strokeComboBox)) {
            stroke = (float) strokeComboBox.getSelectedItem();
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }

    public void updatePaint() {
        width = Math.abs(secondPointer.x - firstPointer.x);
        height = Math.abs(secondPointer.y - firstPointer.y);

        minPointx = Math.min(firstPointer.x, secondPointer.x);
        minPointy = Math.min(firstPointer.y, secondPointer.y);

        Graphics2D g = bufferedImage.createGraphics();

        switch (shapeString) {
            case ("선"):
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstPointer.x, firstPointer.y, secondPointer.x, secondPointer.y);
                break;
            case ("네모"):
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawRect(minPointx, minPointy, width, height);
                break;
            case ("원"):
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawOval(minPointx, minPointy, width, height);
                break;
            case ("펜"):
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstPointer.x, firstPointer.y, secondPointer.x, secondPointer.y);
                break;
            case ("지우개"):
                g.setColor(Color.white);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstPointer.x, firstPointer.y, secondPointer.x, secondPointer.y);
                break;
            case ("전체지우기"):
                setImageBackground(bufferedImage);
                shapeString = "";
                break;
            default:
                break;
        }

        g.dispose();
        repaint();

        if (onPaintListener != null) {
            onPaintListener.onPaint(bufferedImage);
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bufferedImage, 0, 0, null);
    }

    public void setImageBackground(BufferedImage bi) {
        this.bufferedImage = bi;
        Graphics2D g = bufferedImage.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 500, 500);
        g.dispose();
    }

    public void setBufferedImage(BufferedImage image) {
        this.bufferedImage = image;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (shapeString.equals("펜") || shapeString.equals("지우개")) {
            if (secondPointer.x != 0 && secondPointer.y != 0) {
                firstPointer.x = secondPointer.x;
                firstPointer.y = secondPointer.y;
            }
            secondPointer.setLocation(e.getX(), e.getY());
            updatePaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
}