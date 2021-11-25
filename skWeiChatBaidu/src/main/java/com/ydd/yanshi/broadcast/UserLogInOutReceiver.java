package com.ydd.yanshi.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ydd.yanshi.helper.LoginHelper;
import com.ydd.yanshi.ui.MainActivity;

/**
 * Created by Administrator on 2016/7/14.
 * 监听用户登录状态广播
 */
public class UserLogInOutReceiver extends BroadcastReceiver {
    private static final String TAG = "UserLogInOutReceiver";

    private MainActivity activity;

    public UserLogInOutReceiver(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]");
        String action = intent.getAction();
        if (action.equals(LoginHelper.ACTION_LOGIN)) {
            activity.login();
        } else if (action.equals(LoginHelper.ACTION_LOGOUT)) {
            activity.loginOut();
        } else if (action.equals(LoginHelper.ACTION_CONFLICT)) {
            activity.conflict();
        } else if (action.equals(LoginHelper.ACTION_NEED_UPDATE)) {
            activity.need_update();
        } else if (action.equals(LoginHelper.ACTION_LOGIN_GIVE_UP)) {
            activity.login_give_up();
        }
    }
}
