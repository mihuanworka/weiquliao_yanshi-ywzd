package com.ydd.yanshi.ui.me.redpacket;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.SwipeMenu;
import com.yanzhenjie.recyclerview.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;
import com.ydd.yanshi.R;
import com.ydd.yanshi.adapter.CardsAdapter;
import com.ydd.yanshi.bean.redpacket.CardBean;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.util.DisplayUtil;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.view.DeleteBankDialog;
import com.ydd.yanshi.view.EditBankDialog;

import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;

import static com.ydd.yanshi.AppConstant.ZHI_FU_BAO;
import static com.ydd.yanshi.AppConstant.ZHONG_GUO_GONGSHANG_YINHANG;
import static com.ydd.yanshi.AppConstant.ZHONG_GUO_JIANSHE_YINHANG;
import static com.ydd.yanshi.AppConstant.ZHONG_GUO_JIAOTONG_YINHANG;
import static com.ydd.yanshi.AppConstant.ZHONG_GUO_NONGYE_YINHANG;
import static com.ydd.yanshi.AppConstant.ZHONG_GUO_YINHANG;
import static com.ydd.yanshi.AppConstant.ZHONG_GUO_YOUZHENG_YINHANG;

public class MyCardsActivity extends BaseActivity {

    private TextView tvTip;
    private SwipeRecyclerView mRecyclerView;
    private Button btn_addCard;
    private CardsAdapter mAdapter = null;
    private DeleteBankDialog mDeleteBankDialog = null;
    private EditBankDialog mEditBankDialog = null;
    private ArrayList<CardBean> mCards = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_cards);

        initActionBar();
        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCards();
    }

    private void initData() {
    }

    private void testData() {
        CardBean cardBean = new CardBean();
        cardBean.setBankBrandId(ZHI_FU_BAO);
        mCards.add(cardBean);

        CardBean cardBean2 = new CardBean();
        cardBean2.setBankBrandId(ZHONG_GUO_YINHANG);
        mCards.add(cardBean2);

        CardBean cardBean3 = new CardBean();
        cardBean3.setBankBrandId(ZHONG_GUO_JIANSHE_YINHANG);
        mCards.add(cardBean3);

        CardBean cardBean4 = new CardBean();
        cardBean4.setBankBrandId(ZHONG_GUO_GONGSHANG_YINHANG);
        mCards.add(cardBean4);

        CardBean cardBean5 = new CardBean();
        cardBean5.setBankBrandId(ZHONG_GUO_NONGYE_YINHANG);
        mCards.add(cardBean5);

        CardBean cardBean6 = new CardBean();
        cardBean6.setBankBrandId(ZHONG_GUO_JIAOTONG_YINHANG);
        mCards.add(cardBean6);

        CardBean cardBean7 = new CardBean();
        cardBean7.setBankBrandId(ZHONG_GUO_YOUZHENG_YINHANG);
        mCards.add(cardBean7);
    }

    private void initView() {
        mAdapter = new CardsAdapter(R.layout.item_card, this, mCards);
        btn_addCard = findViewById(R.id.btn_addCard);
        mRecyclerView = findViewById(R.id.rec_cards);
        mDeleteBankDialog = new DeleteBankDialog(this);
        mEditBankDialog = new EditBankDialog(this);

        btn_addCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ???????????? ????????????
                Intent intent = new Intent(MyCardsActivity.this, AddCardsActivity.class);
                startActivity(intent);
            }
        });

        mRecyclerView.setSwipeMenuCreator(swipeMenuCreator);
        mRecyclerView.setOnItemMenuClickListener(mMenuItemClickListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                showEditPop(position);
                return false;
            }
        });
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView mTvTitle = (TextView) findViewById(R.id.tv_title_center);
        mTvTitle.setText("???????????????");
    }

    private void getCards() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        HttpUtils.get().url(coreManager.getConfig().CARDS)
                .params(params)
                .build()
                .execute(new ListCallback<CardBean>(CardBean.class) {
                    @Override
                    public void onResponse(ArrayResult<CardBean> result) {
                        DialogHelper.dismissProgressDialog();
                        mCards.clear();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            mCards.addAll(result.getData());
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    private void showDeletePop(int position){
        mDeleteBankDialog.show(position);
        mDeleteBankDialog.setListenner(new DeleteBankDialog.OnClickListenner() {
            @Override
            public void onSure(int position) {
                deleteCard(position);
            }
        });
    }

    private void showEditPop(int position){
        mEditBankDialog.show(position,mCards.get(position));
        mEditBankDialog.setListenner(new EditBankDialog.OnClickListenner() {
            @Override
            public void onSure(int position) {
                editBank(position);
            }
        });
    }

    private void deleteCard(int position){
        String cardId = mCards.get(0).getId();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("cardId", cardId);
        HttpUtils.post().url(coreManager.getConfig().DELETE_CARD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtil.showToast(MyCardsActivity.this,result.getResultMsg());
                            }
                        });
                        getCards();
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(MyCardsActivity.this);
                    }
                });
    }

    private void editBank(int position){

    }

    /**
     * ?????????????????????Item?????????????????????????????????
     */
    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int position) {
            int width = DisplayUtil.dip2px(MyCardsActivity.this, 80f);
            int height = DisplayUtil.dip2px(MyCardsActivity.this, 80f);

            // 1. MATCH_PARENT ???????????????????????????Item?????????;
            // 2. ???????????????????????????80;
            // 3. WRAP_CONTENT???????????????????????????;
//            int height = ViewGroup.LayoutParams.MATCH_PARENT;


            // ??????????????????????????????????????????????????????????????????
            {
                SwipeMenuItem delete = new SwipeMenuItem(MyCardsActivity.this).setBackgroundColorResource(R.color.redpacket_bg)
                        .setText(R.string.delete)
                        .setTextColor(Color.WHITE)
                        .setTextSize(15)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(delete);// ????????????????????????
            }
        }
    };
    /**
     * RecyclerView???Item???Menu???????????????
     */
    private OnItemMenuClickListener mMenuItemClickListener = new OnItemMenuClickListener() {
        @Override
        public void onItemClick(SwipeMenuBridge menuBridge, int position) {
            menuBridge.closeMenu();

            int direction = menuBridge.getDirection(); // ???????????????????????????
            int menuPosition = menuBridge.getPosition(); // ?????????RecyclerView???Item??????Position???

            if (direction == SwipeRecyclerView.RIGHT_DIRECTION) {
                if (menuPosition == 0) {
                    showDeletePop(position);
                }
            }

        }
    };
}
