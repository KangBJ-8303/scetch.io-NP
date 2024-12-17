//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class ChatMsg implements Serializable {
    public static final int MODE_LOGIN = 0x01;
    public static final int MODE_LOGOUT = 0x02;
    public static final int MODE_ENTER = 0x04;
    public static final int MODE_TX_STRING = 0x16;
    public static final int MODE_TX_IMAGE = 0x64;
    public static final int MODE_TX_DRAW = 0x128;
    public static final int MODE_TX_USER = 0x153;
    public static final int MODE_TX_ORDER = 0x256;
    public static final int MODE_TX_START = 0x512;
    public static final int MODE_TX_CORRECT = 0x48;
    public static final int MODE_TX_USERSCORE = 0x32;
    public static final int MODE_TX_END = 0x80;
    public static final int MODE_TX_RESET = 0x96;
    String userID;
    int mode;
    String message;
    BufferedImage image;
    long size;
    Color color;
    float stroke;
    public int x1;
    public int y1;
    public int x2;
    public int y2;
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

    public ChatMsg(String userID, int mode, ArrayList<String> users) {
        this.userID = userID;
        this.users = users;
        this.mode = mode;
    }

    public ChatMsg(String userID, int code, String message, BufferedImage image) {
        this(userID, code, message, image, 0L);
    }

    public ChatMsg(String userID, int code, int order) {
        this.userID = userID;
        this.mode = code;
        this.order = order;
    }

    public ChatMsg(String userID, int code, Map<String, Integer> userScores) {
        this.userID = userID;
        this.mode = code;
        this.userScores = userScores;
    }

    public ChatMsg(String userID, int code, Map<String, Integer> userScores, int orderIndex) {
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
        this(userID, code, null, image);
    }

    public ChatMsg(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size);
    }
}
