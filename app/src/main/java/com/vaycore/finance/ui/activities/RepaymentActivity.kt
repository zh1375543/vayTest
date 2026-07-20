package com.vaycore.finance.ui.activities

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.vaycore.finance.R
import com.vaycore.finance.ui.adapters.RepaymentAdapter
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.data.local.bean.BankAccountResponse
import com.vaycore.finance.databinding.RepaymentActivityBinding
import com.vaycore.finance.util.compressImage
import com.vaycore.finance.util.copyToClipboard
import com.vaycore.finance.util.context.getColor2
import com.vaycore.finance.ui.extension.setSpannableClickableText
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.ui.chooseAccountsDialog
import com.vaycore.finance.ui.viewmodels.RepayViewModel
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.util.start
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepaymentActivity :
    BaseActivity<RepaymentActivityBinding>() {

    override val binding by viewBinding(RepaymentActivityBinding::inflate)
    private val vm by viewModels<RepayViewModel>()

    private val orderId by lazy { intent.getStringExtra("orderId") ?: "" }
    private val orderNo by lazy { intent.getStringExtra("orderNo") }
    private val amount by lazy { intent.getStringExtra("amount") }
    private val bankAdapter by lazy {
        RepaymentAdapter()
    }

    private val photoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            imgPath = result.data?.data
            lifecycleScope.launch {
                imgPath = withContext(Dispatchers.IO) {
                    compressImage(imgPath)
                }
                binding.ivChoose.loadImage(imgPath)
            }
        }
    }

    override fun initView() = with(binding) {
        tvOrderNo.setSpannableClickableText(
            getString(R.string.order_no) + ":" + orderNo,
            orderNo ?: "",
            getColor2(R.color.C_374151)
        ) {
            orderNo?.copyToClipboard()
            getString(R.string.copy_success).showToastMessage()
        }
        tvAmount.text = amount
        rvBank.adapter = bankAdapter
        loadingLayout.showLoading()
        vm.getRepayBankList {
            loadingLayout.showError()
        }
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getRepayBankList {
                loadingLayout.showError()
            }
        }
        tvSelect.singleClick {
            vm.getRepayCardList()
        }
        ivChoose.singleClick {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            photoLauncher.launch(intent)
        }
        btnSubmit.isEnabled = true
        btnSubmit.singleClick {
            if (cardInfo == null) {
                getString(R.string.please_select_repayment_account).showToastMessage()
                return@singleClick
            }
            if (imgPath == null) {
                getString(R.string.please_upload_repayment_voucher).showToastMessage()
                return@singleClick
            }
            vm.repayment(
                imgPath!!, orderId, cardInfo!!.id.toString(), cardInfo!!.type.toString()
            )
        }
    }

    private var cardInfo: BankAccountResponse? = null
    private var imgPath: Uri? = null

    override fun initObserve() =with(vm){
        super.initObserve()
        accountsResult.observe(this@RepaymentActivity) {
            binding.loadingLayout.showContent()
            bankAdapter.submitItems(it)
        }
        cardListResult.observe(this@RepaymentActivity) {
            it?.let {
                chooseAccountsDialog(
                    cardInfo?.account,
                    it,
                    true,
                    getString(R.string.choose_repayment)
                ) {
                    cardInfo = it
                    binding.tvSelect.text = it.account
                }
            }
        }
        repayResult.observe(this@RepaymentActivity) {
            start<RepaymentSuccessActivity>()
            finish()
        }
    }
}
