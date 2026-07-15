package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.vaycore.finance.R

class SelectableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var srcSelected: Int = 0
    private var defaultResId: Int = 0

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.SelectableImageView)
        try {
            srcSelected = array.getResourceId(R.styleable.SelectableImageView_srcSelected, 0)
            defaultResId = array.getResourceId(
                R.styleable.SelectableImageView_android_src,
                0
            )
        } finally {
            array.recycle()
        }
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        if (selected && srcSelected != 0) {
            setImageResource(srcSelected)
        } else if (defaultResId != 0) {
            setImageResource(defaultResId)
        }
    }
}
