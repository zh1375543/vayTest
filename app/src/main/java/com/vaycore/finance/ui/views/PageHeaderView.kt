package com.vaycore.finance.ui.views

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.vaycore.finance.R
import com.vaycore.finance.databinding.ViewPageHeaderBinding
import com.vaycore.finance.ui.extension.addStatusBarTopMargin
import com.vaycore.finance.ui.extension.singleClick

class PageHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding =
        ViewPageHeaderBinding.inflate(LayoutInflater.from(context), this)

    private var fitStatusBar = true

    init {
        applyXmlAttributes(attrs)
        configureDefaultNavigation()
        applyStatusBarSpacingIfNeeded()
    }

    private fun applyXmlAttributes(attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PageHeaderView, 0, 0).apply {
            try {
                updateTitle(getString(R.styleable.PageHeaderView_centerText))
                setAction(getString(R.styleable.PageHeaderView_rightText))
                if (hasValue(R.styleable.PageHeaderView_tintColor)) {
                    updateContentColor(getColor(R.styleable.PageHeaderView_tintColor, 0))
                }
                fitStatusBar = getBoolean(R.styleable.PageHeaderView_fitStatusBar, true)
            } finally {
                recycle()
            }
        }
        updateRightAction()
    }

    private fun configureDefaultNavigation() {
        setNavigationAction {
            (context as? Activity)?.finish()
        }
    }

    private fun applyStatusBarSpacingIfNeeded() {
        if (fitStatusBar) {
            post { addStatusBarTopMargin() }
        }
    }

    private fun updateRightAction() {
        binding.tvBarRight.isVisible = !binding.tvBarRight.text.isNullOrEmpty()
    }

    fun setNavigationAction(action: () -> Unit) {
        binding.ivBack.singleClick { action() }
    }

    fun setAction(text: CharSequence? = null, action: (() -> Unit)? = null) {
        text?.let { binding.tvBarRight.text = it }
        binding.tvBarRight.setOnClickListener(null)
        action?.let { clickAction ->
            binding.tvBarRight.singleClick { clickAction() }
        }
    }

    fun updateTitle(title: CharSequence?) {
        binding.tvBarTitle.text = title ?: ""
    }

    fun updateContentColor(@ColorInt color: Int) {
        binding.ivBack.imageTintList = ColorStateList.valueOf(color)
        binding.tvBarTitle.setTextColor(color)
        binding.tvBarRight.setTextColor(color)
    }

    fun showAction(visible: Boolean) {
        binding.tvBarRight.isVisible = visible
    }
}
