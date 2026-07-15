package com.vaycore.finance.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.vaycore.finance.App
import java.io.File

private val jsonGson = GsonBuilder().disableHtmlEscaping().create()

/** Convert object to JSON string */
fun Any?.toJsonString(): String {
    return jsonGson.toJson(this)
}

inline fun <reified T> String?.parseJsonList(): List<T>? {
    return try {
        val listType = TypeToken.getParameterized(ArrayList::class.java, T::class.java).type
        gson.fromJson(this, listType)
    } catch (e: Exception) {
        LogUtil.e("jsonError:${e.message}")
        null
    }
}

val gson = Gson()
inline fun <reified T> String?.parseJson(): T? {
    return try {
        gson.fromJson(this, T::class.java)
    } catch (e: JsonSyntaxException) {
        LogUtil.e("jsonError:${e.message}")
        null
    }
}

inline fun <reified T> String.parseJsonSafely(): T? {
    return try {
        Gson().fromJson(this, object : TypeToken<T>() {}.type)
    } catch (e: JsonSyntaxException) {
        e.printStackTrace()
        null
    }
}

fun String.writeJsonToCacheFile() {
    val file = File(App.appContext.cacheDir, "json1.txt")
    file.writeText(this)
}
