//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import javax.swing.*;

public class Canvas extends JPanel implements ActionListener, MouseListener, MouseMotionListener {
    String shapeString = "";
    Point firstPointer = new Point(0, 0);
    Point secondPointer = new Point(0, 0);
    BufferedImage bufferedImage;
    Color colors;
    Float stroke;
    JComboBox<String> colorComboBox;
    JComboBox<Float> strokeComboBox;
    String[] colorNames;
    Color[] colorsArray;
    String uid;
    MainDisplay mainDisplay;
    int width;
    int height;
    int minPointx;
    int minPointy;
    private JPanel toolPanel;
    JButton eraseAllButton;
    JButton rectButton;
    JButton lineButton;
    JButton circleButton;
    JButton penButton;
    JButton eraseButton;

    public Canvas(String uid, MainDisplay mainDisplay) {
        this.colors = Color.black;
        this.stroke = 5.0F;
        this.colorNames = new String[]{"검정", "빨강", "파랑", "초록", "노랑", "핑크", "마젠타"};
        this.colorsArray = new Color[]{Color.black, Color.red, Color.blue, Color.green, Color.yellow, Color.pink, Color.magenta};
        this.eraseAllButton = new JButton("전체지우기");
        this.rectButton = new JButton("네모");
        this.lineButton = new JButton("선");
        this.circleButton = new JButton("원");
        this.penButton = new JButton("펜");
        this.eraseButton = new JButton("지우개");
        this.uid = uid;
        this.mainDisplay = mainDisplay;
        this.colorComboBox = new JComboBox(this.colorNames);
        this.strokeComboBox = new JComboBox();
        this.toolPanel = this.createToolPanel();
        this.notOrder();
        this.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
    }

    private void Order() {
        this.eraseAllButton.setEnabled(true);
        this.rectButton.setEnabled(true);
        this.lineButton.setEnabled(true);
        this.circleButton.setEnabled(true);
        this.penButton.setEnabled(true);
        this.eraseButton.setEnabled(true);
    }

