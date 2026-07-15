package com.vaycore.finance.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.SystemClock
import android.text.TextUtils
import com.vaycore.finance.App
import com.vaycore.finance.data.local.loginInfo
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.Objects

class LoanEventUtil private constructor() {
    private var mBaseServerTime = -1L
    private var mBaseSystemLocalTime = -1L
    private val logLineList: MutableList<String> = ArrayList()

    private val mHandlerThread: HandlerThread = HandlerThread(EVENT_LOG_THREAD_NAME, Thread.NORM_PRIORITY)

    private val mHandler: Handler?


    private var mEventFileSuffix: String? = (loginInfo?.id ?: 111).toString() // event file suffix for differentiation

    fun initBaseServerTime(baseServerTime: Long) {
        this.mBaseServerTime = baseServerTime
        this.mBaseSystemLocalTime = SystemClock.elapsedRealtime() // time since boot, including deep sleep
    }

    fun initEventFileUniqueSuffix(suffix: String?) {
        this.mEventFileSuffix = suffix
    }

    private val callback = Handler.Callback { msg: Message? ->
        when (msg!!.what) {
            MSG_APPEND_LOG -> {
                val eventType = msg.obj as String?
                addEventLogLine(eventType)
            }

            MSG_FLUSH_LOG -> writeEventLines2File()
            MSG_UPLOAD_LOG_FILE -> {
                val preparedFile = writeEventLines2File()
                val handler = msg.obj as Handler?
                if (null != handler) {
                    val newMsg = Message.obtain()
                    newMsg.what = MSG_LOG_FILE_PREPARED
                    newMsg.obj = preparedFile
                    handler.sendMessage(newMsg)
                }
            }
        }
        true
    }

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.getLooper(), callback)
    }

    /** Log loan application page enter event */
    fun logViewEnterLoan() {
        logEvent(LoanSubmitPageEventConst.VIEW_ENTER_LOAN)
    }

    /** Log wallet selection event */
    fun logClickChooseWallet() {
        logEvent(LoanSubmitPageEventConst.CLICK_CHOOSE_WALLET)
    }

    /** Log wallet confirmation event */
    fun logClickConfirmWallet() {
        logEvent(LoanSubmitPageEventConst.CLICK_CONFIRM_WALLET)
    }

    /** Log view loan agreement event */
    fun logClickOpenAgreement() {
        logEvent(LoanSubmitPageEventConst.CLICK_OPEN_AGREEMENT)
    }

    /** Log agreement checkbox checked event */
    fun logCheckLoanAgreement() {
        logEvent(LoanSubmitPageEventConst.CLICK_CHECK_AGREEMENT)
    }

    /** Log loan application page exit event */
    fun logViewQuitLoan() {
        logEvent(LoanSubmitPageEventConst.VIEW_QUIT_LOAN)
    }

    /** Log loan apply button click event */
    fun logClickApplyLoan() {
        logEvent(LoanSubmitPageEventConst.CLICK_APPLY_LOAN)
    }

    /** Log confirm dialog confirm button click event */
    fun logClickSubmitLoan() {
        logEvent(LoanSubmitPageEventConst.CLICK_SUBMIT_LOAN)
    }

    /** Log confirm dialog cancel button click event */
    fun logClickCancelLoan() {
        logEvent(LoanSubmitPageEventConst.CLICK_CANCEL_LOAN)
    }

    private fun logEvent(eventType: String?) {
        if (mHandler != null) {
            val msg = mHandler.obtainMessage(MSG_APPEND_LOG)
            msg.obj = eventType
            msg.sendToTarget()
        }
    }

    /** Flush logs to file */
    fun writeLog2File() {
        if (mHandler != null) {
            val msg = mHandler.obtainMessage(MSG_FLUSH_LOG)
            msg.sendToTarget()
        }
    }

    /**
     * Prepare event log file for upload. Before upload, ensure all pending events are flushed to file.
     * After writing completes, sends a message via the provided handler.
     */
    fun preparedUploadLogFile(handler: Handler?) {
        if (mHandler != null) {
            val msg = mHandler.obtainMessage(MSG_UPLOAD_LOG_FILE)
            msg.obj = handler
            msg.sendToTarget()
        }
    }

    /**
     * Write loan application page event strings to file.
     * Returns null if file creation fails.
     */
    private fun writeEventLines2File(): File? {
        val dir = App.appContext.cacheDir
        if (null == dir) return null

        val eventFileSuffix = mEventFileSuffix
        if (TextUtils.isEmpty(eventFileSuffix)) {
            return null
        }

        val logFile = File(dir, "$logFileNameNoSuffix$eventFileSuffix.txt") // append file name suffix
        val success: Boolean = createOrExistsFile(logFile)
        if (!success) return null

        try {
            val pw = PrintWriter(FileWriter(logFile, true))
            for (line in logLineList) {
                pw.println(line)
            }

            pw.flush()
            pw.close()

            logLineList.clear() // clear after writing to file
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return logFile
    }

    private val currentEventServerTime: Long
        /** Return current event server time, based on base server time */
        get() {
            val time = SystemClock.elapsedRealtime()
            val diff = time - this.mBaseSystemLocalTime

            return this.mBaseServerTime + diff
        }

    /**
     * Return event string to write. Each event is one line in the log file, format: timestamp|eventType
     */
    private fun addEventLogLine(eventMarker: String?): String {
        val time = this.currentEventServerTime
        val line: String = join("|", time, eventMarker)
        if (!logLineList.contains(line)) {
            logLineList.add(line)
        }

        return line
    }

    private object LoanSubmitPageEventConst {
        const val VIEW_ENTER_LOAN: String = "view_enter_loan"

        const val CLICK_CHOOSE_WALLET: String = "click_choose_wallet"

        const val CLICK_CONFIRM_WALLET: String = "click_confirm_wallet"

        const val CLICK_OPEN_AGREEMENT: String = "click_open_agreement"

        const val CLICK_CHECK_AGREEMENT: String = "click_check_agreement"

        const val CLICK_APPLY_LOAN: String = "click_apply_loan"

        const val CLICK_SUBMIT_LOAN: String = "click_submit_loan"

        const val CLICK_CANCEL_LOAN: String = "click_cancel_loan"

        const val VIEW_QUIT_LOAN: String = "view_quit_loan"
    }

    companion object {
        var helper: LoanEventUtil? = null

        private const val logFileNameNoSuffix = "loan_submit_event_file_"

        private const val EVENT_LOG_THREAD_NAME = "LoanSubmitEventLogThread"

        private const val MSG_APPEND_LOG = 1 // append event log to memory
        private const val MSG_FLUSH_LOG = 2 // flush memory logs to file
        private const val MSG_UPLOAD_LOG_FILE = 3 // upload log file
        const val MSG_LOG_FILE_PREPARED: Int = 4 // all cached logs have been written to file

        val instance: LoanEventUtil
            get() {
                if (helper == null) {
                    helper = LoanEventUtil()
                }
                return helper!!
            }

        fun createOrExistsFile(file: File): Boolean {
            if (!Objects.requireNonNull<File?>(file.getParentFile())
                    .exists() && !file.getParentFile().mkdirs()
            ) {
                return false
            }
            if (!file.exists()) {
                try {
                    return file.createNewFile()
                } catch (e: IOException) {
                    // file creation failed
                    e.printStackTrace()
                    return false
                }
            } else {
                // file already exists
                return true
            }
        }

        private fun join(delimiter: CharSequence?, vararg params: Any?): String {
            val sb = StringBuilder()
            var firstTime = true
            for (obj in params) {
                if (firstTime) {
                    firstTime = false
                } else {
                    sb.append(delimiter)
                }
                sb.append(obj)
            }
            return sb.toString()
        }
    }
}
