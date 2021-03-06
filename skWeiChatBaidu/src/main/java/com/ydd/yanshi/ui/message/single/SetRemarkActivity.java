package com.ydd.yanshi.ui.message.single;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.Friend;
import com.ydd.yanshi.bean.Label;
import com.ydd.yanshi.broadcast.CardcastUiUpdateUtil;
import com.ydd.yanshi.broadcast.MsgBroadcast;
import com.ydd.yanshi.db.dao.FriendDao;
import com.ydd.yanshi.db.dao.LabelDao;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.ui.base.BaseActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class SetRemarkActivity extends BaseActivity implements View.OnClickListener {
    public static final String ACTION_SET_REMARK = "ACTION_SET_REMARK";

    private String mLoginUserId;
    private String mFriendId;
    @Nullable
    private Friend mFriend;

    private TextView tvRight;
    private EditText mRemarkNameEdit;
    private View rlLabel;
    private TextView tv_setting_label;
    private EditText etDescribe;

    private String originalLabelName;

    private String name, desc;

    public static void start(Context ctx, String friendId) {
        Intent intent = new Intent(ctx, SetRemarkActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendId);
        ctx.startActivity(intent);
    }

    public static void startForResult(Activity ctx, String friendId, String name, String desc, int requestCode) {
        Intent intent = new Intent(ctx, SetRemarkActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friendId);
        intent.putExtra("name", name);
        intent.putExtra("desc", desc);
        ctx.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_remark);
        mLoginUserId = coreManager.getSelf().getUserId();
        mFriendId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
        // ???????????????????????????????????????????????????
        name = getIntent().getStringExtra("name");
        desc = getIntent().getStringExtra("desc");

        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);
        initActionBar();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLabel();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(R.string.finish);
        tvRight.setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.set_remark_and_label);
    }

    private void initView() {
        mRemarkNameEdit = (EditText) findViewById(R.id.department_edit);
        if (!TextUtils.isEmpty(currentRemarkName())) {
            mRemarkNameEdit.setText(currentRemarkName());
        }

        tv_setting_label = findViewById(R.id.tv_setting_label);
        if (mFriend == null) {
            findViewById(R.id.ll1).setVisibility(View.GONE);
        }
        loadLabel();
        originalLabelName = tv_setting_label.getText().toString();
        rlLabel = findViewById(R.id.rlLabel);
        rlLabel.setOnClickListener(v -> {
            Intent intentLabel = new Intent(this, SetLabelActivity.class);
            intentLabel.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
            startActivity(intentLabel);
        });

        etDescribe = (EditText) findViewById(R.id.etDescribe);
        if (!TextUtils.isEmpty(currentDescribe())) {
            etDescribe.setText(currentDescribe());
        }
    }

    private void loadLabel() {
        List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mFriendId);
        String labelNames = "";
        if (friendLabelList != null && friendLabelList.size() > 0) {
            for (int i = 0; i < friendLabelList.size(); i++) {
                if (i == friendLabelList.size() - 1) {
                    labelNames += friendLabelList.get(i).getGroupName();
                } else {
                    labelNames += friendLabelList.get(i).getGroupName() + " ";
                }
            }
            tv_setting_label.setText(labelNames);
            tv_setting_label.setTextColor(getResources().getColor(R.color.app_skin_green));
        } else {
            tv_setting_label.setText(getResources().getString(R.string.remark_tag));
            tv_setting_label.setTextColor(getResources().getColor(R.color.hint_text_color));
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.tv_title_right:
                if (!mRemarkNameEdit.getText().toString().equals(currentRemarkName()) ||
                        !tv_setting_label.getText().toString().equals(currentDescribe())) {
                    // ?????? || ???????????????
                    remarkFriend();
                } else {
                    if (!TextUtils.equals(originalLabelName, tv_setting_label.getText().toString())) {
                        // ???????????????
                        setResult(RESULT_OK);
                    } else {
                        finish();
                    }
                }
                break;
        }
    }

    @Nullable
    private String currentRemarkName() {
        if (mFriend == null) {
            return name;
        }
        return mFriend.getRemarkName();
    }

    @Nullable
    private String currentDescribe() {
        if (mFriend == null) {
            return desc;
        }
        return mFriend.getDescribe();
    }

    private void remarkFriend() {
        String remarkName = mRemarkNameEdit.getText().toString();
        String describe = etDescribe.getText().toString();

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("remarkName", remarkName);
        params.put("describe", describe);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_REMARK)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(SetRemarkActivity.this, result)) {
                            FriendDao.getInstance().updateRemarkNameAndDescribe(
                                    mLoginUserId, mFriendId, remarkName, describe);

                            MsgBroadcast.broadcastMsgUiUpdate(mContext);
                            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                            Intent intent = new Intent(com.ydd.yanshi.broadcast.OtherBroadcast.NAME_CHANGE);
                            intent.putExtra("remarkName", remarkName);
                            intent.putExtra("describe", describe);
                            LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(intent);

                            setResult(RESULT_OK);
                            finish();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_change_remark_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
