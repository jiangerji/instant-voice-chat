package com.linekong.voice.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.linekong.voice.VoiceManager;

public class CacheFile {
    public static final String TAG = "Cache";

    /**
     * 将short数组转换为字节数组
     * @param in        需要转换的字节数组
     * @param in        用於保存轉換后的字節數組
     * @param length    short数组的长度
     * @return  
     */
    public static void short2byte(short[] in, byte[] out, int length) {
        for (int i = 0; i < length; i++) {
            out[2 * i + 1] = (byte) ((in[i] & 0xFF00) >> 8);
            out[2 * i] = (byte) (in[i] & 0xFF);
        }
    }

    public static final int UPLOAD_FAILED_NETWORK_ERROR = -1;
    public static final int UPLOAD_FAILED_NO_CACHE_FILE = -2;
    public static final int UPLOAD_FAILED_UNKNOWN = -3;
    public static final int UPLOAD_SUCCESS = 0;

    public static int doUpload(String audioID) {
        String uploadUrl = Params.fetchUploadUrl();

        File file = CacheFile.openCacheFile(audioID);
        int response = UPLOAD_FAILED_NO_CACHE_FILE;
        if (file != null) {

            try {
                String responseStr = FileUpload.doUpload(uploadUrl, null, audioID, file);
                response = Integer.parseInt(responseStr);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                response = UPLOAD_FAILED_UNKNOWN;
            } catch (IOException e) {
                e.printStackTrace();
                response = UPLOAD_FAILED_UNKNOWN;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                response = UPLOAD_FAILED_UNKNOWN;
            }
            Log.v(TAG, "Upload response: " + response);
        }

        return response;
    }

    public static final int DOWNLOAD_SUCCESS = 0;
    public static final int DOWNLOAD_FAILED_NETWORD = -1;
    public static final int DOWNLOAD_FAILED_UNKNOWN = -2;

