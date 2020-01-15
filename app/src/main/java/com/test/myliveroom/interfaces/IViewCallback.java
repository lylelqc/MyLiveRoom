package com.test.myliveroom.interfaces;

import org.webrtc.MediaStream;

public interface IViewCallback {

    void setLocalStream(MediaStream stream, String userId);

    void onAddRemoteStream(MediaStream stream, String id);

    void onCloseWithId(String connectionId);


}
