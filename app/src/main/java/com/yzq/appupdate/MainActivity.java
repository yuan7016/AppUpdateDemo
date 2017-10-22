package com.yzq.appupdate;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.yzq.updatelibrary.AppUpdateSDK;
import com.yzq.updatelibrary.LogUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import java.io.IOException;
import okhttp3.Response;

/**
 * Created by YuanZhiQiang on 2017/10/20
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private Button button_update1;
    private Button button_update2;
    private AppUpdateSDK appUpdateSDK;

    private String apkUrl = "http://223.111.158.28/imtt.dd.qq.com" +
            "/16891/6277EB8FF33A5B50423837FC8603876B.apk?mkey=" +
            "59eb68787e87b3d1&f=8f5d&c=0&fsname=com.shere.assistivetouch_4.5.22_4052200.apk" +
            "&csr=1bbd&p=.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        /**
         *1:获取appUpdateSDK对象
         */
        appUpdateSDK = AppUpdateSDK.getInstance(this);

    }

    private void initView() {
        button_update1 = (Button) findViewById(R.id.button_update1);
        button_update2 = (Button) findViewById(R.id.button_update2);

        button_update1.setOnClickListener(this);
        button_update2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_update1: //普通升级
            //模拟网络请求
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        String response1 = getSync();
                        LogUtil.w("AppUpdate","response1:" + response1);
                    }
                }.start();


                //2:从服务器获取升级信息后,调用
                appUpdateSDK.beginDealUpdate(this,72,"7.2.0","更新内容：\n" +
                        "- 拍摄照片/短视频时，可按类型挑选挂件、滤镜，体验热门新潮特效；\n" +
                        "- 短视频/视频通话挂件新玩法，做特定手势可触发挂件动态效果，Get最潮手势语言；\n" +
                        "- 拍摄照片及短视频时可分六档调节美颜程度，轻松变美，一键搞定。",apkUrl,false);

                break;
            case R.id.button_update2://强制升级
             //模拟网络请求
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        String response2 = getSync();
                        LogUtil.w("AppUpdate","response2:" + response2);
                    }
                }.start();

                appUpdateSDK.beginDealUpdate(this,72,"7.2.0","更新内容：\n" +
                        "- 拍摄照片/短视频时，可按类型挑选挂件、滤镜，体验热门新潮特效；\n" +
                        "- 短视频/视频通话挂件新玩法，做特定手势可触发挂件动态效果，Get最潮手势语言；\n" +
                        "- 拍摄照片及短视频时可分六档调节美颜程度，轻松变美，一键搞定。",apkUrl,true);

                break;
        }
    }


    /**
     * 同步的get请求
     */
    public String getSync() {
        try {
            Response response =
                    OkHttpUtils.
                            get() // get请求
                            .url("https://www.baidu.com")
                            .build()
                            .execute();
            if (response.isSuccessful()) {
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
