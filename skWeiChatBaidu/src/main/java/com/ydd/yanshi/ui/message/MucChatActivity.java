package com.ydd.yanshi.ui.message;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.http.ApiCallBack;
import com.xuan.xuanhttplibrary.okhttp.http.HttpHelper;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.Reporter;
import com.ydd.yanshi.adapter.MessageLocalVideoFile;
import com.ydd.yanshi.adapter.MessageUploadChatRecord;
import com.ydd.yanshi.adapter.MessageVideoFile;
import com.ydd.yanshi.audio_x.VoicePlayer;
import com.ydd.yanshi.bean.Contacts;
import com.ydd.yanshi.bean.EventNewNotice;
import com.ydd.yanshi.bean.EventNotifyByTag;
import com.ydd.yanshi.bean.EventRoomNotice;
import com.ydd.yanshi.bean.EventUploadCancel;
import com.ydd.yanshi.bean.EventUploadFileRate;
import com.ydd.yanshi.bean.EventXMPPJoinGroupFailed;
import com.ydd.yanshi.bean.Friend;
import com.ydd.yanshi.bean.MsgRoamTask;
import com.ydd.yanshi.bean.PrivacySetting;
import com.ydd.yanshi.bean.RoomMember;
import com.ydd.yanshi.bean.VideoFile;
import com.ydd.yanshi.bean.assistant.GroupAssistantDetail;
import com.ydd.yanshi.bean.assistant.ShareParams;
import com.ydd.yanshi.bean.collection.CollectionEvery;
import com.ydd.yanshi.bean.company.StructBeanNetInfo;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.bean.message.ChatRecord;
import com.ydd.yanshi.bean.message.MucRoom;
import com.ydd.yanshi.bean.message.MucRoomMember;
import com.ydd.yanshi.bean.message.XmppMessage;
import com.ydd.yanshi.bean.redpacket.EventRedReceived;
import com.ydd.yanshi.bean.redpacket.NiuniuBean;
import com.ydd.yanshi.bean.redpacket.OpenRedpacket;
import com.ydd.yanshi.bean.redpacket.RedDialogBean;
import com.ydd.yanshi.bean.redpacket.RedPacket;
import com.ydd.yanshi.broadcast.MsgBroadcast;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.db.dao.ChatMessageDao;
import com.ydd.yanshi.db.dao.FriendDao;
import com.ydd.yanshi.db.dao.MsgRoamTaskDao;
import com.ydd.yanshi.db.dao.RoomMemberDao;
import com.ydd.yanshi.db.dao.VideoFileDao;
import com.ydd.yanshi.downloader.Downloader;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.helper.FileDataHelper;
import com.ydd.yanshi.helper.PrivacySettingHelper;
import com.ydd.yanshi.helper.UploadEngine;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.base.CoreManager;
import com.ydd.yanshi.ui.contacts.SendContactsActivity;
import com.ydd.yanshi.ui.dialog.CreateCourseDialog;
import com.ydd.yanshi.ui.map.MapPickerActivity;
import com.ydd.yanshi.ui.me.MyCollection;
import com.ydd.yanshi.ui.me.redpacket.MucSendRedPacketActivity;
import com.ydd.yanshi.ui.me.redpacket.RedDetailsActivity;
import com.ydd.yanshi.ui.message.multi.InviteVerifyActivity;
import com.ydd.yanshi.ui.message.multi.RoomInfoActivity;
import com.ydd.yanshi.ui.mucfile.XfileUtils;
import com.ydd.yanshi.ui.other.BasicInfoActivity;
import com.ydd.yanshi.ui.tool.WebViewActivity;
import com.ydd.yanshi.util.AppUtils;
import com.ydd.yanshi.util.AsyncUtils;
import com.ydd.yanshi.util.Constants;
import com.ydd.yanshi.util.HtmlUtils;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.StringUtils;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.util.log.FileUtils;
import com.ydd.yanshi.video.MessageEventGpu;
import com.ydd.yanshi.video.VideoRecorderActivity;
import com.ydd.yanshi.view.ChatBottomView;
import com.ydd.yanshi.view.ChatBottomView.ChatBottomListener;
import com.ydd.yanshi.view.ChatContentView;
import com.ydd.yanshi.view.ChatContentView.MessageEventListener;
import com.ydd.yanshi.view.NoDoubleClickListener;
import com.ydd.yanshi.view.PullDownListView;
import com.ydd.yanshi.view.SelectCardPopupWindow;
import com.ydd.yanshi.view.SelectFileDialog;
import com.ydd.yanshi.view.SelectRoomMemberPopupWindow;
import com.ydd.yanshi.view.SelectionFrame;
import com.ydd.yanshi.view.TipDialog;
import com.ydd.yanshi.view.photopicker.PhotoPickerActivity;
import com.ydd.yanshi.view.photopicker.SelectModel;
import com.ydd.yanshi.view.photopicker.intent.PhotoPickerIntent;
import com.ydd.yanshi.view.redDialog.RedDialog;
import com.ydd.yanshi.xmpp.ListenerManager;
import com.ydd.yanshi.xmpp.listener.ChatMessageListener;
import com.ydd.yanshi.xmpp.listener.MucListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardforchat;
import okhttp3.Call;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * ???????????????
 */
