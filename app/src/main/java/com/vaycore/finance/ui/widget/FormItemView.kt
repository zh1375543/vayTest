package com.vaycore.finance.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.vaycore.finance.databinding.FormItemViewBinding
import com.vaycore.finance.ui.views.StyledEditTextView
import com.vaycore.finance.ui.widget.form.FormItemAttributeReader
import com.vaycore.finance.ui.widget.form.FormItemAttributes
import com.vaycore.finance.ui.widget.form.FormItemModeController
import com.vaycore.finance.ui.widget.form.FormItemValidationController
import com.vaycore.finance.ui.extension.singleClick

class FormItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: FormItemViewBinding
    private val attributes: FormItemAttributes
    private val modeController: FormItemModeController
    private val validationController: FormItemValidationController

    init {
        orientation = VERTICAL
        binding = FormItemViewBinding.inflate(LayoutInflater.from(context), this)
        attributes = FormItemAttributeReader(context).read(attrs)
        modeController = FormItemModeController(binding.etInput)
        validationController = FormItemValidationController(
            input = binding.etInput,
            errorView = binding.tvError,
            onErrorStateChanged = modeController::onValidationStateChanged,
        )

        binding.tvTitle.text = attributes.title
        binding.etInput.hint = attributes.hint
        binding.tvError.text = attributes.errorText
        binding.ivEndIcon.isVisible = attributes.showContactIcon || attributes.endIconRes != null
        attributes.endIconRes?.let(binding.ivEndIcon::setImageResource)
        attributes.inputBackgroundColor?.let(binding.etInput::setSolidColor)

        modeController.apply(attributes)
        validationController.bindTextChange()
    }

    fun getText(): String {
        return binding.etInput.text?.toString() ?: ""
    }

    fun setText(text: String?) {
        binding.etInput.setText(text)
        modeController.restoreSelectIndicatorIfNeeded()
    }

    fun setTitle(text: CharSequence?) {
        binding.tvTitle.text = text ?: ""
    }

    fun showError() {
        validationController.showError()
    }

    fun showError(message: CharSequence) {
        binding.tvError.text = message
        validationController.showError()
    }

    fun setEnableEdit(enable: Boolean) {
        modeController.setEnabled(enable)
        if (!enable) {
            validationController.hideError()
        }
    }

    fun hideError() {
        validationController.hideError()
    }

    fun setContactClick(block: () -> Unit) {
        binding.ivEndIcon.singleClick { block() }
    }

    fun setContactVisible(isVisible: Boolean) {
        binding.ivEndIcon.isVisible = isVisible
        modeController.setUsesExternalEndIcon(isVisible)
    }

    fun setEndIcon(@DrawableRes imageRes: Int?) {
        binding.ivEndIcon.isVisible = imageRes != null
        imageRes?.let(binding.ivEndIcon::setImageResource)
        modeController.setUsesExternalEndIcon(imageRes != null)
    }

    fun setEndIconClick(block: () -> Unit) {
        binding.ivEndIcon.singleClick { block() }
    }

    fun setOnClick(block: () -> Unit) {
        binding.etInput.singleClick { block() }
    }

    fun getEditText(): StyledEditTextView = binding.etInput

    fun setInputBackgroundColor(@ColorInt color: Int) {
        binding.etInput.setSolidColor(color)
    }

    fun setDrawableTint(@ColorRes color: Int) {
        modeController.setDrawableTint(color)
    }
}
