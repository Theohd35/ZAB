package com.zaborv.tv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://zaborv.com/o81vqqzrvs9a75/home/zaborv";
    private static final int IMMERSIVE_FLAGS = View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    private WebView myWebView;
    private FrameLayout rootView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootView = new FrameLayout(this);
        rootView.setBackgroundColor(Color.BLACK);
        myWebView = new WebView(this);
        rootView.addView(myWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(rootView);

        configureWebView();
        if (savedInstanceState == null) {
            myWebView.loadUrl(HOME_URL);
        } else {
            myWebView.restoreState(savedInstanceState);
        }
    }

    private void configureWebView() {
        myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        myWebView.setVerticalScrollBarEnabled(false);
        myWebView.setHorizontalScrollBarEnabled(false);
        myWebView.setFocusable(true);
        myWebView.setFocusableInTouchMode(true);
        myWebView.requestFocus();

        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectTvNavigationStyles(view);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                myWebView.setVisibility(View.GONE);
                rootView.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                view.requestFocus();
                setImmersiveMode(true);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });
    }

    private void injectTvNavigationStyles(WebView view) {
        String javascript = "(function(){"
                + "var id='zaborv-tv-styles';"
                + "var old=document.getElementById(id);if(old){old.remove();}"
                + "var style=document.createElement('style');style.id=id;"
                + "style.textContent='html{scroll-behavior:smooth!important;}"
                + "::-webkit-scrollbar{display:none!important;width:0!important;height:0!important;}"
                + "a:focus,button:focus,[tabindex]:focus,.card:focus,.movie-item:focus{"
                + "outline:4px solid #00FF88!important;outline-offset:3px!important;"
                + "transform:scale(1.05)!important;transition:transform .2s ease,outline .2s ease!important;"
                + "z-index:9999!important;}';"
                + "(document.head||document.documentElement).appendChild(style);"
                + "})();";
        view.evaluateJavascript(javascript, null);
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        rootView.removeView(customView);
        customView = null;
        myWebView.setVisibility(View.VISIBLE);
        myWebView.requestFocus();
        setImmersiveMode(false);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }

    private void setImmersiveMode(boolean enabled) {
        getWindow().getDecorView().setSystemUiVisibility(enabled ? IMMERSIVE_FLAGS : View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customView != null) {
                hideCustomView();
                return true;
            }
            if (myWebView.canGoBack()) {
                myWebView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        myWebView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (customView != null) {
            hideCustomView();
        }
        rootView.removeView(myWebView);
        myWebView.stopLoading();
        myWebView.setWebChromeClient(null);
        myWebView.setWebViewClient(null);
        myWebView.destroy();
        super.onDestroy();
    }
}
