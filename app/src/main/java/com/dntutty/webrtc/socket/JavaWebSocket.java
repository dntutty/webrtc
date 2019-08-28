package com.dntutty.webrtc.socket;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.dntutty.webrtc.ChatRoomActivity;
import com.dntutty.webrtc.MainActivity;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class JavaWebSocket {
    private static final String TAG = "JavaWebSocket";
    private WebSocketClient socketClient;
    private MainActivity activity;

    public JavaWebSocket(MainActivity activity) {
        this.activity = activity;
    }

    public void connect(String wss) {
        URI uri = null;
        try {
            uri = new URI(wss);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i(TAG, "onOpen: ");
                ChatRoomActivity.openActivity(activity);
            }

            @Override
            public void onMessage(String message) {
                Log.i(TAG, "onMessage: " + message);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i(TAG, "onClose: " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i(TAG, "onError: " + e.toString());
            }
        };
        if (wss.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                SSLSocketFactory factory = null;
                if (sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }
                if (factory != null) {
                    socketClient.setSocket(factory.createSocket());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            socketClient.connect();
        }
    }
//
    //客户端向服务器 发送信息 请求参数

    /**
     * 事件类型
     * 1 ——join
     * 2 __answer
     * 3 __offer
     * 4 __ice_candidate
     * 5 __peer
     *
     * @param roomId
     */
    public void joinRoom(String roomId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", roomId);
        map.put("data", childMap);
        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toJSONString();
        socketClient.send(jsonString);
    }

    //忽略证书
    public static class TrustManagerTest implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
