package com.crazy.x.network;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.util.Log;

import com.crazy.x.audio.XPlayer;

public class SocketDataHandler {
    private final static String TAG = "SocketDataHandler";

    private static SocketDataHandler _instance = null;

    public static SocketDataHandler getInstance() {
        if (_instance == null) {
            _instance = new SocketDataHandler();
        }

        return _instance;
    }

    private SocketDataHandler() {
    }

    private ArrayList<SocketByteBuffer> mBuffers = new ArrayList<SocketByteBuffer>();
    private int mPreviousBufferSize = 0;

    public void putSocketData(ByteBuffer buffer, int count) {
        synchronized (this) {
            if (buffer != null && count > 0) {
                // buffer不为空
                SocketByteBuffer socketByteBuffer = new SocketByteBuffer(buffer,
                        count);
                mBuffers.add(socketByteBuffer);
                mPreviousBufferSize += count;

                byte[] cmdHeader = getHeader();
                int cmdType = 0xFFFFFFFF;
                int contentLength = 0;
                while (cmdHeader != null) {
                    cmdType = ((cmdHeader[3] & 0x000000FF) << 24)
                            | ((cmdHeader[2] & 0x000000FF) << 16)
                            | ((cmdHeader[1] & 0x000000FF) << 8)
                            | (cmdHeader[0] & 0x000000FF);

                    contentLength = ((cmdHeader[7] & 0x000000FF) << 24)
                            | ((cmdHeader[6] & 0x000000FF) << 16)
                            | ((cmdHeader[5] & 0x000000FF) << 8)
                            | (cmdHeader[4] & 0x000000FF);

                    if (!handleData(cmdType, contentLength)) {
                        break;
                    }

                    cmdHeader = getHeader();
                }
            }
        }
    }

    /**
     * 获取命令的头部信息
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
            for (SocketByteBuffer buffer : mBuffers) {
                if (count + buffer.remainSize() > size) {
                    buffer.copyTo(result, size - count);
                    count = size;
                    break;
                } else {
                    buffer.copyTo(result, buffer.remainSize());
                    count += buffer.remainSize();
                }
            }
        }
        return result;
    }

    private byte[] popByte(int size) {
        byte[] result = null;
        Log.d(TAG, "Pop byte: " + size);

        if (mPreviousBufferSize >= size) {
            result = new byte[size];
            int count = 0;

            // 已经被使用的buffer对象，需要被清楚
            ArrayList<SocketByteBuffer> clearBuffer = new ArrayList<SocketByteBuffer>();

            int remainSize = 0;
            for (SocketByteBuffer buffer : mBuffers) {
                remainSize = buffer.remainSize();
                if (count + remainSize > size) {
                    int copySize = size - count;
                    buffer.copyTo(result, count, copySize);
                    count += copySize;
                    buffer.consume(copySize);
                    break;
                } else {
                    buffer.copyTo(result, count, remainSize);
                    count += remainSize;
                    buffer.consume(remainSize);

                    clearBuffer.add(buffer);
                }
            }

            for (SocketByteBuffer buffer : clearBuffer) {
                mBuffers.remove(buffer);
            }

            mPreviousBufferSize -= size;
        }

        return result;
    }

    private int mContentLength = 0;

    private FileOutputStream mFileOutputStream = null;

    private XPlayer mXPlayer = null;

    /**
     * 对现有的数据进行处理
     * 
     * @param cmdType
     *            命令类型
     * @param contentLength
     *            该命令携带内容长度
     */
    private boolean handleData(int cmdType, int contentLength) {
        boolean result = false;
        int totalLenght = 8 + contentLength;
        if (mPreviousBufferSize >= totalLenght) {
            try {
                Log.d(TAG, "=====================================");
                Log.d(TAG, String.format("Cmd Type: 0x%08x",
                        cmdType));
                Log.d(TAG, String.format("Content Length: %10dbytes",
                        contentLength));

                byte[] totalCmd = popByte(totalLenght);
                Log.d(TAG, "Handle Data Length: " + totalCmd.length
                        + " " + mContentLength);
                switch (cmdType) {
                case SocketCmdUtils.SPEAKING_START:
                    Log.d(TAG, "Speaking start:"
                            + new String(totalCmd, 8, totalCmd.length - 8));
                    mContentLength = 0;
                    result = true;

                    mFileOutputStream = new FileOutputStream("/sdcard/b.pcm");
                    mXPlayer = new XPlayer();
                    mXPlayer.startPlay();
                    break;

                case SocketCmdUtils.SPEAKING_STOP:
                    Log.d(TAG, "Speaking stop:"
                            + new String(totalCmd, 8, totalCmd.length - 8));
                    result = true;
                    if (mFileOutputStream != null) {
                        mFileOutputStream.close();
                    }
                    mXPlayer.finish();
                    break;

                case SocketCmdUtils.SPEAKING_CONTENT:
                    mContentLength += contentLength;
                    result = true;
                    mFileOutputStream.write(totalCmd, 8, totalLenght - 8);
                    mXPlayer.putData(totalCmd, 8, totalLenght - 8);
                    break;

                default:
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

}
