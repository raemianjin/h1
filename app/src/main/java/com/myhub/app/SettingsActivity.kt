package com.myhub.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

/** 설정 화면. 수려한 UI 는 assets/index.html 에 있고, 여기서는 브리지만 노출한다. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        web = findViewById(R.id.settingsWeb)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        web.addJavascriptInterface(Bridge(), "Native")
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class Bridge {
        @JavascriptInterface
        fun getConfig(): String = UrlStore.loadJson(this@SettingsActivity)

        @JavascriptInterface
        fun saveConfig(json: String) {
            UrlStore.saveJson(this@SettingsActivity, json)
        }

        @JavascriptInterface
        fun appVersion(): String = "1.0.0"

        @JavascriptInterface
        fun close() {
            runOnUiThread { finish() }
        }
    }
}
