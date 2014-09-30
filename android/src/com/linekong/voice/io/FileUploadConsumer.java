package com.linekong.voice.io;

import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.linekong.voice.core.Speex;
import com.linekong.voice.ogg.OggSpeexWriter;
import com.linekong.voice.util.ByteBuffer;
import com.linekong.voice.util.CacheFile;
import com.linekong.voice.util.Params;

public class FileUploadConsumer extends Consumer {
    private final static String TAG = "FileUploadConsumer";
    
    FileOutputStream mFileOutputStream = null;
    private String mAudioID;
    private Handler mHandler;
    
    public FileUploadConsumer(Handler handler, int type) throws IOException {
        mAudioID = Params.mGameID + "_" + Params.mUserID + "_" + System.currentTimeMillis();
        mHandler = handler;
        // 作为测试使用
//        mFileOutputStream = CacheFile.openCacheWriteStream(mAudioID+".spx", type);
        
        initOggWrite(CacheFile.openCacheWriteStream(mAudioID, type));
    }

    @Override
    public int doProcess(ByteBuffer byteBuffer) {
        try {
            if (mFileOutputStream != null) {
                mFileOutputStream.write(byteBuffer.mData, 0, byteBuffer.mSize);
            }
            
            writeTag(byteBuffer.mData, byteBuffer.mSize);
        } catch (IOException e) {
            Log.v(TAG, "Consumer doProcess exception:"+e.toString());
        }
        
        return 0;
    }

    @Override
    public void doFinish() {
        try {
            Log.v(TAG, "FileOutputStream: doFinish");
            if (mFileOutputStream != null){
                mFileOutputStream.close();
            }
            
            // ogg
            speexWriter.close();
            
            // TODO: 如果音频录制时间太短，丢弃掉，目前写死为100ms
            // 通知应用录音进程完毕
            Message message;
            long size = CacheFile.getFileSize(mAudioID);
            if (size > 100){
                message = mHandler.obtainMessage();
                message.what = Params.RECORD_FINISH;
                message.arg1 = Math.round(size/(50.f*Speex.mDecodeSize) + 0.5f);
                message.obj = mAudioID;
            
                mHandler.sendMessage(message);
            } else {
                mHandler.sendEmptyMessage(Params.RECORD_FINISH);
            }
            
            Log.v(TAG, "sendMessage doFinish:"+Params.RECORD_FINISH);
            
            // TODO: 开始上传speex编码文件
            int response = CacheFile.doUpload(mAudioID);
            message = mHandler.obtainMessage(Params.UPLOAD_FINISH);
            message.arg1 = response;
            message.obj = mAudioID;
            mHandler.sendMessage(message);
            Log.v(TAG, "sendMessage UPLOAD_FINISH:"+Params.UPLOAD_FINISH+" "+mHandler);
        } catch (IOException e) {
            Log.v(TAG, "FileUpload Consumer doFinish:"+e.toString());
        }
    }
    
    public String getAudioID(){
        return mAudioID;
    }
    
    @Override
    public String getTag() {
        return TAG;
    }
    
    // OGG head support
    OggSpeexWriter speexWriter = null;
    private void initOggWrite(FileOutputStream fos){
        speexWriter = new OggSpeexWriter(0, Params.mFrequency, 1, 1, false);
        try {
            speexWriter.open(fos);
            speexWriter.writeHeader("Encoded with:test by gauss ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void writeTag(byte[] buf, int size) {
//        Log.d(TAG, "here should be:===========================640,actual=" + size);
        try {
            speexWriter.writePacket(buf, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
