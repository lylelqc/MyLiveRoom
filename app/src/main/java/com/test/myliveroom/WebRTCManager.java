package com.test.myliveroom;

import android.app.Activity;

import com.test.myliveroom.connection.PeerConnectionManager;
import com.test.myliveroom.socket.JavaWebSocket;

import org.webrtc.EglBase;

/**
 * 管理socket和peerConnection
 */
public class WebRTCManager {

    private JavaWebSocket mWebSocket;

    private PeerConnectionManager mPeerConnectionManager;

    private static final WebRTCManager ourInstance = new WebRTCManager();
    private String roomID;

    public static WebRTCManager getInstance() {
        return ourInstance;
    }

    private WebRTCManager() {

    }

    public void connect(Activity activity, String roomId){
        this.roomID = roomId;
        mWebSocket = new JavaWebSocket(activity);
        mPeerConnectionManager = PeerConnectionManager.getInstance();
        // websocket协议开头 wss
        mWebSocket.connect("wss://121.41.33.93/wss");
    }

    public void joinRoom(CharRoomActivity activity, EglBase rootEglBase) {
        mPeerConnectionManager.initContext(activity, rootEglBase);
        mWebSocket.joinRoom(roomID);
    }
}
