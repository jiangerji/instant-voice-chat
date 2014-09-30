package com.linekong.voice.util;

public class DataBuffer {
    public int mSize;
    public short[] mData = null;
    
    /**
     * 保存data bufer
     * @param size
     */
    public DataBuffer(int size) {
        mSize = size;
        mData = new short[size];
    }
}
