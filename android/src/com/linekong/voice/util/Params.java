package com.linekong.voice.util;

import android.media.AudioFormat;

public class Params {
    public static String mGameID = "unknown";
    public static String mUserID = "unknown";
    public static String mServerUrl = "http://img.linekong.com";
    
    public static String fetchUploadUrl(){
        return mServerUrl+"/acceptVoice/index.php";
    }
    // http://img.linekong.com/acceptVoice/index.php?act=sendVoice&file=cqzj_dkfjkdfj_1380681244
    public static String fetchDownloadUrl(String id){
        return mServerUrl+"/acceptVoice/index.php?act=sendVoice&file="+id;
    }
    
    // speex音频相关参数
    public static final int mFrequency = 8000;
    public static final int mFormat    = AudioFormat.ENCODING_PCM_16BIT;
    
    // message相关
    
    /**
     * 新生成的声音文件ID
     */
    public static final int RECORD_ID   = 0x1;
    
    /**
     * 录音线程开启
     */
    public static final int RECORD_START = 0x3;
    
    /**
     * 录音线程关闭
     */
    public static final int RECORD_FINISH = 0x4;
    
    /**
     * 会从网络上抓取下来，再进行播放
     */
    public static final int PLAY_BY_NET = 0x5;
    
    /**
     * 已经有cache文件，播放cache文件
     */
    public static final int PLAY_BY_FILE = 0x6;
    
    /**
     * 开始播放声音
     */
    public static final int PLAY_START = 0x7;
    
    /**
     * 播放声音线程关闭
     */
    public static final int PLAY_FINISH  = 0x8;
    
    /**
     * 上传文件状态
     */
    public static final int UPLOAD_FINISH = 0x9;
    
    /**
     * 下载文件状态
     */
    public static final int DOWNLOAD_FINISH = 0x10;
}
