package com.test.myliveroom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharRoomActivity extends AppCompatActivity {

    private FrameLayout wrVideoLayout;
    private WebRTCManager webRTCManager;
    private EglBase rootEglBase;
    private VideoTrack localVideoTrack;
    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();
    private List<String> person = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_room);
        initView();
    }

    private void initView() {
        rootEglBase = EglBase.create();
        wrVideoLayout = findViewById(R.id.wr_video_view);
        wrVideoLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webRTCManager = WebRTCManager.getInstance();
        // 权限判断
        if(!PermissionUtil.isNeedRequestPermission(this)){
            webRTCManager.joinRoom(this, rootEglBase);
        }
    }

    public static void openActivity(Activity activity){
        Intent intent = new Intent(activity, CharRoomActivity.class);
        activity.startActivity(intent);
    }

    /**
     *
     * @param stream 本地流
     * @param id 自己的id
     */
    public void setLocalStream(MediaStream stream, String id){
        List<VideoTrack> videoTracks = stream.videoTracks;
        if(videoTracks.size() > 0){
            localVideoTrack = videoTracks.get(0);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(id, stream);
            }
        });
    }

    /**
     *
     * @param id  用户id
     * @param stream 视频流 本地或者远端
     *   有多少人就调用多少次
     */
    private void addView(String id, MediaStream stream) {
        // 使用webrtc提供的surfaceview
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(this);
        // 初始化surface view
        renderer.init(rootEglBase.getEglBaseContext(), null);
        // 设置缩放模式 按照view的宽度和高度设置
        // SCALE_ASPECT_FILL 按照摄像头的预览画面大小设置
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        // 摄像头翻转
        renderer.setMirror(true);
        // 将摄像头的数据渲染到surfaceviewreander
        if(stream.videoTracks.size() > 0){
            stream.videoTracks.get(0).addSink(renderer);
        }

        // 会议室 1+N人
        videoViews.put(id, renderer);
        person.add(id);
        // 添加到frame layout中 width&height = 0
        wrVideoLayout.addView(renderer);

        // 指定宽 高
        int size = videoViews.size();
        for(int i = 0; i < size; i++){
            String peerId = person.get(i);
            SurfaceViewRenderer renderer1 = videoViews.get(i);
            if(renderer1 != null){
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                layoutParams.height = Utils.getWidth(this, size);
                layoutParams.width = Utils.getWidth(this, size);
                layoutParams.leftMargin = Utils.getX(this, size, i);
                layoutParams.topMargin = Utils.getY(this, size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }

    public void onAddRemoteStream(MediaStream stream, String id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(id, stream);
            }
        });
    }
}
