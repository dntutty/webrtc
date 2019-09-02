package com.dntutty.webrtc.socket;

import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dntutty.webrtc.ChatRoomActivity;
import com.dntutty.webrtc.MainActivity;
import com.dntutty.webrtc.connection.PeerConnectionManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class JavaWebSocket {
    private PeerConnectionManager peerConnectionManager;
    private static final String TAG = "Webrtc_socket";
    private WebSocketClient socketClient;
    private MainActivity activity;

    public JavaWebSocket(MainActivity activity) {
        this.activity = activity;
    }

    public void connect(String wss) {
        peerConnectionManager = PeerConnectionManager.getInstance();
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
                handleMessage(message);
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

    private void handleMessage(String message) {
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        //p2p通信
        if (eventName.equals("_peers")) {
            handleJoinRoom(map);
        }

        if(eventName.equals("_new_peer")) {
            handlerNewPeer(map);
        }

        if(eventName.equals("_offer")) {
            handleOffer(map);
        }

//        offer 对方响应  _ice_candidate   对方的n个小目标 ----->大目标
        if (eventName.equals("_ice_candidate")) {
            handleRemoteCandidate(map);
        }
//          对方的SDP
        if (eventName.equals("_answer")) {
            handleAnswer(map);
        }
    }

    private void handlerNewPeer(Map map) {
        Map data = (Map) map.get("data");
        if (null != data) {
            String socketId = (String) data.get("socketId");
            peerConnectionManager.onConnection(socketId);
        }
    }

    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
        Map sdpDic;
        if (null != data) {
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            Map sdp = (Map) sdpDic.get("sdp");
            String desription = (String) sdp.get("description");
            peerConnectionManager.onReceiveOffer(socketId,desription);
        }
    }

    private void handleAnswer(Map map) {
        Map data = (Map) map.get("data");
        Map sdpDic;
        if (null != data) {
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            Map sdp = (Map) sdpDic.get("sdp");
            String desription = (String) sdp.get("description");
            peerConnectionManager.onReceiveAnswer(socketId,desription);
        }
    }

    private void handleRemoteCandidate(@NonNull Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            String sdpMid = (String) data.get("id");
            sdpMid = (null == sdpMid) ? "video" : sdpMid;
            int sdpMLineIndex = (int) Double.parseDouble(String.valueOf(data.get("label")));
            String candidate = (String) data.get("candidate");
//            IceCandidate对象
            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            peerConnectionManager.onRemoteIceCandidate(socketId, iceCandidate);
        }
    }

    private void handleJoinRoom(@NonNull Map map) {
        Map data = (Map) map.get("data");
        JSONArray array;
        if (data != null) {
            array = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(array, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);
            String mineId = (String) data.get("you");
            peerConnectionManager.joinToRoom(this, true, connections, mineId);
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

    public void sendOffer(String socketId, SessionDescription sdp) {
        HashMap<String, Object> childMap = new HashMap<>();
        childMap.put("type", "offer");
        childMap.put("sdp", sdp);

        HashMap<String, Object> childMap1 = new HashMap<>();
        childMap1.put("socketId", socketId);
        childMap1.put("sdp", childMap);

        HashMap<String, Object> map = new HashMap<>();
        map.put("eventName", "__offer");
        map.put("data", childMap1);

        JSONObject object = new JSONObject(map);
        String jsonString = object.toJSONString();
        Log.i(TAG, "sendOffer >" + jsonString);
        socketClient.send(jsonString);
    }

    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        HashMap<String, Object> childMap = new HashMap<>();
        childMap.put("id", iceCandidate.sdpMid);
        childMap.put("label", iceCandidate.sdpMLineIndex);
        childMap.put("candidate", iceCandidate.sdp);
        childMap.put("socketId", socketId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("eventName", "__ice_candidate");
        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        String jsonString = object.toJSONString();
        Log.i(TAG, "sendIceCandidate: " + jsonString);
        socketClient.send(jsonString);


    }

    public void sendAnswer(String socketId, SessionDescription sdp) {
        HashMap<String, Object> childMap = new HashMap<>();
        childMap.put("type", "answer");
        childMap.put("sdp", sdp);

        HashMap<String, Object> childMap1 = new HashMap<>();
        childMap1.put("socketId", socketId);
        childMap1.put("sdp", childMap);

        HashMap<String, Object> map = new HashMap<>();
        map.put("eventName", "__answer");
        map.put("data", childMap1);

        JSONObject object = new JSONObject(map);
        String jsonString = object.toJSONString();
        Log.i(TAG, "sendAnswer >" + jsonString);
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
