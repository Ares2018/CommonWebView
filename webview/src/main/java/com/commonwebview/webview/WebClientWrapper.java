package com.commonwebview.webview;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;

import port.WebviewCBHelper;
import webutils.ClickTrackerUtils;
import webutils.CssJsUtils;

/**
 * 通用webview
 *
 * @author wanglinjie
 * @date 2018/6/20 09:44.
 */
class WebClientWrapper extends WebViewClient {

    private WebViewClient webViewClient;
    private WebviewCBHelper helper;
    private boolean isRedirect; // true : 重定向

    public WebClientWrapper(WebviewCBHelper helper) {
        super();
        this.helper = helper;
    }

    private WebClientWrapper() {
    }

    public WebViewClient getWrapper() {
        return webViewClient;
    }

    public void setWrapper(WebViewClient webViewClient) {
        this.webViewClient = webViewClient;
    }

    //url加载拦截
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                && webViewClient != null
                && webViewClient.shouldOverrideUrlLoading(view, url)) {
            return true;
        }

        if (!TextUtils.isEmpty(url)) {
            //电话、邮件、短信之类
            if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                view.getContext().startActivity(intent);
                return true;
            }

            Uri uri = Uri.parse(url);
            if (!TextUtils.equals(uri.getScheme(), "http") && !TextUtils.equals(uri.getScheme(), "https")) {
                return true;
            }
            if (isRedirect) { // 重定向 链接稿重定向需要跳转
                if (helper != null) {
                    if (!helper.DoNavLinkUrl(url)) {
                        view.loadUrl(url);
                        return true;
                    } else {
                        helper.shouldOverrideUrlLoading(view, url);
                        return true;
                    }
                } else {
                    view.loadUrl(url);
                    return true;
                }
            } else { // 点击跳转
                if (ClickTrackerUtils.isDoubleClick()) return true;
                if (helper != null) {
                    helper.shouldOverrideUrlLoading(view, url);
                    return true;
                }
            }
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (webViewClient != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && webViewClient.shouldOverrideUrlLoading(view, request)) {
            return true;
        }

        if (!TextUtils.isEmpty(request.getUrl().toString())) {
            //电话、邮件、短信之类
            if (request.getUrl().toString().startsWith("tel:") || request.getUrl().toString().startsWith("sms:") || request.getUrl().toString().startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                view.getContext().startActivity(intent);
                return true;
            }

            Uri uri = request.getUrl();
            if (!TextUtils.equals(uri.getScheme(), "http") && !TextUtils.equals(uri.getScheme(), "https")) {
                return true;
            }
            if (isRedirect) { // 重定向
                if (helper != null) {
                    if (!helper.DoNavLinkUrl(request.getUrl().toString())) {
                        view.loadUrl(request.getUrl().toString());
                        return true;
                    } else {
                        helper.shouldOverrideUrlLoading(view, request.getUrl().toString());
                        return true;
                    }
                } else {
                    view.loadUrl(request.getUrl().toString());
                    return true;
                }
            } else { // 点击跳转
                if (ClickTrackerUtils.isDoubleClick()) return true;
                if (helper != null) {
                    helper.shouldOverrideUrlLoading(view, request.getUrl().toString());
                    return true;
                }
            }
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (webViewClient != null) {
            webViewClient.onPageStarted(view, url, favicon);
        }
        if (helper != null) {
            helper.onPageStarted(view, url, favicon);
        }
        super.onPageStarted(view, url, favicon);
        isRedirect = true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (webViewClient != null) {
            webViewClient.onPageFinished(view, url);
        }
        if (helper != null) {
            helper.onPageFinished(view, url);
        }
        super.onPageFinished(view, url);
        isRedirect = false;
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        if (webViewClient != null) {
            webViewClient.onLoadResource(view, url);
        }
        super.onLoadResource(view, url);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onPageCommitVisible(WebView view, String url) {
        if (webViewClient != null) {
            webViewClient.onPageCommitVisible(view, url);
        }
        super.onPageCommitVisible(view, url);
    }

    //这里有特殊逻辑就自己重写方法
    //该方法在非UI线程中运行，可以在这里做网络访问等耗时操作
    //不适合在onPageFinished中注入JS/CSS，也闪一下(重新加载)
    //兼容5.0以下的链接稿CSS/JS注入
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        WebResourceResponse response = null;
        //省流量模式操作
        //原生需要单独注入css.js
        if (helper != null && helper.isProvinTrafficMode() && !helper.isBrowserLink()) {
            return helper.doProvinTraffic(url);
        }
        //TODO WLJ 外链稿注入js,且没有注入成功
        //注入相关的css和js，只能注入一次
        //先解析html，再做图片处理
        else if (helper != null && helper.isBrowserLink() && !TextUtils.isEmpty(helper.getWebViewJsObject()) && !CssJsUtils.get(view.getContext()).isInject()) {
            String page = CssJsUtils.get(view.getContext()).getUrlData(helper, null, url, "null", "js/basic.js");
            return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream(page.getBytes()));
        } else {

            //屏蔽快应用跳转 例如:微博中的跳转 <img src="http://thefatherofsalmon.com/?i=com.sina.weibo.quickapp&amp;p=WBDetail&amp;a=blog_id%3D4376329367123399%26luicode%3D10000835%26lfid%3D_detail" style="width: 1px; height: 1px; display: none;">
            if (!TextUtils.isEmpty(url)) {
                Uri uri = Uri.parse(url);
                if (uri != null && TextUtils.equals(uri.getHost(), "thefatherofsalmon.com")) {
                    return new WebResourceResponse("image/png", "UTF-8", null);
                }
            }

            if (webViewClient != null) {
                return webViewClient.shouldInterceptRequest(view, url);
            }
            return super.shouldInterceptRequest(view, url);
        }
    }

    //是否需要替换图片的占位图
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest
            request) {
        //注入相关的css和js，只能注入一次
        if (helper != null && helper.isProvinTrafficMode()) {
            return helper.doProvinTraffic(request.getUrl().toString());
        } else if (helper != null && helper.isBrowserLink() && !TextUtils.isEmpty(helper.getWebViewJsObject()) && !CssJsUtils.get(view.getContext()).isInject()) {
            String page = CssJsUtils.get(view.getContext()).getUrlData(helper, request, CookieManager.getInstance().getCookie(request.getUrl().toString()), "null", "js/basic.js");
            return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream(page.getBytes()));
        } else {
            if (webViewClient != null) {
                return webViewClient.shouldInterceptRequest(view, request);
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    @Override
    public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
        if (webViewClient != null) {
            webViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
        } else {
            super.onTooManyRedirects(view, cancelMsg, continueMsg);
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String
            failingUrl) {
        if (webViewClient != null) {
            webViewClient.onReceivedError(view, errorCode, description, failingUrl);
        }
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError
            error) {
        if (webViewClient != null) {
            webViewClient.onReceivedError(view, request, error);
        }
        super.onReceivedError(view, request, error);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                    WebResourceResponse errorResponse) {
        if (webViewClient != null) {
            webViewClient.onReceivedHttpError(view, request, errorResponse);
        }
        super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        if (webViewClient != null) {
            webViewClient.onFormResubmission(view, dontResend, resend);
        } else {
            super.onFormResubmission(view, dontResend, resend);
        }
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        if (webViewClient != null) {
            webViewClient.doUpdateVisitedHistory(view, url, isReload);
        }
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    /**
     * 对于部分网页证书报错的问题
     *
     * @param view
     * @param handler
     * @param error
     */
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // 建议
        handler.proceed(); // 针对https，忽略证书错误，相当于信任所有证书
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
        if (webViewClient != null) {
            webViewClient.onReceivedClientCertRequest(view, request);
        } else {
            super.onReceivedClientCertRequest(view, request);
        }
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host,
                                          String realm) {
        if (webViewClient != null) {
            webViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
        } else {
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
        }
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        if (webViewClient != null && webViewClient.shouldOverrideKeyEvent(view, event)) {
            return true;
        }
        return super.shouldOverrideKeyEvent(view, event);
    }

    @Override
    public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
        if (webViewClient != null) {
            webViewClient.onUnhandledKeyEvent(view, event);
        } else {
            super.onUnhandledKeyEvent(view, event);
        }
    }

    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        if (webViewClient != null) {
            webViewClient.onScaleChanged(view, oldScale, newScale);
        }
        super.onScaleChanged(view, oldScale, newScale);
    }

    @Override
    public void onReceivedLoginRequest(WebView view, String realm, String account, String
            args) {
        if (webViewClient != null) {
            webViewClient.onReceivedLoginRequest(view, realm, account, args);
        }
        super.onReceivedLoginRequest(view, realm, account, args);
    }
}
