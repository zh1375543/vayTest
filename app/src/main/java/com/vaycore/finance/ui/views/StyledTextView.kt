package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.vaycore.finance.ui.widget.shape.ShapeAttributeReader
import com.vaycore.finance.ui.widget.shape.ShapeBackgroundController
import com.vaycore.finance.ui.widget.shape.TextStateController

/** A TextView that supports ShapeView backgrounds, state colors, and state drawables. */
class StyledTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val shapeAttributes = ShapeAttributeReader(context).read(attrs)
    private val backgroundController = ShapeBackgroundController(this)
    private var textStateController: TextStateController? = null

    init {
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
}
