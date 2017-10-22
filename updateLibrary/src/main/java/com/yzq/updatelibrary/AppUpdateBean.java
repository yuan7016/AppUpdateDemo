package com.yzq.updatelibrary;

/**
 * Created by Administrator on 2017/10/15 0015.
 */
public class AppUpdateBean {
    public int versionCode;
    public String versionName;
    public String updateDesc;
    public String appDownloadUrl;
    public boolean isForceUpdate;


    public AppUpdateBean(int versionCode, String versionName, String updateDesc, String appDownloadUrl, boolean isForceUpdate) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.updateDesc = updateDesc;
        this.appDownloadUrl = appDownloadUrl;
        this.isForceUpdate = isForceUpdate;
    }
}
