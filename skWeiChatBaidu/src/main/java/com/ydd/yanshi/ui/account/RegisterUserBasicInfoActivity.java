package com.ydd.yanshi.ui.account;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.Reporter;
import com.ydd.yanshi.adapter.MessageLogin;
import com.ydd.yanshi.bean.Area;
import com.ydd.yanshi.bean.LoginRegisterResult;
import com.ydd.yanshi.bean.User;
import com.ydd.yanshi.bean.WXUserInfo;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.db.dao.AreasDao;
import com.ydd.yanshi.helper.AvatarHelper;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.helper.LoginHelper;
import com.ydd.yanshi.map.MapHelper;
import com.ydd.yanshi.ui.GlideEngine;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.tool.SelectAreaActivity;
import com.ydd.yanshi.util.AsyncUtils;
import com.ydd.yanshi.util.CameraUtil;
import com.ydd.yanshi.util.DateSelectHelper;
import com.ydd.yanshi.util.DeviceInfoUtil;
import com.ydd.yanshi.util.EventBusHelper;
import com.ydd.yanshi.util.LogUtils;
import com.ydd.yanshi.util.StringUtils;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.view.TipDialog;
import com.ydd.yanshi.volley.Result;
import com.ydd.yanshi.wxapi.WXHelper;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;

import static com.ydd.yanshi.util.CameraUtil.REQUEST_CODE_CROP_CAMERA;
import static com.ydd.yanshi.util.CameraUtil.REQUEST_CODE_CROP_PHOTO;

/**
 * ??????-3.????????????
 */
public class RegisterUserBasicInfoActivity extends BaseActivity implements View.OnClickListener {

    private ImageView mAvatarImg;
    private EditText mNameEdit;
    private TextView mSexTv;
    private TextView mBirthdayTv;
    private TextView mCityTv;
    private Button mNextStepBtn;
    private TextView nickNameTv, sexTv, birthdayTv, cityTv;
    /* ?????????????????????????????????????????????????????? */
    private String mobilePrefix;
    private String mPhoneNum;
    private String mPassword;
    // ??????empty?????????null,
    private String mInviteCode;
    private String thirdToken;
    // Temp
    private User mTempData;
    // ?????????????????????
    private File mCurrentFile;
    private boolean isSelectAvatar;
    //    private Uri mNewPhotoUri;
    Photo photo;

    public RegisterUserBasicInfoActivity() {
        noLoginRequired();
    }

    public static void start(
            Context ctx,
            String mobilePrefix,
            String phoneStr,
            String password,
            String inviteCode,
            String thirdToken
    ) {
        Intent intent = new Intent(ctx, RegisterUserBasicInfoActivity.class);
        intent.putExtra(RegisterActivity.EXTRA_AUTH_CODE, mobilePrefix);
        intent.putExtra(RegisterActivity.EXTRA_PHONE_NUMBER, phoneStr);
        intent.putExtra(RegisterActivity.EXTRA_INVITE_CODE, inviteCode);
        intent.putExtra(RegisterActivity.EXTRA_PASSWORD, password);
        intent.putExtra("thirdToken", thirdToken);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user_basic_info);
        EasyPhotos.preLoad(this);
        if (getIntent() != null) {
            mobilePrefix = getIntent().getStringExtra(RegisterActivity.EXTRA_AUTH_CODE);
            mPhoneNum = getIntent().getStringExtra(RegisterActivity.EXTRA_PHONE_NUMBER);
            mPassword = getIntent().getStringExtra(RegisterActivity.EXTRA_PASSWORD);
            mInviteCode = getIntent().getStringExtra(RegisterActivity.EXTRA_INVITE_CODE);
            thirdToken = getIntent().getStringExtra("thirdToken");
        }
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBack();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_BaseInfo"));
        initView();
        requestLocationCity();

