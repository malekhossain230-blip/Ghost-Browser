package com.ghost.browser

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object Favicons {
    private val mem = LruCache<String, Bitmap>(96)
    private val failed = HashMap<String, Long>()
    private val pool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    val LETTER_COLORS = intArrayOf(
        0xFF1668F7.toInt(), 0xFFE11D48.toInt(), 0xFFF97316.toInt(), 0xFF16A34A.toInt(), 0xFF8B5CF6.toInt(), 0xFF0EA5E9.toInt(),
        0xFFD946EF.toInt(), 0xFFF59E0B.toInt(), 0xFF14B8A6.toInt(), 0xFFEF4444.toInt(), 0xFF6366F1.toInt(), 0xFF0D9488.toInt()
    )

    class Mark(val icon: String, val color: Int, val ink: Boolean)

    private val SITE_MARKS: Map<String, Mark> = mapOf(
        "youtube.com" to Mark("site-youtube", 0xFFFF0000.toInt(), false),
        "facebook.com" to Mark("site-facebook", 0xFF0866FF.toInt(), false),
        "tiktok.com" to Mark("site-tiktok", 0, true),
        "telegram.org" to Mark("site-telegram", 0xFF26A5E4.toInt(), false)
    )

    fun hashColor(s: String): Int {
        var h = 0
        for (c in s) h = (h * 31 + c.code) or 0
        return LETTER_COLORS[Math.abs(h) % LETTER_COLORS.size]
    }

    fun siteMark(domain: String): Mark? = SITE_MARKS[UrlUtil.iconDomain(domain)]

    fun cached(domain: String): Bitmap? = mem.get(UrlUtil.iconDomain(domain))

    private fun dir(ctx: Context): File {
        val d = File(ctx.cacheDir, "favicons")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun safeName(d: String): String = d.replace(Regex("[^a-z0-9.-]"), "_") + ".png"

    fun load(ctx: Context, domain: String, cb: (Bitmap?) -> Unit) {
        val key = UrlUtil.iconDomain(domain)
        if (key.isEmpty()) {
            cb(null)
            return
        }
        val hit = mem.get(key)
        if (hit != null) {
            cb(hit)
            return
        }
        val until = synchronized(failed) { failed[key] ?: 0L }
        if (until > System.currentTimeMillis()) {
            cb(null)
            return
        }
        val app = ctx.applicationContext
        pool.execute {
            var bmp: Bitmap? = null
            val f = File(dir(app), safeName(key))
            if (f.exists()) {
                bmp = try {
                    BitmapFactory.decodeFile(f.absolutePath)
                } catch (e: Exception) {
                    null
                }
            }
            if (bmp == null) {
                val sources = ArrayList<String>()
                sources.add("https://www.google.com/s2/favicons?sz=64&domain=" + android.net.Uri.encode(key))
                if (key != domain.lowercase()) sources.add("https://www.google.com/s2/favicons?sz=64&domain=" + android.net.Uri.encode(domain))
                sources.add("https://icons.duckduckgo.com/ip3/$key.ico")
                for (src in sources) {
                    bmp = fetch(src)
                    if (bmp != null) break
                }
                if (bmp != null) {
                    try {
                        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } catch (e: Exception) {
                    }
                }
            }
            if (bmp != null) {
                mem.put(key, bmp)
            } else {
                synchronized(failed) { failed[key] = System.currentTimeMillis() + 10 * 60 * 1000 }
            }
            val out = bmp
            main.post { cb(out) }
        }
    }

    private fun fetch(src: String): Bitmap? {
        var c: HttpURLConnection? = null
        try {
            c = URL(src).openConnection() as HttpURLConnection
            c.connectTimeout = 4000
            c.readTimeout = 4000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0 Mobile Safari/537.36")
            if (c.responseCode != 200) return null
            val bytes = c.inputStream.use { it.readBytes() }
            if (bytes.size < 64) return null
            val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            if (b.width < 8 || b.height < 8) return null
            return b
        } catch (e: Exception) {
            return null
        } finally {
            try {
                c?.disconnect()
            } catch (e: Exception) {
            }
        }
    }
}

class IconBox(ctx: Context, private val ui: Ios, sizeDp: Int, radiusDp: Int, private val innerDp: Int) : FrameLayout(ctx) {
    private val img = ImageView(ctx)
    private val letter = TextView(ctx)
    private val skel = View(ctx)
    private var shimmer: ValueAnimator? = null
    private var token = 0
    private val size = ui.dp(sizeDp)
    private val radius = radiusDp
    private val letterRadius = Math.max(6, (sizeDp * 0.17f).toInt())
    private val letterInset = Math.max(ui.dp(6), (size * 0.19f).toInt())

    init {
        layoutParams = ViewGroup.LayoutParams(size, size)
        background = ui.glass(ui.SOFT, radiusDp, ui.LINE)
        clipToOutline = true

        img.scaleType = ImageView.ScaleType.FIT_CENTER
        img.background = ui.rounded(Color.TRANSPARENT, 7)
        img.clipToOutline = true
        addView(img, LayoutParams(ui.dp(innerDp), ui.dp(innerDp), Gravity.CENTER))

        letter.gravity = Gravity.CENTER
        letter.setTextColor(Color.WHITE)
        letter.typeface = ui.BOLD
        letter.textSize = Math.max(11f, sizeDp * 0.28f)
        letter.includeFontPadding = false
        val llp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        llp.setMargins(letterInset, letterInset, letterInset, letterInset)
        addView(letter, llp)

        skel.setBackgroundColor(if (ui.dark) 0x22FFFFFF else 0x160F172A)
        skel.visibility = View.GONE
        addView(skel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun stopShimmer() {
        shimmer?.cancel()
        shimmer = null
        skel.visibility = View.GONE
    }

    fun setLoading(on: Boolean) {
        if (!on) {
            stopShimmer()
            return
        }
        if (shimmer != null) return
        skel.visibility = View.VISIBLE
        skel.alpha = 0.35f
        val a = ValueAnimator.ofFloat(0.35f, 1f, 0.35f)
        a.duration = 1300
        a.repeatCount = ValueAnimator.INFINITE
        a.addUpdateListener { v -> skel.alpha = v.animatedValue as Float }
        a.start()
        shimmer = a
    }

    fun setLetter(ch: String, color: Int) {
        stopShimmer()
        token++
        img.visibility = View.GONE
        img.setImageDrawable(null)
        letter.visibility = View.VISIBLE
        letter.text = ch
        letter.background = ui.rounded(color, letterRadius)
        background = ui.glass(ui.SOFT, radius, ui.LINE)
    }

    fun setMark(icon: String, color: Int, ink: Boolean) {
        stopShimmer()
        token++
        letter.visibility = View.GONE
        img.visibility = View.VISIBLE
        img.background = null
        val c = if (ink) ui.TEXT else color
        img.setImageDrawable(HomeIcons.drawable(icon, c))
        val pad = (ui.dp(innerDp) * 0.08f).toInt()
        img.setPadding(pad, pad, pad, pad)
        background = ui.glass(if (ink) ui.SOFT else ui.tint(color, if (ui.dark) 0.2f else 0.13f), radius, ui.LINE)
    }

    fun setBitmap(bmp: Bitmap) {
        stopShimmer()
        token++
        letter.visibility = View.GONE
        img.visibility = View.VISIBLE
        img.setPadding(0, 0, 0, 0)
        img.background = ui.rounded(Color.TRANSPARENT, 7)
        img.clipToOutline = true
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        img.setImageBitmap(bmp)
        background = ui.glass(ui.SOFT, radius, ui.LINE)
    }

    fun bind(name: String, url: String, customIcon: File?) {
        val domain = UrlUtil.domainOf(url)
        val label = (if (name.isNotEmpty()) name else if (domain.isNotEmpty()) domain else "?").substring(0, 1).uppercase()
        val color = Favicons.hashColor(if (domain.isNotEmpty()) domain else name)
        if (customIcon != null) {
            val b = try {
                BitmapFactory.decodeFile(customIcon.absolutePath)
            } catch (e: Exception) {
                null
            }
            if (b != null) {
                setBitmap(b)
                return
            }
        }
        val mark = Favicons.siteMark(domain)
        if (mark != null) {
            setMark(mark.icon, mark.color, mark.ink)
            return
        }
        if (domain.isEmpty()) {
            setLetter(label, color)
            return
        }
        val hit = Favicons.cached(domain)
        if (hit != null) {
            setBitmap(hit)
            return
        }
        setLetter(label, color)
        token++
        val my = token
        setLoading(true)
        Favicons.load(context, domain) { bmp ->
            if (my != token) return@load
            setLoading(false)
            if (bmp != null) setBitmap(bmp)
        }
    }

    fun bindEngine(e: Engine) {
        if (HomeIcons.has(e.icon)) setMark(e.icon, e.color, false) else setLetter(e.letter, e.color)
    }
}
