package com.test.myliveroom;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.join_room).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinRoom();
            }
        });
    }


    private void joinRoom() {
        WebRTCManager webRTCManager = WebRTCManager.getInstance();
        webRTCManager.connect(this, "123456");
    }


}
