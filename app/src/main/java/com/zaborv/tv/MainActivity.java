package com.zaborv.tv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
        // RenderPriority is deprecated on newer WebView releases, but remains useful on
        // the Android 11 WebView shipped with the target Formuler hardware.
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
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
                + "style.textContent='::-webkit-scrollbar{display:none!important;width:0!important;height:0!important;}"
                + "a:focus,button:focus,[tabindex]:focus{outline:none!important;"
                + "box-shadow:0 0 10px 3px #E50914!important;border-radius:4px!important;}';"
                + "(document.head||document.documentElement).appendChild(style);"
                + "var labels=['poster','synopsis','regarder','nouveautés','tendances'];"
                + "document.querySelectorAll('a,button,[role=\\\"button\\\"],nav *').forEach(function(el){"
                + "var label=(el.textContent||'').trim().toLowerCase();"
                + "if(labels.indexOf(label)!==-1){var target=el.closest('a,button,[role=\\\"button\\\"]')||el;"
                + "if(!target.hasAttribute('tabindex'))target.tabIndex=0;}});"
                + "function fullscreen(video){if(!video)return;"
                + "var request=video.requestFullscreen||video.webkitRequestFullscreen;"
                + "if(request&&!document.fullscreenElement&&!document.webkitFullscreenElement){"
                + "try{var result=request.call(video);if(result&&result.catch)result.catch(function(){});}catch(ignore){}}}"
                + "if(!window.__zaborvVideoHandlers){window.__zaborvVideoHandlers=true;"
                + "document.addEventListener('play',function(e){if(e.target&&e.target.tagName==='VIDEO')"
                + "fullscreen(e.target);},true);"
                + "document.addEventListener('click',function(e){var item=e.target.closest('a,button,[role=\\\"button\\\"]');"
                + "if(item&&(item.textContent||'').trim().toLowerCase()==='regarder')"
                + "setTimeout(function(){fullscreen(document.querySelector('video'));},0);},true);}"
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
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Android can reveal system chrome while the video surface is changing. Reapply
        // immersive sticky mode as soon as the fullscreen window regains focus.
        if (hasFocus && customView != null) {
            setImmersiveMode(true);
        }
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
