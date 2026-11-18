package com.example.simplequiz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

object Ui {
    fun percent(value: Double): String = String.format(Locale.getDefault(), "%.1f%%", value)

    fun setPhoto(imageView: ImageView, photoFile: String?) {
        val file = photoFile?.let { File(imageView.context.filesDir, it) }
        if (file != null && file.isFile && file.parentFile == imageView.context.filesDir) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        }
        imageView.setImageResource(android.R.drawable.ic_menu_myplaces)
    }

    /** Decode a thumbnail, then store our own small copy so URI access need not persist. */
    fun importProfilePhoto(context: Context, uri: Uri, userId: Long): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected image.")
        boundsStream.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Please choose a valid image." }

        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("Could not read the selected image.")

        val scale = minOf(1.0, 512.0 / maxOf(decoded.width, decoded.height))
        val thumbnail = if (scale < 1.0) {
            Bitmap.createScaledBitmap(decoded,
                maxOf(1, (decoded.width * scale).toInt()),
                maxOf(1, (decoded.height * scale).toInt()), true)
        } else decoded
        val name = "profile_${userId}_${UUID.randomUUID()}.jpg"
        val target = File(context.filesDir, name)
        try {
            target.outputStream().use {
                check(thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, it)) {
                    "Could not save profile photo."
                }
            }
            return name
        } catch (exception: Exception) {
            target.delete()
            throw exception
        } finally {
            if (thumbnail !== decoded) thumbnail.recycle()
            decoded.recycle()
        }
    }
}
