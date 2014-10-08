package com.crazy.x.network;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 客户端写消息线程
 * 
 * @author way
 * 
 */
public class SocketOutputThread extends Thread
{
    private boolean isStart = true;
    private static String TAG = "SocketOutputThread";

    private Queue<SocketByteBuffer> sendMsgList;

    public SocketOutputThread() {
        sendMsgList = new LinkedList<SocketByteBuffer>();
    }

    public void setStart(boolean isStart) {
        this.isStart = isStart;
        synchronized (this) {
            notify();
        }
    }

    // 使用socket发送消息
    public boolean sendMsg(byte[] msg) throws Exception {
        if (msg == null) {
            return false;
        }

        try {
            SocketClient.instance().sendMsg(msg);
        } catch (Exception e) {
            throw (e);
        }

        return true;
    }

    // 使用socket发送消息
    public void addMsgToSendList(byte[] msg) {
        synchronized (this) {
            SocketByteBuffer socketByteBuffer = new SocketByteBuffer(msg,
                    0, msg.length);
            this.sendMsgList.offer(socketByteBuffer);
            notify();
        }
    }

    @Override
    public void run() {
        while (isStart) {
            // 锁发送list
            synchronized (sendMsgList) {
                // 发送消息
                while (sendMsgList.size() > 0) {
                    try {
                        byte[] buf = sendMsgList.poll().array();
                        sendMsg(buf);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //                for (SocketByteBuffer msg : sendMsgList) {
                //
                //                    try {
                //                        sendMsg(msg.array());
                //                        sendMsgList.remove(msg);
                //                        // 成功消息，通过hander回传
                //                    } catch (Exception e) {
                //                        e.printStackTrace();
                //                        // 错误消息，通过hander回传
                //                    }
                //                }
            }

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }// 发送完消息后，线程进入等待状态
            }
        }

    }
}
