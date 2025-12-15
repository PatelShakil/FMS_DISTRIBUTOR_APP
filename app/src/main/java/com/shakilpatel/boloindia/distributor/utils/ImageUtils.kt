package com.shakilpatel.boloindia.distributor.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.ContextCompat
import com.shakilpatel.boloindia.distributor.R
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Add "BoloIndia" watermark to image
     */
    fun addWatermark(context: Context, originalFile: File): File {
        val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
        val watermarkedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(watermarkedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = watermarkedBitmap.width * 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        // Background for text
        val textBounds = Rect()
        val watermarkText = "BoloIndia"
        paint.getTextBounds(watermarkText, 0, watermarkText.length, textBounds)

        val x = watermarkedBitmap.width - textBounds.width() - 30f
        val y = watermarkedBitmap.height - 30f

        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#80000000") // Semi-transparent black
            style = Paint.Style.FILL
        }

        canvas.drawRect(
            x - 20f,
            y - textBounds.height() - 20f,
            x + textBounds.width() + 20f,
            y + 10f,
            backgroundPaint
        )

        // Draw watermark text
        canvas.drawText(watermarkText, x, y, paint)

        // Save watermarked image
        val watermarkedFile = File(
            context.cacheDir,
            "watermarked_${System.currentTimeMillis()}.jpg"
        )
        FileOutputStream(watermarkedFile).use { out ->
            watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        bitmap.recycle()
        watermarkedBitmap.recycle()

        return watermarkedFile
    }

    /**
     * Compress image to max 5MB
     */
    suspend fun compressImage(context: Context, file: File): File {
        return Compressor.compress(context, file) {
            resolution(1920, 1080)
            quality(85)
            format(Bitmap.CompressFormat.JPEG)
        }
    }

    /**
     * Add watermark and compress
     */
    suspend fun processImage(context: Context, file: File): File {
        val watermarked = addWatermark(context, file)
        return compressImage(context, watermarked)
    }
}
