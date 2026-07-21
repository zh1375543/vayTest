package com.vaycore.finance.ui.views

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.DrawableRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import com.vaycore.finance.R
import com.vaycore.finance.ui.widget.shape.ShapeAttributeReader
import com.vaycore.finance.ui.widget.shape.ShapeBackgroundController
import com.vaycore.finance.ui.widget.shape.TextStateController

/** An EditText that supports ShapeView styling and exposes pasted text events. */
class StyledEditTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var shapeAttributes = ShapeAttributeReader(context).read(attrs)
    private val backgroundController = ShapeBackgroundController(this)
    private var textStateController: TextStateController? = null
    private val validationSuccessDrawableResId: Int
    private val validationErrorDrawableResId: Int
    private var validationState = ValidationState.NONE
    private var endDrawableBeforeValidation: Drawable? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.StyledEditTextView).also { attributes ->
            try {
                validationSuccessDrawableResId = attributes.getResourceId(
                    R.styleable.StyledEditTextView_validationSuccessDrawable,
                    NO_RESOURCE,
                )
                validationErrorDrawableResId = attributes.getResourceId(
                    R.styleable.StyledEditTextView_validationErrorDrawable,
                    NO_RESOURCE,
                )
            } finally {
                attributes.recycle()
            }
        }
        backgroundController.apply(shapeAttributes.appearance)
        textStateController = TextStateController(this).also { controller ->
            controller.apply(shapeAttributes.textState)
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        backgroundController.updateBounds(width, height)
        textStateController?.updateDrawableBounds()
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        textStateController?.updateSelectedState(selected)
    }

    /** Shows the XML-configured success icon without changing the background state. */
    fun showValidationSuccess() {
        updateValidationState(ValidationState.SUCCESS)
    }

    /** Shows the XML-configured error icon without changing the background state. */
    fun showValidationError() {
        updateValidationState(ValidationState.ERROR)
    }

    /** Clears the validation icon and restores the drawable previously shown at the end. */
    fun clearValidationState() {
        updateValidationState(ValidationState.NONE)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs)
        if (inputConnection != null) {
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION.inv()
        }
        return inputConnection
    }

    var onPasteListener: ((String) -> Unit)? = null

    /** Updates the shape fill while retaining its corners and state-specific strokes. */
    fun setSolidColor(@ColorInt color: Int) {
        shapeAttributes = shapeAttributes.copy(
            appearance = shapeAttributes.appearance.copy(
                normal = shapeAttributes.appearance.normal.copy(fillColor = color),
                focused = shapeAttributes.appearance.focused.copy(fillColor = color),
            ),
        )
        backgroundController.apply(shapeAttributes.appearance)
        backgroundController.updateBounds(width, height)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        return when (id) {
            android.R.id.paste, android.R.id.pasteAsPlainText -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val pastedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                onPasteListener?.invoke(pastedText)
                super.onTextContextMenuItem(id)
            }

            else -> super.onTextContextMenuItem(id)
        }
    }

    private fun updateValidationState(newState: ValidationState) {
        if (validationState == newState) return

        val drawables = compoundDrawablesRelative
        if (validationState == ValidationState.NONE && newState != ValidationState.NONE) {
            endDrawableBeforeValidation = drawables[END]
        }

        val endDrawable = when (newState) {
            ValidationState.NONE -> endDrawableBeforeValidation
            ValidationState.SUCCESS -> validationDrawable(validationSuccessDrawableResId)
            ValidationState.ERROR -> validationDrawable(validationErrorDrawableResId)
        }
        setCompoundDrawablesRelative(
            drawables[START],
            drawables[TOP],
            endDrawable,
            drawables[BOTTOM],
        )

        validationState = newState
        if (newState == ValidationState.NONE) {
            endDrawableBeforeValidation = null
        }
    }

    private fun validationDrawable(@DrawableRes drawableResId: Int): Drawable? {
        if (drawableResId == NO_RESOURCE) return endDrawableBeforeValidation

        return AppCompatResources.getDrawable(context, drawableResId)?.apply {
            val size = shapeAttributes.textState.drawableSize
            val width = size.width.takeIf { it > 0 } ?: intrinsicWidth
            val height = size.height.takeIf { it > 0 } ?: intrinsicHeight
            setBounds(0, 0, width, height)
        }
    }

    private enum class ValidationState {
        NONE,
        SUCCESS,
        ERROR,
    }

    private companion object {
        const val START = 0
        const val TOP = 1
        const val END = 2
        const val BOTTOM = 3
        const val NO_RESOURCE = 0
    }
}
