package com.dntutty.webrtc.connection;

import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;

import com.dntutty.webrtc.ChatRoomActivity;
import com.dntutty.webrtc.socket.JavaWebSocket;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerConnectionManager {
    private static final String TAG = "Webrtc_peer";
    private List<PeerConnection> peerConnections;
    private boolean videoEnable;
    private ExecutorService executor;
    private PeerConnectionFactory factory;
    private ChatRoomActivity context;
    private EglBase eglBase;

    private MediaStream localStream;
    //googEchoCancellation 回音消除
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    //googNoiseSuppression 噪声抑制
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    //googAutoGainControl 自动增益控制
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    //googHighpassFilter 高通滤波器
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    //获取摄像头设备   camera1 camera2
    private VideoCapturer videoCapturer;
    //视频源
    private VideoSource videoSource;
    //帮助渲染到本地预览
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoTrack localVideoTrack;
    private String mineId;
    //  ICE服务器的集合
    private ArrayList<PeerConnection.IceServer> iceServers;
    //  会议室的所有用户id
    private ArrayList<String> connectionIds = new ArrayList<>();
    //    会议室的每一个用户 会对本地实现一个p2p连接Peer (PeerConnection)
    private Map<String, Peer> connectionPeers = new HashMap<>();

    //    角色 邀请者    被邀请者  1v1通话    别人给你音视频通话  Receiver
//    会议室通话     第一次进入会议室  Caller
//    当你已经加入会议室  别人进入会议室        receiver
//    角色
    private Role role;




    enum Role {Caller, Receiver}

    private JavaWebSocket webSocket;


    private PeerConnectionManager() {
        executor = Executors.newSingleThreadExecutor();
        iceServers = new ArrayList<>();
    }

    private static final PeerConnectionManager instance = new PeerConnectionManager();

    public static PeerConnectionManager getInstance() {
        return instance;
    }

    public void initContext(ChatRoomActivity context, EglBase eglBase) {
        this.eglBase = eglBase;
        this.context = context;
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:47.98.186.185:3478?transport=udp").setUsername("").setPassword("").createIceServer();
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder("turn:47.98.186.185:3478?transport=udp").setUsername("lsx").setPassword("123456").createIceServer();
        iceServers.add(iceServer);
        iceServers.add(iceServer1);

    }

    public void joinToRoom(JavaWebSocket webSocket, boolean isVideoEnable, ArrayList<String> connections, String mineId) {
        this.videoEnable = isVideoEnable;
        this.mineId = mineId;
        this.webSocket = webSocket;
        //PeerConnection    情况1  会议室已经有人 的情况
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (factory == null) {
                    factory = createConnectionFactory();
                }

                if (localStream == null) {
                    createLocalStream();
                }

                connectionIds.addAll(connections);
                createPeerConnections();
//                本地的数据流推向会议室的每一个人
                addStreams();
//                发送邀请
                createOffers();
            }
        });
    }

    /**
     * 为所有连接创建offer
     */
    private void createOffers() {
//        邀请者
        for (Map.Entry<String, Peer> peerEntry : connectionPeers.entrySet()) {
//            赋值角色
            role = Role.Caller;
            Peer peer = peerEntry.getValue();
//            每一位会议室的人发送邀请，并且传递我的数据类型（音频 视频的选择）
            peer.peerConnection.createOffer(peer, offerOrAnswerConstraint());
        }
    }

    /**
     * 设置传输音视频
     * 音频
     * 视频（false)
     *
     * @return
     */
    private MediaConstraints offerOrAnswerConstraint() {
//        媒体约束
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
//          音频 视频
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", String.valueOf(videoEnable)));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    /**
     * 向所有链接添加流数据
     */
    private void addStreams() {
        Log.i(TAG, "addStreams: 向所有链接添加流数据");
        for (Map.Entry<String, Peer> entry : connectionPeers.entrySet()) {
            if (localStream == null) {
                createLocalStream();
            }
            entry.getValue().peerConnection.addStream(localStream);
        }
    }

    /**
     * 建立与会议室每一个用户的连接
     */
    private void createPeerConnections() {
        for (String id : connectionIds) {
            Peer peer = new Peer(id);
            connectionPeers.put(id, peer);
        }
    }

    private void createLocalStream() {
        localStream = factory.createLocalMediaStream("ARDAMS");

        //音频
        audioSource = factory.createAudioSource(createAudioConstraints());
        //采集音频
        localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        localStream.addTrack(localAudioTrack);
        if (videoEnable) {
            //视频源
            videoCapturer = createVideoCapture();
            videoSource = factory.createVideoSource(videoCapturer.isScreencast());
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            //摄像头预览的宽度 高度 帧率
            videoCapturer.startCapture(320, 240, 10);
            //视频轨
            localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);

            localStream.addTrack(localVideoTrack);
            if (context != null) {
                context.onSetLocalStream(localStream, mineId);
            }
        }
    }

    private VideoCapturer createVideoCapture() {
        VideoCapturer videoCapturer = null;
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(context)) {
            enumerator = new Camera2Enumerator(context);
            videoCapturer = createCameraCapture(enumerator);
        } else {
            enumerator = new Camera1Enumerator(true);
            videoCapturer = createCameraCapture(enumerator);
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {

        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }

            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }

            }
        }
        return null;
    }

    private MediaConstraints createAudioConstraints() {
        MediaConstraints audioConstraints = new MediaConstraints();
        //添加回音消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
        return audioConstraints;

    }

    /**
     * 创建PeerConnection的工厂类
     * @return
     */
    private PeerConnectionFactory createConnectionFactory() {
        VideoEncoderFactory encoderFactory;
        encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);

        VideoDecoderFactory decoderFactory;
        decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        //其他参数设置成默认的
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        return PeerConnectionFactory.builder().setOptions(options)
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    /**
     * 当别人在会议室 我再进去
     *
     * @param socketId
     * @param iceCandidate
     */
    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {
//        通过socketId 取出连接对象
        Peer peer = connectionPeers.get(socketId);
        if (peer != null) {
            peer.peerConnection.addIceCandidate(iceCandidate);
        }
    }

    public void onConnection(String socketId) {
        connectionIds.add(socketId);
        Peer peer = new Peer(socketId);
        connectionPeers.put(socketId, peer);
        if (localStream == null) {
            createLocalStream();
        }
        peer.peerConnection.addStream(localStream);
    }

    public void onReceiveOffer(String socketId, String sdp) {
        Peer peer = connectionPeers.get(socketId);
        if (null != peer) {
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, sdp);
            peer.peerConnection.setRemoteDescription(peer,sessionDescription);
            peer.peerConnection.createAnswer(peer,offerOrAnswerConstraint());
        }
    }


    /**
     * @param socketId
     * @param description
     */
    public void onReceiveAnswer(String socketId, String description) {
//        对方的回话 sdp
        Peer peer = connectionPeers.get(socketId);
        SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, description);
        if (peer != null) {
            peer.peerConnection.setRemoteDescription(peer, sessionDescription);
        }

    }

    private class Peer implements SdpObserver, PeerConnection.Observer {
        //        mineId 跟远端用户之间的连接
        private PeerConnection peerConnection;
        //        socketid是其他用户的
        private String socketId;

        public Peer(String socketId) {
            this.socketId = socketId;
            PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
            peerConnection = factory.createPeerConnection(configuration, this);
        }

        //        内网状态发生改变 如音视频通话中 4G---->切换成wifi
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        //        连接上了ICE服务器
        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.i(TAG, "onIceConnectionChange: ");
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        //        onIceCandidate 调用的时机有两次 第一次在链接到ICE服务器的时候，调用次数是网络中有多少个路由节点（1-n)
//        第二次（有人进入这个房间）对方到ICE服务器的路由节点 调用次数是 视频通话的人在网络中离ice服务器有多少个路由节点（1-n)
        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
//            socket  >> 传递
            Log.i(TAG, "onIceCandidate: ");
            webSocket.sendIceCandidate(socketId, iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        //        p2p建立成功之后 mediaStream(视频流 音频流）子线程
        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.i(TAG, "onAddStream: ");
            context.onAddRemoteStream(mediaStream, socketId);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

//-------------------------SDPObserver-------------------------------------------->

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.i(TAG, "onCreateSuccess: ");
//            设置本地的SDP 如果设置成功则回调onSetSuccess
            peerConnection.setLocalDescription(this, sessionDescription);
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG, "onSetSuccess: ");
//            交换彼此的sdp iceCandiadate
            if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
//                websocket
                webSocket.sendOffer(socketId, peerConnection.getLocalDescription());

            }

            if(peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
                webSocket.sendAnswer(socketId,peerConnection.getLocalDescription());
            }
        }

        @Override
        public void onCreateFailure(String s) {
            Log.i(TAG, "onCreateFailure: ");
        }

        @Override
        public void onSetFailure(String s) {
            Log.i(TAG, "onSetFailure: ");
        }
    }
}
