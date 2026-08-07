package com.vaycore.finance.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.vaycore.finance.app.App
import com.vaycore.finance.data.ACT_copy
import com.vaycore.finance.data.PageAll
import com.vaycore.finance.data.bean.TrackBean

/** Launches actions handled outside the app with an explicit caller context. */
object ExternalActionLauncher {

    fun openBrowser(context: Context, url: String) {
        val validUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        try {
            val baseIntent = Intent(Intent.ACTION_VIEW, validUrl.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val browserPackages = listOf(
                "com.android.browser",
                "com.android.chrome",
                "org.mozilla.firefox",
                null,
            )
            for (browserPackage in browserPackages) {
                try {
                    val browserIntent = Intent(baseIntent).apply { setPackage(browserPackage) }
                    if (browserIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(browserIntent)
                        return
                    }
                } catch (exception: Exception) {
                    LogUtil.w("Failed with $browserPackage: ${exception.message}")
                }
            }
            LogUtil.e("No browser available")
        } catch (exception: Exception) {
            LogUtil.e("Failed to open external browser: ${exception.message}")
        }
    }

    /** Opens the Google Play listing through the device browser, matching the tracker flow. */
    fun openRatingPage(context: Context): Boolean {
        val ratingIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=${context.packageName}".toUri(),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val browserPackages = listOf(
            "com.android.browser",
            "com.android.chrome",
            "org.mozilla.firefox",
            null,
        )
        browserPackages.forEach { browserPackage ->
            try {
                val browserIntent = Intent(ratingIntent).apply { setPackage(browserPackage) }
                if (browserIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(browserIntent)
                    return true
                }
            } catch (exception: Exception) {
                LogUtil.w("Failed to open rating page with $browserPackage: ${exception.message}")
            }
        }
        LogUtil.e("No browser available for the rating page")
        return false
    }

    fun openStoreListing(context: Context, listingUrl: String? = null) {
        if (!listingUrl.isNullOrBlank()) {
            openBrowser(context, listingUrl.trim())
            return
        }

        try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                "market://details?id=${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.android.vending")
            }
            context.startActivity(marketIntent)
        } catch (exception: Exception) {
            openBrowser(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
        }
    }

    fun openDialer(context: Context, phoneNumber: String) {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun copyText(context: Context, text: String, label: String = "Phone Number") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        App.appViewModel.submitTrackingEvent(
            TrackBean(
                p = PageAll,
                act = ACT_copy,
                result = "${System.currentTimeMillis()}|$text",
            ),
        )
    }
}
