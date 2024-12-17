//인터넷 참고
//Order, notOrder, updateToolVisiblity,setShapeString, drawing, setClean추가
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
        colors = Color.black;
        stroke = 5.0F;
        colorNames = new String[]{"검정", "빨강", "파랑", "초록", "노랑", "핑크", "마젠타"};
        colorsArray = new Color[]{Color.black, Color.red, Color.blue, Color.green, Color.yellow, Color.pink, Color.magenta};
        eraseAllButton = new JButton("전체지우기");
        rectButton = new JButton("네모");
        lineButton = new JButton("선");
        circleButton = new JButton("원");
        penButton = new JButton("펜");
        eraseButton = new JButton("지우개");
        this.uid = uid;
        this.mainDisplay = mainDisplay;
        colorComboBox = new JComboBox(colorNames);
        strokeComboBox = new JComboBox();
        toolPanel = createToolPanel();
        notOrder();
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
    }


    private void Order(){ // 출제자시 버튼 활성화
        eraseAllButton.setEnabled(true);
        rectButton.setEnabled(true);
        lineButton.setEnabled(true);
        circleButton.setEnabled(true);
        penButton.setEnabled(true);
        eraseButton.setEnabled(true);
    }

    private void notOrder(){ // 나머지는 버튼 비활성화
        eraseAllButton.setEnabled(false);
        rectButton.setEnabled(false);
        lineButton.setEnabled(false);
        circleButton.setEnabled(false);
        penButton.setEnabled(false);
        eraseButton.setEnabled(false);
    }

    private JPanel createToolPanel() { // 그림 버튼 툴
        JPanel panel = new JPanel(new GridLayout(2, 6, 5, 5));
        Color backgroundColor = new Color(240, 248, 255);

        panel.setBackground(backgroundColor);

        int imageSize = 45;

        eraseAllButton = createImageButton("resources/icons/eraseall.png", imageSize, backgroundColor, "전체지우기");
        rectButton = createImageButton("resources/icons/rect.png", imageSize, backgroundColor, "네모");
        lineButton = createImageButton("resources/icons/line.png", imageSize, backgroundColor, "선");
        circleButton = createImageButton("resources/icons/circle.png", imageSize, backgroundColor, "원");
        penButton = createImageButton("resources/icons/pen.png", imageSize, backgroundColor, "펜");
        eraseButton = createImageButton("resources/icons/erase.png", imageSize, backgroundColor, "지우개");

        JButton[] buttons = {eraseAllButton, rectButton, lineButton, circleButton, penButton, eraseButton};
        for (JButton button : buttons) {
            button.addActionListener(this);
            panel.add(button);
        }

        Float[] strokeOptions = {1.0f, 3.0f, 5.0f, 7.0f, 10.0f};
        for (Float strokeValue : strokeOptions) {
            strokeComboBox.addItem(strokeValue);
        }

        eraseAllButton.addActionListener(this);
        rectButton.addActionListener(this);
        lineButton.addActionListener(this);
        circleButton.addActionListener(this);
        penButton.addActionListener(this);
        eraseButton.addActionListener(this);
        strokeComboBox.addActionListener(this);
        colorComboBox.addActionListener(this);
        add(colorComboBox);
        add(strokeComboBox);
        panel.add(eraseButton);
        panel.add(eraseAllButton);
        panel.add(rectButton);
        panel.add(lineButton);
        panel.add(circleButton);
        panel.add(penButton);
        panel.add(colorComboBox);
        panel.add(strokeComboBox);
        eraseAllButton.addActionListener(this);
        rectButton.addActionListener(this);
        lineButton.addActionListener(this);
        circleButton.addActionListener(this);
        penButton.addActionListener(this);
        eraseButton.addActionListener(this);
        colorComboBox.addActionListener(this);
        strokeComboBox.addActionListener(this);
        setLayout(new BorderLayout());
        bufferedImage = new BufferedImage(500, 500, 2);
        setImageBackground(bufferedImage);
        add(panel, "South");
        addMouseListener(this);
        addMouseMotionListener(this);
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



    public void updateToolVisibility(){ // 출제자면 버튼 활성화 아니면 비활성화
        if(mainDisplay.getCurrentDrawer().equals(mainDisplay.getUid())){
            Order();
        }
        else {
            notOrder();
        }

    }

    public void setShapeString() { // 펜 초기화
        shapeString = "";
    }

    public void drawing(int firstX, int firstY, int secondX, int secondY, Color colors, float stroke, String shapeString) { // 서버와 클라이언트 그림 송수신용

        width = Math.abs(secondX - firstX);
        height = Math.abs(secondY - firstY);

        minPointx = Math.min(firstX, secondX);
        minPointy = Math.min(firstY, secondY);

        Graphics2D g = bufferedImage.createGraphics();

        switch (shapeString) {
            case ("선"):
                g.setColor(colors);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstX, firstY, secondX, secondY);
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
                g.drawLine(firstX, firstY, secondX, secondY);
                break;
            case ("지우개"):
                g.setColor(Color.white);
                g.setStroke(new BasicStroke(stroke));
                g.drawLine(firstX, firstY, secondX, secondY);
                break;
            case ("전체지우기"):
                setImageBackground(bufferedImage);
                break;
            default:
                break;
        }
        g.dispose();
        repaint();
    }

    public void setClean() {  // 캔버스 초기화 용
        setImageBackground(bufferedImage);
    }

    public void mousePressed(MouseEvent e) {
        // 다시 클릭됐을경우 좌표 초기화
        firstPointer.setLocation(0, 0);
        secondPointer.setLocation(0, 0);

        firstPointer.setLocation(e.getX(), e.getY());
    }


    public void mouseReleased(MouseEvent e) { //펜이 아닐때 마우스 떨어질 때 그림

        if (shapeString != "펜") {
            secondPointer.setLocation(e.getX(), e.getY());
            updatePaint();
            firstPointer.setLocation(0, 0);
            secondPointer.setLocation(0, 0);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (e.getSource() == colorComboBox) {
            int selectedIndex = colorComboBox.getSelectedIndex();
            colors = colorsArray[selectedIndex];
            System.out.println("선택된 색상: " + colors);
            return;
        }

        if (e.getSource() == strokeComboBox) {
            stroke = (Float) strokeComboBox.getSelectedItem();
            System.out.println("펜 굵기: " + stroke);
            return;
        }

        switch (command) {
            case "전체지우기":
                shapeString = "전체지우기";
                setClean();
                break;
            case "네모":
                shapeString = "네모";
                break;
            case "선":
                shapeString = "선";
                break;
            case "원":
                shapeString = "원";
                break;
            case "펜":
                shapeString = "펜";
                break;
            case "지우개":
                shapeString = "지우개";
                break;
            default:
                System.out.println("알 수 없는 명령: " + command);
        }
    }


    public Dimension getPreferredSize() { // 도화지 크기
        return new Dimension(500, 500);
    }

    public void updatePaint() { //그림 업데이트
        width = Math.abs(secondPointer.x - firstPointer.x);
        height = Math.abs(secondPointer.y - firstPointer.y);

        minPointx = Math.min(firstPointer.x, secondPointer.x);
        minPointy = Math.min(firstPointer.y, secondPointer.y);

        Graphics2D g = bufferedImage.createGraphics();
        // draw on paintImage using Graphics
        mainDisplay.sendDrawing(uid, firstPointer.x, firstPointer.y, secondPointer.x, secondPointer.y, colors, stroke, shapeString);
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
                shapeString ="";
                break;
            default:
                break;
        }
        g.dispose();
        repaint();

    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bufferedImage, 0, 0, null);

    }

    public void setImageBackground(BufferedImage bi) { // 배경 하얀색으로
        this.bufferedImage = bi;
        Graphics2D g = bufferedImage.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, 500, 500);
        g.dispose();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub

        width = Math.abs(secondPointer.x - firstPointer.x);
        height = Math.abs(secondPointer.y - firstPointer.y);

        minPointx = Math.min(firstPointer.x, secondPointer.x);
        minPointy = Math.min(firstPointer.y, secondPointer.y);

        if (shapeString == "펜" | shapeString == "지우개") {
            if (secondPointer.x != 0 && secondPointer.y != 0) {
                firstPointer.x = secondPointer.x;
                firstPointer.y = secondPointer.y;
            }
            secondPointer.setLocation(e.getX(), e.getY());
            updatePaint();
        } else if (shapeString == "선") {
            Graphics g = getGraphics();
            g.drawLine(firstPointer.x, firstPointer.y, secondPointer.x, secondPointer.y);
            secondPointer.setLocation(e.getX(), e.getY());
            repaint();
            g.dispose();
        } else if (shapeString == "네모") {
            Graphics g = getGraphics();
            g.setColor(Color.BLACK);
            g.setXORMode(getBackground());
            g.drawRect(minPointx, minPointy, width, height);
            secondPointer.setLocation(e.getX(), e.getY());
            repaint();
            g.dispose();
        } else if (shapeString == "원") {
            Graphics g = getGraphics();
            g.setColor(Color.BLACK);
            g.setXORMode(getBackground());
            g.drawOval(minPointx, minPointy, width, height);
            secondPointer.setLocation(e.getX(), e.getY());
            g.dispose();
            repaint();
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
