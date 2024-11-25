//2071141홍민혁
import java.awt.image.BufferedImage;
import java.io.Serializable;

import javax.swing.ImageIcon;

public class ChatMsg implements Serializable {
    public final static int MODE_LOGIN		=	0x1;
    public final static int MODE_LOGOUT		=	0x2;
    public final static int MODE_ENTER = 0x4; // 새로운 입장 모드
    public final static int MODE_TX_STRING	=  0x10;
    public final static int MODE_TX_IMAGE	=  0x40;

    String userID;
    int mode;
    String message;
    BufferedImage image;
    long size;

    public ChatMsg(String userID, int code, String message, BufferedImage image, long size) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
    }

    public ChatMsg(String userID, int code, String message, BufferedImage image) {
        this(userID, code, message, image, 0);
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
