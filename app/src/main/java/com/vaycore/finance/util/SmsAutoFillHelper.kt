package com.vaycore.finance.util

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsAutoFillHelper(
    private val onCodeReceived: (code: String) -> Unit
) {
    // OTP regex, adjust per actual SMS format
    private val codeRegex = Regex("\\b(\\d{4,6})\\b")

    private var launcher: ActivityResultLauncher<Intent>? = null
    private var receiver: BroadcastReceiver? = null
    private var context: Context? = null

    // ── call from Fragment ──────────────────────────────────
    fun register(fragment: Fragment) {
        context = fragment.requireContext()
        launcher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                parseSms(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))
            }
        }
    }

    // ── call from Activity ─────────────────────────────────
    fun register(activity: AppCompatActivity) {
        context = activity
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                parseSms(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))
            }
        }
    }

    /** Start listening for SMS, call after "Get OTP" button click */
    fun startListening() {
        val ctx = context ?: return
        SmsRetriever.getClient(ctx).startSmsUserConsent(null)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx2: Context, intent: Intent) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return
                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                if (status.statusCode == CommonStatusCodes.SUCCESS) {
                    val consentIntent =
                        extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    consentIntent?.let { launcher?.launch(it) }
                }
            }
        }

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                ctx,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    /** Call when page is destroyed to prevent leaks */
    fun unregister() {
        receiver?.let { context?.unregisterReceiver(it) }
        receiver = null
        context = null
    }

    // ── private ────────────────────────────────────────────
    private fun parseSms(sms: String?) {
        sms ?: return
        codeRegex.find(sms)?.groupValues?.get(1)?.let { onCodeReceived(it) }
    }
}