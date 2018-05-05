package com.rae.cnblogs.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.rae.cnblogs.AppUI;
import com.rae.cnblogs.BuildConfig;
import com.rae.cnblogs.R;
import com.rae.cnblogs.ThemeCompat;
import com.rae.cnblogs.widget.AppLayout;
import com.rae.cnblogs.widget.PlaceholderView;
import com.rae.cnblogs.widget.RaeWebView;
import com.rae.cnblogs.widget.webclient.RaeJavaScriptBridge;
import com.rae.cnblogs.widget.webclient.RaeWebChromeClient;
import com.rae.cnblogs.widget.webclient.RaeWebViewClient;

import java.io.File;

import butterknife.BindView;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;

/**
 * 网页查看
 * Created by ChenRui on 2016/12/27 23:07.
 */
public class WebViewFragment extends BasicFragment {

    private String mUrl;
    private String mRawUrl;
    private RaeJavaScriptBridge mJavaScriptApi;
    private WebViewClient mRaeWebViewClient;
    private boolean mEnablePullToRefresh = true;
//    private JavaNetCookieJar mJavaNetCookieJar;


    public static WebViewFragment newInstance(String url) {

        Bundle args = new Bundle();
        args.putString("url", url);
        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // @BindView(R.id.web_view_blog_content)
    RaeWebView mWebView;

    @BindView(R.id.content)
    FrameLayout mContentLayout;

    @BindView(R.id.pb_web_view)
    ProgressBar mProgressBar;


    @BindView(R.id.ptr_content)
    AppLayout mAppLayout;

    @Override
    protected int getLayoutId() {
        return R.layout.fm_web;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);


        mWebView = new RaeWebView(getContext().getApplicationContext());
        mWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mContentLayout.addView(mWebView);


        // 夜间模式
        if (ThemeCompat.isNight()) {
            mWebView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.white_night));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // 下载监听
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    AppUI.failed(getContext(), "下载文件错误");
                }
            }
        });

        mAppLayout.setEnabled(mEnablePullToRefresh);
        mAppLayout.setPtrHandler(new PtrDefaultHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout ptrFrameLayout) {
                mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE); // 下拉刷新禁止使用缓存
                mWebView.reload(); // 刷新WebView
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                // 是否处于顶部
                return mProgressBar.getVisibility() != View.VISIBLE && !mWebView.canScrollVertically(-1) && super.checkCanDoRefresh(frame, content, header);
            }
        });

        return view;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mJavaScriptApi = new RaeJavaScriptBridge(getContext());
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);


        File cacheDir = getContext().getExternalCacheDir();

        if (cacheDir != null && cacheDir.canRead() && cacheDir.canWrite()) {
            settings.setAppCacheEnabled(true);
            settings.setAppCachePath(cacheDir.getPath());
        }


        mRaeWebViewClient = getWebViewClient();
        mWebView.addJavascriptInterface(getJavascriptApi(), "app");
        mWebView.setWebChromeClient(getWebChromeClient());
        mWebView.setWebViewClient(mRaeWebViewClient);

        if (mWebView != null && mUrl != null) {
            loadUrl(mUrl);
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mRawUrl = getArguments().getString("url");
            mUrl = mRawUrl;
        }
    }

    public String getRawUrl() {
        return mRawUrl;
    }

    @Override
    public void onDestroy() {
        if (mContentLayout != null) {
            mContentLayout.removeAllViews();
        }
        if (mAppLayout != null) {
            mAppLayout.removeAllViews();
        }
        if (mRaeWebViewClient != null && mRaeWebViewClient instanceof RaeWebViewClient) {
            ((RaeWebViewClient) mRaeWebViewClient).destroy();
        }
        if (mWebView != null) {
            mWebView.removeAllViews();
            mWebView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() == null) return;

        // 点击标题返回顶部
        View titleView = getActivity().findViewById(R.id.tv_web_title);
        if (titleView != null) {
            titleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mWebView.scrollTo(0, 0);
                }
            });
        }

        PlaceholderView placeholderView = (PlaceholderView) getActivity().findViewById(R.id.placeholder_web);
        if (placeholderView != null && mRaeWebViewClient != null && mRaeWebViewClient instanceof RaeWebViewClient) {
            placeholderView.dismiss();
            placeholderView.setOnRetryClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mWebView.reload();
                }
            });
            ((RaeWebViewClient) mRaeWebViewClient).setPlaceHolderView(placeholderView);
        }

    }

    public String getUrl() {
        return mWebView.getUrl();
    }

    /**
     * 获取网页内容
     *
     * @return
     */
    public String getContent() {
        return mJavaScriptApi.getHtml();
    }

    public WebChromeClient getWebChromeClient() {
        return new RaeWebChromeClient(mProgressBar);
    }

    public WebViewClient getWebViewClient() {
        return new RaeWebViewClient(mProgressBar, mAppLayout);
    }

    public Object getJavascriptApi() {
        return mJavaScriptApi;
    }

    public void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    public void reload() {
        mWebView.reload();
    }

    public void enablePullToRefresh(boolean enable) {
        if (mAppLayout != null) {
            mAppLayout.setEnabled(enable);
        } else {
            mEnablePullToRefresh = enable;
        }
    }
}
