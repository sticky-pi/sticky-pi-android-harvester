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
import java.io.FileNotFoundException;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class APIClient {
    static final String TAG =  "APIClient";
    final int MAX_CONCURRENT_REQUESTS = 32;
    final int MAX_RETRIES = 5;
    String token;
    long token_expiration;
    String m_password;

    String m_user_name;
    String m_api_host;
    String m_protocol;
    int ongoing_requests = 0;


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
        new APIClient(api_host, user_name, password, "https");
    }
    APIClient(String api_host, String user_name, String password, String protocol){
        m_api_host = api_host;
        m_user_name = user_name;
        m_password = password;
        m_protocol = protocol;
    }

    private static String read_all(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    static class HTTPError extends RuntimeException{
        public HTTPError(String s) {
            super(s);
        }
    }

    static class HTTPAuthenticationError extends HTTPError{
        public HTTPAuthenticationError(String s) {
            super(s);
        }
    }
    static class MaxRetriesError extends HTTPError{
            public MaxRetriesError(String s) {
                super(s);
            }
    }


    private Object api_call_wrapped(Object payload, String endpoint, String username, String password, int retries){
        if(retries >= MAX_RETRIES){
            throw new MaxRetriesError("Too many failed requests");
        }
        try {
            Thread.sleep(retries * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HttpURLConnection conn = null;
        URL url = null;

        Class<? extends Object> clazz = payload.getClass();
        Object out = null;
        try {
            out = clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        try {
            url = new URL(m_protocol + "://" + m_api_host + "/" + endpoint);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
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
                throw new HTTPAuthenticationError("Failed to log with credentials: " + username);
            }

            if (conn.getResponseCode() != 200) {
                InputStream error_stream = conn.getErrorStream() ;
                Log.e(TAG, "POST failed: " + url + ". Payload: " + payload.toString());
                BufferedReader rd = new BufferedReader(new InputStreamReader(error_stream, Charset.forName("UTF-8")));
                return api_call_wrapped(payload, endpoint, username, password, retries + 1);
            }


            InputStream input_stream = conn.getInputStream() ;
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(input_stream, Charset.forName("UTF-8")));
                String jsonText = read_all(rd);
                Log.e(TAG, String.valueOf(jsonText));
//                out = payload.getClass().getDeclaredConstructors(String.class).newInstance(jsonText);
                if (payload instanceof JSONObject){
                    out = new JSONObject(jsonText);
                }
                else if (payload instanceof JSONArray){
                    out = new JSONArray(jsonText);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, String.valueOf(e));

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
        ongoing_requests++;
        while (ongoing_requests >= MAX_CONCURRENT_REQUESTS){
            Log.e("todel", "Queuing request");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Class<? extends Object> clazz = payload.getClass();
            Object out = null;
            try {
                out = clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }

            // get token if necessary?
            try {
                return api_call_wrapped(payload, endpoint, token, "", 0);
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to authenticate with token. Requesting new token.");
                if (!get_token()) {
                    Log.e(TAG, "Failed to get token!");
                }
                try {
                    return api_call_wrapped(payload, endpoint, token, "", 0);
                } catch (HTTPAuthenticationError f) {
                    Log.e(TAG, "Second authentication failed! Returning empty object");
                    return out;
                }
            }
        }finally {
            ongoing_requests --;
        }
    }

public JSONArray multipartRequest(String urlTo,  String filepath, String filefield, String md5 ) throws IOException, JSONException, HTTPError {
    return multipartRequest(urlTo, filepath, filefield, md5, 0);
}
public JSONArray multipartRequest(String urlTo,  String filepath, String filefield, String md5, int retries ) throws IOException, JSONException, HTTPError {
    if(retries >= MAX_RETRIES)
        throw new MaxRetriesError("Too many failed requests");

    try {
        Thread.sleep(retries * 1000L);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }

    HttpURLConnection connection = null;
    DataOutputStream outputStream = null;
    InputStream inputStream = null;

    String twoHyphens = "--";
    String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
    String lineEnd = "\r\n";

    JSONArray result  = new JSONArray();

    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1 * 1024 * 1024;

    String[] q = filepath.split("/");
    int idx = q.length - 1;


    File file = new File(filepath);
    FileInputStream fileInputStream = new FileInputStream(file);

    URL url = new URL(urlTo);
    connection = (HttpURLConnection) url.openConnection();

    //
    String user_pass = m_user_name + ":" + m_password;
    final String basicAuth = "Basic " + Base64.encodeToString( user_pass.getBytes(), Base64.NO_WRAP);

    connection.setDoInput(true);
    connection.setDoOutput(true);
    connection.setUseCaches(false);

    connection.setRequestMethod("POST");
    connection.setRequestProperty("Connection", "Keep-Alive");
    connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    connection.setRequestProperty("Authorization", basicAuth);

    outputStream = new DataOutputStream(connection.getOutputStream());
    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
    outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
    outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
    outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

    outputStream.writeBytes(lineEnd);

    bytesAvailable = fileInputStream.available();
    bufferSize = Math.min(bytesAvailable, maxBufferSize);
    buffer = new byte[bufferSize];

    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    while (bytesRead > 0) {
        outputStream.write(buffer, 0, bufferSize);
        bytesAvailable = fileInputStream.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    }

    outputStream.writeBytes(lineEnd);

    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
    outputStream.writeBytes("Content-Disposition: form-data; name=\"" +  q[idx] +".md5" + "\"" + lineEnd);
    outputStream.writeBytes("Content-Type: application/json" + lineEnd);
    outputStream.writeBytes(lineEnd);
    outputStream.writeBytes(md5);
    outputStream.writeBytes(lineEnd);


    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


    if (200 != connection.getResponseCode()) {
        multipartRequest(urlTo,  filepath, filefield, md5,retries+1);
    }

    inputStream = connection.getInputStream();

    String result_str = this.convertStreamToString(inputStream);
    result = new JSONArray(result_str);
    fileInputStream.close();
    inputStream.close();
    outputStream.flush();
    outputStream.close();

    return result;


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


    boolean put_image(File img, String md5){
        JSONArray out  = null;
        try {
            out = multipartRequest(m_protocol + "://" + m_api_host + "/" +"_put_new_images", img.getPath(), img.getName(), md5);
        } catch (IOException | JSONException | HTTPError e) {
            e.printStackTrace();
            return false;
        }


        if(out.length() > 0) {
            try {
                String server_md5 = ((JSONObject) out.get(0)).getString("md5");
                if(Objects.equals(md5, server_md5)) {
                    return  true;
                }
                else{
                    Log.e(TAG, "Different local and server md5s for " + img.getName());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        return false;
    }

    public boolean get_token(){
        JSONObject output;
        try {
            output = (JSONObject) api_call_wrapped(new JSONObject(), "get_token", token, "", 0);
        } catch (HTTPAuthenticationError e){
            try {

                Log.i(TAG, "Authentication failure using token: `" + token + "`. requesting new token." );
                output = (JSONObject) api_call_wrapped(new JSONObject(), "get_token", m_user_name, m_password, 0);
                token_expiration = 0;
            }
            catch (HTTPAuthenticationError f){
                Log.e(TAG, "Authentication failure for "+ m_user_name);
                return false;
            }
        }


        try {
            if( output == null)
                throw new JSONException("Object is null, not a valid JSON");

            // renew token every 5min
            if(output.getLong("expiration") - token_expiration > 300) {
                Log.i(TAG, "Renewing token");
                token = output.getString("token");
                token_expiration = output.getLong("expiration");
            }
        return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
