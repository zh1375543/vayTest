package com.vaycore.finance.util

import android.util.Log
import com.vaycore.finance.BuildConfig

object LogUtil {
    var className: String? = null // class name
    var methodName: String? = null // method name
    var lineNumber: Int = 0 // line number

    val isDebuggable = BuildConfig.DEBUG

    // max length per log segment
    private const val LOG_MAXLENGTH = 10000

    fun myLog(TAG: String?, msg: String) {
        val strLength = msg.length
        var start = 0
        var end = LOG_MAXLENGTH
        for (i in 0..3999) {
            // if remaining text still exceeds max length, continue splitting
            if (strLength > end) {
                Log.e(TAG + i, msg.substring(start, end))
                start = end
                end += LOG_MAXLENGTH
            } else {
                Log.e(TAG, msg.substring(start, strLength))
                break
            }
        }
    }

    fun loge(TAG: String, str: String) {
        var str = str
        val max_str_length = 2001 - TAG.length
        // when length exceeds 4000
        while (str.length > max_str_length) {
            Log.e(TAG, str.take(max_str_length))
            str = str.substring(max_str_length)
        }
        // remaining part
        Log.e(TAG, str)
    }

    private fun createLog(log: String?): String {
        val buffer = StringBuffer()
        buffer.append(methodName)
        buffer.append("(").append(className).append(":").append(lineNumber).append(")")
        buffer.append(log)
        return buffer.toString()
    }

    private fun getMethodNames(sElements: Array<StackTraceElement?>) {
        className = sElements[1]!!.fileName
        methodName = sElements[1]!!.methodName
        lineNumber = sElements[1]!!.lineNumber
    }


    fun e(message: String?) {
        if (message == null) return
        if (!isDebuggable) return

        // Throwable instance must be created before any methods
        getMethodNames(Throwable().stackTrace)
        LogUtil.loge(className!!, createLog(message))
    }


    fun i(message: String?) {
        if (!isDebuggable) return

        getMethodNames(Throwable().stackTrace)
        Log.i(className, createLog(message))
    }

    fun d(message: String?) {
        if (!isDebuggable) return

        getMethodNames(Throwable().stackTrace)
        Log.d(className, createLog(message))
    }

    fun v(message: String?) {
        if (!isDebuggable) return

        getMethodNames(Throwable().stackTrace)
        Log.v(className, createLog(message))
    }

    fun w(message: String?) {
        if (!isDebuggable) return

        getMethodNames(Throwable().stackTrace)
        Log.w(className, createLog(message))
    }

    fun wtf(message: String?) {
        if (!isDebuggable) return

        getMethodNames(Throwable().stackTrace)
        Log.wtf(className, createLog(message))
    }
}