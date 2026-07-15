package com.vaycore.finance.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vaycore.finance.R

abstract class BaseSheetDialog<VB : ViewBinding>(
    private val context: Context,
    inflate: (LayoutInflater) -> VB,
    themeResId: Int = R.style.BottomSheetDialog,
) : BottomSheetDialog(context, themeResId) {

    val binding by lazy { inflate(layoutInflater) }
    val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(binding.root.parent as View)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        configureBottomSheet()
        initView()
    }

    open fun initView() {}

    override fun show() {
        val activity = context as? Activity ?: return
        if (!activity.isSafeToShow) return
        runCatching { super.show() }
            .onFailure { it.printStackTrace() }
    }

    private fun configureBottomSheet() {
        bottomSheetBehavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isHideable = true
            isDraggable = false
        }
        window?.attributes?.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        setCanceledOnTouchOutside(true)
    }

    private val Activity.isSafeToShow: Boolean
        get() = !isFinishing &&
            !isDestroyed &&
            window?.decorView?.windowToken != null &&
            window?.attributes?.token != null
}
