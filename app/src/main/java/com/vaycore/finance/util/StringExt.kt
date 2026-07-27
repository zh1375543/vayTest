package com.vaycore.finance.util

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Base64
import android.widget.Toast
import com.vaycore.finance.app.App
import java.security.MessageDigest

/** String extension functions */
fun String.toHtmlSpanned(@SuppressLint("InlinedApi") flag: Int = Html.FROM_HTML_MODE_LEGACY): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, flag)
    } else {
        Html.fromHtml(this)
    }
}

fun String.toMd5(): String {
    val md = MessageDigest.getInstance("MD5")
    val bytes = md.digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String?.maskSensitive(): String? {
    if (this == null || length <= 7) return this
    val prefix = this.substring(0, 3)
    val suffix = this.takeLast(4)
    val stars = "*".repeat(this.length - 7)
    return "$prefix$stars$suffix"
}

fun String.encodeBase64(): String {
    val bytes: ByteArray = this.toByteArray()
    val baseData = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return baseData
}

private var lastToastTime = 0L
private const val INTERVAL = 1500L

fun String?.showToastMessage() {
    if (this.isNullOrBlank()) return

    val now = System.currentTimeMillis()
    if (now - lastToastTime >= INTERVAL) {
        lastToastTime = now
        Toast.makeText(App.appContext, this, Toast.LENGTH_SHORT).show()
    }
}

fun String?.removeWhitespace(): String? {
    return this?.replace("\\s+".toRegex(), "")
}
