package com.vaycore.finance.ui.extension

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.vaycore.finance.data.local.bean.ClickablePart

fun TextView.setSpannableClickableText(
    fullText: String,
    targetText: String,
    @ColorInt color: Int,
    onClick: () -> Unit,
) {
    val spannable = SpannableString(fullText)
    val start = fullText.indexOf(targetText)
    if (start == -1) {
        text = fullText
        return
    }
    val end = start + targetText.length

    spannable.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) = onClick()

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
            ds.color = color
        }
    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    // bold
    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // enlarge 1.1x
    spannable.setSpan(RelativeSizeSpan(1.1f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    text = spannable
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}

fun TextView.setStyledText(
    fullText: String,
    vararg styles: ClickablePart,
) {
    val spannableString = SpannableString(fullText)
    styles.forEach { style ->
        val startIndex = fullText.indexOf(style.text)
        if (startIndex >= 0) {
            val endIndex = startIndex + style.text.length
            // adjust size
            if (style.sizeScale != 1.0f) {
                spannableString.setSpan(
                    RelativeSizeSpan(style.sizeScale),
                    startIndex,
                    endIndex,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            // adjust color
            style.color.let {
                spannableString.setSpan(
                    ForegroundColorSpan(it),
                    startIndex,
                    endIndex,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
    this.text = spannableString
}

fun TextView.setClickableTextWithScale(
    fullText: String,
    targetText: String,
    color: Int,
    proportion: Float = 1.2f,
    onClick: () -> Unit = {},
) {
    val spannable = SpannableString(fullText)
    val start = fullText.indexOf(targetText)
    if (start == -1) {
        text = fullText
        return
    }
    val end = start + targetText.length

    // click behavior + style
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) = onClick()
        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = false
            ds.color = color // custom color
        }
    }

    // add span: click + bold + enlarge
    spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(RelativeSizeSpan(proportion), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    text = spannable
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}

fun TextView.setSpannableClickableTexts(
    fullText: String,
    clickableParts: List<ClickablePart>,
) {
    val spannable = SpannableString(fullText)

    clickableParts.forEach { part ->
        val start = fullText.indexOf(part.text)
        if (start != -1) {
            val end = start + part.text.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = part.onClick()
                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                    ds.color = part.color
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            // bold
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // enlarge
            if (part.sizeScale != 1.0f) {
                spannable.setSpan(
                    RelativeSizeSpan(part.sizeScale),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    text = spannable
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}

fun EditText.setPasswordVisible(visible: Boolean) {
    transformationMethod = if (!visible) {
        HideReturnsTransformationMethod.getInstance() // show plain text
    } else {
        PasswordTransformationMethod.getInstance()    // hide as *
    }
    // move cursor to end
    setSelection(text.length)
}

fun TextView.setTextWithLeadingIcon(
    drawableResId: Int,
    text: String,
    iconSizeDp: Float = 12f, // icon size in dp
    iconPadding: String = " ", // space between icon and text
) {
    val finalText = "$iconPadding$text"
    val spannable = SpannableString(finalText)

    val drawable: Drawable = ContextCompat.getDrawable(context, drawableResId) ?: return

    // convert dp to px
    val density = context.resources.displayMetrics.density
    val sizePx = (iconSizeDp * density).toInt()

    drawable.setBounds(0, 0, sizePx, sizePx)

    val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_CENTER)
    spannable.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    this.text = spannable
}