public class MucChatActivity extends BaseActivity implements
        MessageEventListener, ChatBottomListener, ChatMessageListener, MucListener,
        SelectRoomMemberPopupWindow.SendMember, SelectCardPopupWindow.SendCardS {

    private static final int REQUEST_CODE_INVITE = 895;
    /***********************
     * ?????????????????????
     **********************/
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    private static final int REQUEST_CODE_PICK_PHOTO = 2;
    private static final int REQUEST_CODE_SELECT_VIDEO = 3;
    private static final int REQUEST_CODE_SEND_COLLECTION = 4;// ???????????? ??????
    private static final int REQUEST_CODE_SELECT_Locate = 5;
    private static final int REQUEST_CODE_QUICK_SEND = 6;
    private static final int REQUEST_CODE_SELECT_FILE = 7;
    private static final int REQUEST_CODE_SEND_CONTACT = 21;
    /**
     * ?????????@????????????,??????????????????,???????????? userId???userNames??????????????????
     */
    List<String> atUserId = new ArrayList<>();
    List<String> atUserNames = new ArrayList<>();
    // ???????????????????????????????????????????????????????????????????????????
    List<ChatMessage> chatMessages;
    @SuppressWarnings("unused")
    private ChatContentView mChatContentView;
    // ??????????????????
    private List<ChatMessage> mChatMessages;
    private ChatBottomView mChatBottomView;
    private AudioManager mAudioManager;
    // ??????????????????
    private Friend mFriend;
    private String mLoginUserId;
    private String mLoginNickName;
    private String instantMessage;
    // ????????????????????????
    private boolean isNotificationComing;
    // ?????????????????????UserId???????????????jid???
    private String mUseId;
    // ???????????????????????????????????????????????????
    private String mNickName;
    // ???????????????
    private boolean isGroupChat;

    private String[] noticeFriendList;
    private String roomId;
    private boolean isSearch;
    private double mSearchTime;
    private LinearLayout mNewMsgLl;
    private TextView mNewMsgTv;
    private int mNewMsgNum;
    private TextView mTvTitleLeft;
    private TextView mTvTitle;
    private boolean isFriendNull = false;
    // ???????????????
    private View llNotice;
    private TextView tvNotice;
    // @????????????popWindow
    private SelectRoomMemberPopupWindow mSelectRoomMemberPopupWindow;
    // ???????????????popWindow
    private SelectCardPopupWindow mSelectCardPopupWindow;
    private RedDialog mRedDialog;
    private RoomMember mRoomMember;
    private double mMinId = 0;
    private int mPageSize = 20;
    private boolean mHasMoreData = true;
    private UploadEngine.ImFileUploadResponse mUploadResponse = new UploadEngine.ImFileUploadResponse() {

        @Override
        public void onSuccess(String toUserId, ChatMessage message) {
            send(message);
        }

        @Override
        public void onFailure(String toUserId, ChatMessage message) {
            for (int i = 0; i < mChatMessages.size(); i++) {
                ChatMessage msg = mChatMessages.get(i);
                if (message.get_id() == msg.get_id()) {
                    msg.setMessageState(ChatMessageListener.MESSAGE_SEND_FAILED);
                    ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mUseId, message.get_id(),
                            ChatMessageListener.MESSAGE_SEND_FAILED);
                    mChatContentView.notifyDataSetInvalidated(false);
                    break;
                }
            }
        }
    };
    private Uri mNewPhotoUri;
    private ChatMessage replayMessage;
    private TipDialog tipDialog;
    private int mCurrentMemberNum;
    /*******************************************
     * ?????????????????????????????????
     ******************************************/
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MsgBroadcast.ACTION_MSG_STATE_UPDATE)) {
                // ???????????????????????????  ??????????????????
                String packetId = intent.getStringExtra("packetId");
                for (int i = 0; i < mChatMessages.size(); i++) {
                    ChatMessage chatMessage = mChatMessages.get(i);
                    if (packetId.equals(chatMessage.getPacketId())) {
                        /* if (chatMessage.getFromUserId().equals(mLoginUserId)) { return; } // ?????????????????????????????????????????????????????????  */
                        chatMessage.setReadPersons(chatMessage.getReadPersons() + 1);
                        // mChatContentView.changeReadPersons(i, count + 1);
                        mChatContentView.notifyDataSetChanged();
                        break;
                    }
                }
            } else if (action.equals(com.ydd.yanshi.broadcast.OtherBroadcast.MSG_BACK)) {
                // ????????????
                String packetId = intent.getStringExtra("packetId");
                if (TextUtils.isEmpty(packetId)) {
                    return;
                }
                for (ChatMessage chatMessage : mChatMessages) {
                    if (packetId.equals(chatMessage.getPacketId())) {
                        if (chatMessage.getType() == XmppMessage.TYPE_VOICE
                                && !TextUtils.isEmpty(VoicePlayer.instance().getVoiceMsgId())
                                && packetId.equals(VoicePlayer.instance().getVoiceMsgId())) {// ?????? && ???????????????msgId????????? ?????????msgId==???????????????msgId
                            // ??????????????????
                            VoicePlayer.instance().stop();
                        }
                        ChatMessage chat = ChatMessageDao.getInstance().findMsgById(mLoginUserId, mUseId, packetId);
                        chatMessage.setContent(chat.getContent());
                        chatMessage.setType(chat.getType());
                        break;
                    }
                }
                mChatContentView.notifyDataSetInvalidated(false);
            } else if (action.equals(Constants.CHAT_MESSAGE_DELETE_ACTION)) {
                // ????????????
                if (mChatContentView != null) {
                    int position = intent.getIntExtra(Constants.CHAT_REMOVE_MESSAGE_POSITION, 10000);
                    if (position == 10000) {
                        return;
                    }
                    ChatMessage message = mChatMessages.get(position);

                    deleteMessage(message.getPacketId());// ????????????????????????

                    if (ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message)) {
                        if (mChatMessages.size() > 0 && mChatMessages.size() - 1 == position) {// ???????????????????????????????????????LastContent
                            message.setType(XmppMessage.TYPE_TEXT);
                            message.setContent("");
                            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, mUseId, message);
                        }
                        mChatMessages.remove(position);
                        mChatContentView.notifyDataSetInvalidated(false);
                    } else {
                        Toast.makeText(mContext, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (action.equals(Constants.SHOW_MORE_SELECT_MENU)) {// ??????????????????
                int position = intent.getIntExtra(Constants.CHAT_SHOW_MESSAGE_POSITION, 0);
                moreSelected(true, position);
            } else if (action.equals(Constants.CHAT_TIME_OUT_ACTION)) {
                String friendid = intent.getStringExtra("friend_id");
                double timeOut = intent.getDoubleExtra("time_out", -1);
                mFriend.setChatRecordTimeOut(timeOut);

            } else if (action.equals(Constants.CHAT_HISTORY_EMPTY)) {
                // ??????????????????
                mChatMessages.clear();
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE)) {
                // ??????????????????
                mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mFriend.getUserId());// ????????????friend??????
                if (mFriend.getGroupStatus() == 3) {
                    groupTip(getString(R.string.tip_group_disable_by_service));
                }
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UPDATE_ROOM)) {
                // ?????????????????? | ????????????????????????
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(com.ydd.yanshi.broadcast.OtherBroadcast.REFRESH_MANAGER) || action.equals(MsgBroadcast.ACTION_MSG_ROLE_CHANGED)) {
                // Todo ?????????????????????
                // ??????|| ?????? ?????????????????????????????????
                getMyInfoInThisRoom();
                mChatContentView.notifyDataSetChanged();
            } else if (action.equals(MsgBroadcast.ACTION_MSG_UPDATE_ROOM_GET_ROOM_STATUS)) {
                // ?????? | ?????? | ????????????
                if (tipDialog != null && tipDialog.isShowing()) {
                    tipDialog.dismiss();
                }
                getMyInfoInThisRoom();
            }
        }
    };

    public static void start(Context ctx, Friend friend) {
        Intent intent = new Intent(ctx, MucChatActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
        intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        ctx.startActivity(intent);
    }

    @Override
    public void onCoreReady() {
        super.onCoreReady();
        if (isGroupChat) {
            // ??????friend.getTimeSend????????????????????????????????????coreReady??????mUserId?????????????????????friend??????
            if (TextUtils.isEmpty(mUseId)) {
                if (getIntent() != null) {
                    mUseId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
                }
            }
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
            if (friend != null) {
                Log.d("MucChatActivity", ">>>current thread:" + Thread.currentThread().getName());
                AsyncUtils.doAsync(this, c -> coreManager.joinMucChat(mUseId, friend.getTimeSend()));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(AppConstant.EXTRA_USER_ID, mUseId);
        outState.putString(AppConstant.EXTRA_NICK_NAME, mNickName);
        outState.putBoolean(AppConstant.EXTRA_IS_GROUP_CHAT, isGroupChat);
    }


//    private Handler mHandler = new Handler(Looper.getMainLooper()); // ????????????
//    private Runnable mTimeCounterRunnable = new Runnable() {
//        @Override
//        public void run() {//??????????????????????????????
//            getMyInfoInThisRoom();
//            mHandler.postDelayed(this, 1 * 1000);
//        }
//    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        /*AndroidBug5497Workaround.assistActivity(this);*/
        mLoginUserId = coreManager.getSelf().getUserId();
        mLoginNickName = coreManager.getSelf().getNickName();
        if (getIntent() != null) {
            mUseId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            mNickName = getIntent().getStringExtra(AppConstant.EXTRA_NICK_NAME);
            isGroupChat = getIntent().getBooleanExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
            noticeFriendList = getIntent().getStringArrayExtra(Constants.GROUP_JOIN_NOTICE);// ?????????????????????????????????.
            isSearch = getIntent().getBooleanExtra("isserch", false);
            if (isSearch) {
                mSearchTime = getIntent().getDoubleExtra("jilu_id", 0);
            }
            instantMessage = getIntent().getStringExtra("messageId");
            isNotificationComing = getIntent().getBooleanExtra(Constants.IS_NOTIFICATION_BAR_COMING, false);
        }
        mNewMsgNum = getIntent().getIntExtra(Constants.NEW_MSG_NUMBER, 0);

        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUseId);
        if (mFriend == null) {
            ToastUtil.showToast(mContext, getString(R.string.tip_program_error));
            isFriendNull = true;
            finish();
            return;
        }
        roomId = mFriend.getRoomId();
        mAudioManager = (AudioManager) getSystemService(android.app.Service.AUDIO_SERVICE);
        Downloader.getInstance().init(MyApplication.getInstance().mAppDir + File.separator + mLoginUserId
                + File.separator + Environment.DIRECTORY_MUSIC);

        initView();
//        mTimeCounterRunnable.run();


        // ????????????????????????????????????EventBus,????????????
        mTvTitle.post(() -> ListenerManager.getInstance().addChatMessageListener(MucChatActivity.this));// ???????????????????????????????????????loadData??????????????????????????????mChatMessages?????????
        ListenerManager.getInstance().addMucListener(this);
        EventBus.getDefault().register(this);
        IntentFilter filter = new IntentFilter();
        // ?????????????????????
        filter.addAction(MsgBroadcast.ACTION_MSG_STATE_UPDATE);
        // ????????????
        filter.addAction(com.ydd.yanshi.broadcast.OtherBroadcast.MSG_BACK);
        filter.addAction(Constants.CHAT_MESSAGE_DELETE_ACTION);
        filter.addAction(Constants.SHOW_MORE_SELECT_MENU);
        filter.addAction(Constants.CHAT_HISTORY_EMPTY);
        filter.addAction(Constants.CHAT_TIME_OUT_ACTION);
        filter.addAction(MsgBroadcast.ACTION_DISABLE_GROUP_BY_SERVICE);
        filter.addAction(MsgBroadcast.ACTION_MSG_UPDATE_ROOM);
        filter.addAction(com.ydd.yanshi.broadcast.OtherBroadcast.REFRESH_MANAGER);
        filter.addAction(MsgBroadcast.ACTION_MSG_ROLE_CHANGED);
        filter.addAction(MsgBroadcast.ACTION_MSG_UPDATE_ROOM_GET_ROOM_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    private void setLastNotice(MucRoom.Notice notice) {
        // ????????????7??????
        if (notice != null && TimeUnit.SECONDS.toMillis(notice.getTime()) + TimeUnit.DAYS.toMillis(7) > System.currentTimeMillis()) {
            setLastNotice(notice.getText());
        } else {
            // ??????????????????????????????????????????????????????
            // llNotice.setVisibility(View.GONE);
            tvNotice.setText(getString(R.string.no_notice));
        }
    }

    // ?????????????????????
    private void setLastNotice(String notice) {
        llNotice.setVisibility(View.VISIBLE);
        tvNotice.setText(notice);
        tvNotice.setSelected(true);
    }

    private void initView() {
        mChatMessages = new ArrayList<>();
        mChatBottomView = (ChatBottomView) findViewById(R.id.chat_bottom_view);
        initActionBar();
        mChatBottomView.setChatBottomListener(this);
        mChatBottomView.getmShotsLl().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChatBottomView.getmShotsLl().setVisibility(View.GONE);
                String shots = PreferenceUtils.getString(mContext, Constants.SCREEN_SHOTS, "No_Shots");
                QuickSendPreviewActivity.startForResult(MucChatActivity.this, shots, REQUEST_CODE_QUICK_SEND);
            }
        });
        mChatBottomView.setGroup(true, mFriend.getRoomId(), mFriend.getUserId());

        mChatContentView = (ChatContentView) findViewById(R.id.chat_content_view);
        mChatContentView.setToUserId(mUseId);
        mChatContentView.setRoomId(mFriend.getRoomId());
        mChatContentView.setCurGroup(true, mFriend.getRoomMyNickName());
        mChatContentView.setData(mChatMessages);
        mChatContentView.setChatBottomView(mChatBottomView);// ???????????????????????????????????????
        mChatContentView.setMessageEventListener(this);
        mChatContentView.setRefreshListener(new PullDownListView.RefreshingListener() {
            @Override
            public void onHeaderRefreshing() {
                loadDatas(false);
            }
        });

        // ????????????
        if (isNotificationComing) {
            Intent intent = new Intent();
            intent.putExtra(AppConstant.EXTRA_FRIEND, mFriend);
            intent.setAction(Constants.NOTIFY_MSG_SUBSCRIPT);
            LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(intent);
        } else {
            FriendDao.getInstance().markUserMessageRead(mLoginUserId, mUseId);
        }
        if (mFriend.getIsAtMe() != 0) {// ??????@??????
            FriendDao.getInstance().updateAtMeStatus(mFriend.getUserId(), 0);
        }

        mNewMsgLl = (LinearLayout) findViewById(R.id.msg_up_ll);
        mNewMsgTv = (TextView) findViewById(R.id.msg_up_tv);
        mNewMsgLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewMsgLl.setVisibility(View.GONE);
                mChatContentView.smoothScrollToPosition(0);
            }
        });
        llNotice = findViewById(R.id.llNotice);
        tvNotice = findViewById(R.id.tvNotice);
        llNotice.setVisibility(View.VISIBLE);
        llNotice.setOnClickListener(v -> {
            llNotice.setVisibility(View.GONE);
        });

