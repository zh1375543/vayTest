package com.vaycore.finance.ui.extension

import android.view.View

/** Click event with fast-click prevention */
fun <T : View?> T.singleClick(interval: Long = 500L, action: ((T) -> Unit)?) {
    this?.setOnClickListener(SingleClickListener(interval, action))
}

class SingleClickListener<T : View?>(
    private val interval: Long = 500L,
    private val clickFunc: ((T) -> Unit)?,
) : View.OnClickListener {
    private var lastClickTime = 0L

    override fun onClick(v: View?) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastClickTime > interval) {
            // single click event
            clickFunc?.invoke(v as T)
            lastClickTime = nowTime
        }
    }
}
