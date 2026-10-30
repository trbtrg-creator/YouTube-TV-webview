package com.baotruong.yttv;

import android.app.Activity;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    // Giao diện YouTube TV (leanback) - điều khiển hoàn toàn bằng remote
    private static final String URL = "https://www.youtube.com/tv";
    // User-Agent của smart TV để YouTube trả về giao diện TV
    private static final String UA = "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.5) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) 69.0.3497.106.1 TV Safari/537.36";

    private WebView web;
    private FrameLayout root;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        createWebView();
        web.loadUrl(URL);
    }

    private void createWebView() {
        web = new WebView(this);
        web.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(UA);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient() {
            private View custom;
            private CustomViewCallback cb;
            @Override public void onShowCustomView(View v, CustomViewCallback c) {
                custom = v; cb = c; root.addView(v);
            }
            @Override public void onHideCustomView() {
                if (custom != null) { root.removeView(custom); custom = null; }
                if (cb != null) cb.onCustomViewHidden();
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return false; // luôn ở trong app
            }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler h, SslError e) {
                // TV Android 4.4 thường thiếu chứng chỉ gốc mới / sai giờ hệ thống
                Toast.makeText(MainActivity.this,
                        "Lỗi chứng chỉ SSL - kiểm tra ngày giờ của TV", Toast.LENGTH_LONG).show();
                h.cancel();
            }
        });
        root.addView(web);
        web.requestFocus();
    }

    private void sendKey(int code) {
        String js = "(function(){var t=document.activeElement||document;"
                + "['keydown','keyup'].forEach(function(n){var e=document.createEvent('Event');"
                + "e.initEvent(n,true,true);e.keyCode=" + code + ";e.which=" + code + ";"
                + "t.dispatchEvent(e);});})();";
        if (Build.VERSION.SDK_INT >= 19) web.evaluateJavascript(js, null);
        else web.loadUrl("javascript:" + js);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                long now = System.currentTimeMillis();
                if (now - lastBack < 800) { finish(); return true; } // bấm Back 2 lần nhanh = thoát
                lastBack = now;
                sendKey(27); // Escape = Back của YouTube TV
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                sendKey(179); return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                sendKey(417); return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                sendKey(412); return true;
            case KeyEvent.KEYCODE_MENU:
                web.reload(); return true;
        }
        return super.onKeyDown(keyCode, ev); // phím mũi tên / OK chuyển thẳng cho trang
    }

    @Override protected void onResume() {
        super.onResume();
        web.onResume();
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
    @Override protected void onPause() { super.onPause(); web.onPause(); }
    @Override protected void onDestroy() {
        root.removeAllViews(); web.destroy(); super.onDestroy();
    }
}
