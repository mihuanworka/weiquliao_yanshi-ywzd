package com.ydd.yanshi.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.adapter.NewFriendAdapter;
import com.ydd.yanshi.bean.AddAttentionResult;
import com.ydd.yanshi.bean.AttentionUser;
import com.ydd.yanshi.bean.Friend;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.bean.message.NewFriendMessage;
import com.ydd.yanshi.bean.message.XmppMessage;
import com.ydd.yanshi.broadcast.CardcastUiUpdateUtil;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.db.dao.ChatMessageDao;
import com.ydd.yanshi.db.dao.FriendDao;
import com.ydd.yanshi.db.dao.NewFriendDao;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.helper.FriendHelper;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.base.CoreManager;
import com.ydd.yanshi.util.Constants;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.xmpp.ListenerManager;
import com.ydd.yanshi.xmpp.listener.ChatMessageListener;
import com.ydd.yanshi.xmpp.listener.NewFriendListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;

public class NewFriendActivity extends BaseActivity implements NewFriendListener {
    private PullToRefreshListView mPullToRefreshListView;
    private NewFriendAdapter mAdapter;
    private List<NewFriendMessage> mNewFriends;
    private String mLoginUserId;
    private Handler mHandler = new Handler();
    private NewFriendAdapter.NewFriendActionListener mNewFriendActionListener = new NewFriendAdapter.NewFriendActionListener() {

        @Override
        public void addAttention(int position) {
            doAgreeOrAttention(position, 0);
        }

        @Override
        public void removeBalckList(int position) {
            removeBlacklist(position);
        }

        @Override
        public void agree(int position) {
            doAgreeOrAttention(position, 1);
        }

        @Override
        public void feedback(int position) {
            doFeedbackOrSayHello(position, 1);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_pullrefresh_list);
        mLoginUserId = coreManager.getSelf().getUserId();
        mNewFriends = new ArrayList<>();
        initActionBar();
        initView();
        ListenerManager.getInstance().addNewFriendListener(this);

        FriendDao.getInstance().markUserMessageRead(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
        // ??????????????????????????????
        NewFriendDao.getInstance().markNewFriendRead(mLoginUserId);

        NewFriendDao.getInstance().resetAllNewFriendUnRead(mLoginUserId);
        PreferenceUtils.putInt(MyApplication.getInstance(), Constants.NEW_CONTACTS_NUMBER + CoreManager.requireSelf(this).getUserId(), 0);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JXNewFriendVC_NewFirend"));
    }

    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
    }

