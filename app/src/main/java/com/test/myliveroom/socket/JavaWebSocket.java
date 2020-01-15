package com.test.myliveroom.socket;

import android.app.Activity;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.test.myliveroom.ChatRoomActivity;
import com.test.myliveroom.connection.PeerConnectionManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class JavaWebSocket {
    private WebSocketClient mSocketClient;
    private Activity activity;
    private SSLSocketFactory factory;
    private PeerConnectionManager peerConnectionManager;
    private static final String TAG = "TAG";

    public JavaWebSocket(Activity activity) {
        this.activity = activity;
    }

    public void connect(String wss){
        peerConnectionManager = PeerConnectionManager.getInstance();
        URI uri = null;
        try{
            uri = new URI(wss);
        }catch (Exception e){
            e.printStackTrace();
        }
        // 需加密
        mSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("TAG", "onOpen");
                ChatRoomActivity.openActivity(activity);
            }

            @Override
            public void onMessage(String message) {
                Log.i("TAG", "onMessage - " + message);
                handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("TAG", "onClose");
            }

            @Override
            public void onError(Exception ex) {
                Log.i("TAG", "onError");
            }
        };

        if(wss.startsWith("wss")){
            // 开始加密
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                if(sslContext != null){
                    factory = sslContext.getSocketFactory();
                }
                if(factory != null){
                    mSocketClient.setSocket(factory.createSocket());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        mSocketClient.connect();
    }

    // 忽略证书
    public static class TrustManagerTest implements X509TrustManager{

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private void handleMessage(String message) {
        Map map = JSON.parseObject(message);
        String eventName = (String) map.get("eventName");
        Log.i(TAG, "handleMessage: " + eventName);
        // p2p通信
        if(eventName.equals("_peers")){
            handleJoinRoom(map);
        }

        // 对方的ice_candidate
        if(eventName.equals("_ice_candidate")){
            handleRemaoteCandidate(map);
        }

        // 对方的sdp
        if(eventName.equals("_answer")){
            handleAnswer(map);
        }
        // 新的人加入
        if (eventName.equals("_new_peer")) {
            handleRemoteInRoom(map);
        }
        // 接受邀请
        if (eventName.equals("_offer")) {
            handleOffer(map);
        }

        // 移除房间
        if (eventName.equals("_remove_peer")) {
            handleRemoveRoom(map);
        }
    }

    private void handleRemoveRoom(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String socketId = (String) data.get("socketId");
            peerConnectionManager.onRemoveRoom(socketId);

        }
    }

    // 会议室发过来新的连接之后，随后会给你发送一个offer    被叫
    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
        Map sdpDic;
        if (data != null) {
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpDic.get("sdp");
            peerConnectionManager.onReceiveOffer(socketId, sdp);

        }
    }

    // 自己已经在房间，有人进来
    private void handleRemoteInRoom(Map map) {
        Log.i(TAG, " 在房间，有人进来   handleRemoteInRoom: ");
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            peerConnectionManager.onRemoteJoinToRoom(socketId);
        }
    }

    private void handleAnswer(Map map) {
        Log.i(TAG, " 5  JavaWebSocket  handleAnswer: ");
        Map data = (Map) map.get("data");
        Map sdpDic;
        if(data != null){
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpDic.get("sdp");
            peerConnectionManager.onRemoteAnswer(socketId, sdp);
        }
    }

    // 对方的ice_candidate
    private void handleRemaoteCandidate(Map map) {
        Log.i(TAG, "JavaWebSocket  6   handleRemoteCandidate: ");
        Map data = (Map) map.get("data");
        String socketId;
        if(data != null){
            socketId = (String) data.get("socketId");
            String sdpMid = (String) data.get("id");
            sdpMid = (null == sdpMid) ? "video" : sdpMid;
            int sdpMLineIndex = (int) Double.parseDouble(String.valueOf(data.get("label")));
            String candidate = (String) data.get("candidate");
            // candidate对象
            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
            peerConnectionManager.onRemoteIceCandidate(socketId, iceCandidate);
        }
    }

    /**
     * 加入房间
     * @param map
     */
    private void handleJoinRoom(Map map) {
        Log.i(TAG, "  1  JavaWebSocket   handleJoinToRoom: ");
        Map data = (Map) map.get("data");
        JSONArray arr;
        if(data != null){
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            Log.i("TAG", js);
            ArrayList<String> connections =
                    (ArrayList<String>) JSONObject.parseArray(js, String.class);
            String myID = (String) map.get("you");

            peerConnectionManager.joinToRoom(this, true, connections, myID);
        }
    }

    /**
     * 请求参数
     * __join __answer
     * __offer
     * __ice_candidate
     * __peer
     * @param id
     */
    public void joinRoom(String id) {
        // 传入设定好的json格式
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", id);

        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        map.put("data", childMap);
        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toJSONString().toString();
        Log.i(TAG, "send-->join");
        mSocketClient.send(jsonString);
    }

    public void sendAnswer(String socketId, String description) {
        // 传入设定好的json格式
        HashMap<String, Object> childMap1 = new HashMap();
        childMap1.put("type", "answer");
        childMap1.put("sdp", description);

        HashMap<String, Object> childMap2 = new HashMap();
        childMap2.put("socketId", socketId);
        childMap2.put("sdp", childMap1);
        HashMap<String, Object> map = new HashMap();
        map.put("eventName", "__answer");
        map.put("data", childMap2);

        JSONObject object = new JSONObject(map);
        String jsonString  = object.toString();
        Log.i(TAG, "send-->answer");
        mSocketClient.send(jsonString);
    }

    public void sendOffer(String id, String description) {
        // 传入设定好的json格式
        Map<String, Object> childMap1 = new HashMap<>();
        childMap1.put("type", "offer");
        childMap1.put("sdp", description);

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("socketId", id);
        childMap.put("sdp", childMap1);

        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__offer");
        map.put("data", childMap);
        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toJSONString().toString();
        Log.i(TAG, "send-->offer");
        mSocketClient.send(jsonString);
    }

    public void sendIceCandidate(String id, IceCandidate candidate) {
        // 传入设定好的json格式
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("id", candidate.sdpMid);
        childMap.put("label", candidate.sdpMLineIndex);
        childMap.put("candidate", candidate.sdp);
        childMap.put("socketId", id);

        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__ice_candidate");
        map.put("data", childMap);
        JSONObject jsonObject = new JSONObject(map);
        String jsonString = jsonObject.toJSONString().toString();
        Log.i(TAG, "send-->IceCandidate");
        mSocketClient.send(jsonString);
    }

    public void close() {
        if (mSocketClient != null) {
            mSocketClient.close();
        }
    }
}