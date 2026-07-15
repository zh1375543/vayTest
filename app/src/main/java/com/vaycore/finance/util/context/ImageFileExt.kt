package com.vaycore.finance.util.context

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun Context.saveBytesToCacheJpg(bytes: ByteArray): File {
    val file = File(cacheDir, "face_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output -> output.write(bytes) }
    return file
}