        if (!TextUtils.isEmpty(thirdToken)) {
            AsyncUtils.doAsync(this, t -> {
                LogUtils.log(thirdToken);
                Reporter.post("????????????????????????????????????", t);
            }, c -> {
                WXUserInfo userInfo = WXHelper.requestUserInfo(thirdToken);
                mTempData.setSex(userInfo.getSex());
                mTempData.setNickName(userInfo.getNickname());
                c.uiThread(r -> {
                    Glide.with(r)
                            .load(userInfo.getHeadimgurl())
                            .dontAnimate().skipMemoryCache(true) // ?????????????????????
                            .diskCacheStrategy(DiskCacheStrategy.NONE) // ?????????????????????
                            .downloadOnly(new SimpleTarget<File>() {
                                @Override
                                public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                                    r.mCurrentFile = resource;
                                    r.isSelectAvatar = true;
                                    Glide.with(r).load(resource)
                                            .dontAnimate().skipMemoryCache(true) // ?????????????????????
                                            .diskCacheStrategy(DiskCacheStrategy.NONE) // ?????????????????????
                                            .into(r.mAvatarImg);
                                }
                            });
                    updateUI();
                });
            });
        }
        EventBusHelper.register(this);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(MessageLogin message) {
        finish();
    }

    private void initView() {
        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        mNameEdit = (EditText) findViewById(R.id.name_edit);
        mSexTv = (TextView) findViewById(R.id.sex_tv);
        mBirthdayTv = (TextView) findViewById(R.id.birthday_tv);
        mCityTv = (TextView) findViewById(R.id.city_tv);
        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
//        mNextStepBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        nickNameTv = (TextView) findViewById(R.id.name_text);
        sexTv = (TextView) findViewById(R.id.sex_text);
        birthdayTv = (TextView) findViewById(R.id.birthday_text);
        cityTv = (TextView) findViewById(R.id.city_text);

        nickNameTv.setText(InternationalizationHelper.getString("JX_NickName"));
        sexTv.setText(InternationalizationHelper.getString("JX_Sex"));
        birthdayTv.setText(InternationalizationHelper.getString("JX_BirthDay"));
        cityTv.setText(InternationalizationHelper.getString("JX_Address"));
//        mNameEdit.setHint(InternationalizationHelper.getString("JX_InputName"));
        mNextStepBtn.setText(InternationalizationHelper.getString("JX_Confirm"));

        mAvatarImg.setOnClickListener(this);
        findViewById(R.id.sex_select_rl).setOnClickListener(this);
        findViewById(R.id.birthday_select_rl).setOnClickListener(this);
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.city_select_rl).setVisibility(View.GONE);
        } else {
            findViewById(R.id.city_select_rl).setOnClickListener(this);
        }
        mNextStepBtn.setOnClickListener(this);

        updateUI();
    }

    private void updateUI() {
        if (mTempData == null) {
            mTempData = new User();
            mTempData.setSex(1);
            mTempData.setBirthday(TimeUtils.sk_time_current_time());
        }
        if (!TextUtils.isEmpty(mTempData.getNickName())) {
            mNameEdit.setText(mTempData.getNickName());
        }
        if (mTempData.getSex() == 1) {
            mSexTv.setText(R.string.sex_man);
        } else {
            mSexTv.setText(R.string.sex_woman);
        }
        mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str(mTempData.getBirthday()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.avatar_img:
                showSelectAvatarDialog();
                break;
            case R.id.sex_select_rl:
                showSelectSexDialog();
                break;
            case R.id.birthday_select_rl:
                showSelectBirthdayDialog();
                break;
            case R.id.city_select_rl:
                Intent intent = new Intent(RegisterUserBasicInfoActivity.this, SelectAreaActivity.class);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_PARENT_ID, Area.AREA_DATA_CHINA_ID);// ???????????????
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_TYPE, Area.AREA_TYPE_PROVINCE);
                intent.putExtra(SelectAreaActivity.EXTRA_AREA_DEEP, Area.AREA_TYPE_CITY);// ??????????????????????????????
                startActivityForResult(intent, 4);
                break;
            case R.id.next_step_btn:
                register();
                break;
        }
    }

    private void showSelectAvatarDialog() {
        String[] items = new String[]{InternationalizationHelper.getString("PHOTOGRAPH"), InternationalizationHelper.getString("ALBUM")};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("SELECT_AVATARS")).setSingleChoiceItems(items, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == 0) {
                            openCamera();
                        } else {
                            selectPhoto();
                        }
                    }
                });
        builder.show();
    }

    private void openCamera() {
        String packageName = DeviceInfoUtil.getPackageName(this);
        EasyPhotos.createCamera(this, false)//????????????????????????,???????????????????????????false??????????????????0????????????????????????
                .setFileProviderAuthority(packageName + ".fileprovider")//????????????????????????`FileProvider?????????`
                .start(101);
    }

    private void selectPhoto() {
        EasyPhotos.createAlbum(this, false, true, GlideEngine.getInstance())//???????????????????????????????????????????????????,???????????????????????????false??????????????????0???????????????????????????[??????Glide?????????????????????](https://github.com/HuanTanSheng/EasyPhotos/wiki/12-%E9%85%8D%E7%BD%AEImageEngine%EF%BC%8C%E6%94%AF%E6%8C%81%E6%89%80%E6%9C%89%E5%9B%BE%E7%89%87%E5%8A%A0%E8%BD%BD%E5%BA%93)
                .start(101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CROP_PHOTO) {// ??????????????????,????????????????????????
                mCurrentFile = new File(photo.path);
                try {
                    mCurrentFile.createNewFile();
                    if (!mCurrentFile.exists()) {
                        // ???????????????
                        Log.e("TAG_????????????", "???????????????????????????");
                        return;
                    }
                    if (mCurrentFile != null) {
                        Log.e("TAG_????????????", "uri=" + mCurrentFile.getPath());
                        AvatarHelper.getInstance().displayUrl(mCurrentFile.getPath(), mAvatarImg);
                        isSelectHead = true;

                    } else {
                        ToastUtil.showToast(this, R.string.c_crop_failed);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_CODE_CROP_CAMERA) {// ??????????????????,????????????????????????
                mCurrentFile = CameraUtil.getCurrentFile();
                if (!mCurrentFile.exists()) {
                    // ???????????????
                    Log.e("TAG_????????????", "???????????????????????????");
                    return;
                }
                if (mCurrentFile != null) {
                    Log.e("TAG_????????????", "uri=" + mCurrentFile.getPath());
                    AvatarHelper.getInstance().displayUrl(mCurrentFile.getPath(), mAvatarImg);
                    isSelectHead = true;

                } else {
                    ToastUtil.showToast(this, R.string.c_crop_failed);
                }

            } else if (requestCode == 4) {
                // ????????????
                if (resultCode == RESULT_OK && data != null) {
                    int countryId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTRY_ID, 0);
                    int provinceId = data.getIntExtra(SelectAreaActivity.EXTRA_PROVINCE_ID, 0);
                    int cityId = data.getIntExtra(SelectAreaActivity.EXTRA_CITY_ID, 0);
                    int countyId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTY_ID, 0);

                    String province_name = data.getStringExtra(SelectAreaActivity.EXTRA_PROVINCE_NAME);
                    String city_name = data.getStringExtra(SelectAreaActivity.EXTRA_CITY_NAME);
                    /*String county_name = data.getStringExtra(SelectAreaActivity.EXTRA_COUNTY_ID);*/
                    mCityTv.setText(province_name + "-" + city_name);

                    mTempData.setCountryId(countryId);
                    mTempData.setProvinceId(provinceId);
                    mTempData.setCityId(cityId);
                    mTempData.setAreaId(countyId);
                }
            } else if (requestCode == 101) {
                //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                ArrayList<Photo> resultPhotos =
                        data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS);
                photo = resultPhotos.get(0);
                CameraUtil.cropImage(this, photo.uri, REQUEST_CODE_CROP_PHOTO, true);

            }
        }

    }

    private void showSelectSexDialog() {
        String[] sexs = new String[]{getString(R.string.sex_man), getString(R.string.sex_woman)};
        new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("GENDER_SELECTION"))
                .setSingleChoiceItems(sexs, mTempData.getSex() == 1 ? 0 : 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            mTempData.setSex(1);
                            mSexTv.setText(R.string.sex_man);
                        } else {
                            mTempData.setSex(0);
                            mSexTv.setText(R.string.sex_woman);
                        }
                        dialog.dismiss();
                    }
                }).setCancelable(true).create().show();
    }

    @SuppressWarnings("deprecation")
    private void showSelectBirthdayDialog() {
        DateSelectHelper dialog = DateSelectHelper.getInstance(RegisterUserBasicInfoActivity.this);
        dialog.setDateMin("1900-1-1");
        dialog.setDateMax(System.currentTimeMillis());
        dialog.setCurrentDate(mTempData.getBirthday() * 1000);
        dialog.setOnDateSetListener(new DateSelectHelper.OnDateResultListener() {
            @Override
            public void onDateSet(long time, String dateFromat) {
                mTempData.setBirthday(time / 1000);
                mBirthdayTv.setText(dateFromat);
            }
        });

        dialog.show();
    }

    private void loadPageData() {
        mTempData.setNickName(mNameEdit.getText().toString().trim());
    }

    private boolean isSelectHead = false;

    private void register() {
        if (!MyApplication.getInstance().isNetworkActive()) {
            ToastUtil.showToast(this, R.string.net_exception);
            return;
        }
        loadPageData();

        if (TextUtils.isEmpty(mTempData.getNickName())) {
            mNameEdit.requestFocus();
            mNameEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.name_empty_error));
            return;
        }

        if (!isSelectHead) {//???????????????????????????
            ToastUtil.showToast(this, R.string.head_empty_error);
            return;
        }


        /*if (!StringUtils.isNickName(mTempData.getNickName())) {
            mNameEdit.requestFocus();
            mNameEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.nick_name_format_error));
            return;
        }*/

        if (!coreManager.getConfig().disableLocationServer) {
            if (mTempData.getCityId() <= 0) {
                TipDialog tipDialog = new TipDialog(this);
                tipDialog.setTip(getString(R.string.live_address_empty_error));
                tipDialog.show();
                return;
            }
        }

