package com.rae.cnblogs.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.rae.cnblogs.AppRoute;
import com.rae.cnblogs.fragment.WebLoginFragment;
import com.rae.cnblogs.fragment.WebViewFragment;

/**
 * 网页版登录
 * Created by ChenRui on 2017/2/3 0003 10:47.
 */
@Route(path = AppRoute.PATH_WEB_LOGIN)
public class WebLoginActivity extends WebActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShareView.setVisibility(View.GONE);
        mWebViewFragment.enablePullToRefresh(false);
    }

    @Override
    protected WebViewFragment getWebViewFragment(String url) {
        return WebLoginFragment.newInstance(url);
    }



    @Override
    @NonNull
    protected String getUrl() {
        return "https://passport.cnblogs.com/user/signin";
    }

}
