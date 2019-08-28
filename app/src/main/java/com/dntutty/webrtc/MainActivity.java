package com.dntutty.webrtc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private EditText etRoomID;
    private TextView tvJoinRoom;
    private TextView tvP2P;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etRoomID = findViewById(R.id.et_room_id);
        tvJoinRoom = findViewById(R.id.tv_join_room);
        tvP2P = findViewById(R.id.tv_p_2_p);
        tvJoinRoom.setOnClickListener(this);
        tvP2P.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_join_room:
                joinRoom(etRoomID.getText().toString());
                break;
            case R.id.tv_p_2_p:
                break;
        }
    }

    public void joinRoom(String roomId) {
        WebRTCManager.getInstance().connect(this,roomId);
    }
}
