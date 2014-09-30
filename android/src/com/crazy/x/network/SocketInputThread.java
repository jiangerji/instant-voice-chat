package com.crazy.x.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import android.util.Log;

/**
 * 客户端读消息线程
 * 
 * @author way
 * 
 */
public class SocketInputThread extends Thread {
    private boolean isStart = true;

    private static String TAG = "SocketInputThread";

    public SocketInputThread() {
    }

    public void setStart(boolean isStart) {
        this.isStart = isStart;
    }

    @Override
    public void run() {
        Log.d(TAG, "Start Socket Input Thread!");
        while (isStart) {
            // 手机能联网，读socket数据
            //            if (NetManager.instance().isNetworkConnected())
            //            {
            //
            //                if (!TCPClient.instance().isConnect())
            //                {
            //                    CLog.e(tag,
            //                            "TCPClient connet server is fail read thread sleep second"
            //                                    + Const.SOCKET_SLEEP_SECOND);
            //
            //                    try
            //                    {
            //                        sleep(Const.SOCKET_SLEEP_SECOND * 1000);
            //                    } catch (InterruptedException e)
            //                    {
            //                        // TODO Auto-generated catch block
            //                        e.printStackTrace();
            //                    }
            //                }

            readSocket();

            // 如果连接服务器失败,服务器连接失败，sleep固定的时间，能联网，就不需要sleep

            //                CLog.e("socket", "TCPClient.instance().isConnect() "
            //                        + TCPClient.instance().isConnect());
            //
            //            }
        }
    }

    public void readSocket() {
        Selector selector = SocketClient.instance().getSelector();
        if (selector == null) {
            return;
        }
        try {
            // 如果没有数据过来，一直柱塞
            while (selector.select() > 0) {
                for (SelectionKey sk : selector.selectedKeys()) {
                    // 如果该SelectionKey对应的Channel中有可读的数据
                    if (sk.isReadable()) {
                        // 使用NIO读取Channel中的数据
                        SocketChannel sc = (SocketChannel) sk.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(10240);
                        try {
                            int count = sc.read(buffer);
                            buffer.flip();
                            Log.d(TAG, "Socket read " + count
                                    + "bytes!");

                            SocketDataHandler.getInstance()
                                    .putSocketData(buffer, count);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            // 为下一次读取作准备
                            sk.interestOps(SelectionKey.OP_READ);
                            // 删除正在处理的SelectionKey
                            selector.selectedKeys().remove(sk);
                        } catch (CancelledKeyException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClosedSelectorException e2) {
        }
    }

}
