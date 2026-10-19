package com.ghost.browser

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser

object Icons {
    val paths: Map<String, String> = mapOf(
        "back" to "M15 5l-7 7 7 7",
        "forward" to "M9 5l7 7-7 7",
        "refresh" to "M20 12a8 8 0 1 1-2.6-5.9 M20 4v5h-5",
        "home" to "M4 11l8-7 8 7 M6 9.5V20h12V9.5 M10 20v-6h4v6",
        "search" to "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14z M16.5 16.5L21 21",
        "globe" to "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M3 12h18 M12 3c3.5 3.5 3.5 14.5 0 18 M12 3c-3.5 3.5-3.5 14.5 0 18",
        "settings" to "M4 8h9 M17 8h3 M4 16h3 M11 16h9 M13 8a2 2 0 1 0 4 0 2 2 0 1 0-4 0 M7 16a2 2 0 1 0 4 0 2 2 0 1 0-4 0",
        "phone" to "M8 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z M11 18.5h2",
        "plus" to "M12 5v14 M5 12h14",
        "monitor" to "M4 5h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z M9 21h6 M12 17v4",
        "pin" to "M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12z M12 6.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z",
        "signal" to "M5 20v-4 M10 20v-8 M15 20V8 M20 20V4",
        "fingerprint" to "M12 4a7 7 0 0 0-7 7v2 M12 4a7 7 0 0 1 7 7v3 M12 8a3 3 0 0 0-3 3v3a6 6 0 0 1-1 3 M12 8a3 3 0 0 1 3 3v3a9 9 0 0 1-2 5 M12 12v3a12 12 0 0 1-2 5",
        "battery" to "M4 8h13a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z M21 11v2 M6 11v2 M9 11v2",
        "radio" to "M12 12h.01 M8.5 8.5a5 5 0 0 0 0 7 M15.5 8.5a5 5 0 0 1 0 7 M5.5 5.5a9 9 0 0 0 0 13 M18.5 5.5a9 9 0 0 1 0 13",
        "compass" to "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M15.5 8.5l-2 5-5 2 2-5z",
        "clipboard" to "M9 3.5h6v3H9z M8 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-1",
        "camera" to "M4 7h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1z M12 10a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z",
        "copy" to "M9 9h10a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1z M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1",
        "trash" to "M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v5 M14 11v5",
        "code" to "M8 7l-5 5 5 5 M16 7l5 5-5 5 M14 4l-4 16",
        "check" to "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M8 12l3 3 5-6",
        "shield" to "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z M9 12l2 2 4-4",
        "block" to "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z M5.6 5.6l12.8 12.8",
        "eyeoff" to "M3 3l18 18 M10.6 6.1A10 10 0 0 1 12 6c5 0 8.5 4 9.5 6a14 14 0 0 1-2.6 3.4 M6.5 7.7A14 14 0 0 0 2.5 12c1 2 4.5 6 9.5 6 1.2 0 2.3-.2 3.3-.6",
        "lock" to "M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1z M8 11V8a4 4 0 0 1 8 0v3",
        "dice" to "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M8 8h.01 M16 8h.01 M12 12h.01 M8 16h.01 M16 16h.01",
        "image" to "M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M8.5 8.5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z M21 16l-5-5-8 8",
        "textsize" to "M3 19l5-14 5 14 M4.8 14h6.4 M15 19l3-8 3 8 M16.2 16.5h3.6",
        "warn" to "M12 3l10 18H2z M12 10v5 M12 18h.01"
    )

    fun drawable(name: String, color: Int, strokePx: Float = 2f): Drawable {
        val d = paths[name] ?: paths["settings"]!!
        val p = PathParser.createPathFromPathData(d)
        return IconDrawable(p, color, strokePx)
    }
}

class IconDrawable(private val path: Path, color: Int, private val stroke: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = color
        paint.strokeWidth = stroke
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val s = Math.min(b.width(), b.height()) / 24f
        canvas.save()
        canvas.translate(b.left + (b.width() - 24f * s) / 2f, b.top + (b.height() - 24f * s) / 2f)
        canvas.scale(s, s)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
