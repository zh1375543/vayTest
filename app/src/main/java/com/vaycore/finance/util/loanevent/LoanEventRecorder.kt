package com.vaycore.finance.util.loanevent

import android.os.SystemClock
import com.vaycore.finance.data.loginInfo
import java.io.File

/** Records loan events using server-aligned timestamps and a serialized event-log store. */
object LoanEventRecorder {

    private val store = EventLogStore()

    @Volatile
    private var baseServerTime = -1L

    @Volatile
    private var baseElapsedRealtime = -1L

    init {
        setEventFileSuffix((loginInfo?.id ?: 111).toString())
    }

    fun initializeBaseServerTime(serverTime: Long) {
        baseServerTime = serverTime
        baseElapsedRealtime = SystemClock.elapsedRealtime()
    }

    fun setEventFileSuffix(suffix: String?) {
        store.setEventFileSuffix(suffix)
    }

    fun record(event: LoanEvent) {
        store.append { "${currentServerTime()}|${event.marker}" }
    }

    fun flush() {
        store.flush()
    }

    suspend fun prepareUploadFile(): File? = store.flushAndGetFile()

    private fun currentServerTime(): Long = baseServerTime +
        (SystemClock.elapsedRealtime() - baseElapsedRealtime)
}
