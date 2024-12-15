import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class MainDisplay extends JFrame {

    private static final Color[] USER_COLORS = {Color.WHITE, Color.CYAN}; // 색상 배열
    private Map<String, Integer> userColorMap = new HashMap<>(); // 사용자 ID와 색상 인덱스 매핑
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
    JPanel vocaPanel = new JPanel(new GridLayout(1,3));
    JPanel selectPanel = new JPanel(new BorderLayout());

    String serverAddress;
    int serverPort;

    String selectedWord = "";

    private Canvas canvas;

    private JPanel userInfoPanel; // 사용자 정보를 보여줄 패널
    private ArrayList<String> userList = new ArrayList<>();
    private Map<String, Integer> userScores = new HashMap<>(); // 사용자 점수를 저장하는 맵
    private int orderIndex = -1;

    public MainDisplay(String serverAddress, int serverPort, String uid) {
        super(uid);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.uid = uid;
        userList.add(uid);

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
        userPanel.setPreferredSize(new Dimension(200, 600));
        userInfoPanel = createUserInfoPanel();
        userPanel.add(userInfoPanel, BorderLayout.CENTER);

        JPanel timerPanel = new JPanel();
        timerPanel.add(timerLabel);
        userPanel.add(timerPanel, BorderLayout.NORTH);
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(userList.size() == 2) {
                    send(new ChatMsg(uid, ChatMsg.MODE_TX_RESET));
                }
                disconnect();
                setVisible(false);
                new StartDisplay(serverAddress, serverPort);
                dispose();
            }
        });
        userPanel.add(b_exit, BorderLayout.SOUTH);

        chatPanel.setPreferredSize(new Dimension(500, 600));
        chatPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        chatPanel.add(createInputPanel(), BorderLayout.SOUTH);

        canvas = new Canvas(uid, this);
        paintPanel.add(canvas, BorderLayout.CENTER);

        b_start.setEnabled(false);
        b_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send(new ChatMsg(uid, ChatMsg.MODE_TX_ORDER, orderIndex));
                b_start.setVisible(false);
            }
        });
        paintPanel.add(b_start, BorderLayout.SOUTH);

        mainPanel.add(chatPanel, BorderLayout.EAST);
        mainPanel.add(paintPanel, BorderLayout.CENTER);
        mainPanel.add(userPanel, BorderLayout.WEST);
        mainPanel.add(vocaPanel, BorderLayout.NORTH);
        vocaPanel.setVisible(false);

        add(mainPanel);
    }



    private JPanel createUserInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    public void startTimerFromServer() {
        if (timer != null) {
            timer.cancel();
        }
        if (uid.equals(currentDrawer)) {
            t_input.setEnabled(false);
        }
        remainingSeconds = 80; // 남은 시간 초기화
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (remainingSeconds > 0) {
                        remainingSeconds--;
                        timerLabel.setText(String.valueOf(remainingSeconds)); // 타이머 라벨 업데이트
                    } else {
                        vocaPanel.setVisible(false);
                        canvas.setClean();
                        nextDrawer(); // 60초 후 다음 사용자로 변경
                    }
                });
            }
        }, 1000, 1000); // 1초 (1000 밀리초) 간격으로 타이머 갱신
    }

    public void selectVoca() {
        if (timer != null) {
            timer.cancel();
        }
        remainingSeconds = 15; // 남은 시간 초기화
        timer = new Timer();
        if (uid.equals(currentDrawer)) {
            t_input.setEnabled(false);
            loadRandomWords();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (remainingSeconds > 0) {
                        remainingSeconds--;
                        timerLabel.setText(String.valueOf(remainingSeconds)); // 타이머 라벨 업데이트
                    } else {
                        if (uid.equals(currentDrawer)) {
                            Component[] components = vocaPanel.getComponents();
                            if (components.length > 0) {
                                JButton randomButton = (JButton) components[new Random().nextInt(components.length)];
                                selectedWord = randomButton.getText();
                                resetVocaPanelWithSelectedWord(selectedWord); // 선택된 단어로 vocaPanel 초기화
                                paintPanel.setVisible(true);
                                send(new ChatMsg(uid, ChatMsg.MODE_TX_START, selectedWord)); // 서버에 신호 보내기
                            }
                        }
                        timer.cancel(); // 타이머 중지
                    }
                });
            }
        }, 1000, 1000); // 1초 (1000 밀리초) 간격으로 타이머 갱신
    }



    private void loadRandomWords() {
        try {
            List<String> words = Files.readAllLines(Paths.get("words.txt"));
            Random random = new Random();
            vocaPanel.removeAll();
            vocaPanel.setLayout(new GridLayout(1,3));
            Set<String> selectedWords = new HashSet<>(); // 선택된 단어를 추적하기 위한 Set
            for (int i = 0; i < 3; i++) {
                String randomWord;
                do {
                    randomWord = words.get(random.nextInt(words.size()));
                } while (selectedWords.contains(randomWord));
                selectedWords.add(randomWord);

                JButton wordButton = new JButton(randomWord);
                wordButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectedWord = wordButton.getText();
                        resetVocaPanelWithSelectedWord(selectedWord); // 선택된 단어로 vocaPanel 초기화
                        timer.cancel(); // 타이머 중지
                        paintPanel.setVisible(true);
                        send(new ChatMsg(uid, ChatMsg.MODE_TX_START, selectedWord)); // 서버에 신호 보내기
                    }
                });
                vocaPanel.add(wordButton);
            }
            vocaPanel.revalidate();
            vocaPanel.repaint();
            paintPanel.setVisible(false);
            vocaPanel.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void resetVocaPanelWithSelectedWord(String word) {
        vocaPanel.removeAll(); // 모든 버튼 제거
        JLabel selectedWordLabel = new JLabel("내가 선택한 단어: " + word, SwingConstants.CENTER); // 선택된 단어 표시
        selectedWordLabel.setHorizontalAlignment(SwingConstants.CENTER); // 수평 가운데 정렬
        selectedWordLabel.setVerticalAlignment(SwingConstants.CENTER); // 수직 가운데 정렬
        vocaPanel.setLayout(new BorderLayout()); // BorderLayout으로 변경
        vocaPanel.add(selectedWordLabel, BorderLayout.CENTER); // 선택된 단어 라벨 추가
        vocaPanel.revalidate();
        vocaPanel.repaint();
    }


    private void startDrawing(){
        selectVoca();
    }

    public void nextDrawer() {
        userColorMap.put(currentDrawer, 0);
        updateUserInfoPanel();
        if (uid.equals(currentDrawer)) {
            t_input.setEnabled(true);
            send(new ChatMsg(uid, ChatMsg.MODE_TX_ORDER, orderIndex % userList.size()));
        }
    }


    public void setCurrentDrawer(String userName){
        userColorMap.put(userName, 1);
        updateUserInfoPanel();
        currentDrawer = userName;
        canvas.updateToolVisibility();
    }

    public String getCurrentDrawer() {
        return currentDrawer;
    }

    public String getUid() {
        return this.uid;
    }

    public void addUserInfo(String userName) {
        RoundedLabel roundedLabel;
        if(userList.size() < 2) {
            canvas.setClean();
            b_start.setVisible(true);
            b_start.setEnabled(true);
            currentDrawer = null;
        }
        canvas.setShapeString();
        canvas.setClean();

        if (!userColorMap.containsKey(userName)) {
            userColorMap.put(userName, 0);
        }

        if (!userScores.containsKey(userName)) {
            userScores.put(userName, 0); // 새로운 사용자 점수를 0으로 초기화 }
        }


        int userColorIndex = userColorMap.get(userName) % USER_COLORS.length;
        Color userColor = USER_COLORS[userColorIndex];
        if(uid.equals(userName)) {
            roundedLabel = new RoundedLabel(userName +"(you)", Integer.toString(userScores.get(userName)), userColor, Color.BLACK); // 사용자 정의 패널 생성
        }else {
            roundedLabel = new RoundedLabel(userName, Integer.toString(userScores.get(userName)), userColor, Color.BLACK); // 사용자 정의 패널 생성
        }
        userInfoPanel.add(roundedLabel); // 패널에 추가
        userInfoPanel.revalidate(); // 레이아웃 갱신
        userInfoPanel.repaint(); // 화면 갱신

        if(userList.size() >= 2) {
            if(uid.equals(userList.get(0))) {
                printDisplay(userList.get(0));
                b_start.setEnabled(true);
            }
            else {
                b_start.setVisible(false);
            }
        }
        else {
            b_start.setEnabled(false);
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

    private void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa,3000);

        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;
            private Map<String, Integer> receivedScores;
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
                            break;
                        case ChatMsg.MODE_TX_USER:
                            userList.clear();
                            userList.addAll(inMsg.users);
                            userInfoPanel.removeAll();
                            for(String user : userList){
                                addUserInfo(user);
                            }
                            userInfoPanel.revalidate();
                            userInfoPanel.repaint();
                            break;
                        case ChatMsg.MODE_TX_USERSCORE:
                            receivedScores = (Map<String, Integer>) inMsg.userScores;
                            userScores.clear();
                            userScores.putAll(receivedScores);
                            updateUserInfoPanel();
                            break;
                        case ChatMsg.MODE_TX_ORDER:
                            vocaPanel.setVisible(false);
                            orderIndex = inMsg.order % userList.size();
                            printDisplay("지금턴 index: " + orderIndex);
                            setCurrentDrawer(userList.get(orderIndex));
                            startDrawing();
                            break;
                        case ChatMsg.MODE_TX_START:
                            startTimerFromServer();
                            break;
                        case ChatMsg.MODE_TX_CORRECT:
                            if (uid.equals(inMsg.userID)) {
                                printDisplay(uid + " 가 정답을 맞추었습니다.");
                            }
                            receivedScores = (Map<String, Integer>) inMsg.userScores;
                            userScores.clear();
                            userScores.putAll(receivedScores);
                            for(String user: userList) {
                                if(userScores.get(user) == 4) {
                                    printDisplay(user + "가 승리하였습니다.");
                                    send(new ChatMsg(uid, ChatMsg.MODE_TX_RESET));
                                    break;
                                }
                            }
                            remainingSeconds = 1;
                            canvas.setClean();
                            updateUserInfoPanel();
                            break;
                        case ChatMsg.MODE_TX_END:
                            paintPanel.setVisible(true);
                            vocaPanel.setVisible(false);
                            b_start.setVisible(true);
                            orderIndex = -1;
                            userScores.clear();
                            userScores.putAll(inMsg.userScores);
                            canvas.setClean();
                            timer.cancel();
                            timerLabel.setText("timer");
                            for(String user : userList) {
                                userColorMap.put(user, 0);
                                updateUserInfoPanel();
                            }
                            if(uid.equals(userList.get(0))) {
                                b_start.setVisible(true);
                                b_start.setEnabled(true);
                            }
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

    private void updateUserInfoPanel() {
        userInfoPanel.removeAll();
        for (String user : userList) {
            addUserInfo(user); // 점수를 포함한 userInfo를 추가
        }
        userInfoPanel.revalidate();
        userInfoPanel.repaint();
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

    public void printDisplay(String msg) {
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
        send(new ChatMsg(uid, ChatMsg.MODE_TX_USER, userList));
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
            setOpaque(false); // 배경 투명 설정
            setPreferredSize(new Dimension(190, 100)); // 패널 크기 설정
            setMaximumSize(new Dimension(190, 100)); // 최대 크기 설정
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor); // 배경색 설정
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); // 둥근 사각형 그리기
            g2.setColor(Color.BLACK); // 테두리 색상 설정
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20); // 둥근 테두리 그리기

            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(textColor);
            g2.setFont(new Font("Serif", Font.BOLD, 20)); // 글자 크기 및 굵기 설정

            // userName 중앙 정렬
            int xName = (getWidth() - fm.stringWidth(userName)) / 2;
            int yName = (getHeight() / 2) - fm.getDescent();
            g2.drawString(userName, xName - 10, yName - 10); // userName 그리기

            // score 중앙 정렬
            int xScore = (getWidth() - fm.stringWidth(score)) / 2;
            int yScore = (getHeight() / 2) + fm.getAscent();
            g2.drawString(score, xScore - 10, yScore + 10); // score 그리기
        }
    }



}
