package com.dntutty.webrtc.connection;

import android.content.Context;

import com.dntutty.webrtc.ChatRoomActivity;
import com.dntutty.webrtc.socket.JavaWebSocket;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerConnectionManager {
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


    private PeerConnectionManager() {
        executor = Executors.newSingleThreadExecutor();
    }

    private static final PeerConnectionManager instance = new PeerConnectionManager();

    public static PeerConnectionManager getInstance() {
        return instance;
    }

    public void initContext(ChatRoomActivity context, EglBase eglBase) {
        this.eglBase = eglBase;
        this.context = context;
    }

    public void joinToRoom(JavaWebSocket javaWebSocket, boolean isVideoEnable, ArrayList<String> connections, String mineId) {
        this.videoEnable = isVideoEnable;
        this.mineId = mineId;
        //PeerConnection
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (factory == null) {
                    factory = createConnectionFactory();
                }

                if (localStream == null) {
                    createLocalStream();
                }

            }
        });
    }

    private void createLocalStream() {
        localStream = factory.createLocalMediaStream("ARDAMS");

        //音频
        audioSource = factory.createAudioSource(createAudioConstraints());
        //采集音频
        localAudioTrack = factory.createAudioTrack("ARDAMSa0",audioSource);
        localStream.addTrack(localAudioTrack);
        if (videoEnable) {
            //视频源
            videoCapturer = createVideoCapture();
            videoSource = factory.createVideoSource(videoCapturer.isScreencast());
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread",eglBase.getEglBaseContext());

            videoCapturer.initialize(surfaceTextureHelper,context,videoSource.getCapturerObserver());
            //摄像头预览的宽度 高度 帧率
            videoCapturer.startCapture(320,240,10);
            //视频轨
            localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);

            localStream.addTrack(localVideoTrack);
            if (context!=null) {
                context.onSetLocalStream(localStream,mineId);
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
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName,null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }

            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName,null);
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
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT,"true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT,"true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT,"false"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT,"false"));
        return audioConstraints;

    }

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
}
