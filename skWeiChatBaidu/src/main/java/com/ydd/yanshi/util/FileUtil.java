package com.ydd.yanshi.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.User;
import com.ydd.yanshi.ui.base.CoreManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class FileUtil {

    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_ADUIO = 2;
    private static final int TYPE_VIDEO = 3;

    /**
     * {@link #TYPE_IMAGE}<br/>
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    private static String getPublicFilePath(int type) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = MyApplication.getInstance().mVoicesDir;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = MyApplication.getInstance().mVideosDir;
                fileSuffix = ".mp4";
                break;
            case TYPE_IMAGE:
                fileDir = MyApplication.getInstance().mPicturesDir;
                fileSuffix = ".jpg";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String name =format.format(date)+fileSuffix ;

        Log.e("TAG_????????????","??????fileDir="+fileDir +"/"+name);

        return fileDir +"/"+name;
    }

    /**
     * {@link #TYPE_ADUIO}<br/>
     * {@link #TYPE_VIDEO} <br/>
     *
     * @param type
     * @return
     */
    private static String getPrivateFilePath(int type, String userId) {
        String fileDir = null;
        String fileSuffix = null;
        switch (type) {
            case TYPE_ADUIO:
                fileDir = MyApplication.getInstance().mAppDir + File.separator + userId + File.separator + Environment.DIRECTORY_MUSIC;
                fileSuffix = ".mp3";
                break;
            case TYPE_VIDEO:
                fileDir = MyApplication.getInstance().mAppDir + File.separator + userId + File.separator + Environment.DIRECTORY_MOVIES;
                fileSuffix = ".mp4";
                break;
        }
        if (fileDir == null) {
            return null;
        }
        File file = new File(fileDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        return fileDir + File.separator + UUID.randomUUID().toString().replaceAll("-", "") + fileSuffix;
    }

    public static String getRandomImageFilePath() {
        return getPublicFilePath(TYPE_IMAGE);
    }

    public static String getRandomAudioFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            return getPrivateFilePath(TYPE_ADUIO, user.getUserId());
        } else {
            return getPublicFilePath(TYPE_ADUIO);
        }
    }

    public static String getRandomAudioAmrFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        String filePath = null;
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            filePath = getPrivateFilePath(TYPE_ADUIO, user.getUserId());
        } else {
            filePath = getPublicFilePath(TYPE_ADUIO);
        }
        if (!TextUtils.isEmpty(filePath)) {
            return filePath.replace(".mp3", ".amr");
        } else {
            return null;
        }
    }

    public static String getRandomVideoFilePath() {
        User user = CoreManager.requireSelf(MyApplication.getInstance());
        if (user != null && !TextUtils.isEmpty(user.getUserId())) {
            return getPrivateFilePath(TYPE_VIDEO, user.getUserId());
        } else {
            return getPublicFilePath(TYPE_VIDEO);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static void createFileDir(String fileDir) {
        File fd = new File(fileDir);
        if (!fd.exists()) {
            fd.mkdirs();
        }
    }

    /**
     * @param fullName
     */
    public static void delFile(String fullName) {
        File file = new File(fullName);
        if (file.exists()) {
            if (file.isFile()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param path String ??????????????? ??? /sdcard/data/
     */
    public static void delAllFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            System.out.println(path + tempList[i]);
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]); // ?????????????????????????????????
                delFolder(path + "/" + tempList[i]); // ?????????????????????
            }
        }
    }

    /**
     * ???????????????
     * <p>
     * String ???????????????????????? ???/sdcard/data/
     * String
     *
     * @return boolean
     */
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // ???????????????????????????
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); // ??????????????????
        } catch (Exception e) {
            System.out.println("???????????????????????????");
            e.printStackTrace();
        }
    }

    public static File saveFileByBitmap(Bitmap bitmap, String fileDir, String fileName) {
        File dirFile = new File(fileDir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }

        File myCaptureFile = new File(fileName);
        BufferedOutputStream bufferedOutputStream;
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e("xuan", "saveFileByBitmap: " + myCaptureFile.getAbsolutePath());
        return myCaptureFile;
    }

    public static String getSaveDirectory(String str) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + str + "/";
            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }
            return rootDir;
        } else {
            return null;
        }
    }

    private static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //???????????????
                InputStream inStream = new FileInputStream(oldPath); //???????????????
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //????????? ????????????
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private static void saveImageToGallery(Context content, Bitmap bitmap) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ??????????????????
        LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
    }

    public static File saveImageToGallery2(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            ToastUtil.showToast(context, context.getString(R.string.creating_qr_code));
        }
        // 1.????????????
        File appDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2.??????????????????????????????
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 3.??????????????????
       LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));

        return file;
    }

    /**
     * ?????????(??????/??????)???????????????
     */
    public static void downImageToGallery(final Context context, String url) {
        if (url.toLowerCase().endsWith("gif")) {
            File file = new File(url);
            if (file.exists()) {
                // ???Gif?????????????????????
                String imagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".gif";
                copyFile(url, imagePath);
                Toast.makeText(context, R.string.tip_save_gif_success, Toast.LENGTH_SHORT).show();
                Intent intentBroadcast = new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File mGifFile = new File(imagePath);
                intentBroadcast.setData(Uri.fromFile(mGifFile));
               LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(intentBroadcast);
            } else {
                ToastUtil.showToast(context, context.getString(R.string.tip_save_gif_failed));
            }
        } else {
            Glide.with(context)
                  .load(url)
                    .dontAnimate()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            ToastUtil.showToast(context, context.getString(R.string.tip_save_image_failed));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            BitmapDrawable bd = (BitmapDrawable) resource;
                            saveImageToGallery(context, bd.getBitmap());
                            Toast.makeText(context, R.string.tip_save_image_success, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });
        }
    }

    /**
     * ??????bitmap?????????
     */
    public static String saveBitmap(Bitmap bitmap) {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".png";
        File file = new File(imageDir, fileName);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        readPictureDegree(file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    public static String getDesignationFilePath(String fileName, Bitmap bitmap) {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }
        fileName = fileName + ".png";
        File file = new File(imageDir, fileName);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        readPictureDegree(file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    /**
     * ????????????????????????
     *
     * @param path ????????????
     * @return ??????
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.i("zx", "????????????-" + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("zx", "readPictureDegree: " + degree);
        return degree;
    }

    public static File createImageFileForEdit() {
        File imageDir = new File(Environment.getExternalStorageDirectory(), "image");
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        return new File(imageDir, fileName);
    }

    public static boolean isExist(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        return file.exists();
    }

    // ????????????byte??????
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        // ??????????????????
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            // ???????????????????????????
            throw new IOException("File is to large " + file.getName());
        }
        // ???????????????????????????????????????
        byte[] bytes = new byte[(int) length];
        // ???????????????byte?????????
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        // ??????????????????????????????
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

}
