package com.vaycore.finance.ui.binding

import androidx.databinding.BindingAdapter
import com.vaycore.finance.ui.views.FormItemView

/** Keeps FormItemView editing state one-way to avoid text listener feedback loops. */
@BindingAdapter("editable")
fun FormItemView.bindEditable(editable: Boolean) {
    setEnableEdit(editable)
}

/** Shows the system-contact shortcut without binding any form value. */
@BindingAdapter("contactPickerVisible")
fun FormItemView.bindContactPickerVisible(visible: Boolean) {
    setContactVisible(visible)
}
