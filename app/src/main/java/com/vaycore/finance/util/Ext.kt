package com.vaycore.finance.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.LifecycleCoroutineScope
import com.vaycore.finance.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.math.BigDecimal
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.collections.iterator
import kotlin.time.Duration.Companion.milliseconds

/** Countdown timer implementation */
fun LifecycleCoroutineScope.countdownTimer(
    time: Long = 59,
    start: (scop: CoroutineScope) -> Unit = {},
    end: () -> Unit,
    next: (time: Long) -> Unit,
): Job {
    return launch {
        flow {
            (time downTo 0).forEach {
                delay(1000.milliseconds)
                emit(it)
            }
        }.onStart {
            // countdown started, disable button here if needed
            start(this@launch)

        }.onCompletion {
            // countdown finished, re-enable button here
            end()
        }.catch {
        }.collect {
            // update UI value here
            next(it)
        }

    }
}

fun Uri.uriToPart(
    partName: String,
): MultipartBody.Part {
    val inputStream = App.appContext.contentResolver.openInputStream(this)

    val file =
        File(App.appContext.cacheDir, "${partName}_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { output ->
        inputStream?.copyTo(output)
    }

    val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, file.name, requestBody)
}

fun Map<String, String>.generateRequestBody(): Map<String, RequestBody> {
    val requestBodyMap = HashMap<String, RequestBody>()
    for (key in keys) {
        val requestBody =
            (if (this[key] == null) "" else this[key]!!
                    ).toRequestBody("multipart/form-data".toMediaTypeOrNull())
        requestBodyMap[key] = requestBody
    }
    return requestBodyMap
}

fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix()
    matrix.postRotate(degrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}


fun BigDecimal?.isPositive(): Boolean {
    return (this ?: BigDecimal(0)) > BigDecimal.ZERO
}

fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addresses = intf.inetAddresses
            for (addr in addresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun isNetworkAvailable(): Boolean {
    val manager =
        App.appContext.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.activeNetworkInfo?.isAvailable == true
}

@SuppressLint("Range")
fun Uri.getContactInfo(action: (String, String) -> Unit) {
    val cursor = App.appContext.contentResolver?.query(
        this,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            val name =
                it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val number =
                it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
//            Log.e("Contact", "name:$name, phone:$number")
            action(name, number)
        }
    }
}

inline fun <reified T : Activity> Context.start(block: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java)
    block(intent)
    startActivity(intent)
}
