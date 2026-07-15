package com.vaycore.finance.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.vaycore.finance.R
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.ui.activities.LoginActivity
import com.vaycore.finance.util.context.getColor2
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun Context.showSoftInput(edit: EditText) {
    edit.isFocusable = true
    edit.isFocusableInTouchMode = true
    edit.requestFocus()
    val inputManager =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.setDefaultFont(fontAssetName: String, boldFontAssetName: String) {
    val medium = Typeface.createFromAsset(assets, fontAssetName) // medium font
    val bold = Typeface.createFromAsset(assets, boldFontAssetName) // bold font

    try {
        // 1. replace Typeface.DEFAULT
        replaceTypefaceField("DEFAULT", medium)
        // 2. replace Typeface.DEFAULT_BOLD
        replaceTypefaceField("DEFAULT_BOLD", bold)

        // 3. replace sDefaults (default font array)
        val defaultsField = Typeface::class.java.getDeclaredField("sDefaults")
        defaultsField.isAccessible = true
        defaultsField.set(
            null,
            arrayOf(medium, bold, Typeface.SANS_SERIF, Typeface.SERIF, Typeface.MONOSPACE)
        )

        // 4. for Android 8.0+, replace sSystemFontMap
        val systemFontMapField = Typeface::class.java.getDeclaredField("sSystemFontMap")
        systemFontMapField.isAccessible = true
        val map = systemFontMapField.get(null) as MutableMap<String, Typeface>
        map["sans-serif"] = medium
        map["sans-serif-medium"] = medium
        map["sans-serif-bold"] = bold
        map["sans-serif-light"] = medium
        map["sans-serif-condensed"] = medium
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun replaceTypefaceField(fieldName: String, newTypeface: Typeface) {
    try {
        val staticField = Typeface::class.java.getDeclaredField(fieldName)
        staticField.isAccessible = true
        staticField.set(null, newTypeface)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.formatDays(value: Any?): String {
    val text = value?.toString()?.trim().orEmpty()
    if (text.isEmpty()) return ""

    val dayLabel = getString(R.string.days)
    if (text.endsWith(dayLabel, ignoreCase = true)) return text

    return getString(R.string.num_days, text)
}

fun Context.ifLoginAction(action: () -> Unit) {
    if (isLogin) action.invoke()
    else start<LoginActivity>()
}

fun Context.compressImage(
    inputUri: Uri?,
    targetWidth: Int = 1024,
    targetHeight: Int = 768,
    maxFileSizeKb: Int = 250,
): Uri? {
    if (inputUri == null) return null
    // read original image
    val inputStream = contentResolver.openInputStream(inputUri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()

    // get rotation angle and correct orientation
    val orientation = getImageRotation(inputUri)
    val rotatedBitmap = rotateBitmapIfNeeded(originalBitmap, orientation)

    // scale to target size
    val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)

    // output file path
    val outputFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")

    var quality = 90
    var byteArray: ByteArray

    do {
        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        byteArray = baos.toByteArray()
        baos.close()

        quality -= 5 // reduce quality each iteration
    } while (byteArray.size / 1024 > maxFileSizeKb && quality > 10)

    FileOutputStream(outputFile).use { fos ->
        fos.write(byteArray)
        fos.flush()
    }

    // release memory
    if (scaledBitmap != rotatedBitmap) rotatedBitmap.recycle()
    originalBitmap.recycle()
    scaledBitmap.recycle()

    return FileProvider.getUriForFile(this, "$packageName.fileprovider", outputFile)
}

private fun Context.getImageRotation(uri: Uri): Int {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android N and above
            val inputStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()
            getRotationFromExif(exif)
        } else {
            // below Android N, create temp file
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_image", ".jpg", cacheDir)

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val exif = ExifInterface(tempFile.absolutePath)
            tempFile.delete() // clean up temp file
            getRotationFromExif(exif)
        }
    } catch (e: Exception) {
        0
    }
}

private fun getRotationFromExif(exif: ExifInterface): Int {
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

fun Context.getAdvertisingId(): String {
    return try {
        val info = AdvertisingIdClient.getAdvertisingIdInfo(this@getAdvertisingId)
        info.id ?: ""
    } catch (_: Exception) {
        ""
    }
}

/** Get status bar height */
fun Context.getStatusBarHeight(): Int {
    val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resId > 0) {
        resources.getDimensionPixelSize(resId)
    } else {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f, Resources.getSystem().displayMetrics
        ).toInt()
    }
}

fun AppCompatActivity.setSystemBar(
    @ColorInt statusBarColor: Int = getColor2(R.color.transparent),
    @ColorInt navBarColor: Int = getColor2(R.color.white),
    darkMode: Boolean = false,
    adjustForIme: Boolean = true,
) {
    val isOver15 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = statusBarColor
    window.navigationBarColor = navBarColor
    applyLegacySystemBarIconMode(darkMode)
    ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
        controller.isAppearanceLightStatusBars = darkMode
        controller.isAppearanceLightNavigationBars = darkMode
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val bottom = if (adjustForIme) maxOf(systemBars.bottom, ime.bottom) else systemBars.bottom
        view.setPadding(
            systemBars.left,
            0,
            systemBars.right,
            bottom
        )
        insets
    }
}

@Suppress("DEPRECATION")
private fun AppCompatActivity.applyLegacySystemBarIconMode(useDarkIcons: Boolean) {
    var flags = window.decorView.systemUiVisibility
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        flags = if (useDarkIcons) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        flags = if (useDarkIcons) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
    }
    window.decorView.systemUiVisibility = flags
}
