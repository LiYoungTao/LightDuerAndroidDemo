package com.baidu.lightduer.sample;

import android.app.Application;

import com.baidu.lightduer.lib.util.LogUtil;


/**
 * <p>
 * Created by lishun02@baidu.com on 2017/9/11.
 */

public class LightDuerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.setDEBUG(true);
    }
}
