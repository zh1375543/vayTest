package com.vaycore.finance.ui.widget.form

import android.content.res.ColorStateList
import android.text.InputFilter
import android.text.InputType
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.vaycore.finance.R
import com.vaycore.finance.ui.views.StyledEditTextView

internal class FormItemModeController(
    private val input: StyledEditTextView,
) {

    private var mode = FormMode.INPUT

    val isSelectMode: Boolean
        get() = mode == FormMode.SELECT

    fun apply(attributes: FormItemAttributes) {
        mode = attributes.mode
        input.inputType = attributes.inputType.toAndroidInputType()

        if (attributes.maxLength > -1) {
            input.filters = arrayOf(InputFilter.LengthFilter(attributes.maxLength))
        }

        if (isSelectMode) {
            input.isFocusable = false
            input.isClickable = true
            input.isCursorVisible = false
            input.keyListener = null
            // Keep the original initialization behavior: only install the arrow here.
            showSelectIndicator()
        }
    }

    fun setEnabled(enabled: Boolean) {
        input.isEnabled = enabled
        if (!enabled) {
            clearEndDrawable()
        } else if (isSelectMode) {
            setDrawableTint(R.color.C_9CA3AF)
        }
    }

    fun onValidationStateChanged(hasError: Boolean) {
        if (!isSelectMode) return

        if (hasError) {
            showSelectIndicator()
            setDrawableTint(R.color.C_F62909)
        } else if (input.isEnabled) {
            restoreSelectIndicatorIfNeeded()
        }
    }

    fun restoreSelectIndicatorIfNeeded() {
        if (isSelectMode && input.isEnabled) {
            showSelectIndicator()
            setDrawableTint(R.color.C_9CA3AF)
        }
    }

    fun setDrawableTint(@ColorRes color: Int) {
        TextViewCompat.setCompoundDrawableTintList(
            input,
            ColorStateList.valueOf(ContextCompat.getColor(input.context, color)),
        )
    }

    private fun showSelectIndicator() {
        val drawableEnd = ContextCompat.getDrawable(input.context, R.mipmap.personal_bottom)
        input.setCompoundDrawablesWithIntrinsicBounds(null, null, drawableEnd, null)
    }

    private fun clearEndDrawable() {
        input.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    }

    private fun FormInputType.toAndroidInputType(): Int {
        return when (this) {
            FormInputType.TEXT -> InputType.TYPE_CLASS_TEXT
            FormInputType.NUMBER -> InputType.TYPE_CLASS_NUMBER
            FormInputType.PHONE -> InputType.TYPE_CLASS_PHONE
            FormInputType.EMAIL -> InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
    }
}
