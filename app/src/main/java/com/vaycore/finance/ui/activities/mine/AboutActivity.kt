package com.vaycore.finance.ui.activities.mine

import com.vaycore.finance.BuildConfig
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.AboutActivityBinding
import com.vaycore.finance.util.viewBinding

class AboutActivity : BaseActivity<AboutActivityBinding>() {

    override val binding by viewBinding(AboutActivityBinding::inflate)

    override fun initView() {
        binding.tvVersion.text = BuildConfig.VERSION_NAME
    }
}
