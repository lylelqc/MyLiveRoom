package com.test.myliveroom;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText et_signal;
    private EditText et_port;
    private EditText et_room;

    private EditText edit_test_wss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_signal = findViewById(R.id.et_signal);
        et_port = findViewById(R.id.et_port);
        et_room = findViewById(R.id.et_room);
        edit_test_wss = findViewById(R.id.et_wss);

        et_room.setText("123456");
    }


    public void JoinRoom(View view) {
        WebRTCManager webRTCManager = WebRTCManager.getInstance();
        webRTCManager.connect(this,  et_room.getText().toString().trim());
    }

    public void JoinRoomSingleAudio(View view) {
//        WebrtcUtil.callSingle(this,
//                et_signal.getText().toString(),
//                et_room.getText().toString().trim() + ":" + et_port.getText().toString().trim(),
//                false);
    }

    public void JoinRoomSingleVideo(View view) {
//        WebrtcUtil.callSingle(this,
//                et_signal.getText().toString(),
//                et_room.getText().toString().trim() + ":" + et_port.getText().toString().trim(),
//                true);
    }

    //test wss
    public void wss(View view) {
//        WebrtcUtil.testWs(edit_test_wss.getText().toString());
    }


}
