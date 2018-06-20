package com.bokecc.dwlivedemo_new.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.bokecc.dwlivedemo_new.DWApplication;
import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.view.PilotButton;
import com.bokecc.sdk.mobile.live.util.HttpUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 观看直播 & 观看回放 入口选择页
 */
public class PilotActivity extends AppCompatActivity {

    @BindView(R.id.btn_start_live)
    PilotButton btnStartLive;    // 观看直播 按钮

    @BindView(R.id.btn_start_replay)
    PilotButton btnStartReplay;   // 观看回放 按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_pilot);

        HttpUtil.LOG_LEVEL = HttpUtil.HttpLogLevel.DETAIL;

        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_start_live, R.id.btn_start_replay})
    void onClick(View v) {
        Intent intent = new Intent(this, LoginActivity.class);
        int fragmentIndex = 0;
        switch (v.getId()) {
            case R.id.btn_start_live:
                fragmentIndex = 0;
                break;
            case R.id.btn_start_replay:
                fragmentIndex = 1;
                break;
        }
        intent.putExtra("fragmentIndex", fragmentIndex);
        startActivity(intent);
    }
}
