package com.vaycore.finance.message

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityNoticeListBinding
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding

class NoticeListActivity : BaseActivity<ActivityNoticeListBinding>() {

    override val binding by viewBinding(ActivityNoticeListBinding::inflate)
    private val vm by viewModels<MessageCenterViewModel>()

    private val messageAdapter by lazy {
        NoticeAdapter().apply {
            setOnItemClickListener { item, _ ->
                start<NoticeDetailActivity> {
                    putExtra("msg", item)
                }
                vm.markAsRead(item)
            }
        }
    }

    override fun initView() {
        setupMessageList()
        setupLoadingActions()
        setupToolbarActions()
    }

    private fun setupMessageList() = with(binding) {
        rvMessage.adapter = messageAdapter
        viewModel = vm
    }

    private fun setupLoadingActions() = with(binding) {
        loadingLayout.showLoading()
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getMessageList {
                loadingLayout.showError()
            }
        }
        vm.getMessageList {
            loadingLayout.showError()
        }
    }

    private fun setupToolbarActions() = with(binding) {
        tvOpen.singleClick {
            requestRuntimePermissions(
                arrayOf(PermissionLists.getPostNotificationsPermission()),
                refuseAction = { isPermanentlyDenied, deniedPermissions ->
                    if (isPermanentlyDenied) {
                        XXPermissions.startPermissionActivity(
                            this@NoticeListActivity,
                            deniedPermissions,
                        )
                    }
                },
                isShowGuide = false,
            ) {}
        }
        titleBar.setAction {
            showConfirmDialog(title = getString(R.string.read_msg_title), desc = "") {
                vm.markAllAsRead()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.noticeLayout.isVisible = !XXPermissions.isGrantedPermission(
            this,
            PermissionLists.getPostNotificationsPermission()
        )
    }

    override fun initObserve() = with(vm) {
        super.initObserve()
        msgResult.observe(this@NoticeListActivity) { messages ->
            binding.apply {
                titleBar.showAction(messages.any { !it.readStatus })
                if (messages.isNullOrEmpty()) {
                    loadingLayout.showEmpty(R.mipmap.ic_notice_null, R.string.empty_message)
                } else {
                    loadingLayout.showContent()
                }
            }
        }
    }
}
