package com.crazy.x.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SocketDataHandler {

    private static SocketDataHandler _instance = null;

    public static SocketDataHandler getInstance() {
        if (_instance == null) {
            _instance = new SocketDataHandler();
        }

        return _instance;
    }

    private SocketDataHandler() {
    }

    /**
     * �����յ���socket���ݷ��뵽���ݴ��������н�������
     * 
     * @param buffer
     *            ���յ���socket����
     */
    public void putSocketData(ByteBuffer buffer) {
        synchronized (mBuffers) {

            if (buffer != null && buffer.remaining() > 0) {
                mBuffers.add(buffer);
                mPreviousBufferSize += buffer.remaining();

                /**
                 * while len(self.previousData) >= 8:
                 * cmdType, contentLength = struct.unpack("2I",
                 * self.previousData[0:8])
                 * if not self.handleData(cmdType, contentLength):
                 * break
                 */
                byte[] cmdHeader = getHeader();
                int cmdType = 0xFFFFFFFF;
                int contentLength = 0;
                while (cmdHeader != null) {
                    cmdType = (cmdHeader[0] << 3) | (cmdHeader[1] << 2)
                            | (cmdHeader[2] << 1) | cmdHeader[3];
                    contentLength = (cmdHeader[4] << 3) | (cmdHeader[5] << 2)
                            | (cmdHeader[6] << 1) | cmdHeader[7];

                    System.out.println(String.format("Cmd Type: %08x", cmdType));
                    System.out.println(String.format("Content Length: %010d",
                            contentLength));

                    cmdHeader = getHeader();
                }
            }
        }
    }

    /**
     * ��ȡ�����ͷ����Ϣ
     * 
     * @return
     */
    private byte[] getHeader() {
        return getByte(8);
    }

    private byte[] getByte(int size) {
        byte[] result = null;
        if (mPreviousBufferSize >= size) {
            result = new byte[8];
            int count = 0;
            for (ByteBuffer buffer : mBuffers) {
                byte[] temp = buffer.array();
                if (count + buffer.remaining() >= size) {
                    System.arraycopy(temp, 0, result, count, size - count);
                }
            }
        }
        return result;
    }

    private ArrayList<ByteBuffer> mBuffers = new ArrayList<ByteBuffer>();
    private int mPreviousBufferSize = 0;

    /**
     * �����е����ݽ��д���
     * 
     * @param cmdType
     *            ��������
     * @param contentLength
     *            ������Я�����ݳ���
     */
    private void handleData(int cmdType, int contentLength) {

    }
}
