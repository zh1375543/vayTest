package com.vaycore.finance.base

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.vaycore.finance.App
import com.vaycore.finance.data.ACT_back
import com.vaycore.finance.data.ACT_copy
import com.vaycore.finance.data.ACT_paste
import com.vaycore.finance.data.PageAll
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.ui.activities.LoginActivity
import com.vaycore.finance.util.AppStackUtil
import com.vaycore.finance.util.SPUtil
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.activities.SplashActivity
import com.vaycore.finance.ui.createLoadingDialog
import com.vaycore.finance.ui.createVersionUpdateDialog
import com.vaycore.finance.ui.views.StyledEditTextView
import com.vaycore.finance.util.setSystemBar
import com.vaycore.finance.util.start
import kotlinx.coroutines.launch
import java.util.Locale

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private val loadingDialog by lazy { createLoadingDialog() }

    private val updateDialog by lazy { createVersionUpdateDialog() }

    protected abstract val binding: VB
    protected open val adjustForImeInsets: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSystemBar(darkMode = true, adjustForIme = adjustForImeInsets)
        AppStackUtil.addActivity(this)
        setupDataBindingLifecycle()
        initView()
        initObserve()
        observeGlobalViewModel()
        setupPasteListener(findViewById(android.R.id.content))
        setupClipboardListener()
    }

    /** Connects LiveData in migrated Data Binding layouts to this Activity lifecycle. */
    private fun setupDataBindingLifecycle() {
        (binding as? ViewDataBinding)?.lifecycleOwner = this
    }

    private fun showLoading() {
        lifecycleScope.launch {
            loadingDialog.show()
        }
    }

    private fun hideLoading() {
        lifecycleScope.launch {
            loadingDialog.dismiss()
        }
    }

    override fun getResources(): Resources {
        val resources = super.getResources()
        val configuration = resources.configuration
        configuration.setLayoutDirection(configuration.locale)
        configuration.fontScale = 1f
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return resources
    }

    override fun attachBaseContext(newBase: Context) {
        val configuration = newBase.resources.configuration
        configuration.setLocale(Locale.US)
        val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(Locale.US))
            newBase.createConfigurationContext(configuration)
        } else {
            val resources = newBase.resources
            val dm = resources.displayMetrics
            resources.updateConfiguration(configuration, dm)
            newBase
        }
        super.attachBaseContext(ctx)
    }

    fun logOut(isToLogin: Boolean = true) {
        lifecycleScope.launch {
            SPUtil.getInstance().clear()
            if (isToLogin) {
                start<LoginActivity>()
            }
        }
    }

    abstract fun initView()

    open fun initObserve() {}

    protected fun applyTopInset(target: View) {
        val startPadding = target.paddingStart
        val topPadding = target.paddingTop
        val endPadding = target.paddingEnd
        val bottomPadding = target.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPaddingRelative(
                startPadding,
                topPadding + systemBars.top,
                endPadding,
                bottomPadding
            )
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }

    protected fun applyBottomInset(target: View) {
        val startPadding = target.paddingStart
        val topPadding = target.paddingTop
        val endPadding = target.paddingEnd
        val bottomPadding = target.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPaddingRelative(
                startPadding,
                topPadding,
                endPadding,
                bottomPadding + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }

    protected fun applyBottomMarginInset(target: View) {
        val baseBottomMargin = (target.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
                ?: return@setOnApplyWindowInsetsListener insets
            val keyboardOffset = if (isImeVisible) {
                (ime.bottom - systemBars.bottom).coerceAtLeast(0)
            } else {
                0
            }
            params.bottomMargin = baseBottomMargin + keyboardOffset
            view.layoutParams = params
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }

    private fun observeGlobalViewModel() {
        if (this is SplashActivity) return
        App.appViewModel.isShowLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }
        App.appViewModel.errorResponse.observe(this) { event ->
            event.getContentIfNotHandled()?.let { response ->
                when (response.code) {
                    300 -> {
                        updateDialog.show()
                    }

                    401, 402 -> {
                        response.message.showToastMessage()
                        logOut(true)
                    }

                    409 -> {
                        App.appViewModel.getAppSecret()
                    }

                    else -> {
                        if (!response.message.isNullOrBlank() && !response.disabledToast)
                            response.message.showToastMessage()
                    }
                }
            }
        }
    }

    fun onBackAction(vm: BaseViewModel? = null, action: () -> Unit) {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                vm?.recordEvent(
                    TrackBean(
                        act = ACT_back,
                        p = localClassName
                    )
                )
                action()
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupPasteListener(root: ViewGroup) {
        traverseView(root) { editText ->
            (editText as? StyledEditTextView)?.onPasteListener = { content ->
                App.appViewModel.recordEvent(
                    TrackBean(
                        p = PageAll,
                        act = ACT_paste,
                        result = System.currentTimeMillis().toString() + "|" + content
                    )
                )
            }
        }
    }

    private fun traverseView(view: View, callback: (View) -> Unit) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverseView(view.getChildAt(i), callback)
            }
        }
        callback(view)
    }

    private val clipboard by lazy { getSystemService(CLIPBOARD_SERVICE) as ClipboardManager }
    val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clipData = clipboard.primaryClip
        val content = clipData?.getItemAt(0)?.text.toString()

        App.appViewModel.recordEvent(
            TrackBean(
                p = PageAll,
                act = ACT_copy,
                result = System.currentTimeMillis().toString() + "|" + content
            )
        )
    }

    private fun setupClipboardListener() {
        clipboard.addPrimaryClipChangedListener(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboard.removePrimaryClipChangedListener(listener)
    }
}