//         CoreManager.updateMyBalance();

        loadDatas(true);

        initRoomMember();

        getMyInfoInThisRoom();
    }

    private void loadDatas(boolean scrollToBottom) {
        boolean isFirstEnter;
        if (mChatMessages.size() <= 0) {
            isFirstEnter = true;
            ChatMessage mLastChatMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, mFriend.getUserId());
            if (mLastChatMessage == null) {
                synchronizeChatHistory();
                return;
            } else {
                if (mLastChatMessage.getTimeSend() != 0) {
                    mMinId = mLastChatMessage.getDoubleTimeSend() + 1;  // sq < mMinId
                } else {
                    mMinId = TimeUtils.sk_time_current_time_double();// ??????????????????
                }
            }
        } else {
            isFirstEnter = false;
            mMinId = mChatMessages.get(0).getDoubleTimeSend();
        }

        List<ChatMessage> chatLists;
        if (isSearch) {
            chatLists = ChatMessageDao.getInstance().searchMessagesByTime(mLoginUserId,
                    mFriend.getUserId(), mSearchTime);
        } else {
            if (isFirstEnter && mNewMsgNum > 20) {// ?????????????????????????????????????????????>20,?????????????????????
                chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mLoginUserId,
                        mFriend.getUserId(), mMinId, 100);// ????????????100??? ???????????????????????????????????????????????????????????????100?????????????????????????????????

                mNewMsgTv.setText(getString(R.string.new_message_count_place_holder, chatLists.size()));
                mNewMsgLl.setVisibility(View.VISIBLE);
            } else {
                chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mLoginUserId,
                        mFriend.getUserId(), mMinId, mPageSize);
            }
        }
        if (chatLists == null || chatLists.size() <= 0) {
            /** ???????????? */
            if (!scrollToBottom) {
                getNetSingle();
            }
        } else {
            mTvTitle.post(new Runnable() {
                @Override
                public void run() {
                    long currTime = TimeUtils.sk_time_current_time();
                    for (int i = 0; i < chatLists.size(); i++) {
                        ChatMessage message = chatLists.get(i);
                        // ???????????????????????????????????????
                        if (message.getDeleteTime() > 0 && message.getDeleteTime() < currTime) {
                            // ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                            continue;
                        }
                        mChatMessages.add(0, message);
                    }

                    if (isSearch) {// ?????????????????? ??????
                        isSearch = false;
                        int position = 0;
                        for (int i = 0; i < mChatMessages.size(); i++) {
                            if (mChatMessages.get(i).getDoubleTimeSend() == mSearchTime) {
                                position = i;
                            }
                        }
                        mChatContentView.notifyDataSetInvalidated(position);// ?????????????????????
                    } else {
                        if (scrollToBottom) {
                            mChatContentView.notifyDataSetInvalidatedForSetSelectionInvalid(scrollToBottom);
                        } else {
                            mChatContentView.notifyDataSetAddedItemsToTop(chatLists.size());
                        }
                    }
                    mChatContentView.headerRefreshingCompleted();
                    if (!mHasMoreData) {
                        mChatContentView.setNeedRefresh(false);
                    }
                }
            });
        }
    }

    // ??????
    protected void onSaveContent() {
        String str = mChatBottomView.getmChatEdit().getText().toString().trim();
        // ?????? ???????????????
        str = str.replaceAll("\\s", "");
        str = str.replaceAll("\\n", "");
        if (TextUtils.isEmpty(str)) {
            if (XfileUtils.isNotEmpty(mChatMessages)) {
                ChatMessage chatMessage = mChatMessages.get(mChatMessages.size() - 1);
                String fromUserName;
                if (chatMessage.getType() == XmppMessage.TYPE_TIP) {// ???????????????????????????FromUserId
                    fromUserName = "";
                } else {
                    fromUserName = TextUtils.isEmpty(chatMessage.getFromUserName()) ? "" : chatMessage.getFromUserName() + " : ";
                }
                FriendDao.getInstance().updateFriendContent(mLoginUserId, mFriend.getUserId(),
                        fromUserName + chatMessage.getContent(),
                        chatMessage.getType(),
                        chatMessage.getTimeSend());
            }
        } else {
            // ?????????????????????????????????
            FriendDao.getInstance().updateFriendContent(mLoginUserId,
                    mFriend.getUserId(),
                    "&8824" + str,  //InternationalizationHelper.getString("JX_Draft")
                    XmppMessage.TYPE_TEXT, TimeUtils.sk_time_current_time());
        }
        PreferenceUtils.putString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, str);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // ???????????????????????????????????????????????????
        if (ev.getActionIndex() > 0) {
            return true;
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (IllegalArgumentException ignore) {
            // ????????????ViewPager???bug, ?????????????????????
            // https://stackoverflow.com/a/31306753
            return true;
        }
    }

    private void doBack() {
        if (!TextUtils.isEmpty(instantMessage)) {
            SelectionFrame selectionFrame = new SelectionFrame(this);
            selectionFrame.setSomething(null, getString(R.string.tip_forwarding_quit), new SelectionFrame.OnSelectionFrameClickListener() {
                @Override
                public void cancelClick() {

                }

                @Override
                public void confirmClick() {
                    finish();
                }
            });
            selectionFrame.show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (!JVCideoPlayerStandardforchat.handlerBack()) {
            doBack();
        }
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * ????????????????????????????????????
     */
  /*  private void sendNoticeJoinNewFriend() {
        if (noticeFriendList != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendNotice(InternationalizationHelper.getString("NEW_FRIEND_CHAT"));
                    // ??????????????????????????????
                    noticeFriendList = null;
                }
            }, 1000);
        }
    }*/
    private void sendNotice(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TIP);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        // ??????[??????]
        String draft = PreferenceUtils.getString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, "");
        if (!TextUtils.isEmpty(draft)) {
            String s = StringUtils.replaceSpecialChar(draft);
            CharSequence content = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
            if (draft.contains("@")) {
                // ??????SelectRoomMemberPopupWindow?????????????????????????????????
                mChatBottomView.getmChatEdit().setText(content + ",");
            } else {
                mChatBottomView.getmChatEdit().setText(content);
            }
            softKeyboardControl(true, 200);
        }
        // ???????????????????????????id
        MyApplication.IsRingId = mFriend.getUserId();
    }

    private void updateSecret(boolean secret) {
        // ??????????????????????????????????????????
        mChatContentView.setSecret(secret);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (TextUtils.isEmpty(mChatBottomView.getmChatEdit().getText().toString())) {// ???????????????????????????????????????????????????onPause--onResume???????????????????????????
            PreferenceUtils.putString(mContext, "WAIT_SEND" + mFriend.getUserId() + mLoginUserId, "");
        }
        // ?????????????????????id??????
        MyApplication.IsRingId = "Empty";
        VoicePlayer.instance().stop();
    }

    @Override
    protected void onDestroy() {
        onSaveContent();
        MsgBroadcast.broadcastMsgUiUpdate(mContext);
        super.onDestroy();
//        mHandler.removeCallbacks(mTimeCounterRunnable);
        if (isFriendNull) {
            return;
        }
        JCVideoPlayer.releaseAllVideos();
        if (mChatBottomView != null) {
            mChatBottomView.recordCancel();
        }
        ListenerManager.getInstance().removeChatMessageListener(this);
        ListenerManager.getInstance().removeMucListener(this);
        EventBus.getDefault().unregister(this);
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            // ??????????????????????????????????????????????????????????????????
        }
    }

    /***************************************
     * ChatContentView?????????
     ***************************************/
    @Override
    public void onMyAvatarClick() {
        mChatBottomView.reset();
        mChatBottomView.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(mContext, BasicInfoActivity.class);
                intent.putExtra(AppConstant.EXTRA_USER_ID, mLoginUserId);
                startActivity(intent);
            }
        }, 100);
    }

    @Override
    public void onFriendAvatarClick(final String friendUserId) {
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        if (isAllowSecretlyChat || isOk()) {
            mChatBottomView.reset();
            mChatBottomView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BasicInfoActivity.start(mContext, friendUserId, BasicInfoActivity.FROM_ADD_TYPE_GROUP);
                }
            }, 100);
        } else {
            tip(getString(R.string.tip_member_disable_privately_chat));
        }
    }

    // ????????????@?????????
    @Override
    public void LongAvatarClick(ChatMessage chatMessage) {
        if (chatMessage.getFromUserId().equals(mLoginUserId)) {// @???????????????
            return;
        }
        // ????????????AT????????????????????????????????????????????????
        // AT????????????????????????
/*
        if (atUserId.contains(chatMessage.getFromUserId())) {
            return;
        }
*/
        atUserId.add(chatMessage.getFromUserId());
        atUserNames.add(chatMessage.getFromUserName());
        Editable editContent = mChatBottomView.getmChatEdit().getText();
        RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), chatMessage.getFromUserId());
        String content = chatMessage.getFromUserName();
        if (member != null) {
            content = member.getUserName();
        }
        SpannableString atContent = new SpannableString("@" + content + " ");
        if (editContent.toString().contains(atContent)) {
            // AT????????????????????????
            return;
        }
        atContent.setSpan(new ForegroundColorSpan(Color.parseColor("#63B8FF")), 0, atContent.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editContent.insert(0, atContent);
    }

    @Override
    public void onNickNameClick(String friendUserId) {

    }

    @Override
    public void onMessageClick(ChatMessage chatMessage) {

    }

    @Override
    public void onMessageLongClick(ChatMessage chatMessage) {

    }

    @Override
    public void onEmptyTouch() {
        mChatBottomView.reset();
    }

    @Override
    public void onTipMessageClick(ChatMessage message) {
        if (message.getFileSize() == XmppMessage.TYPE_83) {
            showRedReceivedDetail(message.getFilePath());
        } else if (!TextUtils.isEmpty(message.getObjectId())
                && message.getObjectId().contains("userIds")
                && message.getObjectId().contains("userNames")
                && message.getObjectId().contains("isInvite")) {
            //  ?????????????????????????????????????????????????????????????????????????????????KeyWord ?????????Click??????????????????
            // Todo  ???????????????????????????????????????????????????type?????????????????????????????????????????????????????????????????????????????????
            Intent intent = new Intent(MucChatActivity.this, InviteVerifyActivity.class);
            intent.putExtra("VERIFY_MESSAGE_FRIEND_ID", mUseId);
            intent.putExtra("VERIFY_MESSAGE_PACKET", message.getPacketId());
            intent.putExtra("VERIFY_MESSAGE_ROOM_ID", mFriend.getRoomId());
            startActivityForResult(intent, REQUEST_CODE_INVITE);
        }
    }

    // ????????????????????????
    private void showRedReceivedDetail(String redId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("id", redId);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).RENDPACKET_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (result.getData() != null) {
                            // ???resultCode==1?????????????????????
                            // ???resultCode==0???????????????????????????????????????????????????????????????
                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 0);
                            if (!TextUtils.isEmpty(result.getResultMsg())) //resultMsg??????????????????????????????
                            {
                                bundle.putInt("timeOut", 1);
                            } else {
                                bundle.putInt("timeOut", 0);
                            }

                            bundle.putBoolean("isGroup", true);
                            bundle.putString("mToUserId", mFriend.getUserId());
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                        } else {
                            Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    @Override
    public void onReplayClick(ChatMessage message) {
        ChatMessage replayMessage = new ChatMessage(message.getObjectId());
        AsyncUtils.doAsync(this, t -> {
            Reporter.post("??????????????????????????????<" + message.getObjectId() + ">", t);
        }, c -> {
            List<ChatMessage> chatMessages = ChatMessageDao.getInstance().searchFromMessage(c.getRef(), mLoginUserId, mFriend.getUserId(), replayMessage);
            if (chatMessages == null) {
                return;
            }
            int index = -1;
            for (int i = 0; i < chatMessages.size(); i++) {
                ChatMessage m = chatMessages.get(i);
                if (TextUtils.equals(m.getPacketId(), replayMessage.getPacketId())) {
                    index = i;
                }
            }
            if (index == -1) {
                Reporter.unreachable();
                return;
            }
            int finalIndex = index;
            c.uiThread(r -> {
                mChatMessages = chatMessages;
                mChatContentView.setData(mChatMessages);
                mChatContentView.notifyDataSetInvalidated(finalIndex);
            });
        });
    }

    @Override
    public void onSendAgain(ChatMessage message) {
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                // ??????????????????????????????????????????????????????????????????????????????????????????????????????[??????????????????]????????????????????????????????????
                ChatMessageDao.getInstance().updateMessageSendState(mLoginUserId, mFriend.getUserId(),
                        message.get_id(), ChatMessageListener.MESSAGE_SEND_ING);
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mUseId, message, mUploadResponse);
            } else {
                send(message);
            }
        } else {
            send(message);
        }
    }

    public void deleteMessage(String msgIdListStr) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", msgIdListStr);
        params.put("delete", "1");  // 1???????????? 2-????????????
        params.put("type", "2");    // 1???????????? 2-????????????

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_CHATMESSAGE)
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

    /**
     * ????????????
     */
    @Override
    public void onMessageBack(final ChatMessage chatMessage, final int position) {
        DialogHelper.showMessageProgressDialog(MucChatActivity.this, InternationalizationHelper.getString("MESSAGE_REVOCATION"));
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageId", chatMessage.getPacketId());
        params.put("roomJid", mUseId);
        params.put("type", "2");
        params.put("delete", "2");

        HttpUtils.get().url(coreManager.getConfig().USER_DEL_CHATMESSAGE)
                .params(params)
                .build()
                .execute(new ListCallback<StructBeanNetInfo>(StructBeanNetInfo.class) {
                    @Override
                    public void onResponse(ArrayResult<StructBeanNetInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (chatMessage.getType() == XmppMessage.TYPE_VOICE) {// ???????????????????????????????????????
                            if (VoicePlayer.instance().getVoiceMsgId().equals(chatMessage.getPacketId())) {
                                VoicePlayer.instance().stop();
                            }
                        } else if (chatMessage.getType() == XmppMessage.TYPE_VIDEO) {
                            JCVideoPlayer.releaseAllVideos();
                        }
                        // ??????????????????
                        ChatMessage message = new ChatMessage();
                        message.setType(XmppMessage.TYPE_BACK);
                        message.setFromUserId(mLoginUserId);
                        message.setFromUserName(mLoginNickName);
                        if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
                            message.setFromUserName(mFriend.getRoomMyNickName());
                        }
                        message.setToUserId(mUseId);
                        message.setContent(chatMessage.getPacketId());
                        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                        coreManager.sendMucChatMessage(mUseId, message);
                        ChatMessageDao.getInstance().updateMessageBack(mLoginUserId, mFriend.getUserId(), chatMessage.getPacketId(), getString(R.string.you));
                        mChatMessages.get(position).setType(XmppMessage.TYPE_TIP);
                        mChatMessages.get(position).setContent(InternationalizationHelper.getString("JX_AlreadyWithdraw"));
                        mChatContentView.notifyDataSetInvalidated(false);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MucChatActivity.this);
                    }
                });
    }

    @Override
    public void onMessageReplay(ChatMessage chatMessage) {
        replayMessage = chatMessage;
        mChatBottomView.setReplay(chatMessage);
    }

    @Override
    public void cancelReplay() {
        replayMessage = null;
    }

    @Override
    public void onCallListener(int type) {

    }

    /***************************************
     * ChatBottomView?????????
     ***************************************/

    private void softKeyboardControl(boolean isShow, long delayMillis) {
        // ???????????????
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (isShow) {
            mChatBottomView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mChatBottomView.getmChatEdit().requestFocus();
                    mChatBottomView.getmChatEdit().setSelection(mChatBottomView.getmChatEdit().getText().toString().length());
                    imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
                }
            }, delayMillis);
        } else {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void send(ChatMessage message) {
        // ???????????????????????????????????????xmpp???????????????
        // ??????????????????????????????
        if (isAuthenticated()) {
            return;
        }
        coreManager.sendMucChatMessage(mUseId, message);
    }

    private void sendMessage(ChatMessage message) {
        //??????????????????????????????????????????????????????????????????
//        if (message.getType() == XmppMessage.TYPE_TEXT) {
            RoomMember member = RoomMemberDao.getInstance().getSingleRoomMember(mFriend.getRoomId(), mLoginUserId);
            if (member != null && member.getRole() == 3) {// ???????????????????????????????????????
                if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {

                    ToastUtil.showToast(mContext, InternationalizationHelper.getString("HAS_BEEN_BANNED"));
                    mChatMessages.remove(message);
                    mChatContentView.notifyDataSetInvalidated(true);
                    return;
                }
            } else if (member == null) {// ??????????????????????????????
                if (mFriend != null && mFriend.getRoomTalkTime() > (System.currentTimeMillis() / 1000)) {
                    ToastUtil.showToast(mContext, InternationalizationHelper.getString("HAS_BEEN_BANNED"));
                    mChatMessages.remove(message);
                    mChatContentView.notifyDataSetInvalidated(true);
                    return;
                }
            }
//        }
        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mFriend.getUserId(), false);
        Log.e("TAG_????????????","??????="+(mRoomMember != null) +"isAllShutUp=="+ isAllShutUp);
        if (mRoomMember != null && (isAllShutUp && mRoomMember.isAllBannedEffective())) {
            ToastUtil.showToast(mContext, InternationalizationHelper.getString("HAS_BEEN_BANNED"));
            mChatMessages.remove(message);
            mChatContentView.notifyDataSetInvalidated(true);
            return;
        }

        message.setToUserId(mUseId);
        if (isGroupChat && !TextUtils.isEmpty(mFriend.getRoomMyNickName())) {
            message.setFromUserName(mFriend.getRoomMyNickName());
        }

        if (mFriend.getChatRecordTimeOut() == -1 || mFriend.getChatRecordTimeOut() == 0) {// ??????
            message.setDeleteTime(-1);
        } else {
            long deleteTime = TimeUtils.sk_time_current_time() + (long) (mFriend.getChatRecordTimeOut() * 24 * 60 * 60);
            message.setDeleteTime(deleteTime);
        }

        // ??????
        PrivacySetting privacySetting = PrivacySettingHelper.getPrivacySettings(this);
        boolean isEncrypt = privacySetting.getIsEncrypt() == 1;
        if (isEncrypt) {
            message.setIsEncrypt(1);
        } else {
            message.setIsEncrypt(0);
        }

        message.setReSendCount(ChatMessageDao.fillReCount(message.getType()));
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        message.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        message.setGroup(true);

        ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mUseId, message);
        if (message.getType() == XmppMessage.TYPE_VOICE || message.getType() == XmppMessage.TYPE_IMAGE
                || message.getType() == XmppMessage.TYPE_VIDEO || message.getType() == XmppMessage.TYPE_FILE
                || message.getType() == XmppMessage.TYPE_LOCATION) {
            if (!message.isUpload()) {
                UploadEngine.uploadImFile(coreManager.getSelfStatus().accessToken, coreManager.getSelf().getUserId(), mUseId, message, mUploadResponse);
            } else {
                send(message);
            }
        } else {
            send(message);
        }
    }


    @Override
    public void stopVoicePlay() {
        VoicePlayer.instance().stop();
    }

    // ??????@??????
    @Override
    public void sendAt() {
        List<RoomMember> roomMember = RoomMemberDao.getInstance().getRoomMember(roomId);
        if (mRoomMember != null && roomMember.size() > 0) {
            // ???????????????
            for (int i = 0; i < roomMember.size(); i++) {
                if (roomMember.get(i).getUserId().equals(mLoginUserId)) {
                    roomMember.remove(roomMember.get(i));
                }
            }
            mSelectRoomMemberPopupWindow = new SelectRoomMemberPopupWindow(MucChatActivity.this, this, roomMember, mRoomMember.getRole());
            mSelectRoomMemberPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
            mSelectRoomMemberPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    softKeyboardControl(true, 200);
                }
            });
        } else {
            loadMembers(roomId, true); //????????????????????????????????????
        }
    }

    // ?????????@??????????????????
    @Override
    public void sendAtContent(RoomMember member) {
        String text = mChatBottomView.getmChatEdit().getText().toString();
        String keyword = "@" + member.getUserName() + " ";
        text += member.getUserName() + " ";
        if (text.contains("@????????????")) {
            atUserId.clear();
            atUserNames.clear();
            text = keyword;
        }
        atUserId.add(member.getUserId());
        atUserNames.add(member.getUserName());
        SpannableString spannableString = StringUtils.matcherSearchTitle(Color.parseColor("#63B8FF"),
                text, keyword);
        mChatBottomView.getmChatEdit().setText(spannableString);
    }

    // ?????????@?????????????????????
    @Override
    public void sendEveryOne(String everyOne) {
        atUserId.clear();
        atUserNames.clear();
        SpannableString spannableString = StringUtils.matcherSearchTitle(Color.parseColor("#63B8FF"),
                everyOne, everyOne);
        mChatBottomView.getmChatEdit().setText(spannableString);
    }

    // ??????@??????
    @Override
    public void sendAtMessage(String text) {
        sendText(text);
    }

    @Override
    public void sendText(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }

        // ????????????
        //[10008295 10009232...]
        StringBuilder atIds = new StringBuilder();
        StringBuilder atNames = new StringBuilder();
        if (text.contains("@")){
            if (text.contains("@????????????")) {
                // roomJid
                atIds.append(mUseId);
                atNames.append("????????????");
            } else {
                for (int i = 0; i < atUserId.size(); i++) {
                    atIds.append(atUserId.get(i));
                    atNames.append(atUserNames.get(i));
                    if (i != atUserId.size() - 1) {
                        atIds.append(" ");
                        atNames.append(" ");
                    }
                }
            }
        }

        retrofit2.Call<NiuniuBean> call = HttpHelper.getApiService().sendXiazhu(mLoginUserId, text, mUseId, atIds.toString(), atNames.toString());
        call.enqueue(new ApiCallBack<NiuniuBean>() {
            @Override
            public void onSuccess(NiuniuBean result) {

            }

            @Override
            public void onFail(int code, String msg) {
                super.onFail(code, msg);

            }
        });

        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_TEXT);
        message.setContent(text);
        if (replayMessage != null) {
            message.setType(XmppMessage.TYPE_REPLAY);
            message.setObjectId(replayMessage.toJsonString());
            replayMessage = null;
            mChatBottomView.resetReplay();
        }
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        message.setObjectId(atIds.toString());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
        atUserId.clear();
        atUserNames.clear();
        for (ChatMessage msg : mChatMessages) {
            if (msg.getType() == XmppMessage.TYPE_RED// ??????
                    && StringUtils.strEquals(msg.getFilePath(), "3")// ????????????
                    && text.equalsIgnoreCase(msg.getContent())// ??????????????????????????????
                    && msg.getFileSize() == 1) // ?????????????????????
            {
                RedDialogBean redDialogBean = new RedDialogBean(msg.getFromUserId(), msg.getFromUserName(),
                        msg.getContent(), null);
                mRedDialog = new RedDialog(mContext, redDialogBean, () -> {
                    openRedPacket(msg);
                });
                mRedDialog.show();
            }
        }
    }

    /**
     * ????????????
     */
    public void openRedPacket(final ChatMessage message) {
        HashMap<String, String> params = new HashMap<String, String>();
        String redId = message.getObjectId();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("id", redId);

        HttpUtils.get().url(coreManager.getConfig().REDPACKET_OPEN)
                .params(params)
                .build()
                .execute(new BaseCallback<OpenRedpacket>(OpenRedpacket.class) {

                    @Override
                    public void onResponse(ObjectResult<OpenRedpacket> result) {
                        if (mRedDialog != null) {
                            mRedDialog.dismiss();
                        }
                        if (result.getData() != null) {
                            // ??????????????????????????????,???????????????
                            message.setFileSize(2);
                            ChatMessageDao.getInstance().updateChatMessageReceiptStatus(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                            mChatContentView.notifyDataSetChanged();

                            OpenRedpacket openRedpacket = result.getData();
                            Bundle bundle = new Bundle();
                            Intent intent = new Intent(mContext, RedDetailsActivity.class);
                            bundle.putSerializable("openRedpacket", openRedpacket);
                            bundle.putInt("redAction", 1);
                            bundle.putInt("timeOut", 0);

                            bundle.putBoolean("isGroup", true);
                            bundle.putString("mToUserId", mFriend.getUserId());
                            intent.putExtras(bundle);
                            mContext.startActivity(intent);
                            // ????????????
                            coreManager.updateMyBalance();

                            showReceiverRedLocal(openRedpacket);
                        } else {
                            Toast.makeText(MucChatActivity.this, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        if (mRedDialog != null) {
                            mRedDialog.dismiss();
                        }
                    }
                });
    }

    private void showReceiverRedLocal(OpenRedpacket openRedpacket) {
        // ??????????????????????????????
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setFileSize(XmppMessage.TYPE_83);
        chatMessage.setFilePath(openRedpacket.getPacket().getId());
        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(mFriend.getUserId());
        chatMessage.setType(XmppMessage.TYPE_TIP);
        if (openRedpacket.getPacket().getCount() == openRedpacket.getList().size()) {
            chatMessage.setContent(getString(R.string.red_received_self, openRedpacket.getPacket().getUserName())
                    + getString(R.string.red_packet_has_received));
        } else {
            chatMessage.setContent(getString(R.string.red_received_self, openRedpacket.getPacket().getUserName()));
        }
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage)) {
            mChatMessages.add(chatMessage);
            mChatContentView.notifyDataSetInvalidated(true);
        }
    }

    @Override
    public void sendGif(String text) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_GIF);
        message.setContent(text);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendCollection(String collection) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent(collection);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setUpload(true);// ?????????????????????????????????
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    @Override
    public void sendVoice(String filePath, int timeLen) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VOICE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        message.setTimeLen(timeLen);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendImage(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_IMAGE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        int[] imageParam = FileDataHelper.getImageParamByIntsFile(filePath);
        message.setLocation_x(String.valueOf(imageParam[0]));
        message.setLocation_y(String.valueOf(imageParam[1]));
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendVideo(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_VIDEO);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendFile(File file) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        long fileSize = file.length();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_FILE);
        message.setContent("");
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        String filePath = file.getAbsolutePath();
        message.setFilePath(filePath);
        message.setFileSize((int) fileSize);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    private void sendContacts(List<Contacts> contactsList) {
        for (Contacts contacts : contactsList) {
            sendText(contacts.getName() + '\n' + contacts.getTelephone());
        }
    }

    public void sendLocate(double latitude, double longitude, String address, String snapshot) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_LOCATION);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        message.setLocation_x(latitude + "");
        message.setLocation_y(longitude + "");
        message.setContent("");
        message.setFilePath(snapshot);
        message.setObjectId(address);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    /**
     * ?????????????????????
     */
    @Override
    public void sendCardS(List<Friend> friends) {
        for (int i = 0; i < friends.size(); i++) {
            sendCard(friends.get(i));
        }
    }

    //    @Override
    //    public void clickPwdRed(String str) {
    //        mChatBottomView.getmChatEdit().setText(str);
    //    }

    public void sendCard(Friend friend) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_CARD);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        message.setObjectId(friend.getUserId());
        message.setContent(friend.getNickName());
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    public void sendRed(RedPacket redPacket) {
        String objectId = redPacket.getId();
        ChatMessage message = new ChatMessage();
        message.setType(XmppMessage.TYPE_RED);
        message.setFromUserName(mLoginNickName);
        message.setFromUserId(mLoginUserId);
        message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        message.setContent(redPacket.getGreetings()); // ?????????
        message.setObjectId(objectId); // ??????id
        message.setFilePath(redPacket.getType() + "");// ???FilePath?????????????????????
        // ????????????????????????
        message.setFileSize(redPacket.getStatus());   // ???filesize?????????????????????
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
        // ????????????
        CoreManager.updateMyBalance();
    }

    public void sendRed(String type, String money, String count, String words, String payPassword, String userId) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("type", type);
        params.put("moneyStr", money);
        params.put("count", count);
        params.put("greetings", words);
        params.put("roomJid", mUseId);
        params.put("userIdStr", userId);

        HttpUtils.get().url(coreManager.getConfig().REDPACKET_SEND)
                .params(params)
                .addSecret(payPassword, money)
                .build()
                .execute(new BaseCallback<RedPacket>(RedPacket.class) {
                    @Override
                    public void onResponse(ObjectResult<RedPacket> result) {
                        RedPacket redPacket = result.getData();
                        if (result.getResultCode() != 1) {
                            // ?????????????????????
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            String objectId = redPacket.getId();
                            ChatMessage message = new ChatMessage();
                            message.setType(XmppMessage.TYPE_RED);
                            message.setFromUserName(mLoginNickName);
                            message.setFromUserId(mLoginUserId);
                            message.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                            message.setContent(redPacket.getGreetings()); // ?????????
                            message.setObjectId(objectId); // ??????id
                            message.setFilePath(redPacket.getType() + "");// ???FilePath?????????????????????
                            // ????????????????????????
                            message.setFileSize(redPacket.getStatus());   // ???filesize?????????????????????
                            mChatMessages.add(message);
                            mChatContentView.notifyDataSetInvalidated(true);
                            sendMessage(message);
                            // ????????????
                            CoreManager.updateMyBalance();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }


    @Override
    public void clickPhoto() {
        // ????????????true
        /*MyApplication.GalleyNotBackGround = true;
        CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);*/
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(MucChatActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        // ??????????????????????????? ????????????????????????
        intent.setSelectedPaths(imagePaths);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
        mChatBottomView.reset();
    }

    @Override
    public void clickCamera() {
       /* mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);*/
       /* Intent intent = new Intent(this, EasyCameraActivity.class);
        startActivity(intent);*/
        mChatBottomView.reset();
        Intent intent = new Intent(this, VideoRecorderActivity.class);
        startActivity(intent);
    }

    @Override
    public void clickStartRecord() {
        // ???????????????ui????????????????????????clickCamera?????????
       /* Intent intent = new Intent(this, VideoRecorderActivity.class);
        startActivity(intent);*/
    }

    @Override
    public void clickLocalVideo() {
        // ???????????????ui????????????????????????clickCamera?????????
       /* Intent intent = new Intent(this, LocalVideoActivity.class);
        intent.putExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_SELECT);
        intent.putExtra(AppConstant.EXTRA_MULTI_SELECT, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);*/
    }

    @Override
    public void clickAudio() {
    }

    @Override
    public void clickVideoChat() {
    }

    @Override
    public void clickTalk() {
    }

    @Override
    public void clickFile() {
        boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mUseId, true);
        if (isAllowSendFile || isOk()) {
            SelectFileDialog dialog = new SelectFileDialog(this, new SelectFileDialog.OptionFileListener() {
                @Override
                public void option(List<File> files) {
                    if (files != null && files.size() > 0) {
                        for (int i = 0; i < files.size(); i++) {
                            sendFile(files.get(i));
                        }
                    }
                }

                @Override
                public void intent() {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("*/*");//???????????????????????????????????????????????????????????????????????????
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
                }
            });
            dialog.show();
        } else {
            tip(getString(R.string.tip_cannot_upload));
        }
    }

    @Override
    public void clickContact() {
        SendContactsActivity.start(this, REQUEST_CODE_SEND_CONTACT);
    }

    @Override
    public void clickLocation() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_Locate);
    }

    @Override
    public void clickCard() {
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        if (isAllowSecretlyChat || isOk()) {
            mSelectCardPopupWindow = new SelectCardPopupWindow(MucChatActivity.this, this);
            mSelectCardPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
        } else {
            tip(getString(R.string.tip_card_disable_privately_chat));
        }
    }

    @Override
    public void clickRedpacket() {
        Intent intent = new Intent(this, MucSendRedPacketActivity.class);
        intent.putExtra("groupId", mUseId);
        startActivityForResult(intent, ChatActivity.REQUEST_CODE_SEND_RED);
    }

    @Override
    public void clickTransferMoney() {
        // ????????????????????????
    }

    @Override
    public void clickCollection() {
        Intent intent = new Intent(this, MyCollection.class);
        intent.putExtra("IS_SEND_COLLECTION", true);
        startActivityForResult(intent, REQUEST_CODE_SEND_COLLECTION);
    }

    private void clickCollectionSend(
            int type,
            String content,
            int timeLen,
            String filePath,
            long fileSize
    ) {
        if (isAuthenticated() || getGroupStatus()) {
            return;
        }

        if (TextUtils.isEmpty(content)) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setType(type);
        message.setFromUserId(mLoginUserId);
        message.setFromUserName(mLoginNickName);
        message.setContent(content);
        message.setTimeLen(timeLen);
        message.setFileSize((int) fileSize);
        message.setUpload(true);
        if (!TextUtils.isEmpty(filePath)) {
            message.setFilePath(filePath);
        }
        message.setIsReadDel(0);
        mChatMessages.add(message);
        mChatContentView.notifyDataSetInvalidated(true);
        sendMessage(message);
    }

    private void clickCollectionSend(CollectionEvery collection) {
        // ????????????????????????????????????????????????????????????????????????????????????
        if (!TextUtils.isEmpty(collection.getCollectContent())) {
            sendText(collection.getCollectContent());
        }
        int type = collection.getXmppType();
        if (type == XmppMessage.TYPE_TEXT) {
            // ????????????????????????????????????????????????
            return;
        } else if (type == XmppMessage.TYPE_IMAGE) {
            // ???????????????????????????????????????
            String allUrl = collection.getUrl();
            for (String url : allUrl.split(",")) {
                clickCollectionSend(type, url, collection.getFileLength(), collection.getFileName(), collection.getFileSize());
            }
            return;
        }
        clickCollectionSend(type, collection.getUrl(), collection.getFileLength(), collection.getFileName(), collection.getFileSize());
    }

    @Override
    public void clickShake() {

    }

    @Override
    public void clickGroupAssistant(GroupAssistantDetail groupAssistantDetail) {
        if (groupAssistantDetail == null) {
            return;
        }
        if (groupAssistantDetail.getHelper().getType() == 1) {
            // ?????????????????? ?????????
            Toast.makeText(mContext, "???????????????????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
        } else if (groupAssistantDetail.getHelper().getType() == 2) {
            // ???????????? ?????? || ??????
            ShareParams shareParams = new ShareParams(mLoginUserId, mFriend.getRoomId(), mFriend.getUserId());

            String appPackName = groupAssistantDetail.getHelper().getAppPackName();
            String callBackClassName = groupAssistantDetail.getHelper().getCallBackClassName();
            if (!TextUtils.isEmpty(appPackName)
                    && !TextUtils.isEmpty(callBackClassName)
                    && AppUtils.isAppInstalled(mContext, appPackName)) {
                Intent intent = new Intent();
                intent.setClassName(appPackName, callBackClassName);
                intent.putExtra("shareParams", JSON.toJSONString(shareParams));
                startActivity(intent);
            } else {
                WebViewActivity.start(mContext, groupAssistantDetail.getHelper().getLink(), JSON.toJSONString(shareParams));
            }
        } else if (groupAssistantDetail.getHelper().getType() == 3) {
            // ?????????????????? ?????????????????? ?????? || ??????
            ChatMessage message = new ChatMessage();
            message.setType(XmppMessage.TYPE_SHARE_LINK);
            message.setFromUserId(mLoginUserId);
            message.setFromUserName(coreManager.getSelf().getNickName());
            message.setObjectId(JSON.toJSONString(groupAssistantDetail.getHelper().getOther()));
            mChatMessages.add(message);
            mChatContentView.notifyDataSetInvalidated(true);
            sendMessage(message);
        }
    }

    @Override
    public void onInputState() {

    }

    /**
     * ?????????com.client.yanchat.ui.me.LocalVideoActivity#helloEventBus(com.client.yanchat.adapter.MessageVideoFile)
     * ?????????CameraDemoActivity??????????????????activity result, ?????????EventBus,
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUploadFileRate message) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            if (mChatMessages.get(i).getPacketId().equals(message.getPacketId())) {
                mChatMessages.get(i).setUploadSchedule(message.getRate());
                // ???????????????setUpload????????????????????????????????????????????????????????????????????????url,????????????????????????
                mChatContentView.notifyDataSetChanged();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventUploadCancel message) {
        for (int i = 0; i < mChatMessages.size(); i++) {
            if (mChatMessages.get(i).getPacketId().equals(message.getPacketId())) {
                mChatMessages.remove(i);
                mChatContentView.notifyDataSetChanged();
                ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mFriend.getUserId(), message.getPacketId());
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageVideoFile message) {
        VideoFile videoFile = new VideoFile();
        videoFile.setCreateTime(TimeUtils.f_long_2_str(System.currentTimeMillis()));
        videoFile.setFileLength(message.timelen);
        videoFile.setFileSize(message.length);
        videoFile.setFilePath(message.path);
        videoFile.setOwnerId(coreManager.getSelf().getUserId());
        VideoFileDao.getInstance().addVideoFile(videoFile);
        String filePath = message.path;
        if (TextUtils.isEmpty(filePath)) {
            ToastUtil.showToast(this, R.string.record_failed);
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            ToastUtil.showToast(this, R.string.record_failed);
            return;
        }
        sendVideo(file);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageLocalVideoFile message) {
        sendVideo(message.file);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(EventRedReceived message) {
        showReceiverRedLocal(message.getOpenRedpacket());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_FILE: // ???????????????????????????
                    String file_path = FileUtils.getPath(MucChatActivity.this, data.getData());
                    if (file_path == null) {
                        ToastUtil.showToast(mContext, R.string.tip_file_not_supported);
                    } else {
                        sendFile(new File(file_path));
                    }
                    break;
                case REQUEST_CODE_CAPTURE_PHOTO:
                    // ????????????
                    if (mNewPhotoUri != null) {
                        // ????????????????????????????????????
                        photograph(new File(mNewPhotoUri.getPath()));
                    }
                    break;
                case REQUEST_CODE_PICK_PHOTO:
                    if (data != null) {
                        boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                        album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                    } else {
                        ToastUtil.showToast(this, R.string.c_photo_album_failed);
                    }
                    break;
                case REQUEST_CODE_SELECT_VIDEO: {
                    // ?????????????????????
                    if (data == null) {
                        return;
                    }
                    String json = data.getStringExtra(AppConstant.EXTRA_VIDEO_LIST);
                    List<VideoFile> fileList = JSON.parseArray(json, VideoFile.class);
                    if (fileList == null || fileList.size() == 0) {
                        // ???????????????????????????????????????
                        Reporter.unreachable();
                    } else {
                        for (VideoFile videoFile : fileList) {
                            String filePath = videoFile.getFilePath();
                            if (TextUtils.isEmpty(filePath)) {
                                // ???????????????????????????????????????
                                Reporter.unreachable();
                            } else {
                                File file = new File(filePath);
                                if (!file.exists()) {
                                    // ???????????????????????????????????????
                                    Reporter.unreachable();
                                } else {
                                    sendVideo(file);
                                }
                            }
                        }
                    }
                    break;
                }
                case REQUEST_CODE_SELECT_Locate:
                    double latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
                    double longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
                    String address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
                    String snapshot = data.getStringExtra(AppConstant.EXTRA_SNAPSHOT);

                    if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)
                            && !TextUtils.isEmpty(snapshot)) {
                        sendLocate(latitude, longitude, address, snapshot);
                    } else {
                        ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXServer_CannotLocation"));
                    }
                    break;
                case REQUEST_CODE_SEND_COLLECTION: {
                    String json = data.getStringExtra("data");
                    CollectionEvery collection = JSON.parseObject(json, CollectionEvery.class);
                    clickCollectionSend(collection);
                    break;
                }
                case REQUEST_CODE_QUICK_SEND:
                    String image = QuickSendPreviewActivity.parseResult(data);
                    sendImage(new File(image));
                    break;
                case REQUEST_CODE_INVITE:
                    if (data != null && data.getExtras() != null) {
                        // ???????????????????????????
                        mChatMessages.clear();
                        loadDatas(false);
                    }
                    break;
                case REQUEST_CODE_SEND_CONTACT: {
                    List<Contacts> contactsList = SendContactsActivity.parseResult(data);
                    if (contactsList == null) {
                        ToastUtil.showToast(mContext, R.string.simple_data_error);
                    } else {
                        sendContacts(contactsList);
                    }
                    break;
                }
            }
        } else {
            switch (requestCode) {
                case ChatActivity.REQUEST_CODE_SEND_RED:
                    if (data != null && data.getExtras() != null) {
                        Bundle bundle = data.getExtras();
                        sendRed(bundle.getString("type")
                                , bundle.getString("money")
                                , bundle.getString("count")
                                , bundle.getString("words")
                                , bundle.getString("payPassword")
                                , ""
                        );
                        //  RedPacket redPacket = (RedPacket) bundle.getSerializable("redPacket");
                        //  sendRed(redPacket);
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    // ?????????????????? ??????
    private void photograph(final File file) {
        // ?????????????????????Luban???????????????
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // ????????????100kb ?????????
                // .putGear(2)     // ?????????????????????????????????
                // .setTargetDir() // ??????????????????????????????
                .setCompressListener(new OnCompressListener() { // ????????????
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        sendImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {
                        sendImage(file);
                    }
                }).launch();// ????????????
    }

    // ?????????????????? ??????
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// ????????????????????????
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                sendImage(new File(stringArrayListExtra.get(i)));
            }
            return;
        }

        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            if (stringArrayListExtra.get(i).endsWith("gif")) {
                fileList.add(new File(stringArrayListExtra.get(i)));
                stringArrayListExtra.remove(i);
            } else {
                List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
                boolean support = false;
                for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                    if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                        support = true;
                        break;
                    }
                }
                if (!support) {
                    fileList.add(new File(stringArrayListExtra.get(i)));
                    stringArrayListExtra.remove(i);
                }
            }
        }

        if (fileList.size() > 0) {
            for (File file : fileList) {// ?????????????????????????????????
                sendImage(file);
            }
        }

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// ????????????100kb ?????????
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        sendImage(file);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// ????????????
    }

    /**********************
     * MUC Message Listener
     ********************/
    @Override
    public void onMessageSendStateChange(int messageState, String msgId) {
        if (TextUtils.isEmpty(msgId)) {
            return;
        }
        for (int i = 0; i < mChatMessages.size(); i++) {
            ChatMessage msg = mChatMessages.get(i);
            if (msgId.equals(msg.getPacketId())) {
                /**
                 * ??????????????????????????????????????????????????????????????????????????????????????????????????????
                 * ???????????????????????????????????????????????????????????????1???????????????0??????????????????
                 */
                if (msg.getMessageState() == 1) {
                    return;
                }
                msg.setMessageState(messageState);
                mChatContentView.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public boolean onNewMessage(String fromUserId, ChatMessage message, boolean isGroupMsg) { // ???????????????
        if (isGroupMsg != isGroupChat) {
            return false;
        }

        if (mUseId.compareToIgnoreCase(fromUserId) == 0) {// ????????????????????????
            /**
             *  ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
             *  ?????????????????????????????????????????????????????????????????????(??????????????????)??????????????????????????????????????????????????????
             *  ??????????????????onNewMessage????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
             *  ?????????????????????????????????
             *
             */
            if (mChatMessages.size() > 0) {
                if (mChatMessages.get(mChatMessages.size() - 1).getPacketId().equals(message.getPacketId())) {// ?????????????????????msgId==????????????msgId
                    Log.e("zq", "????????????????????????");
                    return false;
                }
            }

            mChatMessages.add(message);
            if (mChatContentView.shouldScrollToBottom()) {
                mChatContentView.notifyDataSetInvalidated(true);
            } else {
                // ??????????????????
                Vibrator vibrator = (Vibrator) MyApplication.getContext().getSystemService(VIBRATOR_SERVICE);
                long[] pattern = {100, 400, 100, 400};
                if (vibrator != null) {
                    vibrator.vibrate(pattern, -1);
                }
                mChatContentView.notifyDataSetChanged();
            }

            return true;
        }
        return false;
    }

    /**********************
     * MUC Operation Listener
     ********************/
    @Override
    public void onMyBeDelete(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {// ????????????
            Toast.makeText(this, R.string.tip_been_kick_self, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onDeleteMucRoom(String toUserId) {
        if (toUserId != null && toUserId.equals(mUseId)) {// ????????????
            Toast.makeText(this, R.string.tip_group_been_disbanded, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onNickNameChange(String toUserId, String changedUserId, String changedName) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            // ???????????????
            if (changedUserId.equals("ROOMNAMECHANGE")) {
                mFriend.setNickName(changedName);
                updateMemberCount(mCurrentMemberNum);
                return;
            }
            // ?????????????????????
            if (changedUserId.equals(mLoginUserId)) {// ??????????????????????????????
                mFriend.setRoomMyNickName(changedName);
                mChatContentView.setCurGroup(true, changedName);
            }
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (TextUtils.equals(mChatMessages.get(i).getFromUserId(), changedUserId)) {
                    mChatMessages.get(i).setFromUserName(changedName);
                }
            }
            mChatContentView.notifyDataSetChanged();
        }
    }

    /*******************************************
     * ?????????EventBus??????????????????
     ******************************************/

    @Override
    public void onMyVoiceBanned(String toUserId, int time) {
        if (toUserId != null && toUserId.equals(mUseId)) {
            mFriend.setRoomTalkTime(time);
        }
    }

    /**
     * ??????????????????
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNewNotice message) {
        if (TextUtils.equals(mFriend.getUserId(), message.getRoomJid())) {
            setLastNotice(message.getText());
        }
    }

    /**
     * ??????????????????
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventRoomNotice message) {
        setLastNotice(message.getText());
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventNotifyByTag message) {
        if (TextUtils.equals(message.tag, "GroupAssistant")) {
            if (mChatBottomView != null) {
                mChatBottomView.notifyAssistant();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventXMPPJoinGroupFailed message) {
        if (message.roomJId.equals(mFriend.getUserId())) {
            DialogHelper.tip(MucChatActivity.this, "??????????????????????????????????????????????????????????????????????????????????????????????????????app??????");
        }
    }

    // ??????????????????
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final EventMoreSelected message) {
        List<ChatMessage> mSelectedMessageList = new ArrayList<>();
        if (message.getToUserId().equals("MoreSelectedCollection") || message.getToUserId().equals("MoreSelectedEmail")) {// ?????? ?????? || ??????
            moreSelected(false, 0);
            return;
        }
        if (message.getToUserId().equals("MoreSelectedDelete")) {// ?????? ??????
            for (int i = 0; i < mChatMessages.size(); i++) {
                if (mChatMessages.get(i).isMoreSelected) {
                    if (ChatMessageDao.getInstance().deleteSingleChatMessage(mLoginUserId, mUseId, mChatMessages.get(i)))
                        mSelectedMessageList.add(mChatMessages.get(i));
                }
            }

            String mMsgIdListStr = "";
            for (int i = 0; i < mSelectedMessageList.size(); i++) {
                if (i == mSelectedMessageList.size() - 1) {
                    mMsgIdListStr += mSelectedMessageList.get(i).getPacketId();
                } else {
                    mMsgIdListStr += mSelectedMessageList.get(i).getPacketId() + ",";
                }
            }
            deleteMessage(mMsgIdListStr);// ????????????????????????

            mChatMessages.removeAll(mSelectedMessageList);
        } else {// ?????? ??????
            if (message.isSingleOrMerge()) {// ????????????
                List<String> mStringHistory = new ArrayList<>();
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).isMoreSelected) {
                        String body = mChatMessages.get(i).toJsonString();
                        mStringHistory.add(body);
                    }
                }
                String detail = JSON.toJSONString(mStringHistory);
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType(XmppMessage.TYPE_CHAT_HISTORY);
                chatMessage.setFromUserId(mLoginUserId);
                chatMessage.setFromUserName(mLoginNickName);
                chatMessage.setToUserId(message.getToUserId());
                chatMessage.setContent(detail);
                chatMessage.setMySend(true);
                chatMessage.setReSendCount(0);
                chatMessage.setSendRead(false);
                chatMessage.setIsEncrypt(0);
                chatMessage.setIsReadDel(0);
                chatMessage.setObjectId(getString(R.string.group_chat_history));
                chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.getToUserId(), chatMessage);
                if (message.isGroupMsg()) {
                    coreManager.sendMucChatMessage(message.getToUserId(), chatMessage);
                } else {
                    coreManager.sendChatMessage(message.getToUserId(), chatMessage);
                }
                if (message.getToUserId().equals(mFriend.getUserId())) {// ?????????????????????
                    mChatMessages.add(chatMessage);
                }
            } else {// ????????????
                for (int i = 0; i < mChatMessages.size(); i++) {
                    if (mChatMessages.get(i).isMoreSelected) {
                        ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, mFriend.getUserId(), mChatMessages.get(i).getPacketId());
                        if (chatMessage.getType() == XmppMessage.TYPE_RED) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_red_packet));
                        } else if (chatMessage.getType() >= XmppMessage.TYPE_IS_CONNECT_VOICE
                                && chatMessage.getType() <= XmppMessage.TYPE_EXIT_VOICE) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_video_voice));
                        } else if (chatMessage.getType() == XmppMessage.TYPE_SHAKE) {
                            chatMessage.setType(XmppMessage.TYPE_TEXT);
                            chatMessage.setContent(getString(R.string.msg_shake));
                        }
                        chatMessage.setFromUserId(mLoginUserId);
                        chatMessage.setFromUserName(mLoginNickName);
                        chatMessage.setToUserId(message.getToUserId());
                        chatMessage.setUpload(true);
                        chatMessage.setMySend(true);
                        chatMessage.setSendRead(false);
                        chatMessage.setIsEncrypt(0);
                        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                        mSelectedMessageList.add(chatMessage);
                    }
                }

                for (int i = 0; i < mSelectedMessageList.size(); i++) {
                    ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, message.getToUserId(), mSelectedMessageList.get(i));
                    if (message.isGroupMsg()) {
                        coreManager.sendMucChatMessage(message.getToUserId(), mSelectedMessageList.get(i));
                    } else {
                        coreManager.sendChatMessage(message.getToUserId(), mSelectedMessageList.get(i));
                    }

                    if (message.getToUserId().equals(mFriend.getUserId())) {// ?????????????????????
                        mChatMessages.add(mSelectedMessageList.get(i));
                    }
                }
            }
        }
        moreSelected(false, 0);
    }

    public void moreSelected(boolean isShow, int position) {
        mChatBottomView.showMoreSelectMenu(isShow);
        if (isShow) {
            findViewById(R.id.iv_title_left).setVisibility(View.GONE);
            mTvTitleLeft.setVisibility(View.VISIBLE);
            mChatMessages.get(position).setMoreSelected(true);
        } else {
            findViewById(R.id.iv_title_left).setVisibility(View.VISIBLE);
            mTvTitleLeft.setVisibility(View.GONE);
            for (int i = 0; i < mChatMessages.size(); i++) {
                mChatMessages.get(i).setMoreSelected(false);
            }
        }
        mChatContentView.setIsShowMoreSelect(isShow);
        mChatContentView.notifyDataSetChanged();
    }

    /**
     * ??????????????????
     */
    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageUploadChatRecord message) {
        final CreateCourseDialog dialog = new CreateCourseDialog(MucChatActivity.this, new CreateCourseDialog.CoureseDialogConfirmListener() {
            @Override
            public void onClick(String content) {
                upLoadChatList(message.chatIds, content);
            }

        });
        dialog.show();
    }

    private void upLoadChatList(String chatIds, String name) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("messageIds", chatIds);
        params.put("userId", mLoginUserId);
        params.put("courseName", name);
        params.put("createTime", TimeUtils.sk_time_current_time() + "");
        params.put("roomJid", mUseId);
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_ADD_COURSE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showToast(MucChatActivity.this, "??????????????????");
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MucChatActivity.this);
                    }
                });
    }

    private void initRoomMember() {
        if (mFriend.getGroupStatus() == 0) {// ????????????
            List<RoomMember> roomMemberList = RoomMemberDao.getInstance().getRoomMember(roomId);
            if (roomMemberList.size() > 0) {
                mRoomMember = RoomMemberDao.getInstance().getSingleRoomMember(roomId, mLoginUserId);
                if (mRoomMember != null) {// ??????????????????
                    onRoleChanged(mRoomMember.getRole());
                }
                // ??????????????????????????????????????????????????????
                mChatContentView.setRoomMemberList(roomMemberList);
            } else {
                loadMembers(roomId, false);
            }
        }
    }

    /*******************************************
     * ?????????ActionBar??????????????????
     ******************************************/

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBack();
            }
        });

        mTvTitleLeft = (TextView) findViewById(R.id.tv_title_left);
        mTvTitleLeft.setVisibility(View.GONE);
        mTvTitleLeft.setText(getString(R.string.cancel));
        mTvTitleLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreSelected(false, 0);
            }
        });
        mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        if (!TextUtils.isEmpty(mNickName)) {
            mTvTitle.setText(mNickName);
        }
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.chat_more);
        ivRight.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View view) {
                if (mFriend.getGroupStatus() == 0) {
                    mChatBottomView.reset();
                    mChatBottomView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // ?????????????????????Activity
                            Intent intent = new Intent(MucChatActivity.this, RoomInfoActivity.class);
                            intent.putExtra(AppConstant.EXTRA_USER_ID, mUseId);
                            intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
                            startActivity(intent);
                        }
                    }, 100);
                }
            }
        });
    }

    public void synchronizeChatHistory() {
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????
        // ?????????????????????????????????????????????
        mChatContentView.setNeedRefresh(false);

        long startTime;
        String chatSyncTimeLen = String.valueOf(PrivacySettingHelper.getPrivacySettings(this).getChatSyncTimeLen());
        chatSyncTimeLen = "0";
//        if (Double.parseDouble(chatSyncTimeLen) == -2) {// ?????????
//            mChatContentView.setNeedRefresh(true);
//            return;
//        }
        if (Double.parseDouble(chatSyncTimeLen) == -1 || Double.parseDouble(chatSyncTimeLen) == 0) {// ?????? ??????
            startTime = 0;
        } else {
            long syncTimeLen = (long) (Double.parseDouble(chatSyncTimeLen) * 24 * 60 * 60);// ????????????????????????
            startTime = TimeUtils.sk_time_current_time() - syncTimeLen;
        }

        Map<String, String> params = new HashMap();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mFriend.getUserId());
        params.put("startTime", String.valueOf(startTime * 1000));
        params.put("endTime", String.valueOf(TimeUtils.sk_time_current_time() * 1000));
        params.put("pageSize", String.valueOf(Constants.MSG_ROMING_PAGE_SIZE));

        HttpUtils.get().url(coreManager.getConfig().GET_CHAT_MSG_MUC)
                .params(params)
                .build()
                .execute(new ListCallback<ChatRecord>(ChatRecord.class) {
                    @Override
                    public void onResponse(ArrayResult<ChatRecord> result) {
                        final List<ChatRecord> chatRecordList = result.getData();
                        if (chatRecordList != null && chatRecordList.size() > 0) {
                            new Thread(() -> {
                                chatMessages = new ArrayList<>();

                                for (int i = 0; i < chatRecordList.size(); i++) {
                                    ChatRecord data = chatRecordList.get(i);
                                    String messageBody = data.getBody();
                                    messageBody = messageBody.replaceAll("&quot;", "\"");
                                    ChatMessage chatMessage = new ChatMessage(messageBody);

                                    if (!TextUtils.isEmpty(chatMessage.getFromUserId()) &&
                                            chatMessage.getFromUserId().equals(mLoginUserId)) {
                                        chatMessage.setMySend(true);
                                    }

                                    chatMessage.setSendRead(true); // ?????????????????????
                                    // ????????????????????????
                                    chatMessage.setUpload(true);
                                    chatMessage.setUploadSchedule(100);
                                    chatMessage.setMessageState(MESSAGE_SEND_SUCCESS);

                                    if (TextUtils.isEmpty(chatMessage.getPacketId())) {
                                        if (!TextUtils.isEmpty(data.getMessageId())) {
                                            chatMessage.setPacketId(data.getMessageId());
                                        } else {
                                            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                        }
                                    }

                                    if (ChatMessageDao.getInstance().roamingMessageFilter(chatMessage.getType())) {
                                        ChatMessageDao.getInstance().decryptDES(chatMessage);
                                        ChatMessageDao.getInstance().handlerRoamingSpecialMessage(chatMessage);
                                        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage)) {
                                            chatMessages.add(chatMessage);
                                        }
                                    }
                                }

                                mTvTitle.post(() -> {
                                    for (int i = chatMessages.size() - 1; i >= 0; i--) {
                                        mChatMessages.add(chatMessages.get(i));
                                    }
                                    // ????????????????????????????????????????????????????????????mChatMessages????????????
                                    Comparator<ChatMessage> comparator = (c1, c2) -> (int) (c1.getDoubleTimeSend() - c2.getDoubleTimeSend());
                                    Collections.sort(mChatMessages, comparator);
                                    mChatContentView.notifyDataSetInvalidated(true);

                                    mChatContentView.setNeedRefresh(true);
                                });
                            }).start();
                        } else {
                            mChatContentView.setNeedRefresh(true);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mChatContentView.setNeedRefresh(true);
                        ToastUtil.showErrorData(MucChatActivity.this);
                    }
                });
    }

    // ???????????????????????????????????????????????????????????????
    public void getNetSingle() {
        Map<String, String> params = new HashMap<>();

        String startTime = "1262275200000";
        String endTime;
        if (mChatMessages != null && mChatMessages.size() > 0) {
            endTime = String.valueOf(mChatMessages.get(0).getTimeSend() * 1000);
        } else {
            endTime = String.valueOf(TimeUtils.sk_time_current_time() * 1000);
        }

        final MsgRoamTask mLastMsgRoamTask = MsgRoamTaskDao.getInstance().getFriendLastMsgRoamTask(mLoginUserId, mFriend.getUserId());
        if (mLastMsgRoamTask != null) {// ???????????????????????????startTime???endTime????????????
            startTime = String.valueOf(mLastMsgRoamTask.getStartTime() * 1000);
            endTime = String.valueOf(mLastMsgRoamTask.getEndTime() * 1000);
        }

        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", mUseId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("pageSize", String.valueOf(Constants.MSG_ROMING_PAGE_SIZE));

        HttpUtils.get().url(coreManager.getConfig().GET_CHAT_MSG_MUC)
                .params(params)
                .build()
                .execute(new ListCallback<ChatRecord>(ChatRecord.class) {
                    @Override
                    public void onResponse(ArrayResult<ChatRecord> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<ChatRecord> chatRecordList = result.getData();
                            long mLastTaskNewEndTime = 0;
                            long currTime = TimeUtils.sk_time_current_time();

                            if (chatRecordList != null && chatRecordList.size() > 0) {
                                for (int i = 0; i < chatRecordList.size(); i++) {
                                    ChatRecord data = chatRecordList.get(i);
                                    String messageBody = data.getBody();
                                    messageBody = messageBody.replaceAll("&quot;", "\"");
                                    ChatMessage chatMessage = new ChatMessage(messageBody);

                                    // ?????????????????????????????????1???????????????????????????????????????????????????????????????
                                    if (chatMessage.getDeleteTime() > 1 && chatMessage.getDeleteTime() < currTime) {
                                        // ??????????????????,??????
                                        continue;
                                    }

                                    mLastTaskNewEndTime = chatMessage.getTimeSend();

                                    if (!TextUtils.isEmpty(chatMessage.getFromUserId())
                                            && chatMessage.getFromUserId().equals(mLoginUserId)) {
                                        chatMessage.setMySend(true);
                                    }
                                    chatMessage.setSendRead(true);// ???????????????????????????????????????
                                    // ????????????????????????
                                    chatMessage.setUpload(true);
                                    chatMessage.setUploadSchedule(100);
                                    chatMessage.setMessageState(MESSAGE_SEND_SUCCESS);

                                    if (TextUtils.isEmpty(chatMessage.getPacketId())) {
                                        if (!TextUtils.isEmpty(data.getMessageId())) {
                                            chatMessage.setPacketId(data.getMessageId());
                                        } else {
                                            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                                        }
                                    }

                                    if (ChatMessageDao.getInstance().roamingMessageFilter(chatMessage.getType())) {
                                        ChatMessageDao.getInstance().saveRoamingChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage);
                                    }
                                }

                                mHasMoreData = chatRecordList.size() == Constants.MSG_ROMING_PAGE_SIZE;
                                // notifyChatAdapter();
                            } else {
                                mHasMoreData = false;
                                mChatContentView.headerRefreshingCompleted();
                                mChatContentView.setNeedRefresh(false);
                            }

                            if (mLastMsgRoamTask != null) {
                                mHasMoreData = true;// ??????????????????????????????????????????

                                if (chatRecordList != null && chatRecordList.size() == Constants.MSG_ROMING_PAGE_SIZE) {// ????????????page_size???????????????????????????????????????????????????????????????EndTime
                                    MsgRoamTaskDao.getInstance().updateMsgRoamTaskEndTime(mLoginUserId, mLastMsgRoamTask.getUserId(),
                                            mLastMsgRoamTask.getTaskId(), mLastTaskNewEndTime);
                                } else {// ?????????????????????????????????
                                    MsgRoamTaskDao.getInstance().deleteMsgRoamTask(mLoginUserId, mLastMsgRoamTask.getUserId(), mLastMsgRoamTask.getTaskId());
                                }
                            }

                            notifyChatAdapter();// ???????????????updateMsgRoamTaskEndTime????????????

                        } else {
                            ToastUtil.showErrorData(MucChatActivity.this);
                        }

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                    }
                });
    }

    private void notifyChatAdapter() {
        // ??????????????????????????????????????????????????????????????????????????????????????????????????? mMinId ?????????????????????
        if (mChatMessages.size() > 0) {
            mMinId = mChatMessages.get(0).getDoubleTimeSend();
        } else {
            mMinId = TimeUtils.sk_time_current_time_double();
        }
        List<ChatMessage> chatLists = ChatMessageDao.getInstance().getOneGroupChatMessages(mLoginUserId,
                mFriend.getUserId(), mMinId, mPageSize);

        for (int i = 0; i < chatLists.size(); i++) {
            ChatMessage message = chatLists.get(i);
            mChatMessages.add(0, message);
        }
/*
        if (chatLists.size() == 0) {
            mHasMoreData = false;
            mChatContentView.headerRefreshingCompleted();
            mChatContentView.setNeedRefresh(false);
        }
*/
        mChatContentView.notifyDataSetAddedItemsToTop(chatLists.size());
        mChatContentView.headerRefreshingCompleted();
        if (!mHasMoreData) {
            mChatContentView.setNeedRefresh(false);
        }
    }

    /*******************************************
     * ?????????????????????id && @?????????(????????????????????????????????????????????????????????????)
     * ?????? && ???????????? && ????????????@??????
     ******************************************/
    private void loadMembers(final String roomId, final boolean isAtAction) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);
        params.put("pageSize", Constants.MUC_MEMBER_PAGE_SIZE);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();
                                     update(mucRoom, isAtAction);
                                 } else {
                                     ToastUtil.showErrorData(mContext);
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                             }
                         }
                );
    }

    @SuppressLint("SetTextI18n")
    private void updateMemberCount(int userSize) {
        mCurrentMemberNum = userSize;
        mTvTitle.setText(mFriend.getNickName() + "???" + userSize + "" + getString(R.string.people) + "???");
    }

    private void instantChatMessage() {
        if (!TextUtils.isEmpty(instantMessage)) {
            String toUserId = getIntent().getStringExtra("fromUserId");
            ChatMessage chatMessage = ChatMessageDao.getInstance().findMsgById(mLoginUserId, toUserId, instantMessage);
            instantMessage = null;
            boolean isAllowSendFile = PreferenceUtils.getBoolean(mContext, Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mUseId, true);
            if (chatMessage.getType() == ChatMessage.TYPE_FILE && !isAllowSendFile && !isOk()) {
                tip(getString(R.string.tip_cannot_upload));
                return;
            }
            chatMessage.setFromUserId(mLoginUserId);
            chatMessage.setFromUserName(mLoginNickName);
            chatMessage.setToUserId(mFriend.getUserId());
            chatMessage.setUpload(true);
            chatMessage.setMySend(true);
            // ???????????????????????????????????????????????????????????????????????????content??????????????????????????????????????????isEncrypt??????????????????
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????
            chatMessage.setIsEncrypt(0);
            chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
            mChatMessages.add(chatMessage);
            mChatContentView.notifyDataSetInvalidated(true);
            ChatMessageDao.getInstance().saveNewSingleChatMessage(mLoginUserId, mFriend.getUserId(), chatMessage);
            send(chatMessage);
        }
    }

    /**
     * ????????????????????????????????????????????????
     * ??????????????????????????????????????????
     */
    private void getMyInfoInThisRoom() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(coreManager.getConfig().ROOM_GET_ROOM)
                .params(params)
                .build()
                .execute(new BaseCallback<MucRoom>(MucRoom.class) {

                             @Override
                             public void onResponse(ObjectResult<MucRoom> result) {// ???????????????room/get??????????????????????????????????????????????????????????????????
                                 if (result.getResultCode() == 1 && result.getData() != null) {
                                     final MucRoom mucRoom = result.getData();

                                     if (mucRoom.getS() == -1) {// ???????????????????????????
                                         groupTip(getString(R.string.tip_group_disable_by_service));
                                         return;
                                     }

                                     if (mucRoom.getMember() == null) {// ??????????????????
                                         coreManager.exitMucChat(mucRoom.getJid());// XMPP??????
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 1);// ????????????????????????

                                         groupTip(getString(R.string.tip_been_kick_self));
                                     } else {// ????????????
                                         List<RoomMember> roomMemberList = update(mucRoom, false);

                                         mFriend.setGroupStatus(0);
                                         FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mucRoom.getJid(), 0);// ????????????????????????
                                         // ??????????????????
                                         FriendDao.getInstance().updateRoomTalkTime(mLoginUserId, mucRoom.getJid(), mucRoom.getMember().getMemberTalkTime());
                                         onMyVoiceBanned(mucRoom.getJid(), mucRoom.getMember().getMemberTalkTime());

                                         // ??????????????????
                                         // ?????????????????????????????????????????????????????????????????????????????????
                                         RoomMemberDao.getInstance().updateRoomMemberRole(mucRoom.getId(), mLoginUserId, mucRoom.getMember().getRole());
                                         onRoleChanged(mucRoom.getMember().getRole());
                                         mChatContentView.setRoomMemberList(roomMemberList);

                                         // ????????????????????????????????????????????????
                                         instantChatMessage();
                                     }
                                 } else {
                                     FriendDao.getInstance().updateFriendGroupStatus(mLoginUserId, mFriend.getUserId(), 2);// ????????????????????????

                                     groupTip(TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.tip_group_been_disbanded) : result.getResultMsg());
                                 }
                             }

                             @Override
                             public void onError(Call call, Exception e) {
                                 ToastUtil.showNetError(mContext);
                             }
                         }
                );
    }

    private void groupTip(String tip) {
        tip(tip, true);
    }

    /*******************************************
     * ?????????????????????|| ?????????????????? && ????????????&&??????
     ******************************************/
    public boolean getGroupStatus() {
        if (mFriend.getGroupStatus() == 1) {
            tip(getString(R.string.tip_been_kick));
            return true;
        } else if (mFriend.getGroupStatus() == 2) {
            tip(getString(R.string.tip_disbanded));
            return true;
        } else {
            return false;
        }
    }

    public void tip(String tip) {
        tip(tip, false);
    }

    /**
     * @param finish ???????????????????????????????????????ture,
     */
    private void tip(String tip, boolean finish) {
        if (isFinishing()) {
            return;
        }
        if (tipDialog == null) {
            tipDialog = new TipDialog(MucChatActivity.this);
        }
        if (tipDialog.isShowing()) {
            tipDialog.dismiss();
        }
        if (finish) {
            tipDialog.setmConfirmOnClickListener(tip, new TipDialog.ConfirmOnClickListener() {
                @Override
                public void confirm() {
                    finish();
                }
            });
        } else {
            tipDialog.setTip(tip);
        }
        tipDialog.show();
    }

    public boolean isOk() {// ??????????????????????????????
        boolean isOk = true;
        if (mRoomMember != null) {
            if (mRoomMember.getRole() == 1 || mRoomMember.getRole() == 2) {
                isOk = true;
            } else {
                isOk = false;
            }
        }
        return isOk;
    }

    public boolean isAuthenticated() {
        boolean isLogin = coreManager.isLogin();
        if (!isLogin) {
            coreManager.autoReconnect(this);
        }
        // Todo ???????????????????????????return???????????????...??????????????????(?????????)
        // return !isLogin;
        return false;
    }

    // ???????????????????????????????????????????????????????????????
    private void updateBannedStatus() {
        // ????????????
        boolean isAllShutUp = PreferenceUtils.getBoolean(mContext, Constants.GROUP_ALL_SHUP_UP + mFriend.getUserId(), false);
        if (mRoomMember != null) {
            if (mRoomMember.isInvisible()) {
                mChatBottomView.isBanned(true, R.string.hint_invisible);
            } else {
                mChatBottomView.isAllBanned(isAllShutUp && mRoomMember.isAllBannedEffective());
            }
        } else {
            mChatBottomView.isAllBanned(isAllShutUp);
        }
    }

    private void onRoleChanged(int role) {
        if (mRoomMember != null) {
            mRoomMember.setRole(role);
        }
        mChatContentView.setRole(role);
        updateBannedStatus();
        // ?????????????????????????????????????????????ChatContentView,
        boolean isAllowSecretlyChat = PreferenceUtils.getBoolean(mContext, Constants.IS_SEND_CARD + mUseId, true);
        updateSecret(!isAllowSecretlyChat && !isOk());
    }

    private List<RoomMember> update(MucRoom mucRoom, boolean isAtAction) {
        // ?????????????????????
        MyApplication.getInstance().saveGroupPartStatus(mucRoom.getJid(), mucRoom.getShowRead(), mucRoom.getAllowSendCard(),
                mucRoom.getAllowConference(), mucRoom.getAllowSpeakCourse(), mucRoom.getTalkTime());
        PreferenceUtils.putBoolean(MyApplication.getContext(),
                Constants.IS_NEED_OWNER_ALLOW_NORMAL_INVITE_FRIEND + mucRoom.getJid(), mucRoom.getIsNeedVerify() == 1);
        PreferenceUtils.putBoolean(MyApplication.getContext(),
                Constants.IS_ALLOW_NORMAL_SEND_UPLOAD + mucRoom.getJid(), mucRoom.getAllowUploadFile() == 1);

        // ???????????????????????????
        setLastNotice(mucRoom.getLastNotice());
        // ?????????????????????????????????????????????ChatContentView,
        updateSecret(mucRoom.getAllowSendCard() != 1 && !isOk());
        // ????????????????????????
        updateMemberCount(mucRoom.getUserSize());
        // ????????????????????????
        mFriend.setChatRecordTimeOut(mucRoom.getChatRecordTimeOut());
        FriendDao.getInstance().updateChatRecordTimeOut(mFriend.getUserId(), mucRoom.getChatRecordTimeOut());

        List<RoomMember> roomMemberList = new ArrayList<>();
        for (int i = 0; i < mucRoom.getMembers().size(); i++) {
            RoomMember roomMember = new RoomMember();
            roomMember.setRoomId(mucRoom.getId());
            roomMember.setUserId(mucRoom.getMembers().get(i).getUserId());
            roomMember.setUserName(mucRoom.getMembers().get(i).getNickName());
            if (TextUtils.isEmpty(mucRoom.getMembers().get(i).getRemarkName())) {
                roomMember.setCardName(mucRoom.getMembers().get(i).getNickName());
            } else {
                roomMember.setCardName(mucRoom.getMembers().get(i).getRemarkName());
            }
            roomMember.setRole(mucRoom.getMembers().get(i).getRole());
            roomMember.setCreateTime(mucRoom.getMembers().get(i).getCreateTime());
            roomMemberList.add(roomMember);
        }
        MucRoomMember myself = mucRoom.getMember();
        RoomMember roomMember = new RoomMember();
        roomMember.setRoomId(mucRoom.getId());
        roomMember.setUserId(myself.getUserId());
        roomMember.setUserName(myself.getNickName());
        if (TextUtils.isEmpty(myself.getRemarkName())) {
            roomMember.setCardName(myself.getNickName());
        } else {
            roomMember.setCardName(myself.getRemarkName());
        }
        roomMember.setRole(myself.getRole());
        roomMember.setCreateTime(myself.getCreateTime());
        mRoomMember = roomMember;
        onRoleChanged(roomMember.getRole());
        roomMemberList.add(roomMember);

        AsyncUtils.doAsync(this, mucChatActivityAsyncContext -> {
            for (int i = 0; i < roomMemberList.size(); i++) {// ????????????????????????
                RoomMemberDao.getInstance().saveSingleRoomMember(mucRoom.getId(), roomMemberList.get(i));
            }
        });

        if (isAtAction) {// ???@?????? ????????????????????? ?????????@??????
            // ???????????????
            for (int i = 0; i < roomMemberList.size(); i++) {
                if (roomMemberList.get(i).getUserId().equals(mLoginUserId)) {
                    roomMemberList.remove(roomMemberList.get(i));
                }
            }
            mSelectRoomMemberPopupWindow = new SelectRoomMemberPopupWindow(MucChatActivity.this, MucChatActivity.this, roomMemberList, mRoomMember.getRole());
            mSelectRoomMemberPopupWindow.showAtLocation(findViewById(R.id.root_view),
                    Gravity.CENTER, 0, 0);
        }

        return roomMemberList;
    }
}
