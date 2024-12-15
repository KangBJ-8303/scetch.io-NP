import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;


public class Canvas extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    String shapeString = ""; // 도형의 형태를 담는 변수
    Point firstPointer = new Point(0, 0);
    Point secondPointer = new Point(0, 0);
    BufferedImage bufferedImage;
    Color colors = Color.black;
    Float stroke = (float) 5;
    JComboBox<String> colorComboBox;
    JComboBox<Float> strokeComboBox; // float로 설정해주는 이유는 setStroke에서 받는 인자 자료형이 float

    String[] colorNames = {"검정", "빨강", "파랑", "초록", "노랑", "핑크", "마젠타"};
    Color[] colorsArray = {Color.black, Color.red, Color.blue, Color.green, Color.yellow, Color.pink, Color.magenta};

    String uid;
    MainDisplay mainDisplay;

    int width;
    int height;
    int minPointx;
    int minPointy;

    private JPanel toolPanel;

    JButton eraseAllButton = new JButton("전체지우기");
    JButton rectButton = new JButton("네모");
    JButton lineButton = new JButton("선");
    JButton circleButton = new JButton("원");
    JButton penButton = new JButton("펜");
    JButton eraseButton = new JButton("지우개");

    public Canvas(String uid, MainDisplay mainDisplay) {
        this.uid = uid;
        this.mainDisplay = mainDisplay;
        colorComboBox = new JComboBox<>(colorNames);
        strokeComboBox = new JComboBox<Float>();
        toolPanel = createToolPanel();
        notOrder();
    }

    private void Order(){
        eraseAllButton.setEnabled(true);
        rectButton.setEnabled(true);
        lineButton.setEnabled(true);
        circleButton.setEnabled(true);
        penButton.setEnabled(true);
        eraseButton.setEnabled(true);
    }

    private void notOrder(){
        eraseAllButton.setEnabled(false);
        rectButton.setEnabled(false);
        lineButton.setEnabled(false);
        circleButton.setEnabled(false);
        penButton.setEnabled(false);
        eraseButton.setEnabled(false);
    }

    private JPanel createToolPanel() {
        JPanel panel = new JPanel(new GridLayout(2,6));

        strokeComboBox.setModel(new DefaultComboBoxModel<Float>(
                new Float[] { (float) 5, (float) 10, (float) 15, (float) 20, (float) 25 }));

        add(eraseAllButton);
        add(penButton);
        add(lineButton);
        add(rectButton);
        add(circleButton);
        add(colorComboBox);
        add(strokeComboBox);
        add(eraseButton);

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
        bufferedImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        setImageBackground(bufferedImage); // save 할 때 배경이 default로 black이여서 흰색으로
        add(panel, BorderLayout.SOUTH);

        addMouseListener(this);
        addMouseMotionListener(this);

        return panel;
    }

    public void updateToolVisibility(){
        if(mainDisplay.getCurrentDrawer().equals(mainDisplay.getUid())){
            Order();
        }
        else {
            notOrder();
        }

    }

    public void setShapeString() {
        shapeString = "";
    }

    public void drawing(int firstX, int firstY, int secondX, int secondY, Color colors, float stroke, String shapeString) {

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

    public void setClean() {
        setImageBackground(bufferedImage);
    }

    public void mousePressed(MouseEvent e) {
        // 다시 클릭됐을경우 좌표 초기화
        firstPointer.setLocation(0, 0);
        secondPointer.setLocation(0, 0);

        firstPointer.setLocation(e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {

        if (shapeString != "펜") {
            secondPointer.setLocation(e.getX(), e.getY());
            updatePaint();
            firstPointer.setLocation(0, 0);
            secondPointer.setLocation(0, 0);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().getClass().toString().contains("JButton")) {
            shapeString = e.getActionCommand();
        }
        else if (e.getSource().equals(colorComboBox)) {
            int selectedIndex = colorComboBox.getSelectedIndex();
            colors = colorsArray[selectedIndex]; // 선택된 색상 인덱스에 따라 색상 설정
        }
        else if (e.getSource().equals(strokeComboBox)) {
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

    public void setImageBackground(BufferedImage bi) {
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
}// Class dotButton
