package com.ghost.browser

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Captures the visible page and saves it to the gallery (Pictures/Ghost Browser). */
object Screenshot {

    private const val FOLDER = "Ghost Browser"

    /**
     * Takes a screenshot of [content], keeping the views in [hide] out of the shot.
     * [done] is called on the UI thread with true on success.
     */
    fun take(act: Activity, content: View, hide: List<View>, done: (Boolean) -> Unit) {
        val w = content.width
        val h = content.height
        if (w <= 0 || h <= 0) {
            done(false)
            return
        }
        // Keep the floating controls out of the shot.
        val saved = hide.map { it.visibility }
        hide.forEach { it.visibility = View.INVISIBLE }

        val main = Handler(Looper.getMainLooper())
        main.postDelayed({
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val loc = IntArray(2)
            content.getLocationInWindow(loc)
            val rect = Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
            try {
                PixelCopy.request(act.window, rect, bmp, { result ->
                    hide.forEachIndexed { i, v -> v.visibility = saved[i] }
                    val ok = result == PixelCopy.SUCCESS && save(act, bmp)
                    done(ok)
                }, main)
            } catch (e: Exception) {
                hide.forEachIndexed { i, v -> v.visibility = saved[i] }
                done(false)
            }
        }, 60)
    }

    private fun save(ctx: Context, bmp: Bitmap): Boolean {
        val name = "Ghost_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = ctx.contentResolver
                val uri: Uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out: OutputStream ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: return false
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, name)
                file.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                MediaScannerConnection.scanFile(ctx, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
