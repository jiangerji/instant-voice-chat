package com.crazy.x.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * �ͻ��˶���Ϣ�߳�
 * 
 * @author way
 * 
 */
public class SocketInputThread extends Thread {
    private boolean isStart = true;

    private static String tag = "socket";

    // private MessageListener messageListener;// ��Ϣ�����ӿڶ���

    public SocketInputThread() {
    }

    public void setStart(boolean isStart) {
        this.isStart = isStart;
    }

    @Override
    public void run() {
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
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        try {
                            sc.read(buffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        buffer.flip();
                        SocketDataHandler.getInstance().putSocketData(buffer);
                        //                        String receivedString = "";
                        //                        // ��ӡ�յ�������
                        //                        try {
                        //                            receivedString = Charset.forName("UTF-8")
                        //                                    .newDecoder().decode(buffer).toString();
                        //                        } catch (CharacterCodingException e) {
                        //                            e.printStackTrace();
                        //                        }
                        //                        System.out.println("reveive:" + receivedString);
                        //                        buffer.clear();
                        //                        buffer = null;

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
            setStart(false);
        }
    }

}
