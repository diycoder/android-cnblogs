package com.rae.cnblogs.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.rae.cnblogs.R;
import com.rae.cnblogs.RaeImageLoader;
import com.rae.cnblogs.presenter.CnblogsPresenterFactory;
import com.rae.cnblogs.presenter.ILauncherPresenter;
import com.rae.cnblogs.sdk.bean.Blog;

import butterknife.BindView;

/**
 * 启动页
 * Created by ChenRui on 2016/12/22 22:08.
 */
public class LauncherActivity extends BaseActivity implements ILauncherPresenter.ILauncherView {

    @BindView(R.id.img_launcher_display)
    ImageView mDisplayView;

    ILauncherPresenter mLauncherPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        bindView();
        mLauncherPresenter = CnblogsPresenterFactory.getLauncherPresenter(this, this);
        mLauncherPresenter.start();
    }

    private void showImage(String url) {
        ImageLoader.getInstance().displayImage(url, mDisplayView, RaeImageLoader.fadeOptions(800).build(), new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.ad_scale_fade_in));
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        });
    }

    @Override
    public void onLoadImage(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        showImage(url);
    }

    @Override
    public void onJumpToWeb(String url) {

    }

    @Override
    public void onJumpToBlog(Blog blog) {

    }

    @Override
    public void onNormalImage() {

    }
}