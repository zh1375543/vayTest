package com.vaycore.finance.ui.activities.auth

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.permission.PermissionLists
import com.liveness.dflivenesslibrary.liveness.DFSilentLivenessActivity
import com.vaycore.finance.R
import com.vaycore.finance.App
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.databinding.KycAuthActivityBinding
import com.vaycore.finance.data.ACT_clickBack
import com.vaycore.finance.data.ACT_clickContinue
import com.vaycore.finance.data.ACT_clickNext
import com.vaycore.finance.data.ACT_in
import com.vaycore.finance.data.ACT_uploadBackEnd
import com.vaycore.finance.data.ACT_uploadBackStart
import com.vaycore.finance.data.ACT_uploadFaceEnd
import com.vaycore.finance.data.ACT_uploadFaceStart
import com.vaycore.finance.data.ACT_uploadFrontEnd
import com.vaycore.finance.data.ACT_uploadFrontStart
import com.vaycore.finance.data.PageInfoKyc
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.bean.TrackBean
import com.vaycore.finance.ui.activities.WebViewActivity
import com.vaycore.finance.util.KYC_AADHAAR_BACK_CLICK
import com.vaycore.finance.util.KYC_AADHAAR_FRONT_CLICK
import com.vaycore.finance.util.KYC_INFO_COMMIT
import com.vaycore.finance.util.KYC_INFO_PAGE
import com.vaycore.finance.util.compressImage
import com.vaycore.finance.ui.extension.resetScale
import com.vaycore.finance.util.requestRuntimePermissions
import com.vaycore.finance.util.context.saveBytesToCacheJpg
import com.vaycore.finance.util.showToastMessage
import com.vaycore.finance.ui.extension.singleClick
import com.vaycore.finance.util.trackEvent
import com.vaycore.finance.ui.showConfirmDialog
import com.vaycore.finance.ui.showKycCardExampleDialog
import com.vaycore.finance.ui.showKycSelfieExampleDialog
import com.vaycore.finance.ui.viewmodels.KycUploadViewModel
import com.vaycore.finance.ui.viewmodels.DashboardViewModel
import com.vaycore.finance.ui.extension.loadImage
import com.vaycore.finance.util.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class KycAuthActivity : BaseActivity<KycAuthActivityBinding>() {

    override val binding by viewBinding(KycAuthActivityBinding::inflate)
    private val vm by viewModels<KycUploadViewModel>()
    private val homeVm by viewModels<DashboardViewModel>()

    private val isCert by lazy { intent.getBooleanExtra("isCert", false) }
    private val frontLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                frontUri = withContext(Dispatchers.IO) {
                    compressImage(photoUri)
                }
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadFrontEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
//                binding.ivCardFront.load(frontUri)
                frontUri?.let { vm.submitKycFront(it) }
            }
        }
    }

    private val backLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                backUri = withContext(Dispatchers.IO) {
                    compressImage(photoUri)
                }
