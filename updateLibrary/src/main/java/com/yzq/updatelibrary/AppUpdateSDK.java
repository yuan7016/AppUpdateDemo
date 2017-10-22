package com.yzq.updatelibrary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * Created by yzq on 2017/10/15 0015.
 */
public class AppUpdateSDK {
    private static final String TAG = "AppUpdate";

    private static AppUpdateSDK appUpdate;

    /**
     * 是否在通知栏显示下载进度,默认为false;
     */
    public static boolean isShowProgressInNotification = false;
    private AppUpdateBean appUpdateBean;
    private Activity mActivity;

    public static synchronized AppUpdateSDK getInstance(Activity activity){
        if (appUpdate == null){
            appUpdate = new AppUpdateSDK(activity);
        }

        return appUpdate;
    }

    private AppUpdateSDK(Activity mActivity) {
        this.mActivity = mActivity;
    }

    /**
     * 开始处理app升级
     *
     * @param context
     * @param versionCode   版本号 如：11
     * @param versionName   版本名称 如：1.1.1
     * @param updateDesc    升级说明
     * @param apkUrl        apk下载链接
     * @param isForceUpdate 是否是强制升级
     */
    public void beginDealUpdate(Context context, int versionCode, String versionName, String updateDesc, String apkUrl, boolean isForceUpdate){
        if (TextUtils.isEmpty(versionName) || TextUtils.isEmpty(updateDesc) || TextUtils.isEmpty(apkUrl)){
            throw new NullPointerException("params can not be null");
        }

        AppUpdateBean updateInfo = new AppUpdateBean(versionCode, versionName, updateDesc, apkUrl, isForceUpdate);

        saveUpdateInfo(updateInfo);

        String localVersionName = getLocalVersionName(context);

        int compareVersion = compareVersionName(versionName, localVersionName);
        //0 ,-1 不升级  1升级
        if (compareVersion == 1){

            Intent intent = new Intent(mActivity, UpdateDialogActivity.class);
            mActivity.startActivity(intent);

        }else {
            LogUtil.w(TAG,"---已经是最新版本");
            return;
        }

    }

    /**
     * 设置 是否在通知栏显示下载进度
     * @param isShowProgressInNotification
     */
    public void setIsShowProgressInNotification(boolean isShowProgressInNotification){
        isShowProgressInNotification = isShowProgressInNotification;
    }

    private void saveUpdateInfo(AppUpdateBean updateInfo){
        if (updateInfo != null){
            appUpdateBean = updateInfo;
        }
    }

    public AppUpdateBean getUpdateInfo(){
        return appUpdateBean;
    }

    /***
     * 根据版本名称比对应用版本
     * @param versionName1
     * @param versionName2
     * @return 0:版本相等; 1:versionName1 > versionName2; -1:versionName1 小于 versionName2
     *
     */
    private int compareVersionName(String versionName1, String versionName2) {
        if (versionName1.equals(versionName2)) {
            return 0;
        }

        String[] version1Array = versionName1.split("\\.");
        String[] version2Array = versionName2.split("\\.");

        int index = 0;
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;

        while (index < minLen && (diff = Integer.parseInt(version1Array[index]) - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }

        if (diff == 0) {
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }

            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    /**
     * 根据版本号比较
     * @param versionCodeFromNet
     * @param localVersionCode
     * @return 0:版本相等; 1:服务器版本 > 本地版本; -1:服务器版本 本地版本
     */
    private int compareVersionCode(int versionCodeFromNet,int localVersionCode){
        if (versionCodeFromNet == localVersionCode){
            return 0;
        }else if (versionCodeFromNet > localVersionCode){
            return 1;
        }else {
            return -1;
        }
    }

    /**
     * 获取本地版本名称
     *
     * @param context
     * @return versionName
     */
    private String getLocalVersionName(Context context) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = context.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            localVersion = packageInfo.versionName;

            LogUtil.w(TAG, "---getLocalVersionName:" + localVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 获取应用名称
     * @param context
     * @return
     */
    public String getAppName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }

        String applicationName =
                (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

}
