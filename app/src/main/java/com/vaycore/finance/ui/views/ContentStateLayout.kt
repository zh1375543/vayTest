package com.vaycore.finance.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.vaycore.finance.R

class ContentStateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class DisplayState {
        ERROR,
        EMPTY,
        LOADING,
        CONTENT,
    }

    private lateinit var errorStateView: View
    private lateinit var emptyStateView: View
    private lateinit var loadingStateView: View
    private var contentViews: List<View> = emptyList()
    private var currentState = DisplayState.CONTENT
    private var retryClickListener: OnClickListener? = null

    init {
        inflateStateViews(attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        contentViews = (0 until childCount)
            .map(::getChildAt)
            .filterNot(::isStateView)
        bindRetryAction()
        render(DisplayState.CONTENT)
    }

    /** Shows the default empty state. */
    fun showEmpty() {
        render(DisplayState.EMPTY)
    }

    /** Shows an empty state with page-specific artwork and localized text. */
    fun showEmpty(@DrawableRes imageRes: Int, @StringRes textRes: Int) {
        showEmpty(imageRes, context.getText(textRes))
    }

    /** Shows an empty state with page-specific artwork and dynamic text. */
    fun showEmpty(@DrawableRes imageRes: Int, text: CharSequence) {
        emptyStateView.findViewById<TextView>(R.id.tvEmptyState)?.apply {
            setCompoundDrawablesWithIntrinsicBounds(0, imageRes, 0, 0)
            this.text = text
        }
        render(DisplayState.EMPTY)
    }

    fun showError() {
        render(DisplayState.ERROR)
    }

    fun showLoading() {
        render(DisplayState.LOADING)
    }

    fun showContent() {
        render(DisplayState.CONTENT)
    }

    fun setOnRetryClickListener(listener: OnClickListener?) {
        retryClickListener = listener
    }

    private fun render(state: DisplayState) {
        currentState = state
        errorStateView.isVisible = state == DisplayState.ERROR
        emptyStateView.isVisible = state == DisplayState.EMPTY
        loadingStateView.isVisible = state == DisplayState.LOADING
        contentViews.forEach { contentView ->
            contentView.isVisible = state == DisplayState.CONTENT
        }
    }

    private fun inflateStateViews(attrs: AttributeSet?) {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ContentStateLayout,
            0,
            0,
        )
        try {
            val errorLayoutRes = attributes.getResourceId(
                R.styleable.ContentStateLayout_errorView,
                R.layout.view_error_state,
            )
            val emptyLayoutRes = attributes.getResourceId(
                R.styleable.ContentStateLayout_emptyView,
                R.layout.empty_layout,
            )
            val loadingLayoutRes = attributes.getResourceId(
                R.styleable.ContentStateLayout_loadingView,
                R.layout.view_loading_state,
            )
            val inflater = LayoutInflater.from(context)
            errorStateView = inflateChild(inflater, errorLayoutRes)
            emptyStateView = inflateChild(inflater, emptyLayoutRes)
            loadingStateView = inflateChild(inflater, loadingLayoutRes)
        } finally {
            attributes.recycle()
        }
    }

    private fun inflateChild(inflater: LayoutInflater, layoutRes: Int): View {
        return inflater.inflate(layoutRes, this, false).also(::addView)
    }

    private fun isStateView(view: View): Boolean {
        return view === errorStateView || view === emptyStateView || view === loadingStateView
    }

    private fun bindRetryAction() {
        errorStateView.findViewById<View>(R.id.tvEmpty)?.setOnClickListener { clickedView ->
            if (currentState == DisplayState.ERROR) {
                retryClickListener?.onClick(clickedView)
            }
        }
    }
}
