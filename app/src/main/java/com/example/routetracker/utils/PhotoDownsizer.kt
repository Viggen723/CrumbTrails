package com.example.routetracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import coil.size.Size
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Downsizes a picked photo using Coil's own decode/sampling pipeline, then writes
 * the result to app-private storage as a JPEG. Returns the absolute file path of
 * the saved copy, or null if loading/saving failed for that particular photo.
 *
 * Saved layout: <filesDir>/route_photos/<routeId>/<uuid>.jpg
 */
object PhotoDownsizer {

    private const val MAX_DIMENSION_PX = 1024
    private const val JPEG_QUALITY = 80

    suspend fun downsizeAndSave(
        context: Context,
        sourceUri: Uri,
        routeId: String,
        maxDimensionPx: Int = MAX_DIMENSION_PX,
        jpegQuality: Int = JPEG_QUALITY
    ): String? {
        val bitmap = loadDownsized(context, sourceUri, maxDimensionPx) ?: return null
        return runCatching {
            val savedPath = saveToFile(context, bitmap, routeId, jpegQuality)
            copyExifAttributes(context, sourceUri, savedPath)
            savedPath
        }.getOrNull()
    }

    private suspend fun loadDownsized(
        context: Context,
        sourceUri: Uri,
        maxDimensionPx: Int
    ): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(sourceUri)
            .size(Size(maxDimensionPx, maxDimensionPx))
            .scale(Scale.FIT)
            .allowHardware(false) // need a software bitmap so we can compress/write it
            .build()

        val result = context.imageLoader.execute(request)
        if (result !is SuccessResult) return null

        return result.drawable.toBitmap()
    }

    private fun saveToFile(
        context: Context,
        bitmap: Bitmap,
        routeId: String,
        jpegQuality: Int
    ): String {
        val directory = File(context.filesDir, "route_photos/$routeId").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)
        }
        return file.absolutePath
    }

    private fun copyExifAttributes(
        context: Context,
        sourceUri: Uri,
        destinationPath: String
    ) {
        val sourceExif = context.openOriginalMediaInputStream(sourceUri)?.use { inputStream ->
            ExifInterface(inputStream)
        } ?: return

        val destinationExif = ExifInterface(destinationPath)
        EXIF_ATTRIBUTES_TO_COPY.forEach { attribute ->
            sourceExif.getAttribute(attribute)?.let { value ->
                destinationExif.setAttribute(attribute, value)
            }
        }
        destinationExif.saveAttributes()
    }

    private fun Context.openOriginalMediaInputStream(sourceUri: Uri) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val originalUriResult = runCatching {
                MediaStore.setRequireOriginal(sourceUri)
            }
            val originalUri = originalUriResult.getOrDefault(sourceUri)

            runCatching {
                contentResolver.openInputStream(originalUri)
            }.getOrElse {
                contentResolver.openInputStream(sourceUri)
            }
        } else {
            contentResolver.openInputStream(sourceUri)
        }

    private val EXIF_ATTRIBUTES_TO_COPY = listOf(
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP
    )
}
