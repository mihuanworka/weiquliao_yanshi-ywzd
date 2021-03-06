package com.ydd.yanshi.util;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.ydd.yanshi.MyApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.DIRECTORY_PICTURES;

public final class CameraUtil {
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static MediaScannerConnection sMediaScannerConnection;

    public static final int REQUEST_CODE_OPENPHOTO = 10002;
    public static final int REQUEST_CODE_OPENCAMERA = 10001;
    public static final int REQUEST_CODE_CROP_PHOTO = 10000;
    public static final int REQUEST_CODE_CROP_CAMERA = 10003;
    /**
     * Create a file Uri for saving an image or video
     *
     * @param context
     * @param type    the type of the file you want saved {@link #MEDIA_TYPE_IMAGE}
     *                {@link #MEDIA_TYPE_VIDEO}
     * @return return the uri of the file ,if create failed,return null
     */
    static Uri photoUri = null;
    public static Uri getOutputMediaFileUri(Context context, int type) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file = getOutputMediaFile(context, type);
        if (file == null) {
            return null;
        }

        //????????????????????????Intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
        
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    String packageName = DeviceInfoUtil.getPackageName(context);
                    //??????Android 7.0?????????????????????FileProvider????????????content?????????Uri
                    photoUri = FileProvider.getUriForFile(MyApplication.getInstance(), packageName+".fileprovider", file);
                } else {
                    photoUri = Uri.fromFile(file);
                }

        }
        return photoUri;
    }
    public static Uri getPhotoUri() {
        return photoUri;
    }
    /**
     * Create a file for saving an image or video,is default in the
     * ../Pictures/[you app PackageName] directory
     *
     * @param context
     * @param type    the type of the file you want saved {@link #MEDIA_TYPE_IMAGE}
     *                {@link #MEDIA_TYPE_VIDEO}
     * @return return the file you create,if create failed,return null
     */
    static String currentPath=null;
    private static File getOutputMediaFile(Context context, int type) {
        String filePath = null;
        if (type == MEDIA_TYPE_IMAGE) {
            filePath = FileUtil.getRandomImageFilePath();
        } else if (type == MEDIA_TYPE_VIDEO) {
            filePath = FileUtil.getRandomVideoFilePath();
        } else {
            return null;
        }
        if (TextUtils.isEmpty(filePath)) {
            return null;
        } else {

            File ji=null;
            try {
                ji=new File(filePath);
                ji.createNewFile();
                currentPath=ji.getAbsolutePath();
                Log.e("TAG_????????????", "ji=" + ji.getPath());
                Log.e("TAG_????????????", "currentPath=" + currentPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ji;

        }
    }
    public static String getCurrentPath() {
       return currentPath;
    }
    /**
     * invoke the system Camera app and capture a image??? you can received the
     * capture result in {@link (int,int,Intent)}??? If
     * successed,you can use the outputUri to get the image
     *
     * @param activity
     * @param outputUri   ??????????????????????????????
     * @param requestCode
     */
    public static void captureImage(Activity activity, Uri outputUri, int requestCode) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     *
     * @param activity
     * @param
     * @param outputFileUri   ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @return
     */
    public static void cropImage(Activity activity, Uri mNewPhotoUri, int requestCode,boolean fromCamera) {
        Log.e("TAG_????????????", "????????????");
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (fromCamera){
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mNewPhotoUri);
        }else {
            String path = CameraUtil.getFilePathFromURI(activity,mNewPhotoUri);
//                    mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
            File mCurrentFile = new File(path);
            try {
                mCurrentFile.createNewFile();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Uri uri = Uri.fromFile(mCurrentFile);
//            Uri cropImageUri = CameraUtil.getOutputMediaFileUri(activity, CameraUtil.MEDIA_TYPE_IMAGE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        intent.setDataAndType(mNewPhotoUri , "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 500);
        intent.putExtra("outputY", 500);
        intent.putExtra("scale", true);

        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
//        //??????????????????uri???????????????????????????????????????
//        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
//        for (ResolveInfo resolveInfo : resInfoList) {
//            String packageName = resolveInfo.activityInfo.packageName;
//            activity.grantUriPermission(packageName, mNewPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        }

        activity.startActivityForResult(intent, requestCode);

    }

    /**
     * ?????????????????????????????? ?????? {@link}?????????
     * onActivityResult???data.getData()??????????????????Uri
     *
     * @param activity
     * @param requestCode
     * @return
     */
    public static void pickImageSimple(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(intent, requestCode);
    }



    /**
     * ??????The data stream for the file
     */
    public static String getImagePathFromUri(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                return getImagePathFromUriKitkat(context, uri);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }
        return getImagePathFromUriSimple(context, uri);
    }

    /**
     * 4.4??????
     *
     * @param context
     * @param uri
     */
    private static String getImagePathFromUriSimple(Context context, Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String returnStr = cursor.getString(column_index);
        cursor.close();
        return returnStr;
    }

    /**
     * 4.4?????????Document Uri
     *
     * @param context
     * @param uri
     * @return
     */
    private static String getImagePathFromUriKitkat(Context context, Uri uri) {
        String wholeID = DocumentsContract.getDocumentId(uri);
        if (TextUtils.isEmpty(wholeID) || !wholeID.contains(":")) {
            return null;
        }
        // ??????????????????ID
        String id = wholeID.split(":")[1];
        // ??????????????????
        String[] column = {MediaStore.Images.Media.DATA};
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column, sel, new String[]{id}, null);
        int columnIndex = cursor.getColumnIndex(column[0]);

        String filePath = null;
        if (cursor.moveToFirst()) {
            // DATA????????????????????????????????????
            filePath = cursor.getString(columnIndex);
        }
        // ?????????????????????
        cursor.close();
        return filePath;
    }

    /**
     * ??????????????????????????????,???????????????
     * ,4.4?????????????????????????????????????????????????????????????????????????????????????????????pickImageSimple???????????????????????????
     *
     * @param activity
     * @param outputUri   ??????????????????????????????
     * @param requestCode
     * @return
     */
    @Deprecated
    public static void pickImageCrop(Activity activity, Uri outputUri, int requestCode, int aspectX, int aspectY, int outputX, int outputY) {
        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        Intent intent = new Intent();
        // ????????????????????????????????????Action
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        intent.putExtra("crop", "true");
        // ???????????????
        intent.putExtra("aspectX", aspectX);
        intent.putExtra("aspectY", aspectY);
        // ??????????????????
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true); // ???????????????????????????????????????????????????
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // ?????????????????????
        intent.putExtra("noFaceDetection", false);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * ????????????????????????????????????????????????
     */
    public static void scannerImage(Activity activity, final Uri fileUri, final ScannerResult scannerResult) {
        if (fileUri == null) {
            if (scannerResult != null) {
                scannerResult.onResult(false);
            }
            return;
        }
        sMediaScannerConnection = new MediaScannerConnection(activity, new MediaScannerConnectionClient() {
            public void onMediaScannerConnected() {
                sMediaScannerConnection.scanFile(fileUri.getPath(), "image/*");
            }

            public void onScanCompleted(String path, Uri uri) {
                sMediaScannerConnection.disconnect();
                if (scannerResult != null) {
                    scannerResult.onResult(uri != null);
                }
            }
        });
        sMediaScannerConnection.connect();
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param context
     * @param filePath
     * @return ?????????????????????????????????Uri?????????????????????????????????????????????null
     */
    public static Uri isImageFileInMedia(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Images.Media.DISPLAY_NAME + "='" + file.getName() + "'", null, null);
        Uri uri = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToLast();
            long id = cursor.getLong(0);
            uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        }
        return uri;
    }

    /**
     * @param path
     * @return void
     * @Title: setPictureDegreeZero
     */
    public static void setPictureDegreeZero(String path) {
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            // ????????????90????????????ExifInterface.ORIENTATION_ROTATE_90??????????????????????????????String?????????
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "no");
            exifInterface.saveAttributes();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * ??????bitmap
     */
    public static Bitmap restoreRotatedImage(int degrees, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return bitmap;
    }


    /**
     * ??????bitmap (-1,1)????????????  (1,-1)????????????
     */
    public static Bitmap turnCurrentLayer(Bitmap srcBitmap, float sx, float sy) {
        Bitmap cacheBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);// ???????????????????????????
        int w = cacheBitmap.getWidth();
        int h = cacheBitmap.getHeight();

        Canvas cv = new Canvas(cacheBitmap);//??????canvas???bitmap???????????????

        Matrix mMatrix = new Matrix();//???????????? ??????????????????

        mMatrix.postScale(sx, sy);

        Bitmap resultBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, w, h, mMatrix, true);
        cv.drawBitmap(resultBitmap,
                new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()),
                new Rect(0, 0, w, h), null);
        return resultBitmap;
    }

    public static interface ScannerResult {
        void onResult(boolean success);
    }

    public static String getFilePathFromURI(Context context, Uri contentUri) {
        File rootDataDir = context.getExternalFilesDir(null);
        String fileName = getFileName(contentUri);
        if (!TextUtils.isEmpty(fileName)) {
            File copyFile = new File(rootDataDir + File.separator + fileName);
            copyFile(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;
    }
    public static String getFileName(Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            fileName = path.substring(cut + 1);
        }
        return fileName;
    }

    public static void copyFile(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            copyStream(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int copyStream(InputStream input, OutputStream output) throws Exception, IOException {
        final int BUFFER_SIZE = 1024 * 2;
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(output, BUFFER_SIZE);
        int count = 0, n = 0;
        try {
            while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, n);
                count += n;
            }
            out.flush();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        return count;
    }
    public static void gotoCrop(Activity activity,Uri sourceUri,int requestCode) {
        File imageCropFile = createImageFile(activity, true);
        if (imageCropFile != null) {
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.putExtra("crop", "true");

            intent.putExtra("aspectX", 1);    //X??????????????????
            intent.putExtra("aspectY", 1);    //Y??????????????????
            intent.putExtra("outputX", 500);  //???????????????
            intent.putExtra("outputY", 500); //???????????????
            intent.putExtra("scale ", true);  //??????????????????
            intent.putExtra("return-data", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.setDataAndType(sourceUri, "image/*"); //???????????????
            if (Build.VERSION.SDK_INT >= 28) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            } else {
                Uri imgCropUri = Uri.fromFile(imageCropFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imgCropUri);
            }
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public static File getAppRootDirPath() {
        return MyApplication.getContext().getExternalFilesDir(null).getAbsoluteFile();
    }

    public static File getCurrentFile() {
        return mCurrentFile;
    }

    public static Uri uri;
    public static File mCurrentFile;
    public static File createImageFile(Context context, boolean isCrop) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "";
            if (isCrop) {
                fileName = "IMG_"+timeStamp+"_CROP.jpg";
            } else {
                fileName = "IMG_"+timeStamp+".jpg";
            }
            File rootFile = new File(getAppRootDirPath() + File.separator + "capture");
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }

            if (Build.VERSION.SDK_INT >= 30) {
                mCurrentFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + File.separator + fileName);
                // ?????? MediaStore API ??????file ???????????????????????????????????????uri?????????App????????????????????????????????????????????????????????? MediaStore API????????????
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, mCurrentFile.getAbsolutePath());
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }else {
                mCurrentFile = new File(rootFile.getAbsolutePath() + File.separator + fileName);
            }
            return mCurrentFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
