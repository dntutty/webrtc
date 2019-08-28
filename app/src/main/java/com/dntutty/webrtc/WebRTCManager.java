package com.dntutty.webrtc;

import com.dntutty.webrtc.connection.PeerConnectionManager;
import com.dntutty.webrtc.socket.JavaWebSocket;

import org.webrtc.EglBase;

public class WebRTCManager {
    private JavaWebSocket webSocket;
    private PeerConnectionManager peerConnectionManager;
    private String roomId = "";
    private static final WebRTCManager instance = new WebRTCManager();
    public static WebRTCManager getInstance() {
        return instance;
    }
    private WebRTCManager() {}

    public void connect(MainActivity activity,String roomId) {
        this.roomId = roomId;
        webSocket = new JavaWebSocket(activity);
        peerConnectionManager = PeerConnectionManager.getInstance();
        webSocket.connect("wss://47.98.186.185/wss");
    }

    public void joinRoom(ChatRoomActivity chatRoomActivity, EglBase eglBase) {
        peerConnectionManager.initContext(chatRoomActivity,eglBase);
        webSocket.joinRoom(roomId);
    }
}
