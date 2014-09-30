package com.crazy.x.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import android.util.Log;

public class SocketClient {
    private final static String TAG = "SocketClient";

    // �ŵ�ѡ����
    private Selector selector;

    // �������ͨ�ŵ��ŵ�
    SocketChannel socketChannel;

    // Ҫ���ӵķ�����Ip��ַ
    private String hostIp;

    // Ҫ���ӵ�Զ�̷������ڼ����Ķ˿�
    private int hostListenningPort;

    private static SocketClient s_Tcp = null;

    public boolean isInitialized = false;

    private final static String SOCKET_SERVER = "localhost";
    private final static int SOCKET_PORT = 9999;

    public static synchronized SocketClient instance() {
        if (s_Tcp == null) {
            s_Tcp = new SocketClient(SOCKET_SERVER, SOCKET_PORT);
        }
        return s_Tcp;
    }

    /**
     * ���캯��
     * 
     * @param HostIp
     * @param HostListenningPort
     * @throws IOException
     */
    public SocketClient(String HostIp, int HostListenningPort) {
        this.hostIp = HostIp;
        this.hostListenningPort = HostListenningPort;

        try {
            initialize();
            this.isInitialized = true;
        } catch (IOException e) {
            this.isInitialized = false;
            e.printStackTrace();
        } catch (Exception e) {
            this.isInitialized = false;
            e.printStackTrace();
        }
    }

    /**
     * ��ʼ��
     * 
     * @throws IOException
     */
    public void initialize() throws IOException {
        boolean done = false;

        try {
            // �򿪼����ŵ�������Ϊ������ģʽ
            socketChannel = SocketChannel.open(new InetSocketAddress(hostIp,
                    hostListenningPort));
            if (socketChannel != null) {
                socketChannel.socket().setTcpNoDelay(false);
                socketChannel.socket().setKeepAlive(true);
                // ���ö�socket��timeoutʱ��
                socketChannel.socket().setSoTimeout(10000);
                socketChannel.configureBlocking(false);

                // �򿪲�ע��ѡ�������ŵ�
                selector = Selector.open();
                if (selector != null) {
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    done = true;
                }
            }
        } finally {
            if (!done && selector != null) {
                selector.close();
            }
            if (!done) {
                socketChannel.close();
            }
        }

        Log.d(TAG, "Socket Client Initialize Finish!");
    }

    static void blockUntil(SelectionKey key, long timeout) throws IOException {

        int nkeys = 0;
        if (timeout > 0) {
            nkeys = key.selector().select(timeout);

        } else if (timeout == 0) {
            nkeys = key.selector().selectNow();
        }

        if (nkeys == 0) {
            throw new SocketTimeoutException();
        }
    }

    /**
     * �����ַ�����������
     * 
     * @param message
     * @throws IOException
     */
    public void sendMsg(String message) throws IOException {
        ByteBuffer writeBuffer = ByteBuffer.wrap(message.getBytes("utf-8"));

        if (socketChannel == null) {
            throw new IOException();
        }
        socketChannel.write(writeBuffer);
    }

    /**
     * ��������
     * 
     * @param bytes
     * @throws IOException
     */
    public void sendMsg(byte[] bytes) throws IOException {
        ByteBuffer writeBuffer = ByteBuffer.wrap(bytes);

        if (socketChannel == null) {
            throw new IOException();
        }
        socketChannel.write(writeBuffer);
    }

    /**
     * 
     * @return
     */
    public synchronized Selector getSelector() {
        return this.selector;
    }

    /**
     * Socket�����Ƿ���������
     * 
     * @return
     */
    public boolean isConnect() {
        boolean isConnect = false;
        if (this.isInitialized) {
            isConnect = this.socketChannel.isConnected();
        }
        return isConnect;
    }

    /**
     * �ر�socket ��������
     * 
     * @return
     */
    public boolean reConnect() {
        closeTCPSocket();

        try {
            initialize();
            isInitialized = true;
        } catch (IOException e) {
            isInitialized = false;
            e.printStackTrace();
        } catch (Exception e) {
            isInitialized = false;
            e.printStackTrace();
        }
        return isInitialized;
    }

    /**
     * �������Ƿ�رգ�ͨ������һ��socket��Ϣ
     * 
     * @return
     */
    public boolean canConnectToServer() {
        try {
            if (socketChannel != null) {
                socketChannel.socket().sendUrgentData(0xff);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * �ر�socket
     */
    public void closeTCPSocket() {
        try {
            if (socketChannel != null) {
                socketChannel.close();
            }

        } catch (IOException e) {

        }
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
        }
    }

    /**
     * ÿ�ζ������ݺ���Ҫ����ע��selector����ȡ����
     */
    public synchronized void repareRead() {
        if (socketChannel != null) {
            try {
                selector = Selector.open();
                socketChannel.register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
