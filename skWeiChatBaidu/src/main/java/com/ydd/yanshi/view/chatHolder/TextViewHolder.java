package com.ydd.yanshi.view.chatHolder;

import android.view.View;
import android.widget.TextView;

import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.util.Constants;
import com.ydd.yanshi.util.DateFormatUtil;
import com.ydd.yanshi.util.HtmlUtils;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.StringUtils;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.link.HttpTextView;

public class TextViewHolder extends AChatHolderInterface {

    public HttpTextView mTvContent;
    public TextView tvFireTime;
    public TextView tvTime;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_text : R.layout.chat_to_item_text;
    }

    @Override
    public void initView(View view) {
        mTvContent = view.findViewById(R.id.chat_text);
        tvTime = view.findViewById(R.id.send_time);
        mRootView = view.findViewById(R.id.chat_warp_view);
        if (!isMysend) {
            tvFireTime = view.findViewById(R.id.tv_fire_time);
        }
    }

    @Override
    public void fillData(ChatMessage message) {
        // 修改字体功能
        int size = PreferenceUtils.getInt(mContext, Constants.FONT_SIZE) + 14;
        mTvContent.setTextSize(size);
        mTvContent.setTextColor(mContext.getResources().getColor(R.color.black));
        String strTime = TimeUtils.sk_time_long_to_chat_time_str(message.getReadTime());
        String strTime2 = TimeUtils.getTimeMMdd(message.getReadTime());
        String strTime3 = TimeUtils.long_to_yMdHm_str(message.getReadTime());
        tvTime.setText(strTime);

        String content = StringUtils.replaceSpecialChar(message.getContent());
        CharSequence charSequence = HtmlUtils.transform200SpanString(content, true);
        if (message.getIsReadDel() && !isMysend) {// 阅后即焚
            if (!message.isGroup() && !message.isSendRead()) {
                mTvContent.setText(R.string.tip_click_to_read);
                mTvContent.setTextColor(mContext.getResources().getColor(R.color.redpacket_bg));
            } else {
                // 已经查看了，当适配器再次刷新的时候，不需要重新赋值
                mTvContent.setText(charSequence);
            }
        } else {
            mTvContent.setText(charSequence);
        }

        mTvContent.setUrlText(mTvContent.getText());

        mTvContent.setOnClickListener(v -> mHolderListener.onItemClick(mRootView, TextViewHolder.this, mdata));
        mTvContent.setOnLongClickListener(v -> {
            mHolderListener.onItemLongClick(v, TextViewHolder.this, mdata);
            return true;
        });
    }

    @Override
    protected void onRootClick(View v) {

    }

    @Override
    public boolean enableFire() {
        return true;
    }

    @Override
    public boolean enableSendRead() {
        return true;
    }

    public void showFireTime(boolean show) {
        if (tvFireTime != null) {
            tvFireTime.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
