package com.ydd.yanshi.xmpp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.ydd.yanshi.AppConfig;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.BuildConfig;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.Reporter;
import com.ydd.yanshi.audio.NoticeVoicePlayer;
import com.ydd.yanshi.bean.Friend;
import com.ydd.yanshi.bean.MsgRoamTask;
import com.ydd.yanshi.bean.SyncBean;
import com.ydd.yanshi.bean.User;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.bean.message.LastChatHistoryList;
import com.ydd.yanshi.bean.message.NewFriendMessage;
import com.ydd.yanshi.bean.message.XmppMessage;
import com.ydd.yanshi.broadcast.MsgBroadcast;
import com.ydd.yanshi.db.dao.ChatMessageDao;
import com.ydd.yanshi.db.dao.FriendDao;
import com.ydd.yanshi.db.dao.MsgRoamTaskDao;
import com.ydd.yanshi.db.dao.login.MachineDao;
import com.ydd.yanshi.db.dao.login.TimerListener;
import com.ydd.yanshi.helper.PrivacySettingHelper;
import com.ydd.yanshi.ui.MainActivity;
import com.ydd.yanshi.ui.base.CoreManager;
import com.ydd.yanshi.ui.message.ChatActivity;
import com.ydd.yanshi.ui.message.HandleSyncMoreLogin;
import com.ydd.yanshi.ui.message.MucChatActivity;
import com.ydd.yanshi.util.AppUtils;
import com.ydd.yanshi.util.Constants;
import com.ydd.yanshi.util.DES;
import com.ydd.yanshi.util.HttpUtil;
import com.ydd.yanshi.util.Md5Util;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.xmpp.ReceiptManager.SendType;
import com.ydd.yanshi.xmpp.listener.AuthStateListener;
import com.ydd.yanshi.xmpp.listener.ChatMessageListener;

import org.jivesoftware.smack.XMPPConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.Call;

public class CoreService extends Service implements TimerListener {
    static final boolean DEBUG = true;
    static final String TAG = "XmppCoreService";

    private static final Intent SERVICE_INTENT = new Intent();
    private static final String EXTRA_LOGIN_USER_ID = "login_user_id";
    private static final String EXTRA_LOGIN_PASSWORD = "login_password";
    private static final String EXTRA_LOGIN_NICK_NAME = "login_nick_name";

    private static final String MESSAGE_CHANNEL_ID = "message";

    static {
        SERVICE_INTENT.setComponent(new ComponentName(BuildConfig.APPLICATION_ID, CoreService.class.getName()));
    }

    /*
    ?????? ?????? ??????
     */
    ReadBroadcastReceiver receiver = new ReadBroadcastReceiver();
    private boolean isInit;
    private CoreServiceBinder mBinder;
    /* ????????????????????????????????? */
    private String mLoginUserId;
    @SuppressWarnings("unused")
    private String mLoginNickName;
    private String mLoginPassword;
    private XmppConnectionManager mConnectionManager;// ??????
    private XChatManager mXChatManager;// ??????
    private XMucChatManager mXMucChatManager;// ??????
    private ReceiptManager mReceiptManager;// ??????
    private ReceiptManagerNew mReceiptManagerNew;// ??????
    private NotifyConnectionListener mNotifyConnectionListener = new NotifyConnectionListener() {
        @Override
        public void notifyConnecting() {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ING);
        }

