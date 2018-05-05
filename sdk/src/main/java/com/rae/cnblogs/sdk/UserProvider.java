package com.rae.cnblogs.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.rae.cnblogs.sdk.bean.UserInfoBean;
import com.rae.cnblogs.sdk.config.CnblogAppConfig;
import com.rae.swift.session.SessionManager;

import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.annotations.Nullable;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.JavaNetCookieJar;

/**
 * 用户提供者，保存用户信息，对已登录的用户进行操作。
 * <pre>
 * 使用前请先调用{@link #init(Context)}方法进行初始化
 * </pre>
 * Created by ChenRui on 2017/2/3 0003 15:58.
 */
public final class UserProvider {

    private static UserProvider sUserProvider;
    private final Context mContext;
    private UserInfoBean mUserInfo;
    private CnblogAppConfig mConfig;

    public static UserProvider getInstance() {
        if (sUserProvider == null) {
            throw new NullPointerException("用户提供程序没有初始化！");
        }
        return sUserProvider;
    }

    public static void init(Context applicationContext) {
        if (sUserProvider == null) {
            sUserProvider = new UserProvider(applicationContext);
        }
    }

    public UserProvider(Context context) {
        mContext = context;
        mConfig = CnblogAppConfig.getsInstance(context);
    }


    /**
     * 保存当前用户信息，一般在登录的时候调用该方法
     *
     * @param userInfo 用户信息
     */
    public void setLoginUserInfo(UserInfoBean userInfo) {
        this.mUserInfo = userInfo;
        // 保存到偏好中去
        if (userInfo != null) {
            mConfig.saveUserInfo(userInfo);
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息视图
     */
    @Nullable
    public UserInfoBean getLoginUserInfo() {
        // 如果用户为空，从偏好中获取
        if (mUserInfo == null) {
            mUserInfo = mConfig.getUserInfo();
        }
        return mUserInfo;
    }

    /**
     * 是否登录
     */
    public boolean isLogin() {
        UserInfoBean info = getLoginUserInfo();
        return info != null && !TextUtils.isEmpty(info.getUserId());
    }

    /**
     * 退出登录，释放资源
     */
    public void logout() {
        mUserInfo = null;

        SessionManager.getDefault().clear(); // 清除用户信息

        // 移除所有的cookie
        CookieSyncManager.createInstance(mContext);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();

        CookieHandler cookieHandler = java.net.CookieManager.getDefault();
        if (cookieHandler != null && cookieHandler instanceof java.net.CookieManager) {
            ((java.net.CookieManager) cookieHandler).getCookieStore().removeAll();
        }

        // 清除保存的登录信息
        mConfig.clearUserInfo();

    }


    /**
     * 同步Cookie：JavaNetCookieJar covert to WebKit CookieManger
     */
    public void cookieJar2CookieManager() {

        if (java.net.CookieManager.getDefault() == null) {
            return;
        }

        String url = "http://cnblogs.com";
        JavaNetCookieJar cookieJar = new JavaNetCookieJar(java.net.CookieManager.getDefault());
        List<Cookie> cookies = cookieJar.loadForRequest(HttpUrl.parse(url));
        if (cookies != null) {
            // 同步接口的cookie达到同步web登陆
            CookieSyncManager.createInstance(mContext);
            CookieManager cookieManager = CookieManager.getInstance();
            for (Cookie cookie : cookies) {
                cookieManager.setCookie(url, cookie.toString());
            }
            CookieSyncManager.getInstance().sync();
        }

    }


    /**
     * 同步Cookie：WebKit CookieManger covert to JavaNetCookieJar
     */
    public void cookieManager2CookieJar() {
        // 同步接口的cookie达到同步web登陆
        CookieManager cookieManager = CookieManager.getInstance();
        String webCookies = cookieManager.getCookie("http://cnblogs.com");
        cookieManager2CookieJar(webCookies);
    }

    public void cookieManager2CookieJar(String webCookies) {
        if (java.net.CookieManager.getDefault() == null || TextUtils.isEmpty(webCookies)) {
            return;
        }
        JavaNetCookieJar cookieJar = new JavaNetCookieJar(java.net.CookieManager.getDefault());
        String url = "http://cnblogs.com";
        List<Cookie> cookies = new ArrayList<>();
        String[] texts = webCookies.split(";");
        HttpUrl httpUrl = HttpUrl.parse(url);
        // 解析字符串
        for (String text : texts) {
            if (TextUtils.isEmpty(text)) continue;
            text = text.trim(); // 去掉多余的空格
            if (!text.endsWith(";")) {
                text += ";";
            }
            text += " domain=.cnblogs.com; path=/; HttpOnly";
            Cookie cookie = Cookie.parse(httpUrl, text);
            cookies.add(cookie);
        }

        // 保存cookie
        cookieJar.saveFromResponse(httpUrl, cookies);
    }

    /**
     * 校验是否为当前登录用户
     */
    public boolean checkIsMe(String blogApp) {
        return isLogin() && TextUtils.equals(blogApp, getLoginUserInfo().getBlogApp());
    }

//    public void debugLogin() {
//        UserInfoBean userInfo = new UserInfoBean();
//        userInfo.setAvatar("http://pic.cnblogs.com/avatar/446312/20170124105915.png");
//        userInfo.setBlogApp("chenrui7");
//        userInfo.setDisplayName("RAE");
//        userInfo.setUserId("fdeed5f3-11fb-e111-aa3f-842b2b196315");
//        URI url = URI.create("http://www.cnblogs.com");
//        List<String> cookieStrings = new ArrayList<>();
//        cookieStrings.add(".CNBlogsCookie=3479CA4E4D0D34A70D41CA7F641FF395B033357076EB7D40B6E885781771F4424EAD83FFC60C1FC60AE7C4C8265F873A3FE0F67FF068D9607DFA23C8B9A586082F6F524218CCCA684051FE18989643BF8D591927; domain=.cnblogs.com; path=/; HttpOnly");
//        cookieStrings.add(".Cnblogs.AspNetCore.Cookies=CfDJ8PhlBN8IFxtHhqIV3s0LCDnwRFoSQTC7lIE6RZakFxsC4zQjlWxND689FLk6p708fPrjiF36MbSGlbg2IoFc-C4_jVIXfWqyOHigX8-o6v2LmAzDpaos_027aT5ufroTJ3fLnL6TEadGIMlU27eT537As8KTgZ-g06478rwL0N99IJMOsuPsO-4eji0DX5zCoKugWJDlHiZbLCcKpyP-OEEzFpOgUjrmOVXW70phIpQJjlb0DSDPoRrjF6_2lL0_EHkCmRFqPsW1bsgWRUWVHUC-AD65TNvckNj4w8YDbDBf; domain=.cnblogs.com; path=/; HttpOnly");
//
//
//        Map<String, List<String>> map = Collections.singletonMap("Set-Cookie", cookieStrings);
//        CookieHandler cookieManager = java.net.CookieManager.getDefault();
//        try {
//            cookieManager.put(url, map);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        UserProvider.getInstance().setLoginUserInfo(userInfo);
//    }
}
