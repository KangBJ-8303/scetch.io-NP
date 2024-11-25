import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;


public class StartDisplay extends JFrame {

    private JTextField t_userID;

    private String serverAddress;
    private int serverPort;

    private JButton b_connect = new JButton("접속하기");


    public StartDisplay(String serverAddress, int serverPort){
        super("sketch");

        this.serverPort = serverPort;
        this.serverAddress = serverAddress;

        buildGUI();

        setBounds(200, 80, 500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

    }

    private void buildGUI() {
        add(createInfoPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));

        t_userID = new JTextField(7);

        t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);

        p.add(new JLabel("닉네임: "));
        p.add(t_userID);

        return p;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        b_connect.setPreferredSize(new Dimension(100, 30));
        panel.add(b_connect);
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                new MainDisplay(serverAddress, serverPort, t_userID.getText());
            }
        });

        return panel;
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

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 54321;

        new StartDisplay(serverAddress, serverPort);
    }
}
