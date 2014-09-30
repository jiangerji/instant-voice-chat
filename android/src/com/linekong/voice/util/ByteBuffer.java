package com.linekong.voice.util;

public class ByteBuffer {
    public int mSize;
    public byte[] mData = null;
    
    public ByteBuffer(int size) {
        mSize = size;
        mData = new byte[size];
    }
}
