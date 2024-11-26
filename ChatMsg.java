import java.io.Serializable;
import javax.swing.ImageIcon;

public class ChatMsg implements Serializable {
    public static final int MODE_LOGIN = 0x1;
    public static final int MODE_LOGOUT = 0x2;
    public static final int MODE_TX_STRING = 0x10;
    public static final int MODE_TX_FILE = 0x20;
    public static final int MODE_TX_IMAGE = 0x40;
    public static final int MODE_TX_CANVAS = 0x80;


    String userID;
    int mode;
    String message;
    ImageIcon image;
    byte[] canvasImageBytes;

    public ChatMsg(String userID, int mode) {
        this.userID = userID;
        this.mode = mode;
    }

    public ChatMsg(String userID, int mode, String message) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
    }

    public ChatMsg(String userID, int mode, String message, ImageIcon image) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
    }

    public ChatMsg(String userID, int mode, byte[] canvasImageBytes) {
        this.userID = userID;
        this.mode = mode;
        this.canvasImageBytes = canvasImageBytes;
    }
}