        @Override
        public void notifyConnected(XMPPConnection arg0) {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_ING);
        }

        @Override
        public void notifyAuthenticated(XMPPConnection arg0) {
            onAuthenticated();
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_SUCCESS);// ??????????????????
            authenticatedOperating();
        }

        @Override
        public void notifyConnectionClosedOnError(Exception arg0) {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_NOT);
        }

        @Override
        public void notifyConnectionClosed() {
            ListenerManager.getInstance().notifyAuthStateChange(AuthStateListener.AUTH_STATE_NOT);
        }
    };
    /**
     * ?????? ?????? ?????? ??? ?????????
     */
    private int notifyId = 1003020303;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    public static Intent getIntent() {
        return SERVICE_INTENT;
    }

    // ??????ContextCompat.startForegroundService?????????????????????8.0?????????????????????????????????????????????????????????
    public static Intent getIntent(Context context, String userId, String password, String nickName) {
        Intent intent = new Intent(context, CoreService.class);
        intent.putExtra(EXTRA_LOGIN_USER_ID, userId);
        intent.putExtra(EXTRA_LOGIN_PASSWORD, password);
        intent.putExtra(EXTRA_LOGIN_NICK_NAME, nickName);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new CoreServiceBinder();
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService OnCreate :" + android.os.Process.myPid());
        }
        register(); // ???????????????????????????????????????
    }

    @Override
    public IBinder onBind(Intent intent) {
        // ?????????????????????????????????????????????????????????
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onBind");
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onDestroy");
        }
        release();

        if (receiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (CoreService.DEBUG) {
            Log.e(CoreService.TAG, "CoreService onStartCommand");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationBuilder();
            startForeground(1, mBuilder.build());
            stopForeground(true);
        }

        init();

        // return START_NOT_STICKY;
        return START_STICKY;
    }

    private void init() {
        if (isInit) {
            Log.e("zq", "isInit==true,????????????");
            login(mLoginUserId, mLoginPassword);
            return;
        }
        isInit = true;
        User self = CoreManager.requireSelf(this);
        mLoginUserId = self.getUserId();
        mLoginPassword = self.getPassword();
        mLoginNickName = self.getNickName();

        if (Constants.IS_CLOSED_ON_ERROR_END_DOCUMENT && mConnectionManager != null) {
            Log.e("zq", "CLOSED_ON_ERROR_END_DOCUMENT--->??????release??????");
            Constants.IS_CLOSED_ON_ERROR_END_DOCUMENT = false;
            release();
        }

        if (mConnectionManager == null) {
            initConnection();
        }
    }

    public void login(String userId, String password) {
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(password)) {
            mConnectionManager.login(userId, password);
        }
    }

    public void initConnection() {
        mConnectionManager = new XmppConnectionManager(this, mNotifyConnectionListener);
    }

    private void release() {
        if (mConnectionManager != null) {
            mConnectionManager.release();
            mConnectionManager = null;
        }

        mReceiptManager = null;
        mXChatManager = null;
        mXMucChatManager = null;
    }

    private void onAuthenticated() {
        if (!isAuthenticated()) {
            return;
        }

        /* ?????????????????? */
        if (mReceiptManager == null) {
            mReceiptManager = new ReceiptManager(this, mConnectionManager.getConnection());
        } else {
            mReceiptManager.reset();
        }
        if (mReceiptManagerNew != null) {
            mReceiptManagerNew.release();
            mReceiptManagerNew = null;
        }
        mReceiptManagerNew = new ReceiptManagerNew(this, mConnectionManager.getConnection());

        // ?????????????????????
        if (mXChatManager == null) {
            mXChatManager = new XChatManager(this, mConnectionManager.getConnection());
        } else {
            mXChatManager.reset();
        }

        if (mXMucChatManager == null) {
            mXMucChatManager = new XMucChatManager(this, mConnectionManager.getConnection());
        } else {
            mXMucChatManager.reset();
        }

        /*  ???????????? */
        mConnectionManager.sendOnLineMessage();
    }

    /**
     * ??????XmppConnectionManager??????
     */
    public XmppConnectionManager getmConnectionManager() {
        return mConnectionManager;
    }

    public boolean isAuthenticated() {
        if (mConnectionManager != null && mConnectionManager.isAuthenticated()) {
            return true;
        }
        return false;
    }

    public void logout() {
        isInit = false;
        if (CoreService.DEBUG)
            Log.e(CoreService.TAG, "Xmpp??????");
        if (mConnectionManager != null) {
            mConnectionManager.logout();
        }
        stopSelf();
    }

    public void logoutWithOutStopSelf() {
        if (CoreService.DEBUG)
            Log.e(CoreService.TAG, "Xmpp????????????????????????");
        if (mConnectionManager != null) {
            mConnectionManager.logout();
        }
    }

    public void sendReceipt(String messageId) {
        if (TextUtils.isEmpty(messageId)) {
            return;
        }
        if (mReceiptManagerNew != null) {
            mReceiptManagerNew.sendReceipt(messageId);
        } else {
            Reporter.post("???????????????????????????????????????");
        }
    }

    /**
     * ??????????????????
     */
    public void sendChatMessage(String toUserId, ChatMessage chatMessage) {
        if (mXChatManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mXChatManager==null");
        }

        if (mReceiptManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mReceiptManager==null");
        }

        if (!isAuthenticated()) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "isAuthenticated==false");
        }

        if (mXChatManager == null || mReceiptManager == null
                || (!isAuthenticated() && !HttpUtil.isGprsOrWifiConnected(MyApplication.getContext()))) {
            // ??????!isAuthenticated()??????????????????????????????????????????????????????????????????
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(),
                    ChatMessageListener.MESSAGE_SEND_FAILED);// ??????????????????????????? ?????????????????????
        } else {
            /**
             * ??????????????????????????????????????????
             * ???????????????????????????
             */
            mReceiptManager.addWillSendMessage(toUserId, chatMessage, SendType.NORMAL, chatMessage.getContent());
            mXChatManager.sendMessage(toUserId, chatMessage);
        }
    }

    /**
     * ????????????????????????
     */
    public void sendNewFriendMessage(String toUserId, NewFriendMessage message) {
        if (mXChatManager == null || mReceiptManager == null || !isAuthenticated()) {
            ListenerManager.getInstance().notifyNewFriendSendStateChange(toUserId, message, ChatMessageListener.MESSAGE_SEND_FAILED);
        } else {
            Log.e(CoreService.TAG, "CoreService???" + toUserId);
            mReceiptManager.addWillSendMessage(toUserId, message, SendType.PUSH_NEW_FRIEND, message.getContent());
            mXChatManager.sendMessage(toUserId, message);
        }
    }

    public void sendMucChatMessage(String toUserId, ChatMessage chatMessage) {
        if (mXMucChatManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mXMucChatManager==null");
        }

        if (mReceiptManager == null) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "mReceiptManager==null");
        }

        if (!isAuthenticated()) {
            if (CoreService.DEBUG)
                Log.e(CoreService.TAG, "isAuthenticated==false");
        }

        if (mXMucChatManager == null || mReceiptManager == null
                || (!isAuthenticated() && !HttpUtil.isGprsOrWifiConnected(MyApplication.getContext()))) {
            // ??????!isAuthenticated()??????????????????????????????????????????????????????????????????
            ListenerManager.getInstance().notifyMessageSendStateChange(mLoginUserId, toUserId, chatMessage.getPacketId(),
                    ChatMessageListener.MESSAGE_SEND_FAILED);
        } else {
            mReceiptManager.addWillSendMessage(toUserId, chatMessage, SendType.NORMAL, chatMessage.getContent());
            mXMucChatManager.sendMessage(toUserId, chatMessage);
        }
    }

    /* ????????????????????? */
    public boolean isMucEnable() {
        return isAuthenticated() && mXMucChatManager != null;
    }

    public void joinExistGroup() {
        if (isAuthenticated()) {
            if (mXMucChatManager == null) {
                mXMucChatManager = new XMucChatManager(this, mConnectionManager.getConnection());
                mXMucChatManager.joinExistGroup();
            } else {
                mXMucChatManager.joinExistGroup();
            }
        }
    }

    /* ???????????? */
    public String createMucRoom(String roomName) {
        if (isMucEnable()) {
            return mXMucChatManager.createMucRoom(roomName);
        }
        return null;
    }

    /* ???????????? */
    public void joinMucChat(String toUserId, long lastSeconds) {
        if (isMucEnable()) {
            mXMucChatManager.joinMucChat(toUserId, lastSeconds);
        }
    }

    /* ???????????? */
    public void exitMucChat(String toUserId) {
        if (isMucEnable()) {
            mXMucChatManager.exitMucChat(toUserId);
        }
    }

    /********************
     *  ????????????
     *********************/
    /*
    XMPP???????????????????????????
    */
    public void authenticatedOperating() {
        Log.e("zq", "?????????????????????????????????");

        if (MyApplication.IS_SUPPORT_MULTI_LOGIN) {
            Log.e("TAG", "?????????????????????Type 200 ??????");
            loadMachineList();
        }

        new Thread(() -> {
            // ??????????????????????????????
            List<Friend> nearlyFriendMsg = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
            for (int i = 0; i < nearlyFriendMsg.size(); i++) {
                if (nearlyFriendMsg.get(i).getRoomFlag() == 0) {// ???????????????
                    ChatMessageDao.getInstance().deleteOutTimeChatMessage(mLoginUserId, nearlyFriendMsg.get(i).getUserId());
                } else {// ??????????????????
                    ChatMessageDao.getInstance().updateExpiredStatus(mLoginUserId, nearlyFriendMsg.get(i).getUserId());
                }
            }
        }).start();

        // ????????????????????????????????? || ???????????????????????????????????????(???????????????????????????????????????????????? ???????????????????????????100????????????????????????)
        getLastChatHistory();
        getInterfaceTransferInOfflineTime();
    }

    public void getInterfaceTransferInOfflineTime() {
        long syncTimeLen = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);

        Map<String, String> params = new HashMap();
        params.put("access_token", CoreManager.requireSelfStatus(this).accessToken);
        params.put("offlineTime", String.valueOf(syncTimeLen));

        HttpUtils.get().url(CoreManager.requireConfig(this).USER_OFFLINE_OPERATION)
                .params(params)
                .build()
                .execute(new ListCallback<SyncBean>(SyncBean.class) {
                    @Override
                    public void onResponse(ArrayResult<SyncBean> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            List<SyncBean> syncBeans = result.getData();
                            for (int i = 0; i < syncBeans.size(); i++) {
                                HandleSyncMoreLogin.distributionService(syncBeans.get(i), CoreService.this);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }
                });
    }

    public void getLastChatHistory() {
        Map<String, String> params = new HashMap();
        params.put("access_token", CoreManager.requireSelfStatus(this).accessToken);

        long syncTimeLen;
        if (Constants.OFFLINE_TIME_IS_FROM_SERVICE) {// ?????????????????????????????? ????????????????????????
            Constants.OFFLINE_TIME_IS_FROM_SERVICE = false;
            String chatSyncTimeLen = String.valueOf(PrivacySettingHelper.getPrivacySettings(this).getChatSyncTimeLen());
            Double realSyncTime = Double.parseDouble(chatSyncTimeLen);
            if (realSyncTime == -2) {// ?????????
                joinExistGroup();
                return;
            } else if (realSyncTime == -1 || realSyncTime == 0) {// ?????? ?????? syncTime == 0
                syncTimeLen = 0;
            } else {
                syncTimeLen = (long) (realSyncTime * 24 * 60 * 60);// ????????????????????????
            }
        } else {// syncTime???????????????????????????????????????
            syncTimeLen = PreferenceUtils.getLong(MyApplication.getContext(), Constants.OFFLINE_TIME + mLoginUserId, 0);
        }
        params.put("startTime", String.valueOf(syncTimeLen));

        HttpUtils.get().url(CoreManager.requireConfig(this).GET_LAST_CHAT_LIST)
                .params(params)
                .build()
                .execute(new ListCallback<LastChatHistoryList>(LastChatHistoryList.class) {
                    @Override
                    public void onResponse(ArrayResult<LastChatHistoryList> result) {
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            final List<LastChatHistoryList> data = result.getData();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 0; i < data.size(); i++) {
                                        LastChatHistoryList mLastChatHistoryList = data.get(i);

                                        if (mLastChatHistoryList.getIsRoom() == 1) {// ????????????
                                            // ?????????????????????????????????
                                            ChatMessage mLocalLastMessage = ChatMessageDao.getInstance().getLastChatMessage(mLoginUserId, mLastChatHistoryList.getJid());
                                            if (mLocalLastMessage == null
                                                    || mLocalLastMessage.getPacketId().equals(mLastChatHistoryList.getMessageId())) {
                                                // ????????????????????????(??????????????????)
                                                // || ?????????????????????msgId==?????????????????????????????????????????????msgId(????????????????????????????????? ?????????????????????????????????????????????????????????) ?????????????????????
                                            } else {
                                                // ????????????????????????????????????????????????
                                                MsgRoamTask mMsgRoamTask = new MsgRoamTask();
                                                mMsgRoamTask.setTaskId(System.currentTimeMillis());
                                                mMsgRoamTask.setOwnerId(mLoginUserId);
                                                mMsgRoamTask.setUserId(mLastChatHistoryList.getJid());
                                                mMsgRoamTask.setStartTime(mLocalLastMessage.getTimeSend());
                                                mMsgRoamTask.setStartMsgId(mLocalLastMessage.getPacketId());
                                                MsgRoamTaskDao.getInstance().createMsgRoamTask(mMsgRoamTask);
                                            }
                                        }
                                        // ??????????????????????????????????????????
                                        String str = "";
                                        if (mLastChatHistoryList.getIsEncrypt() == 1) {// ????????????
                                            if (!TextUtils.isEmpty(mLastChatHistoryList.getContent())) {
                                                String content = mLastChatHistoryList.getContent().replaceAll("\n", "");
                                                String decryptKey = Md5Util.toMD5(AppConfig.apiKey + mLastChatHistoryList.getTimeSend() + mLastChatHistoryList.getMessageId());
                                                try {
                                                    str = DES.decryptDES(content, decryptKey);
                                                } catch (Exception e) {
                                                    str = mLastChatHistoryList.getContent();
                                                    e.printStackTrace();
                                                }
                                            }
                                        } else {
                                            str = mLastChatHistoryList.getContent();
                                        }

                                        FriendDao.getInstance().updateApartDownloadTime(mLastChatHistoryList.getUserId(), mLastChatHistoryList.getJid(),
                                                str, mLastChatHistoryList.getType(), mLastChatHistoryList.getTimeSend(),
                                                mLastChatHistoryList.getIsRoom(), mLastChatHistoryList.getFrom(), mLastChatHistoryList.getFromUserName(),
                                                mLastChatHistoryList.getToUserName());
                                    }
                                    MsgBroadcast.broadcastMsgUiUpdate(MyApplication.getContext());
                                    // ????????????????????????????????????XMPP???????????? ????????????????????????
                                    joinExistGroup();
                                }
                            }).start();
                        } else {// ??????????????????????????????XMPP????????????
                            joinExistGroup();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        // ??????
                        joinExistGroup();
                    }
                });
    }

    /*
    ??????????????????
     */
    // ???????????????????????????????????????
    @WorkerThread
    public void notificationMessage(ChatMessage chatMessage, boolean isGroupChat) {
        boolean isAppForeground = AppUtils.isAppForeground(this);
        Log.e(TAG, "notificationMessage() called with: chatMessage = [" + chatMessage.getContent() + "]," +
                " isGroupChat = [" + isGroupChat + "]," +
                " isAppForeground = [" + isAppForeground + "]," +
                " messageType = [ " +chatMessage.getType() + "]");

        if (isAppForeground) {// ????????? ?????????
            return;
        }

        int messageType = chatMessage.getType();
        String title;
        String content;
        boolean isSpecialMsg = false;// ???????????? ?????????????????? ??????????????????

        switch (messageType) {
            case XmppMessage.TYPE_REPLAY:
            case XmppMessage.TYPE_TEXT:
                if (chatMessage.getIsReadDel()) {
                    content = getString(R.string.tip_click_to_read);
                } else {
                    content = chatMessage.getContent();
                }
                break;
            case XmppMessage.TYPE_VOICE:
                content = getString(R.string.msg_voice);
                break;
            case XmppMessage.TYPE_GIF:
                content = getString(R.string.msg_animation);
                break;
            case XmppMessage.TYPE_IMAGE:
                content = getString(R.string.msg_picture);
                break;
            case XmppMessage.TYPE_VIDEO:
                content = getString(R.string.msg_video);
                break;
            case XmppMessage.TYPE_RED:
                content = getString(R.string.msg_red_packet);
                break;
            case XmppMessage.TYPE_LOCATION:
                content = getString(R.string.msg_location);
                break;
            case XmppMessage.TYPE_CARD:
                content = getString(R.string.msg_card);
                break;
            case XmppMessage.TYPE_FILE:
                content = getString(R.string.msg_file);
                break;
            case XmppMessage.TYPE_TIP:
                content = getString(R.string.msg_system);
                break;
            case XmppMessage.TYPE_IMAGE_TEXT:
            case XmppMessage.TYPE_IMAGE_TEXT_MANY:
                content = getString(R.string.msg_image_text);
                break;
            case XmppMessage.TYPE_LINK:
            case XmppMessage.TYPE_SHARE_LINK:
                content = getString(R.string.msg_link);
                break;
            case XmppMessage.TYPE_SHAKE:
                content = getString(R.string.msg_shake);
                break;
            case XmppMessage.TYPE_CHAT_HISTORY:
                content = getString(R.string.msg_chat_history);
                break;
            case XmppMessage.TYPE_TRANSFER:
                content = getString(R.string.tip_transfer_money);
                break;
            case XmppMessage.TYPE_TRANSFER_RECEIVE:
                content = getString(R.string.tip_transfer_money) + getString(R.string.transfer_friend_sure_save);
                break;
            case XmppMessage.TYPE_TRANSFER_BACK:
                content = getString(R.string.transfer_back);
                break;
            case XmppMessage.TYPE_PAY_CERTIFICATE:
                content = getString(R.string.pay_certificate);
                break;

            case XmppMessage.TYPE_IS_CONNECT_VOICE:
                content = getString(R.string.suffix_invite_you_voice);
                ringPlay();
                break;
            case XmppMessage.TYPE_IS_CONNECT_VIDEO:
                content = getString(R.string.suffix_invite_you_video);
                ringPlay();
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VOICE:
                content = getString(R.string.suffix_invite_you_voice_meeting);
                ringPlay();
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VIDEO:
                content = getString(R.string.suffix_invite_you_video_meeting);
                ringPlay();
                break;
            case XmppMessage.TYPE_IS_MU_CONNECT_TALK:
                content = getString(R.string.suffix_invite_you_talk);
                break;

            case XmppMessage.TYPE_SAYHELLO:// ?????????
                isSpecialMsg = true;
                content = getString(R.string.apply_to_add_me_as_a_friend);
                break;
            case XmppMessage.TYPE_PASS:    // ???????????????
                isSpecialMsg = true;
                content = getString(R.string.agree_with_my_plus_friend_request);
                break;
            case XmppMessage.TYPE_FRIEND:  // ??????????????????
                isSpecialMsg = true;
                content = getString(R.string.added_me_as_a_friend);
                break;

            case XmppMessage.DIANZAN:// ???????????????
                isSpecialMsg = true;
                content = getString(R.string.notification_praise_me_life_circle);
                break;
            case XmppMessage.PINGLUN:    // ???????????????
                isSpecialMsg = true;
                content = getString(R.string.notification_comment_me_life_circle);
                break;
            case XmppMessage.ATMESEE:  // ?????????????????????
                isSpecialMsg = true;
                content = getString(R.string.notification_at_me_life_circle);
                break;

            default:// ???????????????????????????
                ringStop();
                return;
        }

        createNotificationBuilder();

        String id;
        PendingIntent pendingIntent;
        if (isSpecialMsg) {
            title = chatMessage.getFromUserName();
            content = chatMessage.getFromUserName() + content;
            pendingIntent = pendingIntentForSpecial();
        } else {
            if (isGroupChat) {
                id = chatMessage.getToUserId();
                content = chatMessage.getFromUserName() + "???" + content;// ??????????????????????????????????????????????????????
            } else {
                id = chatMessage.getFromUserId();
            }

            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, id);
            if (friend != null) {
                title = TextUtils.isEmpty(friend.getRemarkName()) ? friend.getNickName() : friend.getRemarkName();
            } else {
                title = chatMessage.getFromUserName();
            }

            if (isGroupChat) {
                pendingIntent = pendingIntentForMuc(friend);
            } else {
                pendingIntent = pendingIntentForSingle(friend);
            }

        }

        if (pendingIntent == null)
            return;
