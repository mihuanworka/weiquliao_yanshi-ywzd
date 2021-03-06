package com.ydd.yanshi.ui.message.single;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.qrcode.Constant;
import com.suke.widget.SwitchButton;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.Reporter;
import com.ydd.yanshi.bean.Friend;
import com.ydd.yanshi.bean.Label;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.bean.message.XmppMessage;
import com.ydd.yanshi.broadcast.MsgBroadcast;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.db.dao.ChatMessageDao;
import com.ydd.yanshi.db.dao.FriendDao;
import com.ydd.yanshi.db.dao.LabelDao;
import com.ydd.yanshi.helper.AvatarHelper;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.pay.TransferRecordActivity;
import com.ydd.yanshi.ui.MainActivity;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.groupchat.SelectContactsActivity;
import com.ydd.yanshi.ui.message.search.SearchChatHistoryActivity;
import com.ydd.yanshi.ui.other.BasicInfoActivity;
import com.ydd.yanshi.util.Constants;
import com.ydd.yanshi.util.LogUtils;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.view.MsgSaveDaysDialog;
import com.ydd.yanshi.view.SelectionFrame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

/**
 * Created by Administrator on 2018/4/18 0018.
 */

public class PersonSettingActivity extends BaseActivity implements View.OnClickListener {

    private ImageView mFriendAvatarIv;
    private TextView mFriendNameTv;
    private TextView mRemarkNameTv;
    private TextView mLabelNameTv;
    private SwitchButton mIsReadFireSb;
    private SwitchButton mTopSb;
    private SwitchButton mIsDisturbSb;
    private TextView mMsgSaveDays;

    private String mLoginUserId;
    private String mFriendId;

