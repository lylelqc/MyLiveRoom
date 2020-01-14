package com.test.myliveroom.connection;

import android.media.AudioManager;
import android.util.Log;

import com.test.myliveroom.ChatRoomActivity;
import com.test.myliveroom.socket.JavaWebSocket;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
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

/**
 * p2p连接管理类
 */
public class PeerConnectionManager {
    //    googEchoCancellation   回音消除
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    //    googNoiseSuppression   噪声抑制
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

    //    googAutoGainControl    自动增益控制
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    //    googHighpassFilter     高通滤波器
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";

    private static final String TAG = PeerConnectionManager.class.getSimpleName();

    private List<PeerConnection> mPeerConnections;

    private static final PeerConnectionManager ourInstance = new PeerConnectionManager();
    private boolean videoEnable;
    private ExecutorService executor;
    private PeerConnectionFactory mFactory;
    private ChatRoomActivity context;
    private EglBase eglBase;

    private MediaStream localStream;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    // 获取摄像头设备
    private VideoCapturer videoCapturer;
    // 视频源
    private VideoSource videoSource;
    // 帮助渲染到本地预览
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoTrack localVideoTrack;
    private String myId;

    // ice服务器集合
    private ArrayList<PeerConnection.IceServer> iceServers;
    // 会议室所有用户ID
    private ArrayList<String> connectionIdArray;
    // 会议室每个用户，会对本地实现一个p2p链接peer(peerconnection)
    private Map<String, Peer> connectionPeerDic;
    // 角色 - 邀请者，被邀请者
    // 1v1 别人给你通话
    // 会议室 1.第一次进入 caller
    private Role curRole;
    // 声音服务类
    private AudioManager mAudioManager;

    enum Role {
        Caller,
        Receiver;
    }

    private JavaWebSocket webSocket;

    public static PeerConnectionManager getInstance() {
        return ourInstance;
    }

    private PeerConnectionManager() {
        executor = Executors.newSingleThreadExecutor();
    }

    public void initContext(ChatRoomActivity context, EglBase eglBase){
        this.context = context;
        this.eglBase = eglBase;
        iceServers = new ArrayList<>();
        this.connectionIdArray = new ArrayList<>();
        this.connectionPeerDic = new HashMap<>();

        // https
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:121.41.33.93:3478?transport=udp")
                .setUsername("").setPassword("").createIceServer();
        // http
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder("stun:121.41.33.93:3478?transport=udp")
                .setUsername("ddssingsong").setPassword("123456").createIceServer();
        iceServers.add(iceServer);
        iceServers.add(iceServer1);
    }

