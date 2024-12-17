//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class MainDisplay extends JFrame {
    private static final Color[] USER_COLORS;
    private Map<String, Integer> userColorMap = new HashMap();
    private int colorIndex = 0;
    private String currentDrawer = null;
    private String uid;
    private Socket socket;
    private ObjectOutputStream out;
    private Thread receiveThread = null;
    private JLabel timerLabel = new JLabel("timer");
    private Timer timer;
    private int remainingSeconds = 60;
    private DefaultStyledDocument document;
    private JTextPane t_display;
    private JTextField t_input;
    private JButton b_send = new JButton("보내기");
    private JButton b_start = new JButton("시작하기");
    private JButton b_exit = new JButton("나가기");
    JPanel mainPanel = new JPanel(new BorderLayout());
    JPanel chatPanel = new JPanel(new BorderLayout());
    JPanel paintPanel = new JPanel(new BorderLayout());
    JPanel userPanel = new JPanel(new BorderLayout());
    JPanel vocaPanel = new JPanel(new GridLayout(1, 3));
    JPanel selectPanel = new JPanel(new BorderLayout());
    String serverAddress;
    int serverPort;
    String selectedWord = "";
    private Canvas canvas;
    private JPanel userInfoPanel;
    private ArrayList<String> userList = new ArrayList();
    private Map<String, Integer> userScores = new HashMap();
    private int orderIndex = -1;

    public MainDisplay(String serverAddress, int serverPort, String uid) {
        super(uid);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;
        this.userList.add(uid);
        this.buildGUI();
        this.setBounds(200, 80, 1200, 600);
        this.setDefaultCloseOperation(3);
        this.setResizable(false);
        this.setVisible(true);

        try {
            this.connectToServer();
            this.sendUserID();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildGUI() {
        this.userPanel.setPreferredSize(new Dimension(200, 600));
        this.userPanel.setBackground(new Color(240, 248, 255));
        this.userPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        this.chatPanel.setBackground(new Color(255, 250, 240));
        this.chatPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        this.timerLabel.setFont(new Font("맑은 고딕", 1, 18));
        this.timerLabel.setHorizontalAlignment(0);
        this.timerLabel.setForeground(Color.DARK_GRAY);
        this.userInfoPanel = this.createUserInfoPanel();
        this.userPanel.add(this.userInfoPanel, "Center");
        JPanel timerPanel = new JPanel();
        timerPanel.add(this.timerLabel);
        this.userPanel.add(timerPanel, "North");
        this.b_exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (MainDisplay.this.userList.size() == 2) {
                    MainDisplay.this.send(new ChatMsg(MainDisplay.this.uid, 96));
                }

                MainDisplay.this.disconnect();
                MainDisplay.this.setVisible(false);
                new StartDisplay(MainDisplay.this.serverAddress, MainDisplay.this.serverPort);
                MainDisplay.this.dispose();
            }
        });
        this.userPanel.add(this.b_exit, "South");
        this.chatPanel.setPreferredSize(new Dimension(500, 600));
        this.chatPanel.add(this.createDisplayPanel(), "Center");
        this.chatPanel.add(this.createInputPanel(), "South");
        this.canvas = new Canvas(this.uid, this);
        this.paintPanel.add(this.canvas, "Center");
        this.b_start.setEnabled(false);
        this.b_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainDisplay.this.send(new ChatMsg(MainDisplay.this.uid, 256, MainDisplay.this.orderIndex));
                MainDisplay.this.b_start.setVisible(false);
            }
        });
        this.paintPanel.add(this.b_start, "South");
        this.mainPanel.add(this.chatPanel, "East");
        this.mainPanel.add(this.paintPanel, "Center");
        this.mainPanel.add(this.userPanel, "West");
        this.mainPanel.add(this.vocaPanel, "North");
        this.vocaPanel.setVisible(false);
        this.add(this.mainPanel);
    }

    private JPanel createUserInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));
        return panel;
    }

    public void startTimerFromServer() {
        if (this.timer != null) {
            this.timer.cancel();
        }

        if (this.uid.equals(this.currentDrawer)) {
            this.t_input.setEnabled(false);
        }

        this.remainingSeconds = 80;
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (MainDisplay.this.remainingSeconds > 0) {
                        --MainDisplay.this.remainingSeconds;
                        MainDisplay.this.timerLabel.setText(String.valueOf(MainDisplay.this.remainingSeconds));
                    } else {
                        MainDisplay.this.vocaPanel.setVisible(false);
                        MainDisplay.this.canvas.setClean();
                        MainDisplay.this.nextDrawer();
                    }

                });
            }
        }, 1000L, 1000L);
    }

    public void selectVoca() {
        if (this.timer != null) {
            this.timer.cancel();
        }

        this.remainingSeconds = 15;
        this.timer = new Timer();
        if (this.uid.equals(this.currentDrawer)) {
            this.t_input.setEnabled(false);
            this.loadRandomWords();
        }

        this.timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (MainDisplay.this.remainingSeconds > 0) {
                        --MainDisplay.this.remainingSeconds;
                        MainDisplay.this.timerLabel.setText(String.valueOf(MainDisplay.this.remainingSeconds));
                    } else {
                        if (MainDisplay.this.uid.equals(MainDisplay.this.currentDrawer)) {
                            Component[] components = MainDisplay.this.vocaPanel.getComponents();
                            if (components.length > 0) {
                                JButton randomButton = (JButton)components[(new Random()).nextInt(components.length)];
                                MainDisplay.this.selectedWord = randomButton.getText();
                                MainDisplay.this.resetVocaPanelWithSelectedWord(MainDisplay.this.selectedWord);
                                MainDisplay.this.paintPanel.setVisible(true);
                                MainDisplay.this.send(new ChatMsg(MainDisplay.this.uid, 512, MainDisplay.this.selectedWord));
                            }
                        }

                        MainDisplay.this.timer.cancel();
                    }

                });
            }
        }, 1000L, 1000L);
    }

    private void loadRandomWords() {
        try {
            List<String> words = Files.readAllLines(Paths.get("words.txt"));
            Random random = new Random();
            this.vocaPanel.removeAll();
            this.vocaPanel.setLayout(new GridLayout(1, 3));
            Set<String> selectedWords = new HashSet();

            for(int i = 0; i < 3; ++i) {
                String randomWord = (String)words.get(random.nextInt(words.size()));
                if (!selectedWords.contains(randomWord)) {
                    selectedWords.add(randomWord);
                    final JButton wordButton = new JButton(randomWord);
                    wordButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            MainDisplay.this.selectedWord = wordButton.getText();
                            MainDisplay.this.resetVocaPanelWithSelectedWord(MainDisplay.this.selectedWord);
                            MainDisplay.this.timer.cancel();
                            MainDisplay.this.paintPanel.setVisible(true);
                            MainDisplay.this.send(new ChatMsg(MainDisplay.this.uid, 512, MainDisplay.this.selectedWord));
                        }
                    });
                    this.vocaPanel.add(wordButton);
                }
            }

            this.vocaPanel.revalidate();
            this.vocaPanel.repaint();
            this.paintPanel.setVisible(false);
            this.vocaPanel.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void resetVocaPanelWithSelectedWord(String word) {
        this.vocaPanel.removeAll();
        JLabel wordLabel = new JLabel(word, 0);
        wordLabel.setFont(new Font("Serif", 1, 20));
        wordLabel.setHorizontalAlignment(0);
        this.vocaPanel.setLayout(new BorderLayout());
        this.vocaPanel.add(wordLabel, "Center");
        this.vocaPanel.setVisible(true);
        this.vocaPanel.revalidate();
        this.vocaPanel.repaint();
    }

    private void startDrawing() {
        this.selectVoca();
    }

    public void nextDrawer() {
        if (this.currentDrawer != null) {
            this.userColorMap.put(this.currentDrawer, 0);
        }

        this.updateUserInfoPanel();
        if (this.uid.equals(this.currentDrawer)) {
            this.t_input.setEnabled(true);
            this.send(new ChatMsg(this.uid, 256, this.orderIndex % this.userList.size()));
        }

    }

    public void setCurrentDrawer(String userName) {
        this.userColorMap.put(this.currentDrawer, 0);
        this.userColorMap.put(userName, 1);
        this.updateUserInfoPanel();
        this.currentDrawer = userName;
        this.canvas.updateToolVisibility();
    }

    public String getCurrentDrawer() {
        return this.currentDrawer;
    }

    public String getUid() {
        return this.uid;
    }

    public void addUserInfo(String userName) {
        if (this.userList.size() < 2) {
            this.canvas.setClean();
            this.b_start.setVisible(true);
            this.b_start.setEnabled(true);
            this.currentDrawer = null;
        }

        this.canvas.setShapeString();
        this.canvas.setClean();
        if (!this.userColorMap.containsKey(userName)) {
            this.userColorMap.put(userName, 0);
        }

        if (!this.userScores.containsKey(userName)) {
            this.userScores.put(userName, 0);
        }

        int userColorIndex = (Integer)this.userColorMap.get(userName) % USER_COLORS.length;
        Color userColor = USER_COLORS[userColorIndex];
        RoundedLabel roundedLabel;
        if (this.uid.equals(userName)) {
            roundedLabel = new RoundedLabel(userName + "(you)", Integer.toString((Integer)this.userScores.get(userName)), userColor, Color.BLACK);
        } else {
            roundedLabel = new RoundedLabel(userName, Integer.toString((Integer)this.userScores.get(userName)), userColor, Color.BLACK);
        }

        this.userInfoPanel.add(roundedLabel);
        this.userInfoPanel.revalidate();
        this.userInfoPanel.repaint();
        if (this.userList.size() >= 2) {
            if (this.uid.equals(this.userList.get(0))) {
                this.b_start.setEnabled(true);
            } else {
                this.b_start.setVisible(false);
            }
        } else {
            this.b_start.setEnabled(false);
        }

    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());
        this.document = new DefaultStyledDocument();
        this.t_display = new JTextPane(this.document);
        this.t_display.setEditable(false);
        p.add(new JScrollPane(this.t_display), "Center");
        return p;
    }

    private void connectToServer() throws UnknownHostException, IOException {
        this.socket = new Socket();
        SocketAddress sa = new InetSocketAddress(this.serverAddress, this.serverPort);
        this.socket.connect(sa, 3000);
        this.out = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
        this.receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;
            private Map<String, Integer> receivedScores;

            private void receiveMessage() {
                try {
                    ChatMsg inMsg = (ChatMsg)this.in.readObject();
                    if (inMsg == null) {
                        MainDisplay.this.disconnect();
                        return;
                    }

                    switch (inMsg.mode) {
                        case 4:
                            MainDisplay.this.printDisplay(inMsg.message);
                            break;
                        case 16:
                            MainDisplay.this.printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;
                        case 32:
                            this.receivedScores = inMsg.userScores;
                            MainDisplay.this.userScores.clear();
                            MainDisplay.this.userScores.putAll(this.receivedScores);
                            MainDisplay.this.updateUserInfoPanel();
                            break;
                        case 48:
                            if (MainDisplay.this.uid.equals(inMsg.userID)) {
                                MainDisplay.this.printDisplay(MainDisplay.this.uid + " 가 정답을 맞추었습니다.");
                            }

                            this.receivedScores = inMsg.userScores;
                            MainDisplay.this.userScores.clear();
                            MainDisplay.this.userScores.putAll(this.receivedScores);
                            if (MainDisplay.this.uid.equals(MainDisplay.this.currentDrawer)) {
                                MainDisplay.this.t_input.setEnabled(true);
                            }

                            for(String user : MainDisplay.this.userList) {
                                if ((Integer)MainDisplay.this.userScores.get(user) == 4) {
                                    MainDisplay.this.printDisplay(user + "가 승리하였습니다.");
                                    MainDisplay.this.send(new ChatMsg(MainDisplay.this.uid, 96));
                                    break;
                                }
                            }

                            MainDisplay.this.remainingSeconds = 1;
                            MainDisplay.this.canvas.setClean();
                            MainDisplay.this.updateUserInfoPanel();
                            break;
                        case 64:
                            MainDisplay.this.printDisplay(inMsg.image);
                            break;
                        case 80:
                            MainDisplay.this.paintPanel.setVisible(true);
                            MainDisplay.this.vocaPanel.setVisible(false);
                            MainDisplay.this.b_start.setVisible(true);
                            MainDisplay.this.orderIndex = -1;
                            MainDisplay.this.userScores.clear();
                            MainDisplay.this.userScores.putAll(inMsg.userScores);
                            MainDisplay.this.canvas.setClean();
                            MainDisplay.this.timer.cancel();
                            MainDisplay.this.timerLabel.setText("timer");

                            for(String user : MainDisplay.this.userList) {
                                MainDisplay.this.userColorMap.put(user, 0);
                                MainDisplay.this.updateUserInfoPanel();
                            }

                            if (MainDisplay.this.uid.equals(MainDisplay.this.userList.get(0))) {
                                MainDisplay.this.b_start.setVisible(true);
                                MainDisplay.this.b_start.setEnabled(true);
                            }
                            break;
                        case 128:
                            MainDisplay.this.canvas.drawing(inMsg.x1, inMsg.y1, inMsg.x2, inMsg.y2, inMsg.color, inMsg.stroke, inMsg.shapeString);
                            break;
                        case 153:
                            MainDisplay.this.userList.clear();
                            MainDisplay.this.userList.addAll(inMsg.users);
                            MainDisplay.this.userInfoPanel.removeAll();

                            for(String user : MainDisplay.this.userList) {
                                MainDisplay.this.addUserInfo(user);
                            }

                            MainDisplay.this.userInfoPanel.revalidate();
                            MainDisplay.this.userInfoPanel.repaint();
                            break;
                        case 256:
                            MainDisplay.this.vocaPanel.setVisible(false);
                            MainDisplay.this.orderIndex = inMsg.order % MainDisplay.this.userList.size();
                            MainDisplay.this.printDisplay("지금 턴 index: " + MainDisplay.this.orderIndex);
                            MainDisplay.this.setCurrentDrawer((String)MainDisplay.this.userList.get(MainDisplay.this.orderIndex));
                            if (MainDisplay.this.uid.equals(MainDisplay.this.userList.get(MainDisplay.this.orderIndex))) {
                                MainDisplay.this.selectVoca();
                            }
                            break;
                        case 512:
                            String displayWord = inMsg.message;
                            System.out.println("Received word: " + displayWord);
                            MainDisplay.this.resetVocaPanelWithSelectedWord(displayWord);
                    }
                } catch (IOException var5) {
                    MainDisplay.this.printDisplay("연결을 종료했습니다.");
                } catch (ClassNotFoundException var6) {
                    MainDisplay.this.printDisplay("잘못된 객체가 전달되었습니다.");
                }

            }

            public void run() {
                try {
                    this.in = new ObjectInputStream(new BufferedInputStream(MainDisplay.this.socket.getInputStream()));
                } catch (IOException var2) {
                    MainDisplay.this.printDisplay("입력 스트림이 열리지 않음");
                }

                while(MainDisplay.this.receiveThread == Thread.currentThread()) {
                    this.receiveMessage();
                }

            }
        });
        this.receiveThread.start();
    }

    private void updateUserInfoPanel() {
        this.userInfoPanel.removeAll();

        for(String user : this.userList) {
            this.addUserInfo(user);
        }

        this.userInfoPanel.revalidate();
        this.userInfoPanel.repaint();
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        this.t_input = new JTextField(30);
        this.t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainDisplay.this.sendMessage();
            }
        });
        this.b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainDisplay.this.sendMessage();
            }
        });
        panel.add(this.t_input, "Center");
        JPanel p_button = new JPanel(new GridLayout(1, 0));
        p_button.add(this.b_send);
        panel.add(p_button, "East");
        this.t_input.setEnabled(true);
        this.b_send.setEnabled(true);
        return panel;
    }

    public void printDisplay(String msg) {
        int len = this.t_display.getDocument().getLength();

        try {
            this.document.insertString(len, msg + "\n", (AttributeSet)null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        this.t_display.setCaretPosition(len);
    }

    private void printDisplay(BufferedImage msg) {
        int len = this.t_display.getDocument().getLength();

        try {
            this.document.insertString(len, String.valueOf(msg) + "\n", (AttributeSet)null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        this.t_display.setCaretPosition(len);
    }

    private void disconnect() {
        this.send(new ChatMsg(this.uid, 2));

        try {
            this.receiveThread = null;
            this.socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류> " + e.getMessage());
            System.exit(-1);
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

    public void sendDrawing(String uid, int x1, int y1, int x2, int y2, Color color, float stroke, String shapeString) {
        this.send(new ChatMsg(uid, 128, x1, y1, x2, y2, color, stroke, shapeString));
    }

    private void sendMessage() {
        String message = this.t_input.getText();
        if (!message.isEmpty()) {
            this.send(new ChatMsg(this.uid, 16, message));
            this.t_input.setText("");
        }
    }

    private void sendUserID() {
        this.send(new ChatMsg(this.uid, 1));
        this.send(new ChatMsg(this.uid, 153, this.userList));
    }

    static {
        USER_COLORS = new Color[]{Color.WHITE, Color.CYAN};
    }

    public class RoundedLabel extends JPanel {
        private String userName;
        private String score;
        private Color backgroundColor;
        private Color textColor;

        public RoundedLabel(String userName, String score, Color backgroundColor, Color textColor) {
            this.userName = userName;
            this.score = score;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.setOpaque(false);
            this.setPreferredSize(new Dimension(190, 100));
            this.setMaximumSize(new Dimension(190, 100));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.backgroundColor);
            g2.fillRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 20, 20);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(this.textColor);
            g2.setFont(new Font("Serif", 1, 20));
            int xName = (this.getWidth() - fm.stringWidth(this.userName)) / 2;
            int yName = this.getHeight() / 2 - fm.getDescent();
            g2.drawString(this.userName, xName - 10, yName - 10);
            int xScore = (this.getWidth() - fm.stringWidth(this.score)) / 2;
            int yScore = this.getHeight() / 2 + fm.getAscent();
            g2.drawString(this.score, xScore - 10, yScore + 10);
        }
    }
}
