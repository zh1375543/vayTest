package com.vaycore.finance.util.loanevent

import com.vaycore.finance.app.App
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Serializes in-memory event lines and their persistence to the event-log file. */
class EventLogStore {

    private val pendingLines = mutableListOf<String>()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, EVENT_LOG_THREAD_NAME).apply { priority = Thread.NORM_PRIORITY }
    }
    private var eventFileSuffix: String? = null

    fun setEventFileSuffix(suffix: String?) {
        executor.execute { eventFileSuffix = suffix }
    }

    fun append(lineProvider: () -> String) {
        executor.execute {
            val line = lineProvider()
            if (line !in pendingLines) pendingLines += line
        }
    }

    fun flush() {
        executor.execute { writePendingLines() }
    }

    suspend fun flushAndGetFile(): File? = suspendCancellableCoroutine { continuation ->
        executor.execute {
            val file = writePendingLines()
            if (continuation.isActive) continuation.resume(file)
        }
    }

    private fun writePendingLines(): File? {
        val suffix = eventFileSuffix?.takeIf { it.isNotEmpty() } ?: return null
        val logFile = File(App.appContext.cacheDir, "$FILE_NAME_PREFIX$suffix.txt")
        if (!createOrExistsFile(logFile)) return null

        try {
            PrintWriter(FileWriter(logFile, true)).use { writer ->
                pendingLines.forEach(writer::println)
            }
            pendingLines.clear()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        return logFile
    }

    private fun createOrExistsFile(file: File): Boolean {
        val parent = file.parentFile ?: return false
        if (!parent.exists() && !parent.mkdirs()) return false
        if (file.exists()) return true
        return try {
            file.createNewFile()
        } catch (exception: IOException) {
            exception.printStackTrace()
            false
        }
    }

    private companion object {
        const val FILE_NAME_PREFIX = "loan_submit_event_file_"
        const val EVENT_LOG_THREAD_NAME = "LoanSubmitEventLogThread"
    }
}
