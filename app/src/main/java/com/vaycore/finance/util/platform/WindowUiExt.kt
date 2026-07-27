package com.vaycore.finance.util.platform

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vaycore.finance.R
import com.vaycore.finance.util.context.resolveColorCompat

fun Context.showSoftInput(editText: EditText) {
    editText.isFocusable = true
    editText.isFocusableInTouchMode = true
    editText.requestFocus()
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
}

val Context.statusBarHeight: Int
    get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                Resources.getSystem().displayMetrics,
            ).toInt()
        }
    }

fun AppCompatActivity.configureSystemBars(
    @ColorInt statusBarColor: Int = resolveColorCompat(R.color.transparent),
    @ColorInt navBarColor: Int = resolveColorCompat(R.color.white),
    darkMode: Boolean = false,
    adjustForIme: Boolean = true,
) {
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
        view.setPadding(systemBars.left, 0, systemBars.right, bottom)
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
