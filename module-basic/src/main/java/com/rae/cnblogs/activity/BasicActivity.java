package com.rae.cnblogs.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.rae.cnblogs.AppStatusBar;
import com.rae.cnblogs.ApplicationCompat;
import com.rae.cnblogs.RxObservable;
import com.rae.cnblogs.basic.R;
import com.rae.cnblogs.sdk.config.CnblogAppConfig;
import com.rae.swift.Rx;
import com.umeng.analytics.MobclickAgent;

import java.util.List;

import butterknife.ButterKnife;

/**
 * 基类
 * Created by ChenRui on 2016/12/1 21:35.
 */
public abstract class BasicActivity extends AppCompatActivity {

    protected View mBackView;
    protected Toolbar mToolBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        onStatusBarColorChanged();
        super.onCreate(savedInstanceState);
    }

    protected void onStatusBarColorChanged() {
        AppStatusBar.setStatusbarToDark(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxObservable.dispose();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
        mBackView = findViewById(R.id.back);
        if (mBackView != null) {
            mBackView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackClick(v);
                }
            });
        }
        View toolBar = findViewById(R.id.title_tool_bar);
        if (toolBar != null) {
            mToolBar = (Toolbar) toolBar;
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                setTitle(title);
            }
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mToolBar != null) {
            TextView titleView = mToolBar.findViewById(R.id.tv_title);
            if (titleView != null) {
                titleView.setText(title);
            }
        }
    }

    protected void showHomeAsUp(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            // 设置返回按钮
            if (getHomeAsUpIndicator() != 0) {
                bar.setHomeAsUpIndicator(getHomeAsUpIndicator());
            }
            bar.setDisplayShowHomeEnabled(false);
            bar.setDisplayShowTitleEnabled(false);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        }
    }

    protected void showHomeAsUp() {
        showHomeAsUp(mToolBar);
    }


    /**
     * 返回按钮的图片
     */
    protected int getHomeAsUpIndicator() {
        return 0;
    }

    protected BasicActivity getContext() {
        return this;
    }

    public void onBackClick(View view) {
        onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 721 && resultCode == RESULT_OK) {
            onLoginCallBack();
        }
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (!Rx.isEmpty(fragments)) {
            for (Fragment fragment : fragments) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * 跳登录的时候回调
     */
    protected void onLoginCallBack() {
    }

    public int getVersionCode() {
        return ApplicationCompat.getVersionCode(getApplicationContext());
    }

    public String getVersionName() {
        return ApplicationCompat.getVersionName(getApplicationContext());
    }

    /**
     * 获取渠道包
     */
    public String getChannel() {
        return ApplicationCompat.getChannel(getApplicationContext());
    }

    protected CnblogAppConfig config() {
        return CnblogAppConfig.getsInstance(getContext().getApplicationContext());
    }

}
