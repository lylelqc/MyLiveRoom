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

    public void connect(Activity activity, String signal, String roomId){
        this.roomID = roomId;
        mWebSocket = new JavaWebSocket(activity);
        mPeerConnectionManager = PeerConnectionManager.getInstance();
        // websocket协议开头 wss
        mWebSocket.connect(signal);
    }

    public void joinRoom(ChatRoomActivity activity, EglBase rootEglBase) {
        mPeerConnectionManager.initContext(activity, rootEglBase);
        mWebSocket.joinRoom(roomID);
    }

    public void toggleMic(boolean enableMic) {
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.toggleSpeaker(enableMic);
        }
    }

    public void toggleLarge(boolean enableSpeaker) {
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.toggleLarge(enableSpeaker);
        }
    }


    public void switchCamera() {
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.switchCamera();
        }
    }

    /*
    退出房间
     */
    public void exitRoom() {
        if (mPeerConnectionManager != null) {
            mWebSocket = null;
            mPeerConnectionManager.exitRoom();
        }
    }
}
