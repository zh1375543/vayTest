package com.vaycore.finance.ui.widget.form

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.vaycore.finance.R

internal class FormItemAttributeReader(
    private val context: Context,
) {

    fun read(attrs: AttributeSet?): FormItemAttributes {
        lateinit var result: FormItemAttributes
        context.withStyledAttributes(attrs, R.styleable.FormItemView) {
            result = FormItemAttributes(
                title = getText(R.styleable.FormItemView_titleText),
                hint = getText(R.styleable.FormItemView_hintText),
                errorText = getText(R.styleable.FormItemView_errorText),
                mode = getInt(R.styleable.FormItemView_formType, 0).toFormMode(),
                inputType = getInt(R.styleable.FormItemView_inputType, 0).toFormInputType(),
                maxLength = getInt(R.styleable.FormItemView_editMaxLength, -1),
                showContactIcon = getBoolean(R.styleable.FormItemView_showContactIcon, false),
            )
        }
        return result
    }

    private fun Int.toFormMode(): FormMode {
        return if (this == 1) FormMode.SELECT else FormMode.INPUT
    }

    private fun Int.toFormInputType(): FormInputType {
        return when (this) {
            1 -> FormInputType.NUMBER
            2 -> FormInputType.PHONE
            else -> if (this == 3) FormInputType.EMAIL else FormInputType.TEXT
        }
    }
}
