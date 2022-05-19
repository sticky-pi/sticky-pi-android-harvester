package com.example.sticky_pi_data_harvester;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class APIClient {
    static final String TAG =  "APIClient";
    String token;
    long token_expiration;
    String m_password;

    String m_user_name;
    String m_api_host;



    public String get_password() {
        return m_password;
    }

    public String get_user_name() {
        return m_user_name;
    }

    public String get_api_host() {
        return m_api_host;
    }


    APIClient(String api_host, String user_name, String password){
        m_api_host = api_host;
        m_user_name = user_name;
        m_password = password;
    }

    private static String read_all(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    static class HTTPAuthenticationError extends RuntimeException{
    }

//
//    private Object api_call_wrapped(Object payload, String endpoint, String username, String password) {
//
//    }

    private Object api_call_wrapped(Object payload, String endpoint, String username, String password){
        HttpURLConnection conn = null;

        URL url = null;

        Class<? extends Object> clazz = payload.getClass();
        Object out = null;
        try {
            out = clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
//        Object out =  new payload.getClass().getConstructor();

        try {
            url = new URL("https://" + m_api_host + "/" + endpoint);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            conn = (HttpURLConnection) url.openConnection();
            String user_pass = username + ":" + password;
            final String basicAuth = "Basic " + Base64.encodeToString( user_pass.getBytes(), Base64.NO_WRAP);

            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", basicAuth);
                String payload_bytes;
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                payload_bytes = payload.toString();
                DataOutputStream out_stream = new DataOutputStream(conn.getOutputStream());
                out_stream.writeBytes(payload_bytes);
                out_stream.flush();
                out_stream.close();
//            }

            if (conn.getResponseCode() == 401) {
                throw new HTTPAuthenticationError();
            }

            if (conn.getResponseCode() != 200) {
                InputStream error_stream = conn.getErrorStream() ;
                Log.e(TAG, "POST failed: " );
                BufferedReader rd = new BufferedReader(new InputStreamReader(error_stream, Charset.forName("UTF-8")));
                Log.e(TAG, "POST failed: " + read_all(rd));
                return out;
            }


            InputStream input_stream = conn.getInputStream() ;
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(input_stream, Charset.forName("UTF-8")));
                String jsonText = read_all(rd);
                out = new JSONObject(jsonText);

            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                input_stream.close();
            }


        } catch (IOException e) {
            Log.e(TAG, "IO exception. No internet?");
            e.printStackTrace();
        }

        return out;
    }

    public Object  api_call(Object payload, String endpoint){
        Class<? extends Object> clazz = payload.getClass();
        Object out = null;
        try {
            out = clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        // get token if necessary?
        try{
            return api_call_wrapped(payload, endpoint, token, "");
        }
        catch(RuntimeException e){
            Log.w(TAG, "Failed to authenticate with token. Requesting new token.");
            if(! get_token()){
                Log.e(TAG, "Failed to get token!");
            }
            try {
                return api_call_wrapped(payload, endpoint, token, "");
            }
            catch (HTTPAuthenticationError f){
                Log.e(TAG, "Second authentication failed! Returning empty object");
                return out;
            }
        }
    }

    public String multipartRequest(String urlTo, String post, String filepath, String filefield) throws ParseException, IOException {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary =  "*****"+Long.toString(System.currentTimeMillis())+"*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024*1024;

        String[] q = filepath.split("/");
        int idx = q.length - 1;

        try {
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);

            URL url = new URL(urlTo);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            String user_pass = token;//fixme
            final String basicAuth = "Basic " + Base64.encodeToString( user_pass.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + "files" + "\"; filename=\"" + q[idx] +"\"" + lineEnd);
            outputStream.writeBytes("Content-Type: application/octet-stream" + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while(bytesRead > 0) {
//                sleep(1000);
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            // Upload POST Data
            outputStream.flush();
            outputStream.close();

            return result;
        } catch(Exception e) {
            Log.e("MultipartRequest","Multipart Form Upload Error");
            e.printStackTrace();
            return "";
        }
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    JSONArray put_images(List<File> images){
        JSONArray out = null;
        for(File img: images) {
            Log.i(TAG, "Uploading "+ img.getName());
            JSONArray result = (JSONArray) api_call (img, "_put_new_images");
            try {
                String out_s = multipartRequest("https://" + m_api_host + "_put_ne_images", "{}",  img.getPath(),  img.getName());
                Log.e("TODEL", out_s);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return out;
    }

    public boolean get_token(){
        JSONObject output;
        try {
             output = (JSONObject) api_call_wrapped(new JSONObject(), "get_token", token, "");
        } catch (HTTPAuthenticationError e){

            output = (JSONObject) api_call_wrapped(new JSONObject(), "get_token", m_user_name, m_password);
        }
        Log.e("TODEL", output.toString());

        try {
            token  = output.getString("token");
            token_expiration = output.getLong("expiration");
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
