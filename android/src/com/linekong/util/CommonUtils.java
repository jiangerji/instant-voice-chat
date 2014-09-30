package com.linekong.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;

@SuppressLint("SetJavaScriptEnabled")
public class CommonUtils {
    private final static String TAG = "CommonUtils";

    private static class WebViewWrapper extends WebView {
        public WebViewWrapper(Context context, String url) {
            super(context);

            getSettings().setJavaScriptEnabled(true);
            getSettings().setUseWideViewPort(true);
            getSettings().setLoadWithOverviewMode(true);

            setWebViewClient(new WebViewClient());
            loadUrl(url);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();

            if (action == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_BACK) {
                if (canGoBack() && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                        && event.getRepeatCount() == 0) {
                    goBack();
                    return true;
                }
            }

            return super.dispatchKeyEvent(event);
        }

    }

    private static class WebViewDialog extends Dialog {
        private String mUrl = null;
        private int mWidth = LayoutParams.MATCH_PARENT;
        private int mHeight = LayoutParams.MATCH_PARENT;

        public WebViewDialog(Activity activity, String url) {
            super(activity,
                    android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            setOwnerActivity(activity);
            mUrl = url;
        }

        public WebViewDialog(Activity activity, String url, int width,
                int height) {
            super(activity,
                    android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
            setOwnerActivity(activity);
            mUrl = url;

            mWidth = width;
            mHeight = height;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setCancelable(true);

            RelativeLayout root = new RelativeLayout(getOwnerActivity());
            root.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            WebView webView = new WebViewWrapper(getOwnerActivity(), mUrl);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    mWidth, mHeight);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            webView.setLayoutParams(layoutParams);
            root.addView(webView);
            webView.setId(0x111222);
            Log.d(TAG, "webview width=" + mWidth + ", height=" + mHeight);

            // add close button
            ImageView closeBtn = new ImageView(getOwnerActivity());
            RelativeLayout.LayoutParams closeBtnLayoutParams = new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            closeBtnLayoutParams.addRule(RelativeLayout.ALIGN_TOP, 0x111222);
            closeBtnLayoutParams.addRule(RelativeLayout.ALIGN_RIGHT, 0x111222);
            closeBtn.setLayoutParams(closeBtnLayoutParams);
            closeBtn.setPadding(20, 20, 20, 20);
            root.addView(closeBtn);

            Bitmap bitmap = BitmapFactory.decodeByteArray(CLOSE_PNG, 0, CLOSE_PNG.length);
            closeBtn.setImageBitmap(bitmap);

            closeBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            setContentView(root);
        }
    }

