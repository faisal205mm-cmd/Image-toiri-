package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object MediaUtils {

    data class MediaUploadResult(
        val file: File,
        val fileName: String,
        val mimeType: String,
        val isVideo: Boolean,
        val sizeBytes: Long
    )

    /**
     * Copies a content URI (image or video from Photo Picker or File Manager) to the app's internal storage
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri): MediaUploadResult? {
        return try {
            val contentResolver = context.contentResolver
            val detectedMime = contentResolver.getType(uri) ?: getMimeTypeFromUri(uri)
            val isVideo = detectedMime.startsWith("video/", ignoreCase = true)
            val isImage = detectedMime.startsWith("image/", ignoreCase = true)

            // Extract file name
            var originalName = "media_${System.currentTimeMillis()}"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        originalName = cursor.getString(nameIndex) ?: originalName
                    }
                }
            }

            val extension = when {
                originalName.contains(".") -> originalName.substringAfterLast(".", "")
                isVideo -> "mp4"
                isImage && detectedMime.contains("png") -> "png"
                isImage && detectedMime.contains("webp") -> "webp"
                else -> "jpg"
            }

            val mediaDir = File(context.filesDir, "product_media").apply { mkdirs() }
            val cleanBaseName = originalName.substringBeforeLast(".").take(20).replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(mediaDir, "${cleanBaseName}_${System.currentTimeMillis()}.$extension")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return null

            MediaUploadResult(
                file = targetFile,
                fileName = originalName,
                mimeType = detectedMime,
                isVideo = isVideo,
                sizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a camera bitmap directly to app's internal storage
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): MediaUploadResult? {
        return try {
            val mediaDir = File(context.filesDir, "product_media").apply { mkdirs() }
            val targetFile = File(mediaDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            MediaUploadResult(
                file = targetFile,
                fileName = "Camera_${System.currentTimeMillis()}.jpg",
                mimeType = "image/jpeg",
                isVideo = false,
                sizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMimeTypeFromUri(uri: Uri): String {
        val path = uri.path?.lowercase() ?: ""
        return when {
            path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".mov") || path.endsWith(".webm") || path.endsWith(".3gp") -> "video/mp4"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }

    /**
     * Downsamples and encodes an image file to Base64 JPEG for Gemini multimodal API
     */
    fun encodeImageToBase64(file: File, maxDimension: Int = 1024): String? {
        return try {
            if (!file.exists()) return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight

            while (w > maxDimension * 2 || h > maxDimension * 2) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            // Handle Exif rotation if necessary
            try {
                val exif = ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (rotationDegrees != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) {
                        bitmap.recycle()
                        bitmap = rotated
                    }
                }
            } catch (_: Exception) {}

            // Scale to exact bounds if still larger than maxDimension
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                val newW = (bitmap.width * ratio).toInt()
                val newH = (bitmap.height * ratio).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            bitmap.recycle()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens a media file in external viewer/player using FileProvider
     */
    fun openMediaFile(context: Context, filePath: String, mimeType: String = "*/*") {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val effectiveMime = if (mimeType.isNotBlank() && mimeType != "*/*") {
                mimeType
            } else if (file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mov", ignoreCase = true) || file.name.endsWith(".mkv", ignoreCase = true)) {
                "video/*"
            } else {
                "image/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, effectiveMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open Media"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