    private void notOrder() {
        this.eraseAllButton.setEnabled(false);
        this.rectButton.setEnabled(false);
        this.lineButton.setEnabled(false);
        this.circleButton.setEnabled(false);
        this.penButton.setEnabled(false);
        this.eraseButton.setEnabled(false);
    }

    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 6, 5, 5));
        Color backgroundColor = new Color(240, 248, 255);

        panel.setBackground(backgroundColor);

        int imageSize = 45;

        this.eraseAllButton = createImageButton("resources/icons/eraseall.png", imageSize, backgroundColor, "전체지우기");
        this.rectButton = createImageButton("resources/icons/rect.png", imageSize, backgroundColor, "네모");
        this.lineButton = createImageButton("resources/icons/line.png", imageSize, backgroundColor, "선");
        this.circleButton = createImageButton("resources/icons/circle.png", imageSize, backgroundColor, "원");
        this.penButton = createImageButton("resources/icons/pen.png", imageSize, backgroundColor, "펜");
        this.eraseButton = createImageButton("resources/icons/erase.png", imageSize, backgroundColor, "지우개");

        JButton[] buttons = {eraseAllButton, rectButton, lineButton, circleButton, penButton, eraseButton};
        for (JButton button : buttons) {
            button.addActionListener(this);
            panel.add(button);
        }

        Float[] strokeOptions = {1.0f, 3.0f, 5.0f, 7.0f, 10.0f};
        for (Float strokeValue : strokeOptions) {
            this.strokeComboBox.addItem(strokeValue);
        }

        this.eraseAllButton.addActionListener(this);
        this.rectButton.addActionListener(this);
        this.lineButton.addActionListener(this);
        this.circleButton.addActionListener(this);
        this.penButton.addActionListener(this);
        this.eraseButton.addActionListener(this);
        this.strokeComboBox.addActionListener(this);
        this.colorComboBox.addActionListener(this);
        this.add(this.colorComboBox);
        this.add(this.strokeComboBox);
        panel.add(this.eraseButton);
        panel.add(this.eraseAllButton);
        panel.add(this.rectButton);
        panel.add(this.lineButton);
        panel.add(this.circleButton);
        panel.add(this.penButton);
        panel.add(this.colorComboBox);
        panel.add(this.strokeComboBox);
        this.eraseAllButton.addActionListener(this);
        this.rectButton.addActionListener(this);
        this.lineButton.addActionListener(this);
        this.circleButton.addActionListener(this);
        this.penButton.addActionListener(this);
        this.eraseButton.addActionListener(this);
        this.colorComboBox.addActionListener(this);
        this.strokeComboBox.addActionListener(this);
        this.setLayout(new BorderLayout());
        this.bufferedImage = new BufferedImage(500, 500, 2);
        this.setImageBackground(this.bufferedImage);
        this.add(panel, "South");
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        return panel;
    }

    private JButton createImageButton(String imagePath, int imageSize, Color buttonBackgroundColor, String command) {
        ImageIcon icon = new ImageIcon(imagePath);
        Image scaledImage = icon.getImage().getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JButton button = new JButton(scaledIcon);
        button.setPreferredSize(new Dimension(50, 50));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(buttonBackgroundColor);
        button.setOpaque(true);
        button.setActionCommand(command);
        return button;
    }



    public void updateToolVisibility() {
        if (this.mainDisplay.getCurrentDrawer().equals(this.mainDisplay.getUid())) {
            this.Order();
        } else {
            this.notOrder();
        }

    }

    public void setShapeString() {
        this.shapeString = "";
    }

    public void drawing(int firstX, int firstY, int secondX, int secondY, Color colors, float stroke, String shapeString) {
        this.width = Math.abs(secondX - firstX);
        this.height = Math.abs(secondY - firstY);
        this.minPointx = Math.min(firstX, secondX);
        this.minPointy = Math.min(firstY, secondY);
        Graphics2D g = this.bufferedImage.createGraphics();
        switch (shapeString) {
            case "선":
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstX, firstY, secondX, secondY);
                break;
            case "네모":
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawRect(this.minPointx, this.minPointy, this.width, this.height);
                break;
            case "원":
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawOval(this.minPointx, this.minPointy, this.width, this.height);
                break;
            case "펜":
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstX, firstY, secondX, secondY);
                break;
            case "지우개":
                g.setColor(Color.white);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstX, firstY, secondX, secondY);
                break;
            case "전체지우기":
                this.setImageBackground(this.bufferedImage);
        }

        g.dispose();
        this.repaint();
    }

    public void setClean() {
        this.setImageBackground(this.bufferedImage);
    }

    public void mousePressed(MouseEvent e) {
        this.firstPointer.setLocation(0, 0);
        this.secondPointer.setLocation(0, 0);
        this.firstPointer.setLocation(e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
        if (this.shapeString != "펜") {
            this.secondPointer.setLocation(e.getX(), e.getY());
            this.updatePaint();
            this.firstPointer.setLocation(0, 0);
            this.secondPointer.setLocation(0, 0);
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (e.getSource() == this.colorComboBox) {
            int selectedIndex = this.colorComboBox.getSelectedIndex();
            this.colors = this.colorsArray[selectedIndex];
            System.out.println("선택된 색상: " + this.colors);
            return;
        }

        if (e.getSource() == this.strokeComboBox) {
            this.stroke = (Float) this.strokeComboBox.getSelectedItem();
            System.out.println("펜 굵기: " + this.stroke);
            return;
        }

        switch (command) {
            case "전체지우기":
                this.shapeString = "전체지우기";
                this.setClean();
                break;
            case "네모":
                this.shapeString = "네모";
                break;
            case "선":
                this.shapeString = "선";
                break;
            case "원":
                this.shapeString = "원";
                break;
            case "펜":
                this.shapeString = "펜";
                break;
            case "지우개":
                this.shapeString = "지우개";
                break;
            default:
                System.out.println("알 수 없는 명령: " + command);
        }
    }


    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }

    public void updatePaint() {
        this.width = Math.abs(this.secondPointer.x - this.firstPointer.x);
        this.height = Math.abs(this.secondPointer.y - this.firstPointer.y);
        this.minPointx = Math.min(this.firstPointer.x, this.secondPointer.x);
        this.minPointy = Math.min(this.firstPointer.y, this.secondPointer.y);
        Graphics2D g = this.bufferedImage.createGraphics();
        this.mainDisplay.sendDrawing(this.uid, this.firstPointer.x, this.firstPointer.y, this.secondPointer.x, this.secondPointer.y, this.colors, this.stroke, this.shapeString);
        switch (this.shapeString) {
            case "선":
                g.setColor(this.colors);
                g.setStroke(new BasicStroke(this.stroke));
                g.drawLine(this.firstPointer.x, this.firstPointer.y, this.secondPointer.x, this.secondPointer.y);
                break;
            case "네모":
                g.setColor(this.colors);
                g.setStroke(new BasicStroke(this.stroke));
                g.drawRect(this.minPointx, this.minPointy, this.width, this.height);
                break;
            case "원":
                g.setColor(this.colors);
                g.setStroke(new BasicStroke(this.stroke));
                g.drawOval(this.minPointx, this.minPointy, this.width, this.height);
                break;
            case "펜":
                g.setColor(this.colors);
                g.setStroke(new BasicStroke(this.stroke));
                g.drawLine(this.firstPointer.x, this.firstPointer.y, this.secondPointer.x, this.secondPointer.y);
                break;
            case "지우개":
                g.setColor(Color.white);
                g.setStroke(new BasicStroke(this.stroke));
                g.drawLine(this.firstPointer.x, this.firstPointer.y, this.secondPointer.x, this.secondPointer.y);
                break;
            case "전체지우기":
                this.setImageBackground(this.bufferedImage);
                this.shapeString = "";
        }

        g.dispose();
        this.repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.bufferedImage, 0, 0, (ImageObserver)null);
    }

    public void setImageBackground(BufferedImage bi) {
        this.bufferedImage = bi;
        Graphics2D g = this.bufferedImage.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 500, 500);
        g.dispose();
    }

    public void mouseDragged(MouseEvent e) {
        this.width = Math.abs(this.secondPointer.x - this.firstPointer.x);
        this.height = Math.abs(this.secondPointer.y - this.firstPointer.y);
        this.minPointx = Math.min(this.firstPointer.x, this.secondPointer.x);
        this.minPointy = Math.min(this.firstPointer.y, this.secondPointer.y);
        if (this.shapeString == "펜" | this.shapeString == "지우개") {
            if (this.secondPointer.x != 0 && this.secondPointer.y != 0) {
                this.firstPointer.x = this.secondPointer.x;
                this.firstPointer.y = this.secondPointer.y;
            }

            this.secondPointer.setLocation(e.getX(), e.getY());
            this.updatePaint();
        } else if (this.shapeString == "선") {
            Graphics g = this.getGraphics();
            g.drawLine(this.firstPointer.x, this.firstPointer.y, this.secondPointer.x, this.secondPointer.y);
            this.secondPointer.setLocation(e.getX(), e.getY());
            this.repaint();
            g.dispose();
        } else if (this.shapeString == "네모") {
            Graphics g = this.getGraphics();
            g.setColor(Color.BLACK);
            g.setXORMode(this.getBackground());
            g.drawRect(this.minPointx, this.minPointy, this.width, this.height);
            this.secondPointer.setLocation(e.getX(), e.getY());
            this.repaint();
            g.dispose();
        } else if (this.shapeString == "원") {
            Graphics g = this.getGraphics();
            g.setColor(Color.BLACK);
            g.setXORMode(this.getBackground());
            g.drawOval(this.minPointx, this.minPointy, this.width, this.height);
            this.secondPointer.setLocation(e.getX(), e.getY());
            g.dispose();
            this.repaint();
        }

    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