    private void initView() {
        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
        mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
        mAdapter = new NewFriendAdapter(this, coreManager.getSelf().getUserId(), mNewFriends, mNewFriendActionListener);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                loadData();
            }
        });

        mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NewFriendMessage mNewFriendMessage = mNewFriends.get(position - 1);
                goTalkHistoryActivity(mNewFriendMessage);
            }
        });
    }

    private void goTalkHistoryActivity(NewFriendMessage mNewFriendMessage) {
        Intent intent = new Intent(this, TalkHistoryActivity.class);
        intent.putExtra(AppConstant.EXTRA_FRIEND, mNewFriendMessage.getUserId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * ??????????????????
     * <p>
     * ????????????????????????????????????
     */
    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                final List<NewFriendMessage> friends = NewFriendDao.getInstance().getAllNewFriendMsg(mLoginUserId);
                // ????????????200ms???????????????
                long delayTime = 200 - (startTime - System.currentTimeMillis());
                if (delayTime < 0) {
                    delayTime = 0;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mNewFriends.clear();
                        if (friends != null && friends.size() > 0) {
                            mNewFriends.addAll(friends);
                        }
                        mAdapter.notifyDataSetChanged();
                        mPullToRefreshListView.onRefreshComplete();
                    }
                }, delayTime);
            }
        }).start();

    }

    /**
     * ???????????????????????????????????????
     *
     * @param position 1??????????????????
     */
    private void doAgreeOrAttention(final int position, final int type) {
        final NewFriendMessage friend = mNewFriends.get(position);
        DialogHelper.showDefaulteMessageProgressDialog(this);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        HttpUtils.get().url(coreManager.getConfig().ADD_FRIENDS)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {
                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        DialogHelper.dismissProgressDialog();
                        int toastResId = type == 0 ? R.string.add_friend_succ : R.string.agreed;
                        ToastUtil.showToast(NewFriendActivity.this, toastResId);

                        // int messageType = type == 0 ? XmppMessage.TYPE_FRIEND : XmppMessage.TYPE_PASS;
                        int messageType = XmppMessage.TYPE_PASS;
                        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                                messageType, null, friend);
                        coreManager.sendNewFriendMessage(friend.getUserId(), message);
                        NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                        FriendHelper.addFriendExtraOperation(mLoginUserId, friend.getUserId());

                        mNewFriends.set(position, message);
                        mAdapter.notifyDataSetChanged();

                        NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_12);
                        ListenerManager.getInstance().notifyNewFriend(mLoginUserId, friend, true);
                        // ???????????????
                        CardcastUiUpdateUtil.broadcastUpdateUi(NewFriendActivity.this);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(NewFriendActivity.this);
                    }
                });
    }

    /**
     * @param position
     * @param type     0?????????<br/>
     *                 1??????<br/>
     */
    public void doFeedbackOrSayHello(final int position, final int type) {
        int titleResId = type == 0 ? R.string.say_hello_dialog_title : R.string.feedback;
        String hint;
        if (type == 0) {
            hint = getString(R.string.say_hello_dialog_hint);
        } else {
            hint = InternationalizationHelper.getString("JX_Talk");
        }
        DialogHelper.showLimitSingleInputDialog(this, getString(titleResId), hint, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = ((EditText) v).getText().toString().trim();
                doFeedbackOrSayHello(position, type, text);
            }
        });
    }

    public void doFeedbackOrSayHello(int position, int type, String text) {
        if (TextUtils.isEmpty(text)) {
            text = getString(R.string.say_hello_default);
        }
        NewFriendMessage friend = mNewFriends.get(position);
        int messageType = type == 0 ? XmppMessage.TYPE_SAYHELLO : XmppMessage.TYPE_FEEDBACK;
        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(), messageType, text, friend);
        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
        if (friend.getState() == Friend.STATUS_11 || friend.getState() == Friend.STATUS_15) {
            NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_15);
        } else {
            NewFriendDao.getInstance().changeNewFriendState(friend.getUserId(), Friend.STATUS_14);
        }
        NewFriendDao.getInstance().updateNewFriendContent(friend.getUserId(), text);

        coreManager.sendNewFriendMessage(friend.getUserId(), message);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setType(XmppMessage.TYPE_TEXT); //????????????
        chatMessage.setFromUserId(mLoginUserId);
        chatMessage.setFromUserName(coreManager.getSelf().getNickName());
        chatMessage.setToUserId(friend.getUserId());
        chatMessage.setContent(text);
        chatMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        chatMessage.setMySend(true);
        chatMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        chatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        ChatMessageDao.getInstance().saveNewSingleAnswerMessage(mLoginUserId, friend.getUserId(), chatMessage);
/*
        ListenerManager.getInstance().notifyNewFriend(mLoginUserId, friend, true);
*/
        ToastUtil.showToast(this, R.string.feedback_succ);
        loadData();
        //?????????????????????
        mAdapter.notifyDataSetChanged();
/*
        // ?????????????????????UI
        EventBus.getDefault().post(new MessageEventHongdian(123));
*/
    }

    private void removeBlacklist(final int position) {
        final NewFriendMessage friend = mNewFriends.get(position);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<AttentionUser>(AttentionUser.class) {

                    @Override
                    public void onResponse(ObjectResult<AttentionUser> result) {
                        DialogHelper.dismissProgressDialog();
                        int currentStatus = Friend.STATUS_UNKNOW;
                        if (result.getData() != null) {
                            currentStatus = result.getData().getStatus();
                        }
                        FriendDao.getInstance().updateFriendStatus(friend.getOwnerId(), friend.getUserId(), currentStatus);

                        NewFriendMessage message = null;
                        switch (currentStatus) {
                            case Friend.STATUS_ATTENTION:
                                message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(), XmppMessage.TYPE_NEWSEE,
                                        null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);
                                FriendHelper.addAttentionExtraOperation(friend.getOwnerId(), friend.getUserId());
                                break;
                            case Friend.STATUS_FRIEND:
                                message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(), XmppMessage.TYPE_FRIEND,
                                        null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);
                                FriendHelper.addFriendExtraOperation(friend.getOwnerId(), friend.getUserId());
/*
                                EventBus.getDefault().post(new MessageEventHongdian(123));
*/
                                break;
                            default:// ???????????????????????????
                                break;
                        }
                        ToastUtil.showToast(NewFriendActivity.this, R.string.remove_blacklist_succ);
                        mNewFriends.set(position, message);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(NewFriendActivity.this);
                    }
                });
    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        loadData();
        return true;
    }
}
