package com.ibluetag.indoor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class DemoWebActivity extends Activity {
    public static final String TAG = "DemoWebActivity";
    public static final String EXTRA_WEB_URL = "extra_web_url";

    private WebView mWebView;
    private String mUrl = null;
    private Handler mHandler = new Handler();
    private ProgressDialog mProgressDialog = null;
    private Context mSelfContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            mUrl = getIntent().getStringExtra(EXTRA_WEB_URL);
            Log.v(TAG, "mUrl: " + mUrl);
        }
        setContentView(R.layout.activity_web);
        setupView();
        mSelfContext = this;
    }

    private void setupView() {
        mWebView = (WebView) findViewById(R.id.web_page);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setWebViewClient(mWebViewClient);
        if (mUrl != null && !mUrl.isEmpty()) {
            loadUrl(mUrl);
        }
    }

    public void loadUrl(String url) {
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "invalid url...");
        } else {
            mUrl = url;
        }
        loadUrl();
    }

    private void loadUrl() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = ProgressDialog.show(mSelfContext,
                        null, getString(R.string.progress_loading), true, true);
                mWebView.loadUrl(mUrl);
            }
        });
    }

    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
            }
        }
    };
}