    public static int doDownload(String audioID, int type) {
        String downloadUrl = Params.fetchDownloadUrl(audioID);
        int response = DOWNLOAD_SUCCESS;

        HttpGet httpGet = new HttpGet(downloadUrl);

        try {
            BasicHttpParams httpParameters = new BasicHttpParams();// Set the timeout in milliseconds until a connection is established.  
            HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);// Set the default socket timeout (SO_TIMEOUT) // in milliseconds which is the timeout for waiting for data.  
            HttpConnectionParams.setSoTimeout(httpParameters, 5000);

            HttpResponse httpResponse = new DefaultHttpClient(httpParameters).execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = httpResponse.getEntity();
                entity.writeTo(openCacheWriteStream(audioID, type));
            } else {
                Log.v(TAG, "http response is "
                        + httpResponse.getStatusLine().getStatusCode());
                response = DOWNLOAD_FAILED_NETWORD;
            }

        } catch (ClientProtocolException e) {
            response = DOWNLOAD_FAILED_UNKNOWN;
            e.printStackTrace();
        } catch (IOException e) {
            response = DOWNLOAD_FAILED_UNKNOWN;
            e.printStackTrace();
        }

        return response;
    }

    private static String mExtCacheDir = Environment.getExternalStorageDirectory().getPath()
            + "/lk/cache/audio/retain";
    private static String mExtTmpCacheDir = Environment.getExternalStorageDirectory().getPath()
            + "/lk/cache/audio/tmp";
    private static String mDataCacheDir = "lk_speech_cache";
    private static String mDataCacheTmpDir = "lk_speech_tmp_cache";

    public static void init() {
        File dir = new File(mExtCacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        dir = new File(mExtTmpCacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        clearTmpCache();
    }

    public static File openCacheFile(String filename) {
        File file = null;
        Context context = null;

        if (VoiceManager.getInstance() != null) {
            context = VoiceManager.getInstance().getContext();
        }

        if (context != null) {
            file = new File(mExtCacheDir, filename);
            if (!file.exists()) {
                file = new File(mExtTmpCacheDir, filename);
                if (!file.exists()) {
                    File dir = context.getDir(mDataCacheDir, Context.MODE_PRIVATE);
                    file = new File(dir, filename);

                    if (!file.exists()) {
                        dir = context.getDir(mDataCacheTmpDir, Context.MODE_PRIVATE);
                        file = new File(dir, filename);
                        if (!file.exists()) {
                            file = null;
                        }
                    }
                }
            }
        }

        return file;
    }

    public static FileInputStream openCacheReadStream(String filename) {
        FileInputStream fis = null;
        Context context = null;

        if (VoiceManager.getInstance() != null) {
            context = VoiceManager.getInstance().getContext();
        }

        File file = null;
        if (context != null) {
            try {
                file = new File(mExtCacheDir, filename);
                if (file.exists()) {
                    fis = new FileInputStream(file);
                } else {
                    fis = new FileInputStream(new File(mExtTmpCacheDir,
                            filename));
                }
            } catch (Exception e) {
                // 在SD card上没有找到相应的cache文件，在私有文件中找
                Log.v(TAG, "Not found cache file in SD card!");
                try {
                    File dir = context.getDir(mDataCacheDir, Context.MODE_PRIVATE);
                    file = new File(dir, filename);
                    if (file.exists()) {
                        fis = new FileInputStream(file);
                    } else {
                        dir = context.getDir(mDataCacheTmpDir, Context.MODE_PRIVATE);
                        fis = new FileInputStream(new File(dir, filename));
                    }
                } catch (FileNotFoundException e1) {
                    // 没有cache文件
                    Log.v(TAG, "Not found cache file in data dir!" + filename);
                }
            }
        }

        return fis;
    }

    // for ogg
    public static String getCacheFilePath(String filename, int type) {
        String dirName;
        if (type == 0) {
            // 临时文件，需要被删除逇
            dirName = mExtTmpCacheDir;
        } else {
            dirName = mExtCacheDir;
        }

        File folder = new File(dirName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return dirName + File.separatorChar + filename;
    }

    public static FileOutputStream openCacheWriteStream(String filename,
            int type) {
        FileOutputStream fos = null;
        Context context = null;

        if (VoiceManager.getInstance() != null) {
            context = VoiceManager.getInstance().getContext();
        }

        if (context != null) {
            String dirName;
            if (type == 0) {
                // 临时文件，需要被删除逇
                dirName = mExtTmpCacheDir;
            } else {
                dirName = mExtCacheDir;
            }

            File folder = new File(dirName);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            try {
                fos = new FileOutputStream(new File(folder, filename));
                //                Log.v(TAG, "Write file to "+folder.getAbsolutePath()+"/"+filename);
            } catch (Exception e) {
                Log.v(TAG, "SD Card is unavailable!");
                if (type == 0) {
                    dirName = mDataCacheTmpDir;
                } else {
                    dirName = mDataCacheDir;
                }

                // 将缓存放到私有目录中
                try {
                    File dir = context.getDir(dirName, Context.MODE_PRIVATE);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    fos = new FileOutputStream(new File(dir, filename));
                } catch (FileNotFoundException e1) {
                    Log.v(TAG, "Open Cache file failed!");
                    fos = null;
                }
            }
        }

        return fos;
    }

    public static boolean deleteCacheFile(String filename) {
        boolean result = true;

        Context context = null;
        if (VoiceManager.getInstance() != null) {
            context = VoiceManager.getInstance().getContext();
        }

        File file = null;
        if (context != null) {
            File dir = context.getDir(mDataCacheDir, Context.MODE_PRIVATE);
            file = new File(dir, filename);
            result = file.delete();
        }

        file = new File(mExtCacheDir, filename);
        if (file.exists()) {
            result = file.delete();
        }

        file = new File(mExtTmpCacheDir, filename);
        if (file.exists()) {
            result = file.delete();
        }

        return result;
    }

    /**
     * 删除不需要保存的临时语音文件
     */
    public static void clearTmpCache() {
        // 清除data目录下的文件
        Context context = null;

        if (VoiceManager.getInstance() != null) {
            context = VoiceManager.getInstance().getContext();
        }

        File dir;
        if (context != null) {
            dir = context.getDir(mDataCacheTmpDir, Context.MODE_PRIVATE);
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
            }
        }

        dir = new File(mExtTmpCacheDir);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }

    public static void clearRecord(String audioId, int type) {
        String dirName = mExtCacheDir;
        File file = null;
        if (type == 0) {
            dirName = mExtTmpCacheDir;
            file = new File(dirName, audioId);

            if (file.exists()) {
                file.delete();
            } else {
                if (VoiceManager.getInstance() != null) {
                    Context context = VoiceManager.getInstance().getContext();

                    dirName = mDataCacheDir;
                    if (type == 0) {
                        dirName = mDataCacheTmpDir;
                    }

                    File dir = context.getDir(dirName, Context.MODE_PRIVATE);
                    file = new File(dir, audioId);
                    file.delete();
                }
            }
        }
    }

    public static boolean clearCache() {
        boolean result = true;

        // 清除data目录下的文件
        Context context = null;

        if (VoiceManager.getInstance() != null) {
            context = VoiceManager.getInstance().getContext();
        }

        if (context != null) {
            File dir = context.getDir(mDataCacheDir, Context.MODE_PRIVATE);
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
            }

            dir = context.getDir(mDataCacheTmpDir, Context.MODE_PRIVATE);
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
            }
        }

        // 删除SD card上缓存文件
        File dir = new File(mExtCacheDir);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }

        dir = new File(mExtTmpCacheDir);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }

        return result;
    }

    public static long getFileSize(String filename) {
        long size = 0;

        File file = new File(mExtCacheDir, filename);
        if (file.exists() && file.isFile()) {
            size = file.length();
        } else {
            file = new File(mExtTmpCacheDir, filename);
            if (file.exists() && file.isFile()) {
                size = file.length();
            } else {
                Context context = null;

                if (VoiceManager.getInstance() != null) {
                    context = VoiceManager.getInstance().getContext();
                }

                if (context != null) {
                    File dir = context.getDir(mDataCacheDir, Context.MODE_PRIVATE);
                    file = new File(dir, filename);
                    if (file.exists() && file.isFile()) {
                        size = file.length();
                    } else {
                        dir = context.getDir(mDataCacheTmpDir, Context.MODE_PRIVATE);
                        file = new File(dir, filename);
                        if (file.exists() && file.isFile()) {
                            size = file.length();
                        }
                    }
                }
            }
        }

        //        Log.v(TAG, filename+" size is "+size);
        return size;
    }

}
