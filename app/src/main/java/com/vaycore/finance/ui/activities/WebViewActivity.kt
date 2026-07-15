package com.vaycore.finance.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.WebActivityBinding
import com.vaycore.finance.data.local.bean.WebBridgeMessage
import com.vaycore.finance.util.LogUtil
import com.vaycore.finance.util.parseJsonSafely
import com.vaycore.finance.util.context.openExternalBrowser
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class WebViewActivity : BaseActivity<WebActivityBinding>() {

    override val binding by viewBinding(WebActivityBinding::inflate)
    companion object {
        fun launch(context: Context, title: String, url: String) {
            context.start<WebViewActivity> {
                putExtra("title", title)
                putExtra("url", url)
            }
        }

        fun getIntent(context: Context, url: String): Intent {
            return Intent(context, WebViewActivity::class.java)
                .putExtra("url", url)
                .putExtra("kyc", true)
        }
    }

    private val webTitle by lazy { intent.getStringExtra("title") }
    private val webUrl by lazy { intent.getStringExtra("url") ?: "" }

    private val isKyc by lazy { intent.getBooleanExtra("kyc", false) }
    private val kycRedirectUrl =
        "liveness_success"//https://www.agarangtulong.com/liveness_success.html

    override fun initView() = with(binding) {
        LogUtil.e("url：$webUrl")
        onBackAction(null) {
            handleBack()
        }
        titleBar.setNavigationAction { handleBack() }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.apply {
            // Basic settings
            settings.javaScriptEnabled = true                 // Enable JS
            settings.domStorageEnabled = true                 // DOM storage
            settings.databaseEnabled = true                   // DB cache
            settings.setSupportZoom(false)                    // Disable zoom
            settings.displayZoomControls = false              // Hide controls
            settings.builtInZoomControls = false

            // Screen fitting
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            // Cache
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            // File access
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true

            // Mixed content
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.defaultTextEncodingName = "utf-8"
            settings.mediaPlaybackRequiresUserGesture = true

            addJavascriptInterface(this@WebViewActivity, "AndroidBridge")

            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Keep links inside this WebView.
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url?.contains(kycRedirectUrl) == true && isKyc) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                progressBar.progress = newProgress
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val resources = request.resources

                    if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                        request.deny()
                    }
                }
            }
        }
        titleBar.updateTitle(webTitle)
        webView.loadUrl(webUrl)
    }

    private fun handleBack() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }

    @JavascriptInterface
    fun sendMessage(data: String) {
        val bridgeMessage = data.parseJsonSafely<WebBridgeMessage>()
        when (bridgeMessage?.key) {
            "openExternalBrowser" -> {
                bridgeMessage.data?.openExternalBrowser()
            }
        }
    }
}
