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
 * �ͻ��˶���Ϣ�߳�
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
            // �ֻ�����������socket����
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

            // ������ӷ�����ʧ��,����������ʧ�ܣ�sleep�̶���ʱ�䣬���������Ͳ���Ҫsleep

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
            // ���û�����ݹ�����һֱ����
            while (selector.select() > 0) {
                for (SelectionKey sk : selector.selectedKeys()) {
                    // �����SelectionKey��Ӧ��Channel���пɶ�������
                    if (sk.isReadable()) {
                        // ʹ��NIO��ȡChannel�е�����
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
                            // Ϊ��һ�ζ�ȡ��׼��
                            sk.interestOps(SelectionKey.OP_READ);
                            // ɾ�����ڴ����SelectionKey
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
