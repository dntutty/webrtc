package com.dntutty.webrtc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ChatRoomActivity extends AppCompatActivity {

    private FrameLayout wrVideoLayout;
    private WebRTCManager webRTCManager;
    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.PARCELABLE_WRITE_RETURN_VALUE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_char_room);
        initView();
    }

    private void initView() {
        wrVideoLayout = findViewById(R.id.wr_video_view);
        wrVideoLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webRTCManager = WebRTCManager.getInstance();
        //获取权限 todo
        webRTCManager.joinRoom(this);

    }
}
