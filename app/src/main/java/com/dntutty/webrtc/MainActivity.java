package com.dntutty.webrtc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnClickListener {

    private EditText etRoomID;
    private TextView tvJoinRoom;
    private TextView tvP2P;
    private String[] permissons = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etRoomID = findViewById(R.id.et_room_id);
        tvJoinRoom = findViewById(R.id.tv_join_room);
        tvP2P = findViewById(R.id.tv_p_2_p);
        tvJoinRoom.setOnClickListener(this);
        tvP2P.setOnClickListener(this);
        requestPermission();
    }

    private void requestPermission() {
        ArrayList<String> notGranted = new ArrayList();
        for (String permisson : permissons) {
           if(ActivityCompat.checkSelfPermission(this,permisson)!= PackageManager.PERMISSION_GRANTED)  {
              notGranted.add(permisson);
           }
        }

        if(notGranted.size()>0) {
            ActivityCompat.requestPermissions(this, (String[]) notGranted.toArray(new String[0]),110);
        }
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
