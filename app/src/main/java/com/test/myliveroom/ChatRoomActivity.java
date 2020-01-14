package com.test.myliveroom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private FrameLayout wrVideoLayout;
    private WebRTCManager webRTCManager;
    private EglBase rootEglBase;
    private VideoTrack localVideoTrack;
    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();
    private List<String> person = new ArrayList<>();
    private int mScreenWidth;
    private final String TAG = "tag";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        initView();
        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
        replaceFragment(chatRoomFragment);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();

    }

    private void initView() {
        rootEglBase = EglBase.create();
        wrVideoLayout = findViewById(R.id.wr_video_view);
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (manager != null) {
            mScreenWidth = manager.getDefaultDisplay().getWidth();
        }
        wrVideoLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, mScreenWidth));
        webRTCManager = WebRTCManager.getInstance();
        // 权限判断
        if(!PermissionUtil.isNeedRequestPermission(this)){
            webRTCManager.joinRoom(this, rootEglBase);
        }
    }

    public static void openActivity(Activity activity){
        Intent intent = new Intent(activity, ChatRoomActivity.class);
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
                Log.i(TAG, "setLocalStream");
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
            SurfaceViewRenderer renderer1 = videoViews.get(id);
            if(renderer1 != null){
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                layoutParams.height = getWidth(size);
                layoutParams.width = getWidth(size);
                layoutParams.leftMargin = getX(size, i);
                layoutParams.topMargin = getY(size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }

    public void onAddRemoteStream(MediaStream stream, String id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onAddRemoteStream");
                addView(id, stream);
            }
        });
    }

    public void toggleMic(boolean enableMic) {
        webRTCManager.toggleMic(enableMic);
    }

    public void toggleLarge(boolean enableSpeaker) {
        webRTCManager.toggleLarge(enableSpeaker);
    }

    public void toggleCamera(boolean enableCamera) {
        if (localVideoTrack != null) {
            // 直接控制videotrack, 也可以通过webrtcManager桥接
            // 自动关闭摄像头内容
            localVideoTrack.setEnabled(enableCamera);
        }
    }

    public void switchCamera() {
        webRTCManager.switchCamera();
    }

    public void hangUp() {
        exit();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        exit();
        super.onDestroy();
    }

    private void exit() {
        webRTCManager.exitRoom();
        for (SurfaceViewRenderer renderer : videoViews.values()) {
            renderer.release();
        }
        videoViews.clear();
        person.clear();
    }

    public void onCloseWithId(String connectionId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeView(connectionId);
            }
        });
    }

    private void removeView(String userId) {
        // 找到会议对应的人的布局
        SurfaceViewRenderer renderer =  videoViews.get(userId);
        if (renderer != null) {
            //释放surfaceView
            renderer.release();
            videoViews.remove(userId);
            person.remove(userId);
            wrVideoLayout.removeView(renderer);

            // 重新刷新布局
            // 宽度 和高度  size = 1
            int size = videoViews.size();
            for (int i = 0; i < size; i++) {
                String peerId = person.get(i);
                SurfaceViewRenderer renderer1 = videoViews.get(peerId);
                if (renderer1 != null) {
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    layoutParams.height = getWidth(size);
                    layoutParams.width = getWidth(size);
                    layoutParams.leftMargin = getX(size, i);
                    layoutParams.topMargin = getY(size, i);
                    renderer1.setLayoutParams(layoutParams);
                }
            }
        }
    }

    private int getWidth(int size) {
        if (size <= 4) {
            return mScreenWidth / 2;
        } else if (size <= 9) {
            return mScreenWidth / 3;
        }
        return mScreenWidth / 3;
    }

    private int getX(int size, int index) {
        if (size <= 4) {
            if (size == 3 && index == 2) {
                return mScreenWidth / 4;
            }
            return (index % 2) * mScreenWidth / 2;
        } else if (size <= 9) {
            if (size == 5) {
                if (index == 3) {
                    return mScreenWidth / 6;
                }
                if (index == 4) {
                    return mScreenWidth / 2;
                }
            }

            if (size == 7 && index == 6) {
                return mScreenWidth / 3;
            }

            if (size == 8) {
                if (index == 6) {
                    return mScreenWidth / 6;
                }
                if (index == 7) {
                    return mScreenWidth / 2;
                }
            }
            return (index % 3) * mScreenWidth / 3;
        }
        return 0;
    }

    private int getY(int size, int index) {
        if (size < 3) {
            return mScreenWidth / 4;
        } else if (size < 5) {
            if (index < 2) {
                return 0;
            } else {
                return mScreenWidth / 2;
            }
        } else if (size < 7) {
            if (index < 3) {
                return mScreenWidth / 2 - (mScreenWidth / 3);
            } else {
                return mScreenWidth / 2;
            }
        } else if (size <= 9) {
            if (index < 3) {
                return 0;
            } else if (index < 6) {
                return mScreenWidth / 3;
            } else {
                return mScreenWidth / 3 * 2;
            }
        }
        return 0;
    }

}
