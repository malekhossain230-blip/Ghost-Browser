package com.ghost.browser

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Downloads {
    private fun cleanName(raw: String, mime: String): String {
        var n = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        if (n.isEmpty()) n = "download-" + System.currentTimeMillis()
        if (!n.contains('.')) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (ext != null) n = "$n.$ext"
        }
        return n
    }

    /** Saves bytes into the Downloads folder. Returns a short description of where, or null on failure. */
    fun saveBytes(ctx: Context, rawName: String, rawMime: String, data: ByteArray): String? {
        val mime = if (rawMime.isBlank()) "application/octet-stream" else rawMime
        val name = cleanName(rawName, mime)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues()
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                cv.put(MediaStore.MediaColumns.MIME_TYPE, mime)
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                cv.put(MediaStore.MediaColumns.IS_PENDING, 1)
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    ?: return null
                val out = ctx.contentResolver.openOutputStream(uri) ?: return null
                out.use { it.write(data) }
                val done = ContentValues()
                done.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(uri, done, null, null)
                return "Download/$name"
            } else {
                val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: ctx.filesDir
                dir.mkdirs()
                var f = File(dir, name)
                var i = 1
                while (f.exists()) {
                    f = File(dir, "($i) $name")
                    i++
                }
                f.writeBytes(data)
                return f.absolutePath
            }
        } catch (e: Exception) {
            return null
        }
    }

    /** Normal http(s) download through the system DownloadManager. Returns the file name. */
    fun startHttp(ctx: Context, url: String, ua: String?, cd: String?, mime: String?): String {
        val name = cleanName(URLUtil.guessFileName(url, cd, mime), mime ?: "")
        val req = DownloadManager.Request(Uri.parse(url))
        if (!mime.isNullOrBlank()) req.setMimeType(mime)
        val ck = CookieManager.getInstance().getCookie(url)
        if (!ck.isNullOrEmpty()) req.addRequestHeader("Cookie", ck)
        if (!ua.isNullOrEmpty()) req.addRequestHeader("User-Agent", ua)
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (Build.VERSION.SDK_INT >= 29) {
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        } else {
            req.setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, name)
        }
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(req)
        return name
    }

    /** Decodes a data: URL into (mime, bytes). */
    fun fromDataUrl(url: String): Pair<String, ByteArray>? {
        val comma = url.indexOf(',')
        if (!url.startsWith("data:") || comma < 0) return null
        val head = url.substring(5, comma)
        val body = url.substring(comma + 1)
        var mime = head.substringBefore(';')
        if (mime.isBlank()) mime = "text/plain"
        val bytes = if (head.endsWith(";base64")) Base64.decode(body, Base64.DEFAULT)
        else Uri.decode(body).toByteArray()
        return Pair(mime, bytes)
    }
}

/** Receives the contents of a blob: download, read inside the page by our own script. */
class GhostDl(private val act: MainActivity) {
    private val pending = ConcurrentHashMap<String, Boolean>()

    fun expect(): String {
        val id = UUID.randomUUID().toString()
        pending[id] = true
        return id
    }

    @JavascriptInterface
    fun save(id: String, name: String, mime: String, b64: String) {
        if (pending.remove(id) == null) return
        val data = try {
            Base64.decode(b64, Base64.DEFAULT)
        } catch (e: Exception) {
            act.toast("ডাউনলোড ব্যর্থ: ডেটা পড়া যায়নি")
            return
        }
        val where = Downloads.saveBytes(act, name, mime, data)
        if (where != null) act.toast("ডাউনলোড হয়েছে: $where") else act.toast("ফাইল সেভ করা যায়নি")
    }

    @JavascriptInterface
    fun fail(id: String, msg: String) {
        if (pending.remove(id) == null) return
        act.toast("ডাউনলোড ব্যর্থ: $msg")
    }
}
