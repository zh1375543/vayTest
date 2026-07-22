package com.vaycore.finance.ui.sidepage.frg

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.data.local.sideBean.PlanHomeResponse
import com.vaycore.finance.databinding.SidepageHomeFragmentBinding
import com.vaycore.finance.ui.activities.LoginActivity
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.sidepage.act.AddPlanActivity
import com.vaycore.finance.ui.sidepage.act.PlanDetailsActivity
import com.vaycore.finance.ui.sidepage.adapter.PlanAdapter
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.formatAmountWithPrefix
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

/** Overview page without analytics hooks. */
class OverviewFragment : BaseFragment<SidepageHomeFragmentBinding>(
    R.layout.sidepage_home_fragment
) {
    override val binding by viewBinding(SidepageHomeFragmentBinding::bind)
    private val viewModel by viewModels<SideHomeViewModel>()
    private val planAdapter by lazy { PlanAdapter() }

    override fun initView() = with(binding) {
        rvPlan.adapter = planAdapter
        planAdapter.setOnItemClickListener { plan, _ ->
            plan.id?.let { planId ->
                context?.start<PlanDetailsActivity> {
                    putExtra(PlanDetailsActivity.EXTRA_PLAN_ID, planId)
                }
            }
        }
        tvAddPlan.singleClick {
            openLoginIfNeeded()
        }
        tvLogin.singleClick {
            openLoginIfNeeded()
        }
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        loadingLayout.setOnRetryClickListener {
            refreshData()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        if (!isLogin) {
            binding.loadingLayout.showContent()
            binding.swipeRefreshLayout.isRefreshing = false
            renderLoggedOutState()
            return
        }
        binding.loadingLayout.showLoading()
        viewModel.getPlanHomeData()
    }

    private fun renderLoggedOutState() = with(binding) {
        tvLoanAmount.setText(R.string.portal_masked_amount)
        tvSavedAmount.setText(R.string.portal_masked_saved_amount)
        ivPlanState.setImageResource(R.mipmap.ic_at_no_login)
        tvEmptyPlanMessage.setText(R.string.portal_login_to_view_plan)
        tvLogin.isVisible = true
        ivPlanState.isVisible = true
        emptyPlanCard.isVisible = true
        rvPlan.isVisible = false
        planAdapter.submitItems(emptyList())
        tvPlanTitle.setText(R.string.portal_my_plan)
    }

    private fun renderPlanHome(data: PlanHomeResponse?) = with(binding) {
        loadingLayout.showContent()
        tvLoanAmount.text = data?.totalSavedAmount.formatAmountWithPrefix()
        tvSavedAmount.text = data?.monthSavedAmount.formatAmountWithPrefix()
        tvLogin.isVisible = false

        val plans = data?.planList.orEmpty()
        val hasPlan = plans.isNotEmpty()
        ivPlanState.isVisible = !hasPlan
        emptyPlanCard.isVisible = !hasPlan
        rvPlan.isVisible = hasPlan
        tvPlanTitle.setText(
            if (hasPlan) R.string.home_question_title else R.string.portal_my_plan
        )
        planAdapter.submitItems(plans)

        if (!hasPlan) {
            ivPlanState.setImageResource(R.mipmap.ic_at_no_plan)
            tvEmptyPlanMessage.setText(R.string.portal_no_plan_yet)
        }
    }

    private fun openLoginIfNeeded() {
        if (isLogin) {
            context?.start<AddPlanActivity>()
        } else {
            context?.start<LoginActivity>()
        }
    }

    override fun initObserve() {
        viewModel.planHomeResult.observe(viewLifecycleOwner) {
            renderPlanHome(it)
        }
        viewModel.requestCompleted.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
        viewModel.planHomeFailed.observe(viewLifecycleOwner) {
            if (it.getContentIfNotHandled() != null) {
                binding.loadingLayout.showError()
            }
        }
    }

}
