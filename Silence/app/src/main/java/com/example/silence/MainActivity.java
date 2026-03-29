package com.example.silence;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EmotionClassifier ai = new EmotionClassifier();

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // TRAIN AI (used only for testing)

        ai.train("happy", "I feel amazing today");
        ai.train("happy", "this is wonderful");
        ai.train("happy", "I am very happy");
        ai.train("happy", "life is beautiful");
        ai.train("happy", "I feel joyful");

        ai.train("sad", "I feel terrible");
        ai.train("sad", "I am very lonely");
        ai.train("sad", "I feel depressed");
        ai.train("sad", "I want to cry");
        ai.train("sad", "I feel broken");

        ai.train("angry", "I am very mad");
        ai.train("angry", "this makes me angry");
        ai.train("angry", "I hate everything");
        ai.train("angry", "I am furious");

        ai.train("anxious", "I feel nervous");
        ai.train("anxious", "I am very worried");
        ai.train("anxious", "I feel stressed");
        ai.train("anxious", "I am scared");

        // TEST AI
        String emotion = ai.predict("I feel terrible today");
        System.out.println("Detected Emotion: " + emotion);

        WebView.setWebContentsDebuggingEnabled(true);

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();

        /* =========================================
           WEBVIEW SETTINGS
        ========================================= */

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        // Prevent security issues
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        /* =========================================
           SCROLL + PERFORMANCE SETTINGS
        ========================================= */

        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

        /* =========================================
           WEBVIEW CLIENTS
        ========================================= */

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Allow smooth touch scrolling
        webView.setOnTouchListener((v, event) -> false);

        /* =========================================
           ANDROID BRIDGE
        ========================================= */

        webView.addJavascriptInterface(
                new AndroidBridge(this, webView),
                "Android"
        );

        /* =========================================
           SESSION LOGIN CHECK
        ========================================= */

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);

        boolean loggedIn = prefs.getBoolean("is_logged_in", false);
        String role = prefs.getString("user_role", null);

        if (loggedIn) {

            if ("admin".equals(role)) {
                webView.loadUrl("file:///android_asset/admin.html");
            } else {
                webView.loadUrl("file:///android_asset/index.html");
            }

        } else {
            webView.loadUrl("file:///android_asset/login.html");
        }
    }
}