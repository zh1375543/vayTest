package com.vaycore.finance.ui.widget.form

internal data class FormItemAttributes(
    val title: CharSequence?,
    val hint: CharSequence?,
    val errorText: CharSequence?,
    val mode: FormMode,
    val inputType: FormInputType,
    val maxLength: Int,
    val showContactIcon: Boolean,
)

internal enum class FormMode {
    INPUT,
    SELECT,
}

internal enum class FormInputType {
    TEXT,
    NUMBER,
    PHONE,
    EMAIL,
}
