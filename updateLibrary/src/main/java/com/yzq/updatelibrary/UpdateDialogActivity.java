package com.yzq.updatelibrary;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by YuanZhiQiang on 2017/10/20
 */
public class UpdateDialogActivity extends AppCompatActivity {
    private final String TAG = "AppUpdate";
    private Context mContext;
    private int REQUEST_CODE_WRITE_STORAGE = 0x1314;
    private int REQUEST_CODE_PERMISSION = 0x111;

    private AppUpdateBean mUpdateInfo;

    //保存信息的文件夹名称
    private String dirName = "updateAppSDK";

    private File mUpdateFileDir;
    //txt文件
    private File mTxtFile;
    //txt文件路径
    private String mTxtFilePath;
    //apk文件路径
    private String mApkPath;

    private UpdateReceiver mUpdateReceiver;
    private String APK_NAME = "new.apk";
    private DownloadManager mDownloadManager;
    private static long DOWNLOAD_ID = 0l;
    private ProgressDialog mProgressDialog;
    //是否下载完成
    private boolean isDownloadCompleted;
    private AlertDialog mUpdateDialog;
    private AppUpdateSDK updateSDK;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        updateSDK = AppUpdateSDK.getInstance(this);

        mUpdateInfo = updateSDK.getUpdateInfo();

        dirName = getPackageName();

        APK_NAME = updateSDK.getAppName(this).concat("V").concat(mUpdateInfo.versionName).concat(".apk"); // ex:手机百度V1.1.1.apk

