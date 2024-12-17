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
        userList.add(uid);
        buildGUI();
        setBounds(200, 80, 1200, 600);
        setDefaultCloseOperation(3);
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
        userPanel.setBackground(new Color(240, 248, 255));
        userPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        chatPanel.setBackground(new Color(255, 250, 240));
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        timerLabel.setFont(new Font("맑은 고딕", 1, 18));
        timerLabel.setHorizontalAlignment(0);
        timerLabel.setForeground(Color.DARK_GRAY);
        userInfoPanel = createUserInfoPanel();
        userPanel.add(userInfoPanel, BorderLayout.CENTER);
        JPanel timerPanel = new JPanel();
        timerPanel.add(timerLabel);
        userPanel.add(timerPanel, BorderLayout.NORTH);
        b_exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (userList.size() == 2) {// 남은 인원 2명일때 나가면 리셋
                    send(new ChatMsg(uid, ChatMsg.MODE_TX_RESET));
                }

                disconnect();//연결 종료
                setVisible(false);
                new StartDisplay(serverAddress, serverPort); // 시작화면으로
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

    private JPanel createUserInfoPanel() { // 유저 목록 패널 생성
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));
        return panel;
    }

    public void startTimerFromServer() { // 그림 타이머 작동
        if (timer != null) {
            timer.cancel();
        }

        if (uid.equals(currentDrawer)) {
            t_input.setEnabled(false);
        }

        remainingSeconds = 80;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> { // 쓰레드 겹침 방지
                    if (remainingSeconds > 0) {
                        remainingSeconds--;
                        timerLabel.setText(String.valueOf(remainingSeconds));
                    } else {
                        vocaPanel.setVisible(false);
                        canvas.setClean();
                        nextDrawer();
                    }

                });
            }
        }, 1000, 1000);
    }

    public void selectVoca() { // 단어 선택 시간, 화면
        if (timer != null) {
            timer.cancel();
        }

        remainingSeconds = 15;
        timer = new Timer();
        if (uid.equals(currentDrawer)) { // 문제 출제자 일때
            t_input.setEnabled(false);
            loadRandomWords(); // 랜덤 단어 가져오기
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (remainingSeconds > 0) {
                        --remainingSeconds;
                        timerLabel.setText(String.valueOf(remainingSeconds));
                    } else {
                        if (uid.equals(currentDrawer)) {// 현재 그림 그리는 사람인지 확인
                            Component[] components = vocaPanel.getComponents();
                            if (components.length > 0) {// 시간내에 못 정했을 경우 vocaPanel에 생성된 컴포넌트를 가져와 랜덤으로 선택
                                JButton randomButton = (JButton)components[(new Random()).nextInt(components.length)];
                                selectedWord = randomButton.getText();
                                resetVocaPanelWithSelectedWord(selectedWord);
                                paintPanel.setVisible(true);
                                send(new ChatMsg(uid, ChatMsg.MODE_TX_START, selectedWord));// 게임 시작(80초)
                            }
                        }
                        timer.cancel();//기존 타이머 캔슬
                    }

                });
            }
        }, 1000, 1000);// 1초 마다 반복
    }

    private void loadRandomWords() {
        try {
            List<String> words = Files.readAllLines(Paths.get("words.txt")); // words.txt에서 단어 읽어온후 List에 저장
            Random random = new Random();
            vocaPanel.removeAll();
            vocaPanel.setLayout(new GridLayout(1, 3)); // 가로로 3칸생성
            Set<String> selectedWords = new HashSet();

            for(int i = 0; i < 3; ++i) {
                String randomWord = (String)words.get(random.nextInt(words.size())); // 리스트에서 랜덤으로 선택
                if (!selectedWords.contains(randomWord)) {// Set에 랜덤 단어가 이미 있지 않으면
                    selectedWords.add(randomWord); //단어 추가
                    final JButton wordButton = new JButton(randomWord);// 단어 버튼 생성
                    wordButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            selectedWord = wordButton.getText();// 선택한 단어 저장
                            resetVocaPanelWithSelectedWord(selectedWord);
                            timer.cancel();// 버튼 눌리면 타이머 종료
                            paintPanel.setVisible(true); // 그림패널 다시 보이게
                            send(new ChatMsg(uid, ChatMsg.MODE_TX_START, selectedWord));// 게임 시작 호출
                        }
                    });
                    vocaPanel.add(wordButton);// 버튼 생성
                }//3개 생성
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
        vocaPanel.removeAll();// 버튼 모두 지우기
        JLabel wordLabel = new JLabel(word, 0);
        wordLabel.setFont(new Font("Serif", 1, 20));
        wordLabel.setHorizontalAlignment(0);
        vocaPanel.setLayout(new BorderLayout());
        vocaPanel.add(wordLabel, BorderLayout.CENTER);
        vocaPanel.setVisible(true);
        vocaPanel.revalidate();
        vocaPanel.repaint();
    }

    public void nextDrawer() {// 다음 그리는 유저로 변경
        if (currentDrawer != null) { // 출제자 없으면 색 하얀색으로
            userColorMap.put(currentDrawer, 0);
        }

        updateUserInfoPanel();// 유저패널 업데이트
        if (uid.equals(currentDrawer)) { //유저가 출제자 인지
            t_input.setEnabled(true);
            send(new ChatMsg(uid, ChatMsg.MODE_TX_ORDER, orderIndex % userList.size())); // 순서 변경
        }

    }

    public void setCurrentDrawer(String userName) { // 출제자 선정
        userColorMap.put(currentDrawer, 0); // 기존 출제자 하얀색으로
        userColorMap.put(userName, 1); // 새 출제자 파란색으로
        updateUserInfoPanel();
        currentDrawer = userName; // 출제자 변경
        canvas.updateToolVisibility(); // canvas 툴 업데이트
    }

    public String getCurrentDrawer() {
        return currentDrawer;
    }

    public String getUid() {
        return uid;
    }

    public void addUserInfo(String userName) {
        if (userList.size() < 2) { // 2명 보다 적으면 게임 리셋을 위해 캔버스 클리어
            canvas.setClean();
            b_start.setVisible(true); //시작버튼 다시 사용가능하게
            b_start.setEnabled(true);
            currentDrawer = null; // 출제자 null 처리
        }

        canvas.setShapeString(); //펜 초기화
        canvas.setClean();// 캔버스 초기화
        if (!userColorMap.containsKey(userName)) {//유저이름이 없으면 배경색 흰색으로 추가
            userColorMap.put(userName, 0);
        }

        if (!userScores.containsKey(userName)) { //유저 이름이 없이 없으면 0점으로 초기화
            userScores.put(userName, 0);
        }

        int userColorIndex = userColorMap.get(userName) % USER_COLORS.length;
        Color userColor = USER_COLORS[userColorIndex]; // 유저 색 결정
        RoundedLabel roundedLabel; // 둥근 라벨 생성
        if (uid.equals(userName)) { //본인 확인
            roundedLabel = new RoundedLabel(userName + "(you)", Integer.toString(userScores.get(userName)), userColor, Color.BLACK);
        } else {
            roundedLabel = new RoundedLabel(userName, Integer.toString(userScores.get(userName)), userColor, Color.BLACK);
        }

        userInfoPanel.add(roundedLabel);
        userInfoPanel.revalidate();
        userInfoPanel.repaint();
        if (userList.size() >= 2) { // 2명 이상일 경우 스타트
            if (uid.equals(userList.get(0))) { // 방장만 시작 가능하게
                b_start.setEnabled(true);
            } else {
                b_start.setVisible(false);
            }
        } else {
            b_start.setEnabled(false);
        }

    }

    private JPanel createDisplayPanel() { // 채팅 화면
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
        socket.connect(sa, 3000);
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        receiveThread = new Thread(new Runnable() {
            private ObjectInputStream in;
            private Map<String, Integer> receivedScores;

            private void receiveMessage() {
                try {
                    ChatMsg inMsg = (ChatMsg)in.readObject();
                    if (inMsg == null) {
                        disconnect();
                        return;
                    }

                    switch (inMsg.mode) {
                        case ChatMsg.MODE_ENTER: // 입장 퇴장 메세지
                            printDisplay(inMsg.message);
                            break;
                        case ChatMsg.MODE_TX_STRING: // 메세지 전송
                            printDisplay(inMsg.userID + ": " + inMsg.message);
                            break;
                        case ChatMsg.MODE_TX_USERSCORE: // 점수 연동
                            receivedScores = inMsg.userScores;
                            userScores.clear();
                            userScores.putAll(receivedScores);
                            updateUserInfoPanel();
                            break;
                        case ChatMsg.MODE_TX_CORRECT: // 정답 확인 증가
                            if (uid.equals(inMsg.userID)) {
                                printDisplay(uid + " 가 정답을 맞추었습니다.");
                            }

                            receivedScores = inMsg.userScores;
                            userScores.clear();
                            userScores.putAll(receivedScores);
                            if (uid.equals(currentDrawer)) {
                                t_input.setEnabled(true);
                            }

                            for(String user : userList) {
                                if (userScores.get(user) == 4) { // 원하는 점수 승리
                                    printDisplay(user + "가 승리하였습니다.");
                                    send(new ChatMsg(uid, ChatMsg.MODE_TX_RESET));
                                    break;
                                }
                            }

                            remainingSeconds = 1; // 다음 순서로 넘어가기 위해 1초로 설정
                            canvas.setClean();
                            updateUserInfoPanel();
                            break;
                        case ChatMsg.MODE_TX_IMAGE:
                            printDisplay(inMsg.image);
                            break;
                        case ChatMsg.MODE_TX_END: // 게임 종료
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

                            if (uid.equals(userList.get(0))) {
                                b_start.setVisible(true);
                                b_start.setEnabled(true);
                            }
                            break;
                        case ChatMsg.MODE_TX_DRAW: //그림 전송
                            canvas.drawing(inMsg.x1, inMsg.y1, inMsg.x2, inMsg.y2, inMsg.color, inMsg.stroke, inMsg.shapeString);
                            break;
                        case ChatMsg.MODE_TX_USER: // 유저 갱신
                            userList.clear();
                            userList.addAll(inMsg.users);
                            userInfoPanel.removeAll();
                            for(String user : userList) {
                                addUserInfo(user);
                            }

                            userInfoPanel.revalidate();
                            userInfoPanel.repaint();
                            break;
                        case ChatMsg.MODE_TX_ORDER: // 순서 변경
                            vocaPanel.setVisible(false);
                            orderIndex = inMsg.order % userList.size();
                            setCurrentDrawer(userList.get(orderIndex));
                            printDisplay("지금 턴 : " + currentDrawer);

                            selectVoca();

                            break;
                        case ChatMsg.MODE_TX_START: // 게임 시작
                            String displayWord = inMsg.message;
                            System.out.println("Received word: " + displayWord);
                            startTimerFromServer();
                            resetVocaPanelWithSelectedWord(displayWord);
                    }
                } catch (IOException e) {
                    printDisplay("연결을 종료했습니다.");
                } catch (ClassNotFoundException e) {
                    printDisplay("잘못된 객체가 전달되었습니다.");
                }

            }

            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }

                while(receiveThread == Thread.currentThread()) {
                    receiveMessage();
                }

            }
        });
        receiveThread.start();
    }

    private void updateUserInfoPanel() {
        userInfoPanel.removeAll();

        for(String user : userList) {
            addUserInfo(user);
        }

        userInfoPanel.revalidate();
        userInfoPanel.repaint();
    }

    private JPanel createInputPanel() { // 입력 패널 생성
        JPanel panel = new JPanel(new BorderLayout());
        t_input = new JTextField(30);
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        panel.add(t_input, BorderLayout.CENTER);
        JPanel p_button = new JPanel(new GridLayout(1, 0));
        p_button.add(b_send);
        panel.add(p_button, BorderLayout.EAST);
        t_input.setEnabled(true);
        b_send.setEnabled(true);
        return panel;
    }

    public void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();

        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        t_display.setCaretPosition(len);
    }

    private void printDisplay(BufferedImage msg) {
        int len = t_display.getDocument().getLength();

        try {
            document.insertString(len, String.valueOf(msg) + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        this.t_display.setCaretPosition(len);
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
        send(new ChatMsg(uid, ChatMsg.MODE_TX_DRAW, x1, y1, x2, y2, color, stroke, shapeString)); // 그림 전송
    }

    private void sendMessage() {
        String message = t_input.getText();
        if (!message.isEmpty()) {
            send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));
            t_input.setText("");
        }
    }

    private void sendUserID() {
        send(new ChatMsg(uid, ChatMsg.MODE_LOGIN)); // 로그인
        send(new ChatMsg(uid, ChatMsg.MODE_TX_USER, userList)); // 유저 목록
    }

    static {
        USER_COLORS = new Color[]{Color.WHITE, Color.CYAN}; // 순서 색
    }

    public class RoundedLabel extends JPanel { // 둥근 라벨 생성
        private String userName;
        private String score;
        private Color backgroundColor;
        private Color textColor;

        public RoundedLabel(String userName, String score, Color backgroundColor, Color textColor) {
            this.userName = userName;
            this.score = score;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            setOpaque(false);
            setPreferredSize(new Dimension(190, 100));
            setMaximumSize(new Dimension(190, 100));
        }

        //검색 참고
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(textColor);
            g2.setFont(new Font("Serif", 1, 20));
            int xName = (getWidth() - fm.stringWidth(userName)) / 2;
            int yName = getHeight() / 2 - fm.getDescent();
            g2.drawString(userName, xName - 10, yName - 10);
            int xScore = (getWidth() - fm.stringWidth(score)) / 2;
            int yScore = getHeight() / 2 + fm.getAscent();
            g2.drawString(score, xScore - 10, yScore + 10);
        }
    }
}
