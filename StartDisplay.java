//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class StartDisplay extends JFrame {
    private JTextField t_userID;
    private String serverAddress;
    private int serverPort;
    private JButton b_connect = new JButton("접속하기");

    public StartDisplay(String serverAddress, int serverPort) {
        super("sketch");
        this.serverPort = serverPort;
        this.serverAddress = serverAddress;
        this.buildGUI();
        this.setBounds(200, 80, 500, 300);
        this.setDefaultCloseOperation(3);
        this.setVisible(true);
    }

    private void buildGUI() {
        this.add(this.createInfoPanel(), "Center");
        this.add(this.createControlPanel(), "South");
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new FlowLayout(1));
        this.t_userID = new JTextField(7);
        JTextField var10000 = this.t_userID;
        String[] var10001 = this.getLocalAddr().split("\\.");
        var10000.setText("guest" + var10001[3]);
        this.t_userID.setBackground(new Color(224, 255, 255));
        this.t_userID.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        p.add(new JLabel("닉네임: "));
        p.add(this.t_userID);
        return p;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(1));
        this.b_connect.setPreferredSize(new Dimension(100, 30));
        this.b_connect.setBackground(new Color(173, 216, 230));
        this.b_connect.setFont(new Font("맑은 고딕", 1, 14));
        panel.add(this.b_connect);
        this.b_connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StartDisplay.this.setVisible(false);
                new MainDisplay(StartDisplay.this.serverAddress, StartDisplay.this.serverPort, StartDisplay.this.t_userID.getText());
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
        } catch (UnknownHostException e) {
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
