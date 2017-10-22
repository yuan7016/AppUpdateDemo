package com.yzq.updatelibrary;

import android.util.Log;

/**
 * Created by yzq on 2017/10/15 0015.
 */
public class LogUtil {
    private static final boolean isDebug = true;

    public static void d(String tag,String content){
        if (isDebug){
            Log.d(tag,content);
        }
    }

    public static void w(String tag,String content){
        if (isDebug){
            Log.w(tag,content);
        }
    }

    public static void e(String tag,String content){
        if (isDebug){
            Log.e(tag,content);
        }
    }
}
