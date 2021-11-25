package com.ydd.yanshi.ui.life;

import android.os.Bundle;

import com.ydd.yanshi.R;
import com.ydd.yanshi.ui.base.BaseActivity;

import fm.jiecao.jcvideoplayer_lib.JCMediaManager;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardSecond;

public class LifeCircleActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_life_circle);
        getSupportActionBar().hide();
    }

    @Override
    public void onBackPressed() {
        // 点返回键退出全屏视频，
        // 如果DiscoverFragment用在其他activity, 也要加上，
        if (JVCideoPlayerStandardSecond.backPress()) {
            JCMediaManager.instance().recoverMediaPlayer();
        } else {
            super.onBackPressed();
        }
    }
}
