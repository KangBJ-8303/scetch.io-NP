import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.Random;

public class StartDisplay extends JFrame { // 시작 화면

    private UnderlineTextField t_userID;
    private String serverAddress;
    private int serverPort;
    private TransparentButton b_connect = new TransparentButton("접속하기");

    public StartDisplay(String serverAddress, int serverPort) {
        super("sketch");

        this.serverPort = serverPort;
        this.serverAddress = serverAddress;

        buildGUI();

        setBounds(200, 80, 800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
    }

    private void buildGUI() {
        Image backgroundImage = loadImage("resources/images/sketch.png"); // 텍스트 필드 배경 이미지 경로
        setContentPane(new BackgroundPanel(backgroundImage)); // 창 전체 배경 이미지 설정

        setLayout(null); // null 레이아웃 설정

        createInfoPanel();
        createControlPanel();
    }

    private void createInfoPanel() {
        Random random = new Random();
        int randomNumber = random.nextInt(90000) + 10000; // 랜덤 5자리 생성

        t_userID = new UnderlineTextField("guest" + randomNumber);
        t_userID.setBounds(430, 300, 100, 30); // 원하는 좌표와 크기로 설정

        add(t_userID);
    }

    private void createControlPanel() {
        b_connect.setBounds(320, 400, 200, 100); // 원하는 좌표와 크기로 설정
        b_connect.setFont(new Font("Malgun Gothic", Font.BOLD, 32));

        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                new MainDisplay(serverAddress, serverPort, t_userID.getText()); // 게임 입장
            }
        });

        add(b_connect);
    }

    private String getLocalAddr() {
        InetAddress local = null;
        String addr = "";
        try {
            local = InetAddress.getLocalHost();
            addr = local.getHostAddress();
            System.out.println(addr);
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
        }

        return addr;
    }

    private Image loadImage(String path) {
        ImageIcon icon = new ImageIcon(path);
        return icon.getImage();
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        new StartDisplay(serverAddress, serverPort);
    }
}

class BackgroundPanel extends JPanel { // 배경 이미지 설정
    private Image backgroundImage;

    public BackgroundPanel(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }
}

class TransparentButton extends JButton { //버튼 배경 없애기
    public TransparentButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(Color.BLACK); // 텍스트 색상 설정
    }
}

class UnderlineTextField extends JTextField { // 텍스트 필드 바닥만 보이게
    public UnderlineTextField(String text) {
        super(text);
        setOpaque(false); // 배경을 투명하게 설정
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 기본 테두리 제거
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 밑줄 그리기
        g.setColor(Color.BLACK); // 밑줄 색상 설정
        g.fillRect(0, getHeight() - 1, getWidth(), 1); // 밑줄 그리기
    }
}
