package com.vaycore.finance.ui.widget.form

import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.vaycore.finance.ui.views.StyledEditTextView

internal class FormItemValidationController(
    private val input: StyledEditTextView,
    private val errorView: TextView,
    private val onErrorStateChanged: (Boolean) -> Unit,
) {

    fun bindTextChange() {
        input.doAfterTextChanged {
            hideError()
        }
    }

    fun showError() {
        errorView.isVisible = true
        input.isSelected = true
        onErrorStateChanged(true)
    }

    fun hideError() {
        errorView.isVisible = false
        input.isSelected = false
        onErrorStateChanged(false)
    }
}
