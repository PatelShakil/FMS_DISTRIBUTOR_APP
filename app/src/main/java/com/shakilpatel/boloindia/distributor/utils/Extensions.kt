package com.shakilpatel.boloindia.distributor.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.shareImage(file: File, caption: String) {
    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
}

fun String.toFormattedDate(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(this)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        this
    }
}

fun String.toRelativeTime(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = inputFormat.parse(this)
        val now = Date()
        val diff = now.time - (date?.time ?: 0)

        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 2592000000 -> "${diff / 86400000} days ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
        }
    } catch (e: Exception) {
        this
    }
}
