package com.vaycore.finance.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Base64
import android.widget.Toast
import com.vaycore.finance.R
import com.vaycore.finance.App
import com.vaycore.finance.data.local.bean.TrackBean
import java.security.MessageDigest
import androidx.core.net.toUri
import com.vaycore.finance.data.ACT_copy
import com.vaycore.finance.data.PageAll

/** String extension functions */
fun String.toHtmlSpanned(@SuppressLint("InlinedApi") flag: Int = Html.FROM_HTML_MODE_LEGACY): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, flag)
    } else {
        Html.fromHtml(this)
    }
}

fun String.copyToClipboard() {
    val clipboard = App.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Phone Number", this)
    clipboard.setPrimaryClip(clip)
    App.appViewModel.recordEvent(
        TrackBean(
            p = PageAll,
            act = ACT_copy,
            result = System.currentTimeMillis().toString() + "|" + this
        )
    )
}

fun String.dialNumber() {
    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = "tel:$this".toUri()
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    App.appContext.startActivity(intent)
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

fun String?.permissionLabel(context: Context): String {
    return when (this) {
        Manifest.permission.READ_PHONE_STATE -> context.getString(R.string.dialog_permission_phone)
        Manifest.permission.READ_CALL_LOG -> context.getString(R.string.dialog_permission_call)
        Manifest.permission.READ_CALENDAR -> context.getString(R.string.dialog_permission_calendar)
        Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.dialog_permission_location)
        Manifest.permission.READ_SMS -> context.getString(R.string.dialog_permission_sms)
        Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.dialog_permission_notification)
        Manifest.permission.CAMERA -> context.getString(R.string.camera_str)
        else -> ""
    }
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
