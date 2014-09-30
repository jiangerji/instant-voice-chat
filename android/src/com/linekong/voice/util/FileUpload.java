package com.linekong.voice.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import android.util.Log;

public class FileUpload {
    private static final String TAG = "FileUpload";

    /**
     * A generic method to execute any type of Http Request and constructs a response object
     * @param requestBase the request that needs to be exeuted
     * @return server response as <code>String</code>
     */
//    public static String executeRequest(HttpRequestBase requestBase){
//        String responseString = "" ;
// 
//        InputStream responseStream = null ;
//        HttpClient client = new DefaultHttpClient () ;
//        try{
//            HttpResponse response = client.execute(requestBase) ;
//            if (response != null){
//                HttpEntity responseEntity = response.getEntity() ;
// 
//                if (responseEntity != null){
//                    responseStream = responseEntity.getContent() ;
//                    if (responseStream != null){
//                        BufferedReader br = new BufferedReader (new InputStreamReader (responseStream)) ;
//                        String responseLine = br.readLine() ;
//                        String tempResponseString = "" ;
//                        while (responseLine != null){
//                            tempResponseString = tempResponseString + responseLine + System.getProperty("line.separator") ;
//                            responseLine = br.readLine() ;
//                        }
//                        br.close() ;
//                        if (tempResponseString.length() > 0){
//                            responseString = tempResponseString ;
//                        }
//                    }
//                }
//            }
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally{
//            if (responseStream != null){
//                try {
//                    responseStream.close() ;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        client.getConnectionManager().shutdown() ;
// 
//        return responseString ;
//    }
//    
//    /**
//     * Method that builds the multi-part form data request
//     * @param urlString the urlString to which the file needs to be uploaded
//     * @param file the actual file instance that needs to be uploaded
//     * @param fileName name of the file, just to show how to add the usual form parameters
//     * @param fileDescription some description for the file, just to show how to add the usual form parameters
//     * @return server response as <code>String</code>
//     * @throws UnsupportedEncodingException 
//     */
//    public static String executeMultiPartRequest(String urlString, File file, String fileName, String fileDescription) throws UnsupportedEncodingException {
// 
//        HttpPost postRequest = new HttpPost (urlString) ;
//        MultipartEntity multiPartEntity = new MultipartEntity () ;
//        
//        //The usual form parameters can be added this way
////            multiPartEntity.addPart("description", new StringBody(fileDescription != null ? fileDescription : "")) ;
//        multiPartEntity.addPart("filename", new StringBody(fileName != null ? fileName : file.getName())) ;
// 
//        /*Need to construct a FileBody with the file that needs to be attached and specify the mime type of the file. Add the fileBody to the request as an another part.
//        This part will be considered as file part and the rest of them as usual form-data parts*/
//        FileBody fileBody = new FileBody(file, "application/octect-stream") ;
//        multiPartEntity.addPart("attachment", fileBody) ;
////            multiPartEntity.addPart("", contentBody)
// 
//        postRequest.addHeader("referer", "Voice Char SDK");
//        postRequest.setEntity(multiPartEntity) ;
// 
//        return executeRequest (postRequest) ;
//    }
    
    public static String doUpload(String actionUrl, Map<String, String> params,
            String filename, File file) throws IOException {
        StringBuilder sb2 = new StringBuilder();
        String BOUNDARY = java.util.UUID.randomUUID().toString();
        String PREFIX = "--", LINEND = "\r\n";
        String MULTIPART_FROM_DATA = "multipart/form-data";
        String CHARSET = "UTF-8";
 
        URL uri = new URL(actionUrl);
        HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
        conn.setReadTimeout(5 * 1000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("connection", "keep-alive");
        conn.setRequestProperty("Charsert", "UTF-8");
        conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA
                + ";boundary=" + BOUNDARY);
        conn.setRequestProperty("referer", "LK_Voice_Chat_SDK");
 
        StringBuilder sb = new StringBuilder();
        if (params != null){
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINEND);
                sb.append("Content-Disposition: form-data; name=\""
                        + entry.getKey() + "\"" + LINEND);
                sb.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);
                sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
                sb.append(LINEND);
                sb.append(entry.getValue());
                sb.append(LINEND);
            }
        }
 
        DataOutputStream outStream = new DataOutputStream(
                conn.getOutputStream());
        outStream.write(sb.toString().getBytes());
        
        StringBuilder sb1 = new StringBuilder();
        sb1.append(PREFIX);
        sb1.append(BOUNDARY);
        sb1.append(LINEND);
        // sb1.append("Content-Disposition: form-data; name=\"file"+(i++)+"\"; filename=\""+file.getKey()+"\""+LINEND);
        sb1.append("Content-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"" + LINEND);
        sb1.append("Content-Type: application/octet-stream; charset="
                        + CHARSET + LINEND);
        sb1.append(LINEND);
        outStream.write(sb1.toString().getBytes());
 
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        
        is.close();
        outStream.write(LINEND.getBytes());
 
        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
        outStream.write(end_data);
        outStream.flush();
 
        int res = conn.getResponseCode();
        InputStream in = null;
        if (res == 200) {
            in = conn.getInputStream();
            int ch;
 
            while ((ch = in.read()) != -1) {
                sb2.append((char) ch);
            }
            Log.i(TAG, sb2.toString());
        }
        
        conn.disconnect();
 
        return in == null ? null : sb2.toString();
    }
}
