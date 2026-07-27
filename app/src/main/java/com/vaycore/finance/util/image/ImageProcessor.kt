package com.vaycore.finance.util.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/** Centralized image compression, orientation correction, and cache storage. */
object ImageProcessor {

    fun compressToCache(
        context: Context,
        inputUri: Uri?,
        targetWidth: Int = 1024,
        targetHeight: Int = 768,
        maxFileSizeKb: Int = 250,
    ): Uri? {
        if (inputUri == null) return null

        val originalBitmap = context.contentResolver.openInputStream(inputUri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null

        val rotatedBitmap = rotateByDegrees(originalBitmap, readExifRotation(context, inputUri))
        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)

        return try {
            val compressedBytes = compressJpeg(scaledBitmap, maxFileSizeKb)
            val outputFile = saveJpegToCache(context, compressedBytes, "compressed")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
        } finally {
            if (scaledBitmap !== rotatedBitmap && !scaledBitmap.isRecycled) scaledBitmap.recycle()
            if (rotatedBitmap !== originalBitmap && !rotatedBitmap.isRecycled) rotatedBitmap.recycle()
            if (!originalBitmap.isRecycled) originalBitmap.recycle()
        }
    }

    fun readExifRotation(context: Context, uri: Uri): Int = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.contentResolver.openInputStream(uri)?.use(::ExifInterface)
                ?.let(::rotationFromExif)
                ?: 0
        } else {
            val tempFile = File.createTempFile("image_exif_", ".jpg", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use(input::copyTo)
                }
                rotationFromExif(ExifInterface(tempFile.absolutePath))
            } finally {
                tempFile.delete()
            }
        }
    }.getOrDefault(0)

    fun rotateByDegrees(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(degrees.toFloat()) },
            true,
        )
    }

    fun saveJpegToCache(context: Context, bytes: ByteArray, prefix: String = "face"): File {
        val outputFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { it.write(bytes) }
        return outputFile
    }

    private fun compressJpeg(bitmap: Bitmap, maxFileSizeKb: Int): ByteArray {
        var quality = 90
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.toByteArray()
            }
            quality -= 5
        } while (bytes.size / 1024 > maxFileSizeKb && quality > 10)
        return bytes
    }

    private fun rotationFromExif(exif: ExifInterface): Int = when (
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}
