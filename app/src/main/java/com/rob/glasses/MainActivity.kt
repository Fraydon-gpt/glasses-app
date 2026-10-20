package com.rob.glasses

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** Shows Rob's easy-read screens (assets/index.html) and links them to the glasses. */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private val glasses by lazy { GlassesLink(this) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        WebView.setWebContentsDebuggingEnabled(true)
        web.addJavascriptInterface(Bridge(), "AndroidGlasses")
        web.webViewClient = object : WebViewClient() {
            // Links that leave the app (Google Maps) open in their own app.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "file") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (_: Exception) { }
                return true
            }
        }
        web.loadUrl("file:///android_asset/index.html")
        askPermissions()
    }

    private fun askPermissions() {
        val wanted = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) wanted += Manifest.permission.BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= 33) wanted += Manifest.permission.POST_NOTIFICATIONS
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        js("window.onGlassesPermission && window.onGlassesPermission()")
    }

    fun js(code: String) {
        runOnUiThread { web.evaluateJavascript(code, null) }
    }

    @Deprecated("Back gesture moves between the app's own screens first")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    override fun onDestroy() {
        glasses.close()
        super.onDestroy()
    }

    /** What the screens can ask the phone to do. */
    inner class Bridge {
        @JavascriptInterface fun connect() = glasses.connect()
        @JavascriptInterface fun write(b64: String) = glasses.write(Base64.decode(b64, Base64.NO_WRAP))
        @JavascriptInterface fun disconnect() = glasses.close()
        @JavascriptInterface fun scheduleAlarm(id: String, atMillis: Double, label: String) =
            Alarms.schedule(this@MainActivity, id, atMillis.toLong(), label)
        @JavascriptInterface fun cancelAlarm(id: String) = Alarms.cancel(this@MainActivity, id)
    }
}