//        notifycationAndroid26(chatMessage,pendingIntent,title,content);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setContentTitle(title) // ????????????
                .setContentText(content)  // ????????????
                .setTicker(getString(R.string.tip_new_message))
                .setWhen(System.currentTimeMillis()) // ????????????
                .setPriority(Notification.PRIORITY_HIGH) // ???????????????
                .setAutoCancel(true)// ???????????????????????????????????????????????????
                .setOngoing(false)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.mipmap.logo_1); // ??????icon
        Notification n = mBuilder.build();
        int numMessage = FriendDao.getInstance().getMsgUnReadNumTotal(mLoginUserId);
        // ????????????????????????????????????????????????????????????????????????????????????1???
        ShortcutBadger.applyNotification(getApplicationContext(), n, numMessage + 1);
        mNotificationManager.notify(chatMessage.getFromUserId(), notifyId, n);
        if (isSpecialMsg) {// ????????????????????????
            NoticeVoicePlayer.getInstance().start();
        }
    }
    MediaPlayer mediaPlayer;
    private void ringPlay() {
        Log.e("TAG_notificationMege1","???????????????");
        if (mediaPlayer == null){
            mediaPlayer = MediaPlayer.create(this, R.raw.call);
//            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

    }

    private void ringStop() {
        Log.e("TAG_notificationMessage","????????????");
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();//????????????
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    private void createNotificationBuilder() {
        // ??????????????????????????????????????????????????????????????????
        if (mNotificationManager == null) {
            synchronized (this) {
                if (mNotificationManager == null) {
                    mNotificationManager = (NotificationManager) getApplicationContext()
                            .getSystemService(NOTIFICATION_SERVICE);
                }
            }
        }
        if (mBuilder == null) {
            synchronized (this) {
                if (mBuilder == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                MESSAGE_CHANNEL_ID,
                                getString(R.string.message_channel_name),
                                NotificationManager.IMPORTANCE_DEFAULT);
                        // ?????????????????????????????????????????????
                        channel.setSound(null, null);
                        mNotificationManager.createNotificationChannel(channel);
                        mBuilder = new NotificationCompat.Builder(this, channel.getId());
                    } else {
                        //noinspection deprecation
                        mBuilder = new NotificationCompat.Builder(this);
                    }
                }
            }
        }
    }

    /**
     * <????????????????????????>
     */
    public PendingIntent pendingIntentForSingle(Friend friend) {
        Intent intent;
        if (friend != null) {
            intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_FRIEND, friend);
        } else {
            intent = new Intent(getApplicationContext(), MainActivity.class);
        }
        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * <????????????????????????>
     */
    public PendingIntent pendingIntentForMuc(Friend friend) {
        Intent intent;
        if (friend != null) {
            intent = new Intent(getApplicationContext(), MucChatActivity.class);
            intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
            intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
        } else {
            intent = new Intent(getApplicationContext(), MainActivity.class);
        }
        intent.putExtra(Constants.IS_NOTIFICATION_BAR_COMING, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    /**
     * <???????????????>
     */
    public PendingIntent pendingIntentForSpecial() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.ydd.yanshi.broadcast.OtherBroadcast.Read);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    /*
    ????????????
     */
    // ???????????????
    public void loadMachineList() {
        MachineDao.getInstance().loadMachineList(this);

        MyApplication.IS_SEND_MSG_EVERYONE = true;
        sendOnLineMessage();
    }

    @Override
    public void onFinish(String machineName) {
        Log.e(TAG, machineName + "???????????????????????????" + machineName + "??????????????? ");
        if (MachineDao.getInstance().getMachineSendReceiptStatus(machineName)) {
            sendOnLineMessage();
            // ???????????????????????????????????????????????????????????????false
            MachineDao.getInstance().updateMachineSendReceiptStatus(machineName, false);
        } else {// ??????machine????????????????????????????????? || ?????????????????????????????????????????????????????????????????????????????????????????????false
            Log.e(TAG, "????????????????????????false?????????" + machineName + "????????? ");
            MachineDao.getInstance().updateMachineOnLineStatus(machineName, false);
        }
    }

    // ???????????????????????????
    public void sendOnLineMessage() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_SEND_ONLINE_STATUS);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(mLoginUserId);
        chatMessage.setContent("1");// 0 ?????? 1 ??????

        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(mLoginUserId, chatMessage);
    }

    // ??????????????????
    public void sendOffLineMessage() {
        MyApplication.IS_SEND_MSG_EVERYONE = true;
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_SEND_ONLINE_STATUS);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(mLoginUserId);
        chatMessage.setContent("0");// 0 ?????? 1??????

        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(mLoginUserId, chatMessage);
    }

    // ??????????????????
    public void sendBusyMessage(String toUserId) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_IS_BUSY);

        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(mLoginNickName);
        chatMessage.setToUserId(toUserId);

        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sendChatMessage(toUserId, chatMessage);
    }

    // Binder
    public class CoreServiceBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }

    /*
    ??????????????????
     */
    public class ReadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(com.ydd.yanshi.broadcast.OtherBroadcast.Read)) {
                Bundle bundle = intent.getExtras();
                String packetId = bundle.getString("packetId");
                boolean isGroup = bundle.getBoolean("isGroup");
                String friendId = bundle.getString("friendId");
                String friendName = bundle.getString("fromUserName");

                ChatMessage msg = new ChatMessage();
                msg.setType(XmppMessage.TYPE_READ);
                msg.setFromUserId(mLoginUserId);
                msg.setFromUserName(friendName);
                msg.setToUserId(friendId);
                msg.setContent(packetId);
                // ?????????????????? ??????????????????
                msg.setSendRead(true);
                msg.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
                msg.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                if (isGroup) {
                    sendMucChatMessage(friendId, msg);
                } else {
                    sendChatMessage(friendId, msg);
                }
            }
        }
    }
}
