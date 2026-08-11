package com.vaycore.finance.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.vaycore.finance.R

class AlphabetIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val letters = ('A'..'Z').toList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.sp_10)
    }
    private val activeColor = ContextCompat.getColor(context, R.color.brand_primary)
    private val normalColor = ContextCompat.getColor(context, R.color.text_tertiary)
    private val disabledColor = ContextCompat.getColor(context, R.color.text_disabled)

    private var availableLetters: Set<Char> = emptySet()
    private var selectedLetter: Char? = null
    private var onLetterSelected: ((Char) -> Unit)? = null

    init {
        isClickable = true
    }

    fun setAvailableLetters(letters: Set<Char>) {
        availableLetters = letters
        if (selectedLetter !in availableLetters) selectedLetter = null
        invalidate()
    }

    fun setSelectedLetter(letter: Char?) {
        if (letter == selectedLetter || letter !in availableLetters) return
        selectedLetter = letter
        invalidate()
    }

    fun setOnLetterSelectedListener(listener: ((Char) -> Unit)?) {
        onLetterSelected = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (height == 0) return

        val slotHeight = height.toFloat() / letters.size
        val textOffset = -(paint.ascent() + paint.descent()) / 2f
        letters.forEachIndexed { index, letter ->
            paint.color = when {
                letter == selectedLetter -> activeColor
                letter in availableLetters -> normalColor
                else -> disabledColor
            }
            paint.isFakeBoldText = letter == selectedLetter
            canvas.drawText(
                letter.toString(),
                width / 2f,
                slotHeight * (index + 0.5f) + textOffset,
                paint,
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
                selectLetterAt(event.y)
                return true
            }

            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun selectLetterAt(y: Float) {
        if (height == 0) return
        val index = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[index]
        if (letter !in availableLetters || letter == selectedLetter) return
        selectedLetter = letter
        invalidate()
        onLetterSelected?.invoke(letter)
    }
}