//                binding.ivCardBack.load(backUri)
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadBackEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
                backUri?.let { vm.submitKycBack(it) }
            }
        }
    }
    private val photoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                selfUri = withContext(Dispatchers.IO) {
                    compressImage(photoUri)
                }
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_uploadFaceEnd,
                        result = System.currentTimeMillis().toString()
                    )
                )
                selfUri?.let { vm.submitKycSelf(it, null) }
            }
        }
    }

    private val selfLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            App.instance.result?.let {
                if (it.livenessImageResults.isNullOrEmpty()) return@let
                lifecycleScope.launch(Dispatchers.IO) {
                    val file = saveBytesToCacheJpg(it.livenessImageResults[0].detectImage)
                    selfUri = compressImage(getUri(file))
                    vm.recordEvent(
                        TrackBean(
                            p = PageInfoKyc,
                            act = ACT_uploadFaceEnd,
                            result = System.currentTimeMillis().toString()
                        )
                    )
                    selfUri?.let { it1 ->
                        vm.submitKycSelf(
                            it1,
                            saveBytesToCacheJpg(it.livenessEncryptResult)
                        )
                    }
                }
            }
        }
    }

    private val h5Launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            vm.getH5LiveResult()
        }
    }

    private var photoUri: Uri? = null

    private var selfUri: Uri? = null
    private var frontUri: Uri? = null
    private var backUri: Uri? = null

    override fun initView() = with(binding) {
        trackEvent(KYC_INFO_PAGE)
        vm.recordEvent(
            TrackBean(
                p = PageInfoKyc,
                act = ACT_in
            )
        )
        titleBar.setNavigationAction { handleBackPressed() }
        onBackAction(vm) {
            handleBackPressed()
        }
        titleBar.setAction(
            "${authConfigList.indexOf("KYC") + 1}/${authConfigList.size}"
        )
        titleBar.showAction(!isCert)
        tvExample1.singleClick {
            showKycCardExampleDialog()
        }
        tvExample2.singleClick {
            showKycSelfieExampleDialog()
        }
        ivCardFront.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoKyc,
                    act = ACT_uploadFrontStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            requestRuntimePermissions(arrayOf(PermissionLists.getCameraPermission())) {
                trackEvent(KYC_AADHAAR_FRONT_CLICK)
                val outputFile = File(cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                photoUri = getUri(outputFile)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                setIntents(intent, false)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                frontLauncher.launch(intent)
            }
        }
        ivSelf.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoKyc,
                    act = ACT_uploadFaceStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            requestRuntimePermissions(arrayOf(PermissionLists.getCameraPermission())) {
                when (kycType) {
                    1 -> {
                        val outputFile =
                            File(cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                        photoUri = getUri(outputFile)
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        setIntents(intent, true)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        photoLauncher.launch(intent)
                    }

                    2 -> {
                        selfLauncher.launch(
                            Intent(this@KycAuthActivity, DFSilentLivenessActivity::class.java)
                                .putExtra(
                                    DFSilentLivenessActivity.KEY_DETECT_IMAGE_RESULT,
                                    true
                                )
                        )
                    }

                    3 -> {
                        vm.fetchH5Live {
                            selfLauncher.launch(
                                Intent(
                                    this@KycAuthActivity,
                                    DFSilentLivenessActivity::class.java
                                )
                                    .putExtra(
                                        DFSilentLivenessActivity.KEY_DETECT_IMAGE_RESULT,
                                        true
                                    )
                            )
                        }
                    }
                }
            }
        }
        ivCardBack.singleClick {
            vm.recordEvent(
                TrackBean(
                    p = PageInfoKyc,
                    act = ACT_uploadBackStart,
                    result = System.currentTimeMillis().toString()
                )
            )
            requestRuntimePermissions(arrayOf(PermissionLists.getCameraPermission())) {
                trackEvent(KYC_AADHAAR_BACK_CLICK)
                val outputFile = File(cacheDir, "camera_temp_${System.currentTimeMillis()}.jpg")
                photoUri = getUri(outputFile)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                setIntents(intent, false)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                backLauncher.launch(intent)
            }
        }
        bottomActionLayout.isVisible = false
        btNext.singleClick {
            if (frontLayout.isVisible) {
                if (frontUri == null && frontImageUrl.isNullOrBlank()) {
                    getString(R.string.please_upload_nic_card_front).showToastMessage()
                    return@singleClick
                }
            }
            if (backLayout.isVisible) {
                if (backUri == null && backImageUrl.isNullOrBlank()) {
                    getString(R.string.please_upload_nic_card_back).showToastMessage()
                    return@singleClick
                }
            }
            if (selfGroup.isVisible) {
                if (selfUri == null && liveImageUrl.isNullOrBlank() && vm.h5Result.value == null) {
                    getString(R.string.please_upload_self_photo).showToastMessage()
                    return@singleClick
                }
            }
            vm.recordEvent(
                TrackBean(
                    p = PageInfoKyc,
                    act = ACT_clickNext,
                )
            )
            trackEvent(KYC_INFO_COMMIT)
            vm.compareFace()
        }
        binding.frontUp.isVisible = false
        binding.backUp.isVisible = false
        binding.selfUp.isVisible = false
        binding.bottomActionLayout.isVisible = !isCert
        binding.tvSelfDesc.isVisible = !isCert
        binding.tvExample1.isVisible = !isCert
        binding.tvExample2.isVisible = !isCert
        binding.ivCardFront.isEnabled = !isCert
        binding.ivCardBack.isEnabled = !isCert
        binding.ivSelf.isEnabled = !isCert
        binding.bottomActionLayout.isVisible = !isCert
        if (!isCert) {
            btNext.resetScale()
        }
        loadingLayout.showLoading()
        loadingLayout.setOnRetryClickListener {
            loadingLayout.showLoading()
            vm.getKycConfig()
        }
        vm.getKycConfig()
    }

    private var isCompare = false
    private var kycType: Int = 1
    private var frontImageUrl: String? = null
    private var backImageUrl: String? = null
    private var liveImageUrl: String? = null
    override fun initObserve() =with(vm){
        super.initObserve()
        kycResult.observe(this@KycAuthActivity) {
            it?.let {
                binding.loadingLayout.showContent()
                frontImageUrl = it.frontImageUrl
                backImageUrl = it.backImageUrl
                liveImageUrl = it.liveImageUrl
                if (!it.frontImageUrl.isNullOrBlank()) {
                    binding.ivCardFront.loadImage(it.frontImageUrl)
                }
                if (!it.backImageUrl.isNullOrBlank()) {
                    binding.ivCardBack.loadImage(it.backImageUrl)
                }
                if (!it.liveImageUrl.isNullOrBlank()) {
                    binding.ivSelf.loadImage(it.liveImageUrl)
                }
            }
        }
        compareResult.observe(this@KycAuthActivity) {
            homeVm.getUserAuthStatus()
        }
        homeVm.userAuthStatusResult.observe(this@KycAuthActivity) {
            it?.routeToNextAuthStep(this@KycAuthActivity)
            finish()
        }
        configResult.observe(this@KycAuthActivity) {
            binding.apply {
                frontLayout.isVisible = it?.KYC_FRONT != 0
                backLayout.isVisible = it?.KYC_BACK != 0
                selfGroup.isVisible = it?.FACE != 0
                isCompare = it?.FACE_COMPARE == 1
                kycType = it?.FACE ?: 1
                idLayout.isVisible = frontLayout.isVisible ||backLayout.isVisible
            }
            vm.getKycInfo {
                binding.loadingLayout.showError()
            }
        }
        submitFrontResult.observe(this@KycAuthActivity) {
            binding.ivCardFront.loadImage(it)
            binding.frontUp.isVisible = true
        }
        submitBackResult.observe(this@KycAuthActivity) {
            binding.ivCardBack.loadImage(it)
            binding.backUp.isVisible = true
        }
        submitSelfResult.observe(this@KycAuthActivity) {
            binding.ivSelf.loadImage(it)
            binding.selfUp.isVisible = true
        }
        h5Live.observe(this@KycAuthActivity) {
            if (it.verifyUrl == null) {
                selfLauncher.launch(
                    Intent(
                        this@KycAuthActivity,
                        DFSilentLivenessActivity::class.java
                    )
                        .putExtra(
                            DFSilentLivenessActivity.KEY_DETECT_IMAGE_RESULT,
                            true
                        )
                )
                return@observe
            }
            h5Launcher.launch(
                WebViewActivity.getIntent(this@KycAuthActivity, it.verifyUrl)
            )
        }
        h5Result.observe(this@KycAuthActivity) {
            it?.let {
                binding.ivSelf.loadImage(it)
                binding.selfUp.isVisible = true
            }
        }
    }

    private fun getUri(outputFile: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            FileProvider.getUriForFile(
                this@KycAuthActivity,
                "$packageName.fileprovider",
                outputFile
            )
        else
            Uri.fromFile(outputFile)
    }

    private fun setIntents(intent: Intent, isFace: Boolean) {
        // Set camera facing based on the type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", isFace)
            intent.putExtra("camerafacing", if (isFace) "front" else "rear")
            intent.putExtra("previous_mode", if (isFace) "front" else "rear")
        }
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())

        intent.putExtra("android.intent.extras.CAMERA_FACING", if (isFace) 1 else 0)
    }

    private fun handleBackPressed() {
        if (binding.bottomActionLayout.isVisible) {
            val list = authConfigList.filterNot { it1 -> it1.isBlank() }
            val step = list.size - max(0, list.indexOf("KYC"))
            showConfirmDialog(
                desc = String.format(
                    getString(R.string.auth_exit_confirm),
                    step.toString()
                ),
                cancel = getString(R.string.give_up),
                ok = getString(R.string.continue_str),
                highLight = step.toString(),
                cancelAction = {
                    vm.recordEvent(
                        TrackBean(
                            p = PageInfoKyc,
                            act = ACT_clickBack,
                        )
                    )
                    finish()
                }
            ) {
                vm.recordEvent(
                    TrackBean(
                        p = PageInfoKyc,
                        act = ACT_clickContinue,
                    )
                )
            }
        } else {
            finish()
        }
    }
}
