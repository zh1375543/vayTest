package com.vaycore.finance.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.ACT_inApp
import com.vaycore.finance.data.local.EnterTime
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.data.local.pCount
import com.vaycore.finance.databinding.SplashActivityBinding
import com.vaycore.finance.data.PageHome
import com.vaycore.finance.ui.navigation.MainNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

import com.vaycore.finance.util.viewBinding

class SplashActivity :
    BaseActivity<SplashActivityBinding>() {

    override val binding by viewBinding(SplashActivityBinding::inflate)

    private var hasJump = false
    private var timeoutJob: Job? = null

    override fun initView() = with(binding) {
        applySplashSystemBars()
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return@with
        }
        App.appViewModel.apply {
            getAppSecret()
            hasDeviceInfo(PageHome) {}
            recordEvent(
                TrackBean(
                    act = ACT_inApp
                )
            )
            pCount = 0
            EnterTime = System.currentTimeMillis().toString()
        }
        // 10 second timeout
        timeoutJob = lifecycleScope.launch {
            delay(10_000.milliseconds)
            jumpNext()
        }
    }

    override fun initObserve() = with(App.appViewModel) {
        super.initObserve()
        secretResult.observe(this@SplashActivity) {
            timeoutJob?.cancel()
            jumpNext()
        }
    }

    private fun jumpNext() {
        if (hasJump) return
        hasJump = true
        if (isLogin) {
            MainNavigator.launch(this@SplashActivity, clearTask = true)
        } else {
            startActivity(
                Intent(this@SplashActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

    }

    private fun applySplashSystemBars() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            insets
        }
    }
}
