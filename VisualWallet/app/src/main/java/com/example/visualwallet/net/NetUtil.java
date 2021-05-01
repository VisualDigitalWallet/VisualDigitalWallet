package com.example.visualwallet.net;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.visualwallet.common.Constant;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class NetUtil {

    private static String urlBase;

    static {
        urlBase = Constant.protocol
                + "://"
                + Constant.domain
                + ":"
                + Constant.port
                + Constant.projectRoot;
    }

    public static Map<String, Object> Get(String subUrlStr, Map<String, Object> args) {

        HttpURLConnection connection = null;
        String response = null;

        StringBuilder urlStr = new StringBuilder(urlBase + subUrlStr);
        boolean firstArg = true;
        if (args != null) {
            for (Map.Entry<String, Object> it : args.entrySet()) {
                urlStr.append(firstArg ? "?" : "&");
                if (firstArg) {
                    firstArg = false;
                }
                urlStr.append(it.getKey());
                urlStr.append("=");
                urlStr.append(args.get(it.getValue().toString()));
            }
        }

        try {
            URL url = new URL(urlStr.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(Constant.connectTimeout);
            connection.setReadTimeout(Constant.readTimeout);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            InputStream in = connection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            response = reader.readLine();

        } catch (Exception e) {
            e.printStackTrace();
            if (connection != null) {
                connection.disconnect();
            }
        }
        return JSONObject.parseObject(response);
    }

    public static Map Post(String subUrlStr, Map<String, Object> args) {

        HttpURLConnection connection = null;
        String response = null;
        int code;

        String jsonString = JSON.toJSONString(args);
        byte[] argsByte = jsonString.getBytes();

        Log.i("post url", urlBase + subUrlStr);
        Log.i("post body", jsonString.toString());

        try {
            URL url = new URL(urlBase + subUrlStr + "/");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(Constant.connectTimeout);
            connection.setReadTimeout(Constant.readTimeout);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(argsByte, 0, argsByte.length);

            code = connection.getResponseCode();
            InputStream in;
            if (code == 200) {
                in = connection.getInputStream(); // 得到网络返回的正确输入流
            } else {
                in = connection.getErrorStream(); // 得到网络返回的错误输入流
            }
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            response = reader.readLine();
            Log.i("response", response);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (code == 200) {
            Map resMap = (Map) JSONObject.parse(response);
            resMap.put("code", "200");
            return resMap;
        }
        else {
            Map<String, String> errorMap = new HashMap<String, String>();
            errorMap.put("code", String.valueOf(code));
            return errorMap;
        }
    }
}