//        if (!isSelectAvatar) {
//            DialogHelper.tip(this, getString(R.string.must_select_avatar_can_register));
//            return;
//        }

        Map<String, String> params = new HashMap<>();
        // ???????????????????????????
        params.put("userType", "1");
        params.put("telephone", mPhoneNum);
        params.put("password", mPassword);
        if (!TextUtils.isEmpty(mInviteCode)) {
            params.put("inviteCode", mInviteCode);
        }
        params.put("areaCode", mobilePrefix);//TODO AreaCode ??????????????????
        // ???????????????
        params.put("nickname", mTempData.getNickName());
        params.put("sex", String.valueOf(mTempData.getSex()));
        params.put("birthday", String.valueOf(mTempData.getBirthday()));
        params.put("xmppVersion", "1");
        params.put("countryId", String.valueOf(mTempData.getCountryId()));
        params.put("provinceId", String.valueOf(mTempData.getProvinceId()));
        params.put("cityId", String.valueOf(mTempData.getCityId()));
        params.put("areaId", String.valueOf(mTempData.getAreaId()));

        params.put("isSmsRegister", String.valueOf(RegisterActivity.isSmsRegister));

        // ????????????
        params.put("apiVersion", DeviceInfoUtil.getVersionCode(mContext) + "");
        params.put("model", DeviceInfoUtil.getModel());
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
        // ????????????
        double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
        double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
        String location = MyApplication.getInstance().getBdLocationHelper().getAddress();
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));
        if (!TextUtils.isEmpty(location))
            params.put("location", location);

        String url;
        if (TextUtils.isEmpty(thirdToken)) {
            url = coreManager.getConfig().USER_REGISTER;
        } else {
            url = coreManager.getConfig().USER_THIRD_REGISTER;
            params.put("type", "2");
            params.put("loginInfo", WXHelper.parseOpenId(thirdToken));
        }

        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(new BaseCallback<LoginRegisterResult>(LoginRegisterResult.class) {

                    @Override
                    public void onResponse(ObjectResult<LoginRegisterResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (!com.xuan.xuanhttplibrary.okhttp.result.Result.checkSuccess(getApplicationContext(), result)) {
                            if (result == null) {
                                Reporter.post("???????????????result??????");
                            } else {
                                Reporter.post("???????????????" + result.toString());
                            }
                            return;
                        }
                        // ????????????
                        boolean success = LoginHelper.setLoginUser(RegisterUserBasicInfoActivity.this, coreManager, mPhoneNum, mPassword, result);
                        if (success) {
                            // ???????????????????????????????????????
                            MyApplication.getInstance().initPayPassword(result.getData().getUserId(), 0);
                            if (mCurrentFile != null && mCurrentFile.exists()) {
                                // ???????????????????????????????????????
                                uploadAvatar(result.getData().getIsupdate(), mCurrentFile);
                                return;
                            } else {
                                // ?????????????????????????????????????????????
                                // startActivity(new Intent(RegisterUserBasicInfoActivity.this, DataDownloadActivity.class));
                                DataDownloadActivity.start(mContext, result.getData().getIsupdate());
                                finish();
                            }
                            ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.register_success);
                        } else {
                            // ??????
                            if (TextUtils.isEmpty(result.getResultMsg())) {
                                ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.register_error);
                            } else {
                                ToastUtil.showToast(RegisterUserBasicInfoActivity.this, result.getResultMsg());
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        ToastUtil.showErrorNet(RegisterUserBasicInfoActivity.this);
                    }
                });
    }

    /**
     * ????????????...
     */
    private void requestLocationCity() {
        MapHelper.getInstance().requestLatLng(new MapHelper.OnSuccessListener<MapHelper.LatLng>() {
            @Override
            public void onSuccess(MapHelper.LatLng latLng) {
                MapHelper.getInstance().requestCityName(latLng, new MapHelper.OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        String cityName = MyApplication.getInstance().getBdLocationHelper().getCityName();
                        Area area = null;
                        if (!TextUtils.isEmpty(cityName)) {
                            area = AreasDao.getInstance().searchByName(cityName);
                        }
                        if (area != null) {
                            Area countryArea = null;
                            Area provinceArea = null;
                            Area cityArea = null;
                            Area countyArea = null;
                            switch (area.getType()) {
                                case Area.AREA_TYPE_COUNTRY:
                                    countryArea = area;
                                    break;
                                case Area.AREA_TYPE_PROVINCE:
                                    provinceArea = area;
                                    break;
                                case Area.AREA_TYPE_CITY:
                                    cityArea = area;
                                    break;
                                case Area.AREA_TYPE_COUNTY:
                                default:
                                    countyArea = area;
                                    break;
                            }
                            if (countyArea != null) {
                                mTempData.setAreaId(countyArea.getId());
                                cityArea = AreasDao.getInstance().getArea(countyArea.getParent_id());
                            }

                            if (cityArea != null) {
                                mTempData.setCityId(cityArea.getId());
                                mCityTv.setText(cityArea.getName());
                                provinceArea = AreasDao.getInstance().getArea(cityArea.getParent_id());
                            }

                            if (provinceArea != null) {
                                mTempData.setProvinceId(provinceArea.getId());
                                countryArea = AreasDao.getInstance().getArea(provinceArea.getParent_id());
                            }

                            if (countryArea != null) {
                                mTempData.setCountryId(countryArea.getId());
                            }
                        } else {
                            Log.e(TAG, "?????????????????????", new RuntimeException("??????????????????" + cityName));
                        }
                    }
                }, new MapHelper.OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "???????????????????????????", t);

                    }
                });
            }
        }, new MapHelper.OnErrorListener() {
            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "????????????????????????", t);
            }
        });
    }


    @Override
    public void onBackPressed() {
        doBack();
    }

    @Override
    protected boolean onHomeAsUp() {
        doBack();
        return true;
    }

    private void doBack() {
        TipDialog tipDialog = new TipDialog(this);
        tipDialog.setmConfirmOnClickListener(getString(R.string.cancel_register_prompt), new TipDialog.ConfirmOnClickListener() {
            @Override
            public void confirm() {
                finish();
            }
        });
        tipDialog.show();
    }

    private void uploadAvatar(int isupdate, File file) {
        if (!file.exists()) {
            // ???????????????
            return;
        }
        // ?????????????????????ProgressDialog
        DialogHelper.showMessageProgressDialog(this, InternationalizationHelper.getString("UPLOAD_AVATAR"));
        RequestParams params = new RequestParams();
        String loginUserId = coreManager.getSelf().getUserId();
        params.put("userId", loginUserId);
        try {
            params.put("file1", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(coreManager.getConfig().AVATAR_UPLOAD_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
                boolean success = false;
                if (arg0 == 200) {
                    Result result = null;
                    try {
                        result = JSON.parseObject(new String(arg2), Result.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (result != null && result.getResultCode() == Result.CODE_SUCCESS) {
                        success = true;
                    }
                }

                DialogHelper.dismissProgressDialog();
                if (success) {
                    ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.upload_avatar_success);
                } else {
                    ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.upload_avatar_failed);
                }

                // startActivity(new Intent(RegisterUserBasicInfoActivity.this, DataDownloadActivity.class));
                DataDownloadActivity.start(mContext, isupdate);
                finish();
            }

            @Override
            public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
                DialogHelper.dismissProgressDialog();
                ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.upload_avatar_failed);
                // startActivity(new Intent(RegisterUserBasicInfoActivity.this, DataDownloadActivity.class));
                DataDownloadActivity.start(mContext, isupdate);
                finish();
            }
        });
    }
}
