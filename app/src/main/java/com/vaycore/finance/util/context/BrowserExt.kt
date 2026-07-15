package com.vaycore.finance.util.context

import android.content.Intent
import androidx.core.net.toUri
import com.vaycore.finance.App
import com.vaycore.finance.util.LogUtil

fun String.openExternalBrowser() {
    try {
        val validUrl = if (!startsWith("http://") && !startsWith("https://")) {
            "https://$this"
        } else {
            this
        }
        val intent = Intent(Intent.ACTION_VIEW, validUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val browsers = listOf(
            "com.android.browser",
            "com.android.chrome",
            "org.mozilla.firefox",
            null
        )
        for (browserPackage in browsers) {
            try {
                val browserIntent = intent.clone() as Intent
                browserPackage?.let(browserIntent::setPackage)
                if (browserIntent.resolveActivity(App.appContext.packageManager) != null) {
                    App.appContext.startActivity(browserIntent)
                    return
                }
            } catch (e: Exception) {
                LogUtil.w("Failed with $browserPackage: ${e.message}")
            }
        }
        LogUtil.e("No browser available")
    } catch (e: Exception) {
        LogUtil.e("Failed to open external browser: ${e.message}")
    }
}

fun String.openPlayStore() {
    if (isNotBlank()) {
        trim().openExternalBrowser()
        return
    }
    try {
        val marketUri = "market://details?id=${App.appContext.packageName}"
        val intent = Intent(Intent.ACTION_VIEW, marketUri.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.vending")
        }
        App.appContext.startActivity(intent)
    } catch (e: Exception) {
        val webUri = "https://play.google.com/store/apps/details?id=${App.appContext.packageName}"
        val intent = Intent(Intent.ACTION_VIEW, webUri.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        App.appContext.startActivity(intent)
    }
}