    /**
     * 全屏显示一个webview界面
     * 
     * @param activity  需要显示webview的activity
     * @param url   显示url地址
     */
    public static void showWebView(final Activity activity, final String url) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Dialog webviewDialog = new WebViewDialog(activity, url);
                    webviewDialog.show();
                }
            });

        }
    }

    /**
     * 显示一个webview界面
     * 
     * @param activity  需要显示webview的activity
     * @param url   显示url地址
     * @param width 网页显示的宽度
     * @param height 网页显示的高度
     */
    public static void showWebView(final Activity activity, final String url,
            final int width, final int height) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Dialog webviewDialog = new WebViewDialog(activity, url,
                            width, height);
                    webviewDialog.show();
                }
            });
        }
    }

    private static String getDeviceId(Activity activity) {
        String deviceId = "";

        try {
            Class<?> utils = Class.forName("com.lk.sdk.Utils");
            Method idMethod = utils.getMethod("id", Context.class);
            deviceId = (String) idMethod.invoke(null, activity);
        } catch (Exception e) {

        }

        return deviceId;
    }

    /**
     * 提供给用户进行自定义统计
     * 
     * @param type      事件类型
     * @param uid       用户id
     * @param gameId    游戏id
     * @param desc      事件描述
     */
    public static void actionStatistic(Activity activity, int type,
            String gameId, String uid, String desc) {
        final Hashtable<String, String> params = new Hashtable<String, String>();
        try {
            params.put("type", String.valueOf(type));
            params.put("gameId", gameId);
            params.put("uid", uid);
            params.put("desc", desc);
            params.put("getDeviceId", getDeviceId(activity));
        } catch (Exception e) {
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    postRequest(params);
                } catch (Exception e) {

                }
            }
        }).start();

    }

    /**
     * 对url做post操作，params是post的变量，返回post结果
     * 
     * @param type
     * @param params
     * @return 是否提交成功
     * @throws UnsupportedEncodingException 
     */
    public static boolean postRequest(Hashtable<String, String> params)
            throws UnsupportedEncodingException {
        boolean status = false;
        String url = "http://z.8864.com/api/analytics/post";

        // 初始化网络请求
        DefaultHttpClient client = new DefaultHttpClient();
        // 请求超时
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
        // 读取超时
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 15000);

        Log.d(TAG, "Post url is " + url);
        if (params != null) {
            for (String key : params.keySet()) {
                Log.d(TAG, "  " + key + "=" + params.get(key));
            }
        }

        // 构造data
        JSONObject dataJson = new JSONObject(params);
        String data = Base64.encodeToString(dataJson.toString().getBytes("utf-8"), Base64.NO_WRAP
                | Base64.URL_SAFE);

        HttpPost httpRequest = null;
        String statusMsg = null;
        try {
            httpRequest = new HttpPost(url);

            ArrayList<NameValuePair> postParams = new ArrayList<NameValuePair>();
            postParams.add(new BasicNameValuePair("data", data));

            Log.d(TAG, "Post params:");
            for (int i = 0; i < postParams.size(); i++) {
                Log.d(TAG, "  " + postParams.get(i).getName() + "="
                        + postParams.get(i).getValue());
            }

            httpRequest.setEntity(new UrlEncodedFormEntity(postParams,
                    HTTP.UTF_8));

            HttpResponse httpResponse = client.execute(httpRequest);

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            Log.d(TAG, "  Response status code: " + statusCode);
            Log.v(TAG, "  Rresponse status is "
                    + httpResponse.getStatusLine().getStatusCode());
            if (statusCode == 200) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                HttpEntity entity = httpResponse.getEntity();
                entity.writeTo(bos);
                bos.close();
                statusMsg = bos.toString();
                status = true;
            } else {
                statusMsg = httpResponse.getStatusLine().toString();
            }

        } catch (Exception e) {
            statusMsg = e.toString();
        } finally {
            if (httpRequest != null) {
                httpRequest.abort();
            }

            if (client != null) {
                try {
                    client.getConnectionManager().closeExpiredConnections();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Http Connect Manager is shut down");
                }
            }
        }

        Log.d(TAG, "Return msg:" + statusMsg);
        return status;
    }

    private final static byte[] CLOSE_PNG = { -119, 80, 78, 71, 13, 10, 26, 10,
            0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 52, 0, 0, 0, 52, 8, 6, 0, 0,
            0, -59, 120, 27, -21, 0, 0, 0, 4, 115, 66, 73, 84, 8, 8, 8, 8, 124,
            8, 100, -120, 0, 0, 9, 35, 73, 68, 65, 84, 104, -127, -27, -38, 93,
            111, -37, -42, 25, 7, -16, 63, -113, 76, 81, -94, 41, 29, 49, 82,
            20, -59, -118, 38, -57, 49, -110, 5, -39, 18, 24, -23, -110, 20,
            -102, -30, 38, 40, -70, 20, -67, 106, -81, -42, 1, -21, -18, -106,
            -34, 12, 11, -10, 5, -106, 126, -127, -95, -59, -82, -46, -69, 110,
            -64, -70, -101, -83, -69, 41, -102, 13, 69, 90, -57, 85, -112,
            -105, 5, 70, -77, 120, 70, 10, -57, -106, -90, -55, -42, 104, 41,
            -44, -111, 100, 74, 20, 77, 114, 23, -90, 18, -55, -106, 108, -57,
            -94, -101, 1, -5, 3, 2, 12, -127, -121, 62, 63, -45, 124, 59, -49,
            -61, -63, -91, -40, -74, 61, 4, -128, 2, 24, 113, 62, 17, 0, 33, 0,
            34, 0, -34, -39, -52, 0, -96, 1, -88, 0, 40, 1, 88, 114, 62, -116,
            -29, -72, 53, 55, -26, -63, 13, 50, -40, 65, -124, 1, 28, 5, 112,
            -118, 49, -106, 42, -105, -53, -89, 25, 99, -95, 106, -75, 106, 84,
            42, 21, -93, -43, 106, 89, -122, 97, 88, 0, -64, -13, 60, -15, 122,
            -67, 36, 20, 10, -15, -63, 96, -112, -89, -108, 86, -62, -31, -16,
            125, 74, 105, 6, -64, 3, 0, -113, 0, -108, 7, -63, -19, 10, 100,
            -37, -74, 23, 64, 28, -64, 25, -58, -40, 91, -123, 66, -31, -62,
            -62, -62, 66, 99, -75, -47, -80, -122, -91, -96, 95, -12, -117, 62,
            -63, 39, -14, 60, 63, -60, 123, -122, 120, 66, 60, 32, -128, 7,
            -106, 105, 90, -26, -102, 97, 25, -58, -102, -95, 55, 53, 67, 107,
            104, -51, -43, 122, -75, 49, -20, -9, -109, -79, -79, 49, 127, 60,
            30, -1, -126, 82, -6, 103, 0, 119, 1, 20, 56, -114, 107, -19, 41,
            -56, -74, 109, 15, -128, 40, -128, 116, -87, 84, -6, -7, -93, 71,
            -113, 78, 44, 46, 46, -42, -9, -113, 36, -10, 81, 26, -110, 4, -63,
            -25, 125, -34, 9, 0, -128, -82, 55, 91, -116, 85, -22, 43, 75, -7,
            39, -121, 15, 31, -106, -114, 29, 59, 54, 27, -119, 68, 62, 4, 48,
            13, 64, -31, 56, -50, -36, -23, -66, 118, 12, -78, 109, 123, 24,
            -64, -9, 53, 77, -69, -4, -32, -63, -125, -41, -25, -25, -25, 107,
            35, -55, -61, -5, 101, 57, 66, 119, 97, -24, 27, 85, 45, -79, -91,
            -36, -30, -54, -8, -8, 120, -32, -28, -55, -109, -97, -119, -94,
            120, 13, -64, 63, 56, -114, 91, -35, -55, -8, 29, -127, 108, -37,
            -114, 0, -8, 81, 46, -105, -69, 122, -9, -18, 93, 34, -121, -93,
            -95, 112, 52, -74, 111, -112, -119, 111, -105, -78, 82, 124, -94,
            -106, -107, -54, -103, 51, 103, -84, 100, 50, 121, 21, -64, 95, 57,
            -114, 43, 109, 55, 110, 75, -112, 109, -37, 28, -128, -72, 101, 89,
            -17, -52, -52, -52, 92, 89, 94, 86, 76, 57, 18, -91, -94, 36, -7,
            93, -102, -9, -106, -47, -22, -11, -122, 90, 82, -40, -63, -125,
            81, -49, -60, -60, -60, -5, -124, -112, -33, 97, -3, -36, -78, -5,
            -115, -23, 11, 114, 48, 9, 93, -41, 127, 113, -5, -10, -19, -97,
            104, 77, -61, 27, -117, 39, 34, 123, 49, -15, -19, 82, 44, -28, 75,
            -94, -113, 111, -99, 59, 119, -18, 15, -126, 32, -4, 22, 64, -66,
            31, 106, 43, -48, 33, 93, -41, 127, 121, -13, -26, -51, 31, 91,
            -100, -57, 31, -115, -59, -61, 123, 54, -29, 29, 68, 41, 22, -54,
            -60, 54, 27, -25, -49, -97, -1, -93, 32, 8, -17, 115, 28, 87, -24,
            -75, 29, -23, -11, -91, 109, -37, 17, -53, -78, -34, -55, 100, 50,
            111, 91, -100, -57, -9, -94, 49, 0, 16, -115, -59, -61, 22, -25,
            -15, 101, 50, -103, -73, 45, -53, -6, -103, 115, 94, 111, -54, 38,
            -112, 115, 53, -69, 52, 51, 51, 115, 69, 55, 44, 33, 26, -117, -65,
            -112, 127, -77, 94, -119, -58, -30, 17, -35, -80, -124, -103, -103,
            -103, 43, 0, 46, 57, 115, -19, 74, 23, -56, -71, -49, -100, -54,
            102, -77, 87, -105, 21, -59, -36, 120, -50, 16, 66, 32, -118, 126,
            72, -94, 31, 60, -33, -13, -32, 14, 28, -98, 39, -112, 68, 63, 68,
            -47, 15, 66, 54, -1, -114, 88, 60, 17, 89, 86, 20, 51, -101, -51,
            -66, 7, -32, -108, 51, -25, 103, 115, -36, -72, 125, -83, 86, 123,
            -9, -34, -67, 123, -100, 28, -114, 118, -35, 95, 8, 33, -96, -110,
            8, -115, -107, -95, 44, -25, 33, 7, -87, -21, 40, -98, 39, -112,
            -125, 20, -54, 114, 30, 26, 43, -125, 74, 98, 79, -108, 28, -114,
            -46, 123, -9, -18, -95, 86, -85, -67, 11, 32, -42, 19, -28, 60,
            -50, -92, 103, 103, 103, 95, -109, -61, -47, -112, 40, 118, 95,
            -102, 61, 0, -76, 70, 29, -103, 76, 6, -73, 111, -35, 66, 62, -73,
            -32, 42, -86, -115, -55, -25, 22, 112, -5, -42, 45, 100, 50, 25,
            104, -115, 58, 60, 61, -74, 21, 69, -55, 47, -121, -93, -95, -39,
            -39, -39, -41, 0, -92, -99, -71, 119, -125, 0, 28, 42, -107, 74,
            -105, -25, -25, -25, 107, -67, 110, -102, 38, 0, -47, 31, 68, 42,
            -107, 2, 0, 100, -90, -89, 93, 67, 117, 98, 50, -45, -45, 0, -128,
            84, 42, 5, -47, 31, 68, -65, 103, -98, 112, 52, -74, 111, 126, 126,
            -66, 86, 42, -107, 46, 3, 56, -44, 5, 114, -98, -102, -49, -50,
            -51, -51, 29, 75, 36, 15, 71, 123, -19, -64, -78, 44, -80, 122, 29,
            -31, 104, 28, 23, 95, 125, -43, 53, 84, 47, -52, 43, 23, 47, 34,
            28, -117, -125, -43, -21, -80, 44, -85, -17, -40, 68, -14, 112,
            116, 110, 110, -18, 24, -128, 115, -114, -31, -23, 17, 10, 51, -58,
            -34, -52, -27, 114, -85, 65, 57, 18, -20, -73, 3, -53, -78, -96,
            50, -122, 112, 44, -114, 87, 46, 94, 28, 24, -43, 15, -77, 127, 36,
            1, 85, 101, 91, 98, 0, 32, 40, 71, -126, -71, 92, 110, -107, 49,
            -10, 22, -42, -33, -65, -98, -126, -114, 22, 10, -123, 11, -5, 99,
            35, -14, 118, -109, -80, 44, 11, -86, -54, -80, 127, 36, 49, 16,
            106, 80, 76, 59, -79, -111, -60, -66, 124, 62, 127, 1, -64, 119, 1,
            -128, 56, -121, -22, -44, -62, -62, -126, 70, -27, 112, 96, 39, 59,
            25, 20, -27, 22, 6, 0, -126, 52, 20, 120, -4, -8, 113, 13, -64, 73,
            -37, -74, -121, 8, 0, -54, 24, 75, -83, 54, 26, -42, -13, -68, -49,
            -76, 81, -79, 13, -88, 66, 62, -69, 37, -86, -115, 41, -28, -77,
            93, -104, -40, 46, 48, 0, -64, 11, 62, -66, -43, 106, -127, 49,
            -106, 6, 32, 19, 0, -15, 114, -71, -4, -46, -80, 20, 124, -18, 39,
            -24, 94, -88, -23, -87, -87, -66, -88, 78, -52, -12, -44, -44, -64,
            -104, 118, 36, 41, -24, 87, 20, 101, 2, -64, 8, 1, 112, -112, 49,
            70, 69, -65, -24, -37, -51, -50, -116, 29, -94, -74, -61, 24, -69,
            -60, 0, -128, -32, 23, -123, 122, -67, 30, 4, 112, -112, 0, -40,
            95, -83, 86, 13, -63, 39, -14, -37, 13, -36, 45, 74, 16, -124, 61,
            -61, 0, -128, -32, 19, 121, 85, 85, 13, 0, 17, 2, 32, 84, -87, 84,
            12, 94, 24, -38, 53, 104, 35, 42, 61, 57, -39, -123, -6, -50, -63,
            104, 23, 38, 61, 57, -23, 26, 6, 0, 120, 97, -120, -81, -43, 106,
            6, 0, 58, 4, 96, -72, -39, 108, -102, 30, -62, -9, 122, -54, 120,
            -82, -76, 81, -15, -60, 40, -46, -109, -21, -96, -23, -87, 41,
            -108, -53, 101, -4, -13, -31, 67, 0, -21, -104, 120, 98, -44, 53,
            12, 0, 120, 8, 79, 90, -83, -106, 5, 64, 36, 0, -68, -122, 97, 88,
            -60, -29, 113, -27, -95, -84, -115, 74, -116, -114, 33, 61, 57,
            -119, 90, -75, -118, -49, 62, -3, 20, -75, 106, 21, -23, -55, 73,
            36, 70, -57, 92, -59, 0, 0, -15, 120, -120, -77, -10, -73, 71, -17,
            0, 47, 46, 28, 1, -48, -30, 121, -98, 88, -90, -23, -54, -97, -116,
            39, 4, -78, 76, -111, -49, 46, 96, 122, 106, 10, -127, 96, 16, -81,
            -65, -15, 6, 2, -63, 32, -90, -89, -90, -112, -49, 46, 64, -106,
            41, -8, 30, -81, 5, -69, -115, 101, -102, 22, -65, 126, 57, 109,
            17, 0, -85, 62, -97, -49, 99, 90, -58, -114, 23, -13, -6, -91,
            -115, -39, 120, 1, 72, -89, 39, 55, 93, 40, -36, 68, -103, -106,
            97, 121, -67, 94, 2, 64, 35, 0, 42, -95, 80, -120, 55, -12, 53, 99,
            -112, -99, -74, 49, -59, -91, 124, 23, 38, -98, 24, -59, -65, -106,
            21, -25, 66, -15, 12, 85, 92, -54, -69, -122, 50, -12, 53, 35, 16,
            8, -16, 0, 24, 1, -80, 18, 12, 6, 121, -67, -87, -19, 26, -44,
            -119, -7, -14, -58, -115, 46, -116, 90, 101, -48, 117, 29, 106,
            -107, 117, -95, -66, -68, 113, -61, 53, -108, -34, -44, 12, 89,
            -106, 121, 0, 37, 2, 96, -103, 82, -54, -76, -122, -42, -36, 11,
            -116, 83, 120, -128, 97, 88, 123, -122, -46, 27, -102, 46, 73, 82,
            21, -64, 50, 1, 80, 8, -121, -61, 127, 95, -83, 87, 27, -49, -69,
            35, 66, 8, -126, 27, 48, -87, 116, 122, 19, -90, -99, 78, 84, 42,
            -99, -34, -124, -22, -75, 126, -80, -109, -44, -21, -43, 70, 52,
            26, -99, 1, -80, 68, 0, 48, 74, 105, 102, -40, -17, 39, -70, -34,
            -36, 113, -7, -126, 56, 71, 102, 101, 3, 38, -111, 28, -21, -119,
            -39, -120, 74, 36, -57, 92, 65, 25, 122, -45, -16, 122, -67, -96,
            -108, 126, 5, 64, 37, 78, 113, -23, -21, -79, -79, 49, -111, -87,
            -27, -38, 94, 98, -74, 67, -83, -20, 2, 85, 101, -107, -38, -111,
            35, 71, 2, 0, -66, -26, 56, 110, -83, 61, -14, -101, 120, 60, -2,
            -59, 74, 113, 73, -35, 107, -116, -37, -88, -30, 82, -2, 73, 34,
            -111, -8, 18, -21, -43, -65, -89, -81, -32, 101, 74, -23, 95, -110,
            -55, -28, 112, 85, 45, 85, -73, -60, 80, -118, 114, -79, 48, 16,
            -58, 45, 84, 85, 45, 85, -109, -55, -28, 48, -91, -12, 79, 0, 86,
            -98, -126, -100, 127, -69, 59, 71, -113, 30, -3, 38, -97, 91, 84,
            -6, 97, -88, 36, -95, -84, 20, 112, -29, -13, -49, 7, -58, 108,
            -121, 42, 23, 11, -96, -110, -76, 37, 42, -97, 91, 84, -114, 31,
            63, -2, 8, -64, -19, 118, 93, -74, 115, -21, -4, -127, 3, 7, -82,
            -115, -113, -113, 7, -54, 74, -15, -55, -58, -63, -21, 11, -115,
            85, 100, 50, 25, -41, 48, 91, -95, -42, 23, 26, -85, 61, 23, 26,
            -127, -11, -126, -40, -8, -8, 120, 32, 18, -119, 92, 3, -16, -17,
            -10, -9, 67, -19, 31, 56, -114, 107, -39, -74, 125, -13, -60, -119,
            19, 127, -69, 126, -3, 122, -54, 47, 73, -2, -50, -43, 83, 19,
            -128, -28, -105, -16, -14, -53, 41, 52, -101, -102, 107, -104, 94,
            -88, 115, -90, 9, -97, 79, -124, -24, -105, -64, -22, -38, -90,
            109, 53, -83, -34, 80, -53, 74, -27, -20, 15, 46, 101, 0, 76, 119,
            22, -105, -69, -22, 67, -50, -62, -9, -39, 108, 54, -5, -5, -121,
            115, 115, 98, 60, 49, -42, -67, 110, 76, 8, 124, 62, 1, 4, -128,
            110, -24, -82, 97, 58, -61, -13, 4, 2, 47, -64, 2, -48, 108, -22,
            61, -41, 25, 10, -7, -123, -30, -9, -114, 31, -41, 70, 71, 71, 127,
            10, -32, 78, 103, 81, 121, 83, -63, -53, 41, 81, -68, 121, -1, -2,
            -3, -33, 40, 37, -107, 123, 81, 85, -69, 126, 41, 22, -14, -91,
            104, 68, -74, 79, -97, 62, -3, 43, 0, -97, 108, 44, 38, 111, 58,
            -29, -100, 13, -82, 79, 76, 76, 124, 32, -16, 68, 87, -118, -123,
            109, 11, -75, -33, 86, -108, 98, -95, 36, -16, 68, -97, -104, -104,
            -8, 0, -64, -11, 94, -107, -15, -98, -105, 16, -114, -29, 74, -124,
            -112, -113, 82, -87, -44, -57, -60, 54, -101, 74, -79, 80, -34,
            -13, -39, 110, 19, -89, 36, -39, 76, -91, 82, 31, 19, 66, 62, -22,
            87, 17, -1, -1, 41, 26, 3, -1, 3, 101, 125, -83, -34, 80, 21, -105,
            -54, -6, -99, 113, 10, -76, -105, 114, -71, -36, -81, 95, 80, -29,
            -59, 123, 88, 63, 103, 6, 107, -68, -24, 76, -97, -42, -104, -88,
            -68, 69, -7, 101, 55, 81, -43, 82, 117, 41, -73, -88, 116, -76,
            -58, 124, 8, -32, -127, -85, -83, 49, -19, 56, -9, -87, 3, 0, 126,
            -24, 102, -13, -110, -95, 55, 91, 106, -17, -26, -91, -81, 0, -4,
            103, 79, -102, -105, 58, -77, 125, 123, -103, -28, 19, 124, -66,
            118, 123, -103, -121, 120, 60, 28, 0, 88, -90, 105, -101, 107,
            -122, -71, -34, 94, -42, 52, -76, 70, -67, 87, 123, -39, 39, 0,
            -18, -32, -37, 104, 47, -21, 1, 107, 55, 0, 30, 3, 112, -46, 105,
            0, 124, 73, 85, 85, -54, 24, 51, 106, -75, -102, -47, 106, -75, 76,
            -61, 48, 108, 0, -32, 121, -98, -13, 122, -67, -98, 64, 32, -64,
            83, 74, 121, 89, -106, -103, -45, 0, -8, 21, 94, 100, 3, 96, -81,
            116, -76, 104, -58, -15, -84, 69, -109, 2, 24, 70, 119, -117, -26,
            42, 0, -122, 103, 45, -102, 5, -72, -40, -94, -7, 95, -39, 87, -64,
            34, 54, -87, 120, 101, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96,
            -126 };
}
