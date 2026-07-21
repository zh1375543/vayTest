package com.vaycore.finance.ui.activities

import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.vaycore.finance.R
import com.vaycore.finance.ui.adapters.NoticeAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.ActivityNoticeListBinding
import com.vaycore.finance.ui.viewmodels.MessageCenterViewModel
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

    override fun initView() = with(binding) {
        rvMessage.adapter = messageAdapter
        viewModel = vm
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
        tvOpen.singleClick {
            requestRuntimePermissions(
                arrayOf(PermissionLists.getPostNotificationsPermission()),
                refuseAction = { it, p ->
                    if (it) {
                        XXPermissions.startPermissionActivity(this@NoticeListActivity, p)
                    }
                },
                isShowGuide = false
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

    override fun initObserve() =with(vm){
        super.initObserve()
        msgResult.observe(this@NoticeListActivity) {
            binding.apply {
                titleBar.showAction(it.any { it1 -> !it1.readStatus })
                if (it.isNullOrEmpty()) {
                    loadingLayout.showEmpty(R.mipmap.ic_notice_null, R.string.empty_message)
                } else {
                    loadingLayout.showContent()
                }
            }
        }
    }
}
