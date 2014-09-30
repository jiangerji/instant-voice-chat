package com.crazy.x.network;

public class SocketThreadManager {

    private static SocketThreadManager s_SocketManager = null;

    private SocketInputThread mInputThread = null;

    //    private SocketOutputThread mOutThread = null;
    //
    //    private SocketHeartThread mHeartThread = null;

    // 获取单例
    public static SocketThreadManager sharedInstance() {
        if (s_SocketManager == null) {
            s_SocketManager = new SocketThreadManager();
        }
        return s_SocketManager;
    }

    // 单例，不允许在外部构建对象
    private SocketThreadManager() {
        //        mHeartThread = new SocketHeartThread();
        mInputThread = new SocketInputThread();
        //        mOutThread = new SocketOutputThread();
    }

    /**
     * 启动线程
     */

    public void startThreads() {
        //        mHeartThread.start();
        mInputThread.start();
        mInputThread.setStart(true);
        //        mOutThread.start();
        // mDnsthread.start();
    }

    /**
     * stop线程
     */
    public void stopThreads() {
        //        mHeartThread.stopThread();
        mInputThread.setStart(false);
        //        mOutThread.setStart(false);
    }

    public static void releaseInstance() {
        if (s_SocketManager != null) {
            s_SocketManager.stopThreads();
            s_SocketManager = null;
        }
    }

    //    public void sendMsg(byte[] buffer, Handler handler) {
    //        MsgEntity entity = new MsgEntity(buffer, handler);
    //        mOutThread.addMsgToSendList(entity);
    //    }

    public static void main(String[] args) throws Exception {
        SocketThreadManager socketThreadManager = SocketThreadManager.sharedInstance();
        socketThreadManager.startThreads();
    }
}
