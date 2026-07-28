package com.vaycore.finance.sidepage.act

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.vaycore.finance.databinding.SidepageFirstSavingsPlanActivityBinding
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.viewBinding
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.sidepage.PortalActivity

/** Collects an optional plan name during the one-time savings-plan introduction. */
class FirstSavingsPlanActivity : BaseActivity<SidepageFirstSavingsPlanActivityBinding>() {

    override val binding by viewBinding(SidepageFirstSavingsPlanActivityBinding::inflate)

    private val planDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            completeOnboarding()
        }
    }

    override fun initView() = with(binding) {
        applyTopInset(root)
        applyBottomInset(bottomAction)

        titleBar.showNavigation(false)
        titleBar.setAction(action = ::completeOnboarding)
        btNext.singleClick {
            val planName = planNameView.getText().trim()
            if (planName.isEmpty()) {
                planNameView.showError()
                planNameView.getEditText().requestFocus()
                return@singleClick
            }
            planDetailsLauncher.launch(
                Intent(this@FirstSavingsPlanActivity, FirstSavingsPlanDetailsActivity::class.java)
                    .putExtra(FirstSavingsPlanDetailsActivity.EXTRA_PLAN_NAME, planName),
            )
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    private fun completeOnboarding() {
        startActivity(
            Intent(this, PortalActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
    }

    companion object {
        const val KEY_FIRST_LOGIN_HANDLED = "first_login_handled"
    }
}
