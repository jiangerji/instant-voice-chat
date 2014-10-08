package com.crazy.x.network;

public class SocketCmdUtils {

    public final static int SPEAKING_START = 0x00020001; // 开始讲话
    public final static int SPEAKING_STOP = 0x00020002; // 结束讲话
    public final static int SPEAKING_CONTENT = 0x00030001; // 语音数据

    public final static int CMD_TYPE_NONE = 0xFFFFFFFF; // 没有命令类型

    public static byte[] buidlCmd(int cmd, byte[] content) {
        byte[] cmdContent = new byte[content.length + 8];

        cmdContent[0] = (byte) (cmd & 0x000000FF);
        cmdContent[1] = (byte) ((cmd & 0x0000FF00) >> 8);
        cmdContent[2] = (byte) ((cmd & 0x00FF0000) >> 16);
        cmdContent[3] = (byte) ((cmd & 0xFF000000) >> 24);

        int length = content.length;
        cmdContent[4] = (byte) (length & 0x000000FF);
        cmdContent[5] = (byte) ((length & 0x0000FF00) >> 8);
        cmdContent[6] = (byte) ((length & 0x00FF0000) >> 16);
        cmdContent[7] = (byte) ((length & 0xFF000000) >> 24);

        System.arraycopy(content, 0, cmdContent, 8, length);
        return cmdContent;
    }

    public static byte[] buidlCmd(int cmd, byte[] content, int count) {
        byte[] cmdContent = new byte[count + 8];

        cmdContent[0] = (byte) (cmd & 0x000000FF);
        cmdContent[1] = (byte) ((cmd & 0x0000FF00) >> 8);
        cmdContent[2] = (byte) ((cmd & 0x00FF0000) >> 16);
        cmdContent[3] = (byte) ((cmd & 0xFF000000) >> 24);

        cmdContent[4] = (byte) (count & 0x000000FF);
        cmdContent[5] = (byte) ((count & 0x0000FF00) >> 8);
        cmdContent[6] = (byte) ((count & 0x00FF0000) >> 16);
        cmdContent[7] = (byte) ((count & 0xFF000000) >> 24);

        System.arraycopy(content, 0, cmdContent, 8, count);
        return cmdContent;
    }

    public static byte[] sendSpeakingStart() {
        return buidlCmd(SPEAKING_START, "Speaking start!".getBytes());
    }

    public static byte[] sendSpeakingStop() {
        return buidlCmd(SPEAKING_STOP, "Stop start!".getBytes());
    }

    public static byte[] sendSpeakingContent(byte[] content, int length) {
        return buidlCmd(SPEAKING_CONTENT, content, length);
    }
}
