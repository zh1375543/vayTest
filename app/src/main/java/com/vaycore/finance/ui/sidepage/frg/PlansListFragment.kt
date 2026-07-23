package com.vaycore.finance.ui.sidepage.frg

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseFragment
import com.vaycore.finance.data.local.isLogin
import com.vaycore.finance.data.local.sideBean.PlanItem
import com.vaycore.finance.databinding.SidepagePlansListFragmentBinding
import com.vaycore.finance.ui.sidepage.act.PlanDetailsActivity
import com.vaycore.finance.ui.sidepage.act.SavingsRecordActivity
import com.vaycore.finance.ui.sidepage.adapter.CancelledPlanAdapter
import com.vaycore.finance.ui.sidepage.adapter.CompletedPlanAdapter
import com.vaycore.finance.ui.sidepage.adapter.PlanAdapter
import com.vaycore.finance.ui.viewmodels.SideHomeViewModel
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

/** Shared list shell for plans in one status. Data sources will be connected per status. */
abstract class PlansListFragment(
    private val emptyMessageRes: Int,
) : BaseFragment<SidepagePlansListFragmentBinding>(R.layout.sidepage_plans_list_fragment) {

    override val binding by viewBinding(SidepagePlansListFragmentBinding::bind)

    override fun initView() = with(binding) {
        tvEmptyPlans.setText(emptyMessageRes)
        rvPlans.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun initObserve() = Unit
}

class ActivePlansFragment : PlansListFragment(R.string.portal_no_active_plans) {

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planAdapter by lazy { PlanAdapter() }
    private val savingsRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) refreshPlans()
    }
    private var nextPage = FIRST_PAGE
    private var isLoading = false
    private var hasMorePages = true

    override fun initView() {
        super.initView()
        binding.rvPlans.apply {
            adapter = planAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0 || !shouldLoadNextPage()) return
                    loadPlans()
                }
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener { refreshPlans() }
        planAdapter.setOnItemClickListener { plan, _ -> openPlanDetails(plan) }
        planAdapter.onSaveMoney = ::openSavingsRecord
        refreshPlans()
    }

    override fun initObserve() = with(viewModel) {
        planListResult.observe(viewLifecycleOwner) { page ->
            if (page.status != ACTIVE_STATUS) return@observe
            renderPlanPage(page.pageNum, page.response?.list.orEmpty(), page.response?.pages)
        }
        planListFailed.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.takeIf { it.status == ACTIVE_STATUS } ?: return@observe
            isLoading = false
            binding.swipeRefreshLayout.isRefreshing = false
            renderEmptyState()
        }
    }

    private fun refreshPlans() {
        if (isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        nextPage = FIRST_PAGE
        hasMorePages = true
        loadPlans()
    }

    private fun loadPlans() {
        if (isLoading || !hasMorePages) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        if (!isLogin) {
            planAdapter.submitItems(emptyList())
            binding.tvEmptyPlans.setText(R.string.portal_login_to_view_plan)
            binding.tvEmptyPlans.isVisible = true
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        isLoading = true
        viewModel.getPlanList(ACTIVE_STATUS, nextPage, PAGE_SIZE)
    }

    private fun shouldLoadNextPage(): Boolean {
        val layoutManager = binding.rvPlans.layoutManager as? LinearLayoutManager ?: return false
        return hasMorePages && !isLoading &&
            layoutManager.findLastVisibleItemPosition() >= planAdapter.itemCount - LOAD_MORE_THRESHOLD
    }

    private fun renderPlanPage(
        pageNum: Int,
        plans: List<PlanItem>,
        totalPages: Int?,
    ) {
        isLoading = false
        binding.swipeRefreshLayout.isRefreshing = false
        if (pageNum == FIRST_PAGE) {
            planAdapter.submitItems(plans)
        } else {
            planAdapter.addItems(plans)
        }
        nextPage = pageNum + 1
        hasMorePages = (totalPages?.let { pageNum < it }) ?: (plans.size >= PAGE_SIZE)
        renderEmptyState()
    }

    private fun renderEmptyState() {
        binding.tvEmptyPlans.isVisible = planAdapter.itemCount == 0
    }

    private fun openPlanDetails(plan: PlanItem) {
        val planId = plan.id ?: return
        context?.start<PlanDetailsActivity> {
            putExtra(PlanDetailsActivity.EXTRA_PLAN_ID, planId)
        }
    }

    private fun openSavingsRecord(plan: PlanItem) {
        val context = context ?: return
        savingsRecordLauncher.launch(SavingsRecordActivity.createIntent(context, plan))
    }

    private companion object {
        const val ACTIVE_STATUS = 1
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 10
        const val LOAD_MORE_THRESHOLD = 3
    }
}

class CompletedPlansFragment : PlansListFragment(R.string.portal_no_completed_plans) {

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planAdapter by lazy { CompletedPlanAdapter() }
    private var nextPage = FIRST_PAGE
    private var isLoading = false
    private var hasMorePages = true

    override fun initView() {
        super.initView()
        binding.rvPlans.apply {
            adapter = planAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0 || !shouldLoadNextPage()) return
                    loadPlans()
                }
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener { refreshPlans() }
        planAdapter.setOnItemClickListener { plan, _ -> openPlanDetails(plan) }
        planAdapter.onViewDetails = ::openPlanDetails
        refreshPlans()
    }

    override fun initObserve() = with(viewModel) {
        planListResult.observe(viewLifecycleOwner) { page ->
            if (page.status != COMPLETED_STATUS) return@observe
            renderPlanPage(page.pageNum, page.response?.list.orEmpty(), page.response?.pages)
        }
        planListFailed.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.takeIf { it.status == COMPLETED_STATUS } ?: return@observe
            isLoading = false
            binding.swipeRefreshLayout.isRefreshing = false
            renderEmptyState()
        }
    }

    private fun refreshPlans() {
        if (isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        nextPage = FIRST_PAGE
        hasMorePages = true
        loadPlans()
    }

    private fun loadPlans() {
        if (isLoading || !hasMorePages) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        if (!isLogin) {
            planAdapter.submitItems(emptyList())
            binding.tvEmptyPlans.setText(R.string.portal_login_to_view_plan)
            binding.tvEmptyPlans.isVisible = true
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        isLoading = true
        viewModel.getPlanList(COMPLETED_STATUS, nextPage, PAGE_SIZE)
    }

    private fun shouldLoadNextPage(): Boolean {
        val layoutManager = binding.rvPlans.layoutManager as? LinearLayoutManager ?: return false
        return hasMorePages && !isLoading &&
            layoutManager.findLastVisibleItemPosition() >= planAdapter.itemCount - LOAD_MORE_THRESHOLD
    }

    private fun renderPlanPage(
        pageNum: Int,
        plans: List<PlanItem>,
        totalPages: Int?,
    ) {
        isLoading = false
        binding.swipeRefreshLayout.isRefreshing = false
        if (pageNum == FIRST_PAGE) {
            planAdapter.submitItems(plans)
        } else {
            planAdapter.addItems(plans)
        }
        nextPage = pageNum + 1
        hasMorePages = (totalPages?.let { pageNum < it }) ?: (plans.size >= PAGE_SIZE)
        renderEmptyState()
    }

    private fun renderEmptyState() {
        binding.tvEmptyPlans.isVisible = planAdapter.itemCount == 0
    }

    private fun openPlanDetails(plan: PlanItem) {
        val planId = plan.id ?: return
        context?.start<PlanDetailsActivity> {
            putExtra(PlanDetailsActivity.EXTRA_PLAN_ID, planId)
        }
    }

    private companion object {
        const val COMPLETED_STATUS = 2
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 10
        const val LOAD_MORE_THRESHOLD = 3
    }
}

class CancelledPlansFragment : PlansListFragment(R.string.portal_no_cancelled_plans) {

    private val viewModel by viewModels<SideHomeViewModel>()
    private val planAdapter by lazy { CancelledPlanAdapter() }
    private var nextPage = FIRST_PAGE
    private var isLoading = false
    private var hasMorePages = true

    override fun initView() {
        super.initView()
        binding.rvPlans.apply {
            adapter = planAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0 || !shouldLoadNextPage()) return
                    loadPlans()
                }
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener { refreshPlans() }
        planAdapter.setOnItemClickListener { plan, _ -> openPlanDetails(plan) }
        planAdapter.onViewDetails = ::openPlanDetails
        refreshPlans()
    }

    override fun initObserve() = with(viewModel) {
        planListResult.observe(viewLifecycleOwner) { page ->
            if (page.status != CANCELLED_STATUS) return@observe
            renderPlanPage(page.pageNum, page.response?.list.orEmpty(), page.response?.pages)
        }
        planListFailed.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.takeIf { it.status == CANCELLED_STATUS } ?: return@observe
            isLoading = false
            binding.swipeRefreshLayout.isRefreshing = false
            renderEmptyState()
        }
    }

    private fun refreshPlans() {
        if (isLoading) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        nextPage = FIRST_PAGE
        hasMorePages = true
        loadPlans()
    }

    private fun loadPlans() {
        if (isLoading || !hasMorePages) {
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }
        if (!isLogin) {
            planAdapter.submitItems(emptyList())
            binding.tvEmptyPlans.setText(R.string.portal_login_to_view_plan)
            binding.tvEmptyPlans.isVisible = true
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        isLoading = true
        viewModel.getPlanList(CANCELLED_STATUS, nextPage, PAGE_SIZE)
    }

    private fun shouldLoadNextPage(): Boolean {
        val layoutManager = binding.rvPlans.layoutManager as? LinearLayoutManager ?: return false
        return hasMorePages && !isLoading &&
            layoutManager.findLastVisibleItemPosition() >= planAdapter.itemCount - LOAD_MORE_THRESHOLD
    }

    private fun renderPlanPage(
        pageNum: Int,
        plans: List<PlanItem>,
        totalPages: Int?,
    ) {
        isLoading = false
        binding.swipeRefreshLayout.isRefreshing = false
        if (pageNum == FIRST_PAGE) {
            planAdapter.submitItems(plans)
        } else {
            planAdapter.addItems(plans)
        }
        nextPage = pageNum + 1
        hasMorePages = (totalPages?.let { pageNum < it }) ?: (plans.size >= PAGE_SIZE)
        renderEmptyState()
    }

    private fun renderEmptyState() {
        binding.tvEmptyPlans.isVisible = planAdapter.itemCount == 0
    }

    private fun openPlanDetails(plan: PlanItem) {
        val planId = plan.id ?: return
        context?.start<PlanDetailsActivity> {
            putExtra(PlanDetailsActivity.EXTRA_PLAN_ID, planId)
        }
    }

    private companion object {
        const val CANCELLED_STATUS = 3
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 10
        const val LOAD_MORE_THRESHOLD = 3
    }
}
