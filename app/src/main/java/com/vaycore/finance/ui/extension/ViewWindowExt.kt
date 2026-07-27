package com.vaycore.finance.ui.extension

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.vaycore.finance.util.platform.statusBarHeight

fun View.observeKeyboardVisibility(callback: (isVisible: Boolean, height: Int) -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener {
        val rect = Rect()
        getWindowVisibleDisplayFrame(rect)
        val screenHeight = rootView.rootView.height
        val keyboardHeight = screenHeight - rect.bottom

        if (keyboardHeight > screenHeight * 0.15) {
//            Log.d("Keyboard", "keyboard shown, height=$keyboardHeight")
            callback(true, keyboardHeight)
        } else {
//            Log.d("Keyboard", "keyboard hidden")
            callback(false, 0)
        }
    }
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.addStatusBarTopMargin() {
    val lp = layoutParams
    if (lp is ViewGroup.MarginLayoutParams) {
        lp.topMargin += context.statusBarHeight
        layoutParams = lp
    }
}

fun View.addStatusBarTopPaddingAndHeight() {
    val lp = layoutParams
    if (lp != null && lp.height > 0) {
        lp.height += context.statusBarHeight // increase height
    }
    setPadding(
        paddingLeft, paddingTop + context.statusBarHeight,
        paddingRight, paddingBottom
    )
}
