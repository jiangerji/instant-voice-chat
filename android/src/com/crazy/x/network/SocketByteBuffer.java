package com.crazy.x.network;
import java.nio.ByteBuffer;

public class SocketByteBuffer {

    private byte[] mBuffer = null;

    // 未被使用的buffer开始的位置
    private int mOffset = 0;

    // 整个buffer的长度
    private int mLength = 0;

    public SocketByteBuffer(ByteBuffer buffer, int count) {
        mBuffer = new byte[count];
        mLength = mBuffer.length;
        buffer.get(mBuffer, 0, mLength);
    }

    /**
     * 当前未读取的buffer的大小
     * 
     * @return
     */
    public int remainSize() {
        return mLength - mOffset;
    }

    /**
     * 当前buffer的大小
     * 
     * @return
     */
    public int size() {
        return mLength;
    }

    /**
     * buffer重置，表示buffer中的数据都已无效
     */
    public void reset() {
        mOffset = mLength;
    }

    /**
     * 返回当前buffer中所有数据
     * 
     * @return
     */
    public byte[] array() {
        return mBuffer;
    }

    /**
     * 消费buffer中字节
     * 
     * @param size
     */
    public void consume(int size) {
        mOffset += size;
    }

    /**
     * 将buffer中的count字节复制到dest中
     * 
     * @param dest
     * @param count
     */
    public void copyTo(byte[] dest, int count) {
        System.arraycopy(mBuffer, mOffset, dest, 0, count);
    }

    /**
     * 将buffer中的count字节复制到dest offset开始的位置
     * 
     * @param dest
     * @param offset
     * @param count
     */
    public void copyTo(byte[] dest, int offset, int count) {
        System.arraycopy(mBuffer, mOffset, dest, offset, count);
    }

}
