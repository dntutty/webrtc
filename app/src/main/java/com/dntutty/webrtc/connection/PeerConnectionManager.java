package com.dntutty.webrtc.connection;

import org.webrtc.PeerConnection;

import java.util.List;

public class PeerConnectionManager {
    private List<PeerConnection> peerConnections;
    private PeerConnectionManager() {

    }
    private static final PeerConnectionManager instance = new PeerConnectionManager();
    public static PeerConnectionManager getInstance() {
        return instance;
    }
}