        checkWritePermission();
    }


    /**
     * 检查存储权限
     */
    private void checkWritePermission() {
        int mWriteStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (Build.VERSION.SDK_INT >= 23) {
            if (mWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.
                        requestPermissions(UpdateDialogActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_CODE_WRITE_STORAGE);
            } else {
                initFileDir();

                checkTxtInfo();
            }

        } else {
            initFileDir();

            checkTxtInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initFileDir();

            checkTxtInfo();
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage("为了应用的正常使用，请前往设置中开启存储权限!");
            builder.setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    builder.create().dismiss();
                    finish();
                }
            });

            builder.setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent();
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT > 20) {
                        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                        startActivityForResult(intent, REQUEST_CODE_PERMISSION);
                    } else {
                        Intent intent1 = new Intent(Settings.ACTION_SETTINGS);
                        startActivityForResult(intent1, REQUEST_CODE_PERMISSION);
                    }
                }
            });
            AlertDialog alertDialog1 = builder.create();
            alertDialog1.setCancelable(false);
            alertDialog1.show();
            alertDialog1.setCanceledOnTouchOutside(false);
        }
    }

    /**
     * 创建文件夹
     */
    private void initFileDir() {
        //创建文件夹
        mUpdateFileDir = UpdateFileUtil.createFileDir(this, dirName);
        boolean fileExists = UpdateFileUtil.isFileExists(mUpdateFileDir, "appUpdateInfo.txt");
        LogUtil.w(TAG, "...fileExists:" + fileExists);

        if (fileExists) {
            mTxtFilePath = mUpdateFileDir.getAbsolutePath() + "/appUpdateInfo.txt";
            mTxtFile = new File(mTxtFilePath);
        } else {
            mTxtFile = UpdateFileUtil.createFile(this, mUpdateFileDir.getAbsolutePath(), "appUpdateInfo.txt");
            mTxtFilePath = mTxtFile.getAbsolutePath();

            try {
                UpdateFileUtil.saveToFile(mTxtFilePath, "updateVersion");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LogUtil.w(TAG, "initAppUpdateInfo--mTxtFilePath:" + mTxtFilePath);
        LogUtil.w(TAG, "initAppUpdateInfo...dirName:" + dirName + "--APK_NAME:" + APK_NAME);
    }

    /**
     * 检查本地忽略库是否存在服务器版本
     */
    private void checkTxtInfo() {
        try {
            String read = UpdateFileUtil.read(mTxtFilePath);
            LogUtil.w(TAG, "checkTxtInfo--read:" + read);
            if (read.contains(mUpdateInfo.versionName)) {
                LogUtil.w(TAG, "readTxtInfo--忽略库中存在服务器版本,,forceUpdate:" + mUpdateInfo.isForceUpdate);
                if (mUpdateInfo.isForceUpdate) {
                    findLocalApkFile();
                } else {
                    finish();
                }

            } else {
                //忽略库中不存在服务器版本
                LogUtil.w(TAG, "readTxtInfo--忽略库中不存在服务器版本");
                showUpdateDialog("版本更新", mUpdateInfo.updateDesc, mUpdateInfo.isForceUpdate);
            }

        } catch (IOException e) {
            //忽略库中不存在服务器版本
            LogUtil.w(TAG, "IOException-忽略库中不存在服务器版本");
            showUpdateDialog("版本更新", mUpdateInfo.updateDesc, mUpdateInfo.isForceUpdate);
            e.printStackTrace();
        }
    }

    /**
     * 本地搜索apk文件
     */
    private void findLocalApkFile() {
        LogUtil.w(TAG, "---findLocalApkFile---");
        boolean fileExists = UpdateFileUtil.isFileExists(mUpdateFileDir, APK_NAME);

        if (fileExists) {
            Toast.makeText(this, "发现本地有新版本，请安装", Toast.LENGTH_SHORT).show();

            mApkPath = mUpdateFileDir.getPath() + File.separator + APK_NAME;

            //安装apk
            installApkWithPath(this, mApkPath);
        } else {
            LogUtil.e(TAG, "apk--不存在，开始下载");
            //开始下载
            Toast.makeText(this, "发现有新版本，正在下载...", Toast.LENGTH_SHORT).show();

            beginDownloadApk();
        }
    }

    /**
     * 显示升级对话框
     */
    private void showUpdateDialog(String title, String message, final boolean isForceUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(mContext, "正在下载最新版本...", Toast.LENGTH_SHORT).show();

                saveVersionToTxt();
                beginDownloadApk();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isForceUpdate) {
                    dialog.dismiss();
                    finish();
                    System.exit(0);
                } else {
                    //把版本号存在忽略文件中
                    saveVersionToTxt();

                    dialog.dismiss();

                    finish();
                }
            }
        });

        mUpdateDialog = builder.create();
        mUpdateDialog.setCancelable(false);
        mUpdateDialog.show();
        mUpdateDialog.setCanceledOnTouchOutside(false);
        mUpdateDialog.setCancelable(false);
    }

    private void initBroadcastReceiver() {
        mUpdateReceiver = new UpdateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        registerReceiver(mUpdateReceiver, intentFilter);
    }


    /**
     * 开始下载apk
     */
    private void beginDownloadApk() {
        initBroadcastReceiver();

        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mUpdateInfo.appDownloadUrl));
        //下载后apk保存的路径及名字
        request.setDestinationInExternalPublicDir(dirName, APK_NAME);
        //允许被系统扫描到文件
        request.allowScanningByMediaScanner();

        if (AppUpdateSDK.isShowProgressInNotification) {
            //通知栏显示，只在下载过程中显示，下载完成后消失
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            //通知栏显示的title
            request.setTitle(updateSDK.getAppName(mContext));
            //描述
            request.setDescription(updateSDK.getAppName(mContext) + "最新版");
        } else {
            //通知栏显示，在下载过程中和下载完成后一直显示
            //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            //通知栏不显示下载进度
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        }

        //可以展示在系统下载UI中
        request.setVisibleInDownloadsUi(true);
        //移动网络情况下是否允许漫游
