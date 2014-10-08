package com.crazy.x.utils;

public class CommonUtils {

    public static void short2byte(short[] in, byte[] out, int length) {
        for (int i = 0; i < length; i++) {
            out[2 * i + 1] = (byte) ((in[i] & 0x0000FF00) >> 8);
            out[2 * i] = (byte) (in[i] & 0x000000FF);
        }
    }
}
