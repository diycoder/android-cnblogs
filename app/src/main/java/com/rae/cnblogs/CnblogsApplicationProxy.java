package com.rae.cnblogs;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.tinker.TinkerApplicationLike;

/**
 * 应用程序
 * Created by ChenRui on 2016/12/1 21:35.
 */
public class CnblogsApplicationProxy extends TinkerApplicationLike {


    public CnblogsApplicationProxy(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag, long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            RaeImageLoader.clearCache(getApplication());
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);

        // 安装tinker
        Beta.installTinker(this);
    }
}