//        request.setAllowedOverRoaming(false);
        //设置mimeType
        request.setMimeType("application/vnd.android.package-archive");
        //开始下载
        DOWNLOAD_ID = mDownloadManager.enqueue(request);

        queryProgress(DOWNLOAD_ID);
    }

    /**
     * 查询进度
     *
     * @param downloadId
     */
    private void queryProgress(final long downloadId) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("正在更新...");
        mProgressDialog.setProgressNumberFormat("%1d MB/%2d MB");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);


        //使用RxJava对下载信息进行轮询，400毫秒一次
        Observable.interval(200, 400, TimeUnit.MILLISECONDS)
                .takeUntil(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        return isDownloadCompleted;
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong){
                        checkDownloadStatus(downloadId);
                    }
                });

    }

    private void checkDownloadStatus(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);//筛选下载任务，传入任务ID，可变参数
        Cursor cursor = mDownloadManager.query(query);
        if (cursor.moveToFirst()) {
            long downloadedBytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long totalBytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            mProgressDialog.setMax(((int) (totalBytes / 1024 / 1024)));
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                case DownloadManager.STATUS_RUNNING:
                    mProgressDialog.setProgress(((int) (downloadedBytes / 1024 / 1024)));
                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.show();
                    }
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    isDownloadCompleted = true;
                    if (mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    break;
            }
        }
        cursor.close();
    }

    /**
     * 把版本号存在忽略文件里
     */
    private void saveVersionToTxt() {
        try {
            String read = UpdateFileUtil.read(mTxtFilePath);
            LogUtil.w(TAG, "saveVersionToTxt---忽略库--read:" + read);
            if (read.contains(mUpdateInfo.versionName)) {
                LogUtil.w(TAG, "saveVersionToTxt---已经保存忽略库--read:" + read);
            } else {
                UpdateFileUtil.appendToFile(";" + mUpdateInfo.versionName, mTxtFile);
            }
        } catch (IOException e) {
            finish();
            e.printStackTrace();
        }
    }

    public class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.e(TAG, "-UpdateReceiver--onReceive----下载完成---");

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isAvailable()) {
                Toast.makeText(context, "请检查网络...", Toast.LENGTH_SHORT).show();
            }

            if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                //下载完成
                mProgressDialog.dismiss();
                long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //安装apk
                installApk(context, completeDownloadId);
            }
        }
    }

    /**
     * 安装APK
     *
     * @param context
     * @param completeDownloadId
     */
    public void installApk(Context context, long completeDownloadId) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = null;
        if (Build.VERSION.SDK_INT < 23) {
            DownloadManager dManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            apkUri = dManager.getUriForDownloadedFile(completeDownloadId);
            if (apkUri != null) {
                LogUtil.w(TAG, "installApk--apkUri:" + apkUri.toString());
            } else {
                LogUtil.w(TAG, "installApk--download error");
            }
        } else {
            File apkFile = new File(mUpdateFileDir.getAbsolutePath(), APK_NAME);
            apkUri = FileProvider.getUriForFile(mContext, "com.yzq.updatelibrary", apkFile);
        }

        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        if (Build.VERSION.SDK_INT < 23) {
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        LogUtil.w(TAG, "---安装APK---");
        startActivityForResult(install, 99);

    }

    /**
     * 安装APK
     *
     * @param context
     * @param apkPath
     */
    public void installApkWithPath(Context context, String apkPath) {
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = null;

            if (Build.VERSION.SDK_INT < 23) {
                File file = new File(apkPath);
                apkUri = Uri.fromFile(file);
            } else {
                File apkFile = new File(mUpdateFileDir.getAbsolutePath(), APK_NAME);
                apkUri = FileProvider.getUriForFile(mContext, "com.psbc.citizencard.fileProvider", apkFile);
            }

            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            if (Build.VERSION.SDK_INT < 23) {
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            install.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else {
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            install.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            LogUtil.w(TAG, "---installApkWithPath-安装APK---");
            startActivityForResult(install, 99);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.w(TAG, "--onActivityResult--:" + resultCode);
        if (requestCode == 99 && resultCode == RESULT_CANCELED) {  // RESULT_CANCELED = 0
            saveVersionToTxt();
            finish();
        }

        if (requestCode == REQUEST_CODE_PERMISSION) {
            //检查权限
            LogUtil.w(TAG, "--onActivityResult--requestCode:" + requestCode);
            checkWritePermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateReceiver != null){
            unregisterReceiver(mUpdateReceiver);
        }
    }
}
