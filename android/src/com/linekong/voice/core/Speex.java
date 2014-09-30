package com.linekong.voice.core;

public class Speex {
    static Speex _instance = null;

    /* quality 
     * 1 : 4kbps (very noticeable artifacts, usually intelligible) 
     * 2 : 6kbps (very noticeable artifacts, good intelligibility) 
     * 4 : 8kbps (noticeable artifacts sometimes) 
     * 6 : 11kpbs (artifacts usually only noticeable with headphones) 
     * 8 : 15kbps (artifacts not usually noticeable) 
     */  
    private static final int DEFAULT_COMPRESSION = 4;
    
    public Speex(){
        
    }
    
    // 默认值,在encode后可以得到这个比例
    public static int mEncodeSize = 320;
    public static int mDecodeSize = 20;
    
    public synchronized static void init(int level){
        if (_instance == null) {
            System.loadLibrary("audio");
            _instance = new Speex();
        }
        
        if (level < 1 || level > 8){
            level = DEFAULT_COMPRESSION;
        }
        _instance.open(level);
    }
    
    public synchronized static void setLevel(int level){
        if (_instance != null) {
            _instance.close();
            _instance.open(level);
        }
    }
    
    public synchronized static void deinit(){
        if (_instance != null) {
            _instance.close();
        }
        
        _instance = null;
    }
    
    public synchronized static Speex getInstance() {
        return _instance;
    }
    
    /**
     * 初始化Speex处理音频的质量
     * @param compression
     * @return
     */
    public native int open(int compression);
    public native int getEncodeFrameSize();
    public native int getDecodeFrameSize();
    
    /**
     * 
     * @param encoded
     * @param lin
     * @param size
     * @return
     */
    public native int decode(byte encoded[], short lin[], int size);
    
    /**
     * 
     * @param lin
     * @param offset
     * @param encoded
     * @param size
     * @return
     */
    public native int encode(short lin[], int offset, byte encoded[], int size);
    
    /**
     * 释放speex资源
     */
    public native void close();
}
