//2071141홍민혁
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.ImageIcon;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN		=	0x1;
    public final static int MODE_LOGOUT		=	0x2;
    public final static int MODE_ENTER = 0x4; // 새로운 입장 모드
    public final static int MODE_TX_STRING	=  0x10;
    public final static int MODE_TX_IMAGE	=  0x40;
    public final static int MODE_TX_DRAW	=  0x80;
    public final static int MODE_TX_USER    =  0x99;
    public final static int MODE_TX_ORDER   =  0x100;
    public final static int MODE_TX_START   =  0x200;
    public final static int MODE_TX_CORRECT	=  0x30;
    public final static int MODE_TX_USERSCORE= 0x20;
    public final static int MODE_TX_END		=  0x50;
    public final static int MODE_TX_RESET	=  0x60;

    String userID;
    int mode;
    String message;
    BufferedImage image;
    long size;

    //그림
    Color color;
    float stroke;
    public int x1, y1, x2, y2;
    String shapeString;
    ArrayList<String> users;
    int order;
    Map<String, Integer> userScores;

    public ChatMsg(String userID, int mode, String message, BufferedImage image, long size) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
        this.size = size;
    }

    public ChatMsg(String userID, int mode, int x1, int y1, int x2, int y2, Color color, float stroke, String shapeString) {
        this.userID = userID;
        this.mode = mode;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
        this.stroke = stroke;
        this.shapeString = shapeString;
    }

    public ChatMsg(String userID, int mode, ArrayList<String> users){
        this.userID = userID;
        this.users = users;
        this.mode = mode;
    }

    public ChatMsg(String userID, int code, String message, BufferedImage image) {
        this(userID, code, message, image, 0);
    }

    public ChatMsg(String userID, int code, int order){
        this.userID = userID;
        this.mode = code;
        this.order = order;
    }

    public ChatMsg(String userID, int code, Map<String, Integer> userScores){
        this.userID = userID;
        this.mode = code;
        this.userScores = userScores;
    }

    public ChatMsg(String userID, int code, Map<String, Integer> userScores, int orderIndex){
        this.userID = userID;
        this.mode = code;
        this.userScores = userScores;
        this.order = orderIndex;
    }

    public ChatMsg(String userID, int code) {
        this(userID, code, null, null);
    }

    public ChatMsg(String userID, int code, String message) {
        this(userID, code, message, null);
    }

    public ChatMsg(String userID, int code, BufferedImage image) {
        this(userID, code, null, image );
    }

    public ChatMsg(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size);
    }

}