    public void onRemoteAnswer(String id, String sdp) {
        // 对方的会话 sdp
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Peer peer = connectionPeerDic.get(id);
                SessionDescription sessionDescription =
                        new SessionDescription(SessionDescription.Type.ANSWER, sdp);
                if(null != peer){
                    // 重点 设置远程sdp,设置本地sdp
                    peer.pc.setRemoteDescription(peer, sessionDescription);
                }
            }
        });
    }

    /**
     * 当别人已在会议室，自己再进入
     * @param id
     * @param candidate
     */
    public void onRemoteIceCandidate(String id, IceCandidate candidate) {
        // 通过socketid 取出连接对象
        Peer peer = connectionPeerDic.get(id);
        if(peer != null){
            peer.pc.addIceCandidate(candidate);
        }

    }

    public void joinToRoom(JavaWebSocket socket, boolean isVideoEnable, ArrayList<String> connections, String id) {
        this.webSocket = socket;
        this.videoEnable = isVideoEnable;
        this.myId = id;
        // peerConnection  大量初始化，需要用子线程
        // 1.会议室已经有人，
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(mFactory == null){
                    mFactory = createConnectionFactory();
                }
                if(localStream == null){
                    createLocalStream();
                }
                connectionIdArray.addAll(connections);
                createPeerConnections();
                // 本地的数据流推向会议室的每一个人的能力
                addStream();
                // 发送邀请
                createOffers();
            }
        });

    }

    /**
     * 为所有连接创建offer
     */
    private void createOffers() {
        // 邀请者
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()){
            // 赋值角色
            curRole = Role.Caller;
            Peer mPeer = entry.getValue();
            // 每个会议室的人发送邀请。 并传递我的数据类型（音/视频）
            // 内部网络请求
            mPeer.pc.createOffer(mPeer, offerOrAnswerConstraint());
        }
    }

    /**
     * 设置传输音视频
     * 音频（）
     * 视频（）
     * @return
     */
    private MediaConstraints offerOrAnswerConstraint() {
        // 媒体约束
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        // 音频 必须传输
        keyValuePairs.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        // 视频随便
        keyValuePairs.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", String.valueOf(videoEnable)));

        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    // 为所有数据添加流
    private void addStream() {
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()){
            if(localStream == null){
                createLocalStream();
            }
            entry.getValue().pc.addStream(localStream);
        }
    }

    /**
     * 建立对会议室每个用户的链接
     */
    private void createPeerConnections() {

        for(String id : connectionIdArray){
            Peer peer = new Peer(id);
            connectionPeerDic.put(id, peer);
        }
    }

    private void createLocalStream() {
        localStream = mFactory.createLocalMediaStream("ARDAMS");
        // 音频
        audioSource = mFactory.createAudioSource(createAudioConstraints());
        // 采集音频
        localAudioTrack = mFactory.createAudioTrack("ARDAMSa0",audioSource);

        localStream.addTrack(localAudioTrack);
        if(videoEnable){
            // 视频源
            videoCapturer = createVideoCapture();
            videoSource = mFactory.createVideoSource(videoCapturer.isScreencast());
            surfaceTextureHelper = SurfaceTextureHelper.create("videoCapturer", eglBase.getEglBaseContext());

            // 初始化videoCapturer
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            // 开始预览，设置摄像头预览参数 宽度  高度  帧率
            videoCapturer.startCapture(320, 240, 10);
            // 视频轨
            localVideoTrack = mFactory.createVideoTrack("ARDAMSv0", videoSource);
            localStream.addTrack(localVideoTrack);

            if(context != null){
                context.setLocalStream(localStream, myId);
            }
        }
    }

    private VideoCapturer createVideoCapture() {
        VideoCapturer videoCapturer = null;
        if(Camera2Enumerator.isSupported(context)){
            Camera2Enumerator enumerator = new Camera2Enumerator(context);
            videoCapturer = createCameraCapture(enumerator);

        }else{
            Camera1Enumerator enumerator = new Camera1Enumerator(true);
            videoCapturer = createCameraCapture(enumerator);
        }
        return videoCapturer;
    }

    // camera2 预览摄像头
    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();
        for(String deviceName :deviceNames){
            if(enumerator.isFrontFacing(deviceName)){
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if(capturer != null){
                    return capturer;
                }
            }
        }
        for(String deviceName :deviceNames){
            if(enumerator.isBackFacing(deviceName)){
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if(capturer != null){
                    return capturer;
                }
            }
        }
        return null;
    }

    private MediaConstraints createAudioConstraints() {
        MediaConstraints audioConstraints = new MediaConstraints();
        // 回音消除
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));

        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));

        return audioConstraints;
    }

    private PeerConnectionFactory createConnectionFactory(){
        // 其他参数设置成默认的
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions());
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
       return PeerConnectionFactory.builder().setOptions(options).setAudioDeviceModule(
                JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
                .setVideoDecoderFactory(decoderFactory)
                .setVideoEncoderFactory(encoderFactory)
                .createPeerConnectionFactory();
    }

    private class Peer implements SdpObserver, PeerConnection.Observer {
        // myid和远程用户之间的链接
        private PeerConnection pc;
        // 其他用户的id
        private String socketId;
        public Peer(String socketId){
            this.socketId = socketId;
            PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
            pc = mFactory.createPeerConnection(configuration, this);
        }

        //内网状态发生改变 4G-->wifi
        @Override
        public void onSignalingChange(PeerConnection.SignalingState state) {

        }

        // 连接上ICE服务器
        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {

        }

        // 调用时机两次，
        // 第一次连接ice服务器时，调用次数是网络中有几个路由节点（1-n）
        // 第二次是有人连接到ice服务器时，调用次数时 通话的人在网络中离ice服务器有几个路由节点
        @Override
        public void onIceCandidate(IceCandidate candidate) {
            // socket --> 去传递
            Log.i(TAG, candidate.toString());
            webSocket.sendIceCandidate(socketId, candidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {

        }

        // p2p建立成功之后 media(音/视频流) 子线程中进行的
        @Override
        public void onAddStream(MediaStream stream) {
            context.onAddRemoteStream(stream, socketId);
        }

        @Override
        public void onRemoveStream(MediaStream stream) {

        }

        @Override
        public void onDataChannel(DataChannel channel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {

        }

        // --------------- sdpobserver ------------------

        @Override
        public void onCreateSuccess(SessionDescription description) {
            // sdp => SessionDescription
            Log.i(TAG, "onCreateSuccess");
            // 设置本地的sdp,成功则回调onSetSuccess
            pc.setLocalDescription(this, description);
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG, "onSetSuccess");
            // 交换彼此的sdp
            if(pc.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER){
                // websocket
                webSocket.sendOffer(socketId, pc.getLocalDescription());
            }
        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    }

    public void toggleSpeaker(boolean mic) {
        if (localAudioTrack != null) {
            // 切换是否允许将本地的麦克风数据推送到远端
            localAudioTrack.setEnabled(mic);
        }
    }


    public void toggleLarge(boolean enableSpeaker) {
        if (mAudioManager != null) {
            mAudioManager.setSpeakerphoneOn(enableSpeaker);
        }
    }

    /**
     *  摄像头切换
     *  调整摄像头前置后置
     */
    public void switchCamera() {
        if (videoCapturer == null)
            return;

        if (videoCapturer instanceof CameraVideoCapturer) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        }

    }

    /**
     * 耗时操作  webrtc内部访问网络
     */
    public void exitRoom() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> myCopy;
                myCopy = (ArrayList) connectionIdArray.clone();
                for (String Id : myCopy) {
                    closePeerConnection( Id);
                }
                // 释放ID集合
                if (connectionIdArray != null) {
                    connectionIdArray.clear();
                }
                // 释放音频源
                if (audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }
                // 释放视频源
                if (videoSource != null) {
                    videoSource.dispose();
                    videoSource = null;
                }
                // 停止预览摄像头
                if (videoCapturer != null) {
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoCapturer.dispose();
                    videoCapturer = null;
                }
                // 释放Surfacetext
                if (surfaceTextureHelper != null) {
                    surfaceTextureHelper.dispose();
                    surfaceTextureHelper = null;
                }
                if (mFactory != null) {
                    mFactory.dispose();
                    mFactory = null;
                }
                if (webSocket != null) {
                    webSocket.close();
                }
            }
        });
    }

    /**
     * 关闭底层连接
     * @param connectionId
     */
    private void closePeerConnection(String connectionId) {
        // 拿到连接的封装对象
        Peer mPeer = connectionPeerDic.get(connectionId);
        if (mPeer != null) {
            // 关闭了P2P连接
            mPeer.pc.close();
        }
        connectionPeerDic.remove(connectionId);
        connectionIdArray.remove(connectionId);
//        通知UI层更新
        context.onCloseWithId(connectionId);
    }
}
