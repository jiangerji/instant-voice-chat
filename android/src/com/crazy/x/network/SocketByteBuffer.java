package com.crazy.x.network;
import java.nio.ByteBuffer;

public class SocketByteBuffer {

    private byte[] mBuffer = null;

    // δ��ʹ�õ�buffer��ʼ��λ��
    private int mOffset = 0;

    // ����buffer�ĳ���
    private int mLength = 0;

    public SocketByteBuffer(ByteBuffer buffer, int count) {
        mBuffer = new byte[count];
        mLength = mBuffer.length;
        buffer.get(mBuffer, 0, mLength);
    }

    /**
     * ��ǰδ��ȡ��buffer�Ĵ�С
     * 
     * @return
     */
    public int remainSize() {
        return mLength - mOffset;
    }

    /**
     * ��ǰbuffer�Ĵ�С
     * 
     * @return
     */
    public int size() {
        return mLength;
    }

    /**
     * buffer���ã���ʾbuffer�е����ݶ�����Ч
     */
    public void reset() {
        mOffset = mLength;
    }

    /**
     * ���ص�ǰbuffer����������
     * 
     * @return
     */
    public byte[] array() {
        return mBuffer;
    }

    /**
     * ����buffer���ֽ�
     * 
     * @param size
     */
    public void consume(int size) {
        mOffset += size;
    }

    /**
     * ��buffer�е�count�ֽڸ��Ƶ�dest��
     * 
     * @param dest
     * @param count
     */
    public void copyTo(byte[] dest, int count) {
        System.arraycopy(mBuffer, mOffset, dest, 0, count);
    }

    /**
     * ��buffer�е�count�ֽڸ��Ƶ�dest offset��ʼ��λ��
     * 
     * @param dest
     * @param offset
     * @param count
     */
    public void copyTo(byte[] dest, int offset, int count) {
        System.arraycopy(mBuffer, mOffset, dest, offset, count);
    }

}