    MsgSaveDaysDialog.OnMsgSaveDaysDialogClickListener onMsgSaveDaysDialogClickListener = new MsgSaveDaysDialog.OnMsgSaveDaysDialogClickListener() {
        @Override
        public void tv1Click() {
            updateChatRecordTimeOut(-1);
        }

        @Override
        public void tv2Click() {
            updateChatRecordTimeOut(0.04);
            // updateChatRecordTimeOut(0.00347); // ???????????????
        }

        @Override
        public void tv3Click() {
            updateChatRecordTimeOut(1);
        }

        @Override
        public void tv4Click() {
            updateChatRecordTimeOut(7);
        }

        @Override
        public void tv5Click() {
            updateChatRecordTimeOut(30);
        }

        @Override
        public void tv6Click() {
            updateChatRecordTimeOut(90);
        }

        @Override
        public void tv7Click() {
            updateChatRecordTimeOut(365);
        }
    };
    private Friend mFriend;
    private String mFriendName;
    private RefreshBroadcastReceiver receiver = new RefreshBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_setting);

        mLoginUserId = coreManager.getSelf().getUserId();
        mFriendId = getIntent().getStringExtra("ChatObjectId");
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);

        if (mFriend == null) {
            LogUtils.log(getIntent());
            Reporter.unreachable();
            ToastUtil.showToast(this, R.string.tip_friend_not_found);
            finish();
            return;
        }

        initActionBar();
        initView();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.ydd.yanshi.broadcast.OtherBroadcast.QC_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriendId);// Friend????????????
        if (mFriend == null) {
            Toast.makeText(this, R.string.tip_friend_removed, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            mFriendName = TextUtils.isEmpty(mFriend.getRemarkName()) ? mFriend.getNickName() : mFriend.getRemarkName();
            mFriendNameTv.setText(mFriendName);
            if (mFriend.getRemarkName() != null) {
                mRemarkNameTv.setText(mFriend.getRemarkName());
            }
            List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mFriendId);
            String labelNames = "";
            if (friendLabelList != null && friendLabelList.size() > 0) {
                for (int i = 0; i < friendLabelList.size(); i++) {
                    if (i == friendLabelList.size() - 1) {
                        labelNames += friendLabelList.get(i).getGroupName();
                    } else {
                        labelNames += friendLabelList.get(i).getGroupName() + "???";
                    }
                }
            }
            mLabelNameTv.setText(labelNames);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch (Exception e) {
            // ????????????????????????destroy?????????
        }
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(this);
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.chat_settings));
    }

    private void initView() {
        mFriendAvatarIv = (ImageView) findViewById(R.id.avatar);
        AvatarHelper.getInstance().displayAvatar(mFriendId, mFriendAvatarIv, true);
        mFriendNameTv = (TextView) findViewById(R.id.name);
        mRemarkNameTv = (TextView) findViewById(R.id.remark_name);
        mLabelNameTv = (TextView) findViewById(R.id.label_name);
        TextView mNoDisturbTv = (TextView) findViewById(R.id.no_disturb_tv);
        mNoDisturbTv.setText(InternationalizationHelper.getString("JX_MessageFree"));
        // ???????????? && ?????? && ???????????????
        mIsReadFireSb = (SwitchButton) findViewById(R.id.sb_read_fire);
        int isReadDel = PreferenceUtils.getInt(mContext, Constants.MESSAGE_READ_FIRE + mFriendId + mLoginUserId, 0);
        mIsReadFireSb.setChecked(isReadDel == 1);
        mIsReadFireSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                updateDisturbStatus(1, isChecked);
            }
        });

        mTopSb = (SwitchButton) findViewById(R.id.sb_top_chat);
        mTopSb.setChecked(mFriend.getTopTime() != 0);// TopTime??????0????????????????????????
        mTopSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                updateDisturbStatus(2, isChecked);
            }
        });

        mIsDisturbSb = (SwitchButton) findViewById(R.id.sb_no_disturb);
        mIsDisturbSb.setChecked(mFriend.getOfflineNoPushMsg() == 1);
        mIsDisturbSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                updateDisturbStatus(0, isChecked);
            }
        });

        mMsgSaveDays = (TextView) findViewById(R.id.msg_save_days_tv);
        mMsgSaveDays.setText(conversion(mFriend.getChatRecordTimeOut()));

        findViewById(R.id.avatar).setOnClickListener(this);
        if (coreManager.getLimit().cannotCreateGroup()) {
            findViewById(R.id.add_contacts).setVisibility(View.GONE);
        } else {
            findViewById(R.id.add_contacts).setOnClickListener(this);
        }
        findViewById(R.id.chat_history_search).setOnClickListener(this);
        findViewById(R.id.remark_rl).setOnClickListener(this);
        findViewById(R.id.label_rl).setOnClickListener(this);
        findViewById(R.id.msg_save_days_rl).setOnClickListener(this);
        findViewById(R.id.set_background_rl).setOnClickListener(this);
        findViewById(R.id.chat_history_empty).setOnClickListener(this);
        findViewById(R.id.sync_chat_history_empty).setOnClickListener(this);
        findViewById(R.id.rl_transfer).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_title_left:
                finish();
                break;
            case R.id.avatar:
                Intent intentBasic = new Intent(this, BasicInfoActivity.class);
                intentBasic.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentBasic);
                break;
            case R.id.add_contacts:
                Intent intentAdd = new Intent(this, SelectContactsActivity.class);
                intentAdd.putExtra("QuicklyCreateGroup", true);
                intentAdd.putExtra("ChatObjectId", mFriendId);
                intentAdd.putExtra("ChatObjectName", mFriendName);
                startActivity(intentAdd);
                break;
            case R.id.chat_history_search:
                Intent intentChat = new Intent(this, SearchChatHistoryActivity.class);
                intentChat.putExtra("isSearchSingle", true);
                intentChat.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentChat);
                break;
            case R.id.remark_rl:
                SetRemarkActivity.start(this, mFriendId);
                break;
            case R.id.label_rl:
                Intent intentLabel = new Intent(this, SetLabelActivity.class);
                intentLabel.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentLabel);
                break;
            case R.id.msg_save_days_rl:
                MsgSaveDaysDialog msgSaveDaysDialog = new MsgSaveDaysDialog(this, onMsgSaveDaysDialogClickListener);
                msgSaveDaysDialog.show();
                break;
            case R.id.set_background_rl:
                Intent intentBackground = new Intent(this, SetChatBackActivity.class);
                intentBackground.putExtra(AppConstant.EXTRA_USER_ID, mFriendId);
                startActivity(intentBackground);
                break;
            case R.id.chat_history_empty:
                clean(false);
                break;
            case R.id.sync_chat_history_empty:
                clean(true);
                break;
            case R.id.rl_transfer:
                Intent intentTransfer = new Intent(this, TransferRecordActivity.class);
                intentTransfer.putExtra(Constant.TRANSFE_RRECORD, mFriendId);
                startActivity(intentTransfer);
                break;
        }
    }

    private void clean(boolean isSync) {
        String tittle = isSync ? getString(R.string.sync_chat_history_clean) : getString(R.string.clean_chat_history);
        String tip = isSync ? getString(R.string.tip_sync_chat_history_clean) : getString(R.string.tip_confirm_clean_history);

        SelectionFrame selectionFrame = new SelectionFrame(mContext);
        selectionFrame.setSomething(tittle, tip, new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                if (isSync) {
                    // ??????????????????????????????????????????????????????????????????????????????????????????
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setFromUserId(mLoginUserId);
                    chatMessage.setFromUserName(coreManager.getSelf().getNickName());
                    chatMessage.setToUserId(mFriendId);
                    chatMessage.setType(XmppMessage.TYPE_SYNC_CLEAN_CHAT_HISTORY);
                    chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                    chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                    coreManager.sendChatMessage(mFriendId, chatMessage);
                }
                emptyServerMessage();

                FriendDao.getInstance().resetFriendMessage(mLoginUserId, mFriendId);
                ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, mFriendId);
                LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(new Intent(Constants.CHAT_HISTORY_EMPTY));// ??????????????????
                MsgBroadcast.broadcastMsgUiUpdate(mContext);
                Toast.makeText(PersonSettingActivity.this, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
            }
        });
        selectionFrame.show();
    }

    // ???????????????????????????
    private void updateDisturbStatus(final int type, final boolean isChecked) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mLoginUserId);
        params.put("toUserId", mFriendId);
        params.put("type", String.valueOf(type));
        params.put("offlineNoPushMsg", isChecked ? String.valueOf(1) : String.valueOf(0));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_NOPULL_MSG)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (type == 0) {// ???????????????
                                FriendDao.getInstance().updateOfflineNoPushMsgStatus(mFriendId, isChecked ? 1 : 0);
                            } else if (type == 1) {// ????????????
                                PreferenceUtils.putInt(mContext, Constants.MESSAGE_READ_FIRE + mFriendId + mLoginUserId, isChecked ? 1 : 0);
                                if (isChecked) {
                                    ToastUtil.showToast(PersonSettingActivity.this, R.string.tip_status_burn);
                                }
                            } else {// ????????????
                                if (isChecked) {
                                    FriendDao.getInstance().updateTopFriend(mFriendId, mFriend.getTimeSend());
                                } else {
                                    FriendDao.getInstance().resetTopFriend(mFriendId);
                                }
                            }
                        } else {
                            Toast.makeText(PersonSettingActivity.this, R.string.tip_edit_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showNetError(PersonSettingActivity.this);
                    }
                });
    }

    // ????????????????????????
    private void updateChatRecordTimeOut(final double outTime) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mFriendId);
        params.put("chatRecordTimeOut", String.valueOf(outTime));

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_UPDATE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            Toast.makeText(PersonSettingActivity.this, getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                            mMsgSaveDays.setText(conversion(outTime));
                            FriendDao.getInstance().updateChatRecordTimeOut(mFriendId, outTime);
                            LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(new Intent(com.ydd.yanshi.broadcast.OtherBroadcast.NAME_CHANGE));// ??????????????????
                        } else {
                            Toast.makeText(PersonSettingActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // ???????????????????????????????????????????????????
    private void emptyServerMessage() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", String.valueOf(0));// 0 ???????????? 1 ????????????
        params.put("toUserId", mFriendId);

        HttpUtils.get().url(coreManager.getConfig().EMPTY_SERVER_MESSAGE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    private String conversion(double outTime) {
        String outTimeStr;
        if (outTime == -1 || outTime == 0) {
            outTimeStr = getString(R.string.permanent);
        } else if (outTime == -2) {
            outTimeStr = getString(R.string.no_sync);
        } else if (outTime == 0.04) {
            outTimeStr = getString(R.string.one_hour);
        } else if (outTime == 1) {
            outTimeStr = getString(R.string.one_day);
        } else if (outTime == 7) {
            outTimeStr = getString(R.string.one_week);
        } else if (outTime == 30) {
            outTimeStr = getString(R.string.one_month);
        } else if (outTime == 90) {
            outTimeStr = getString(R.string.one_season);
        } else {
            outTimeStr = getString(R.string.one_year);
        }
        return outTimeStr;
    }


    public class RefreshBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(com.ydd.yanshi.broadcast.OtherBroadcast.QC_FINISH)) {
                // ?????????????????? || ?????????????????? ?????????????????????????????????????????????
                finish();
            }
        }
    }
}
