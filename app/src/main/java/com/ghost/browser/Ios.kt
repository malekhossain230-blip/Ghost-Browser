package com.ghost.browser

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MaxHeightScroll(ctx: Context, private val maxPx: Int) : ScrollView(ctx) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(maxPx, View.MeasureSpec.AT_MOST))
    }
}

class RowView(val view: LinearLayout, val value: TextView?)

class SheetHandle(val dialog: Dialog, val header: LinearLayout, val body: LinearLayout, val close: () -> Unit)

class AlertBtn(val label: String, val kind: Int, val action: () -> Unit) {
    companion object {
        const val NORMAL = 0
        const val BOLD = 1
        const val DESTRUCTIVE = 2
    }
}

class Ios(val ctx: Context) {

    val dark: Boolean =
        (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val ACCENT = if (dark) 0xFF3BA9FF.toInt() else 0xFF1668F7.toInt()
    val ACCENT_SOFT = if (dark) 0x293BA9FF else 0x1F1668F7
    val ACC1 = if (dark) 0xFF22C3FF.toInt() else 0xFF00B7FF.toInt()
    val ACC2 = if (dark) 0xFF4B93FF.toInt() else 0xFF1668F7.toInt()
    val ACC3 = if (dark) 0xFF7FB0FF.toInt() else 0xFF0A3FD0.toInt()
    val BLUE = ACCENT
    val GREEN = 0xFF22C55E.toInt()
    val ORANGE = 0xFFF97316.toInt()
    val TEAL = 0xFF14B8A6.toInt()
    val INDIGO = 0xFF6366F1.toInt()
    val PURPLE = 0xFF8B5CF6.toInt()
    val PINK = 0xFFE11D48.toInt()
    val RED = if (dark) 0xFFF87171.toInt() else 0xFFEF4444.toInt()
    val RED_SOFT = if (dark) 0x24F87171 else 0x1FEF4444

    val BG = if (dark) 0xFF0A0C10.toInt() else 0xFFF3F4F7.toInt()
    val TEXT = if (dark) 0xFFEEF1F6.toInt() else 0xFF0F172A.toInt()
    val GRAY = if (dark) 0xFF959DAE.toInt() else 0xFF6B7280.toInt()
    val LABEL = if (dark) 0xFF717A8C.toInt() else 0xFF9AA1AD.toInt()
    val LINE = if (dark) 0x1AFFFFFF else 0x140F172A
    val CHEV = LABEL
    val CARD = if (dark) 0xFF161A21.toInt() else 0xFFFFFFFF.toInt()
    val SOFT = if (dark) 0x12FFFFFF else 0x0D0F172A
    val SHEET = CARD
    val DLG = CARD
    val PRESS = if (dark) 0x1AFFFFFF else 0x120F172A
    val SW_OFF = if (dark) 0x2EFFFFFF else 0x1F0F172A
    val TOAST_BG = if (dark) 0xFFEEF1F6.toInt() else 0xFF0F172A.toInt()
    val TOAST_FG = if (dark) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    val HEAVY: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)

    fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun dpf(v: Float): Float = v * ctx.resources.displayMetrics.density

    fun hairPx(): Int = Math.max(1, (ctx.resources.displayMetrics.density * 0.9f).toInt())

    fun tint(color: Int, alpha: Float): Int = (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

    private fun lighten(c: Int, f: Float): Int {
        val r = (Color.red(c) + (255 - Color.red(c)) * f).toInt()
        val g = (Color.green(c) + (255 - Color.green(c)) * f).toInt()
        val b = (Color.blue(c) + (255 - Color.blue(c)) * f).toInt()
        return Color.argb(Color.alpha(c), r, g, b)
    }

    fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    fun oval(color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(color)
        return g
    }

    fun gradient(c1: Int, c2: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    fun gradient3(radiusDp: Int): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(ACC1, ACC2, ACC3))
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    fun grad2(base: Int, radiusDp: Int): GradientDrawable = gradient(lighten(base, 0.28f), base, radiusDp)

    fun accent(radiusDp: Int): GradientDrawable = gradient(ACC1, ACC2, radiusDp)

    fun pageBg(): GradientDrawable = rounded(BG, 0)

    fun glass(fill: Int, radiusDp: Int, stroke: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dp(radiusDp).toFloat()
        g.setStroke(hairPx(), stroke)
        return g
    }

    fun cardBg(radiusDp: Int = 16): GradientDrawable = glass(CARD, radiusDp, LINE)

    fun text(t: String, size: Float = 16f, color: Int = TEXT, bold: Boolean = false): TextView {
        val v = TextView(ctx)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.typeface = BOLD
        return v
    }

    fun medium(t: String, size: Float = 16f, color: Int = TEXT): TextView {
        val v = TextView(ctx)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        v.typeface = MEDIUM
        return v
    }

    fun heavy(t: String, size: Float, color: Int = TEXT): TextView {
        val v = text(t, size, color, true)
        v.letterSpacing = -0.02f
        return v
    }

    @Suppress("UNUSED_PARAMETER")
    fun hideBars(w: Window?) {
    }

    fun pressable(): StateListDrawable {
        val s = StateListDrawable()
        s.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(PRESS))
        s.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        return s
    }

    fun pressableRound(radiusDp: Int, base: Drawable): StateListDrawable {
        val s = StateListDrawable()
        val p = GradientDrawable()
        p.setColor(PRESS)
        p.cornerRadius = dp(radiusDp).toFloat()
        val copy = base.constantState?.newDrawable()?.mutate() ?: base
        val pressed = LayerDrawable(arrayOf(copy, p))
        s.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        s.addState(intArrayOf(), base)
        return s
    }

    fun header(t: String): TextView {
        val v = TextView(ctx)
        v.text = t.uppercase()
        v.textSize = 11.5f
        v.setTextColor(LABEL)
        v.typeface = BOLD
        v.letterSpacing = 0.06f
        v.setPadding(dp(28), dp(22), dp(18), dp(9))
        return v
    }

    fun footer(t: String): TextView {
        val v = text(t, 12.5f, GRAY)
        v.setPadding(dp(28), dp(9), dp(24), dp(2))
        v.setLineSpacing(dp(2).toFloat(), 1f)
        return v
    }

    fun card(): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.background = cardBg(16)
        c.clipToOutline = true
        c.elevation = if (dark) 0f else dp(2).toFloat()
        return c
    }

    fun addCard(parent: LinearLayout, card: LinearLayout) {
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.leftMargin = dp(14)
        lp.rightMargin = dp(14)
        parent.addView(card, lp)
    }

    fun add(card: LinearLayout, v: View, inset: Int = 16) {
        if (card.childCount > 0) {
            val sep = View(ctx)
            sep.setBackgroundColor(LINE)
            val lp = LinearLayout.LayoutParams(MATCH, hairPx())
            lp.leftMargin = dp(inset)
            card.addView(sep, lp)
        }
        card.addView(v, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    fun iconView(name: String, color: Int, sizeDp: Int): ImageView {
        val v = ImageView(ctx)
        v.setImageDrawable(Icons.drawable(name, color))
        v.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return v
    }

    fun badge(icon: String, c1: Int, c2: Int, sizeDp: Int): ImageView {
        val v = ImageView(ctx)
        v.setImageDrawable(Icons.drawable(icon, Color.WHITE, 2.3f))
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
        g.shape = GradientDrawable.OVAL
        v.background = g
        val pad = Math.max(1, (sizeDp * 0.26f).toInt())
        v.setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
        v.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return v
    }

    fun softBadge(icon: String, color: Int, sizeDp: Int = 32, fill: Boolean = false): ImageView {
        val v = ImageView(ctx)
        v.setImageDrawable(if (fill) HomeIcons.drawable(icon, color) else Icons.drawable(icon, color, 2f))
        v.background = rounded(tint(color, if (dark) 0.18f else 0.12f), 10)
        val pad = dp(if (fill) 8 else 6)
        v.setPadding(pad, pad, pad, pad)
        v.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        return v
    }

    fun chevron(): ImageView {
        val c = ImageView(ctx)
        c.setImageDrawable(HomeIcons.drawable("chevron-right", CHEV))
        c.layoutParams = LinearLayout.LayoutParams(dp(12), dp(12))
        return c
    }

    private fun tile(icon: String, color: Int): ImageView {
        val t = softBadge(icon, color, 34)
        val lp = LinearLayout.LayoutParams(dp(34), dp(34))
        lp.rightMargin = dp(12)
        t.layoutParams = lp
        return t
    }

    fun row(
        icon: String?, tileColor: Int, title: String, sub: String? = null,
        value: String? = null, chevron: Boolean = false, titleColor: Int = TEXT,
        onClick: (() -> Unit)? = null
    ): RowView {
        val r = LinearLayout(ctx)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.minimumHeight = dp(54)
        r.setPadding(dp(14), dp(12), dp(14), dp(12))
        if (icon != null) r.addView(tile(icon, tileColor))

        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        val tt = medium(title, 15f, titleColor)
        tt.maxLines = 2
        mid.addView(tt)
        if (sub != null) {
            val s = text(sub, 12f, GRAY)
            s.maxLines = 2
            s.ellipsize = android.text.TextUtils.TruncateAt.END
            val slp = LinearLayout.LayoutParams(MATCH, WRAP)
            slp.topMargin = dp(2)
            mid.addView(s, slp)
        }
        r.addView(mid, LinearLayout.LayoutParams(0, WRAP, 1f))

        var vv: TextView? = null
        if (value != null) {
            val t = medium(value, 14f, GRAY)
            t.maxWidth = dp(150)
            t.maxLines = 1
            t.ellipsize = android.text.TextUtils.TruncateAt.END
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = dp(8)
            r.addView(t, lp)
            vv = t
        }
        if (chevron) {
            val c = chevron()
            val lp = LinearLayout.LayoutParams(dp(12), dp(12))
            lp.leftMargin = dp(8)
            r.addView(c, lp)
        }
        if (onClick != null) {
            r.background = pressable()
            r.setOnClickListener { onClick() }
        }
        return RowView(r, vv)
    }

    fun toggle(checked: Boolean, onChange: (Boolean) -> Unit): Switch {
        val sw = Switch(ctx)
        sw.isChecked = checked
        sw.showText = false
        sw.thumbTextPadding = 0
        sw.switchMinWidth = dp(44)
        sw.switchPadding = 0
        val thumb = oval(Color.WHITE)
        thumb.setSize(dp(22), dp(22))
        val thumbWrap = LayerDrawable(arrayOf<Drawable>(thumb))
        thumbWrap.setLayerInset(0, dp(2), dp(2), dp(2), dp(2))
        sw.thumbDrawable = thumbWrap
        val off = rounded(SW_OFF, 13)
        off.setSize(dp(44), dp(26))
        val on = rounded(GREEN, 13)
        on.setSize(dp(44), dp(26))
        val track = StateListDrawable()
        track.addState(intArrayOf(android.R.attr.state_checked), on)
        track.addState(intArrayOf(), off)
        sw.trackDrawable = track
        sw.splitTrack = false
        sw.setOnCheckedChangeListener { _, c -> onChange(c) }
        return sw
    }

    fun switchRow(icon: String?, tileColor: Int, title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val rv = row(icon, tileColor, title, sub)
        val sw = toggle(checked, onChange)
        val lp = LinearLayout.LayoutParams(WRAP, WRAP)
        lp.leftMargin = dp(8)
        rv.view.addView(sw, lp)
        rv.view.background = pressable()
        rv.view.setOnClickListener { sw.isChecked = !sw.isChecked }
        return rv.view
    }

    fun hero(icon: String, title: String, sub: String, tag: String?): LinearLayout {
        val h = LinearLayout(ctx)
        h.orientation = LinearLayout.HORIZONTAL
        h.gravity = Gravity.CENTER_VERTICAL
        h.setPadding(dp(16), dp(16), dp(16), dp(16))
        h.background = gradient3(16)
        h.elevation = dp(4).toFloat()

        val box = ImageView(ctx)
        box.setImageDrawable(Icons.drawable(icon, Color.WHITE, 2.1f))
        box.setPadding(dp(11), dp(11), dp(11), dp(11))
        box.background = rounded(0x38FFFFFF, 12)
        val blp = LinearLayout.LayoutParams(dp(44), dp(44))
        blp.rightMargin = dp(14)
        h.addView(box, blp)

        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        mid.addView(text(title, 16.5f, Color.WHITE, true))
        val s = text(sub, 12.5f, 0xDDFFFFFF.toInt())
        val slp = LinearLayout.LayoutParams(MATCH, WRAP)
        slp.topMargin = dp(2)
        mid.addView(s, slp)
        h.addView(mid, LinearLayout.LayoutParams(0, WRAP, 1f))

        if (tag != null) {
            val b = medium(tag, 11f, Color.WHITE)
            b.setPadding(dp(11), dp(6), dp(11), dp(6))
            b.background = rounded(0x38FFFFFF, 20)
            b.letterSpacing = 0.05f
            h.addView(b)
        }
        return h
    }

    fun addHero(parent: LinearLayout, h: LinearLayout) {
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.leftMargin = dp(14)
        lp.rightMargin = dp(14)
        lp.topMargin = dp(4)
        parent.addView(h, lp)
    }

    fun infoBox(): LinearLayout {
        val b = LinearLayout(ctx)
        b.orientation = LinearLayout.VERTICAL
        b.background = glass(SOFT, 14, LINE)
        b.clipToOutline = true
        return b
    }

    fun bullet(t: String, dotColor: Int): LinearLayout {
        val r = LinearLayout(ctx)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(dp(13), dp(10), dp(13), dp(10))
        val d = View(ctx)
        d.background = oval(dotColor)
        val dlp = LinearLayout.LayoutParams(dp(7), dp(7))
        dlp.topMargin = dp(6)
        dlp.rightMargin = dp(9)
        r.addView(d, dlp)
        val tv = text(t, 12.5f, if (dark) 0xFFC3C8D4.toInt() else 0xFF41495A.toInt())
        tv.setLineSpacing(dp(2).toFloat(), 1f)
        r.addView(tv, LinearLayout.LayoutParams(0, WRAP, 1f))
        return r
    }

    fun bigButton(label: String, bg: Int, fg: Int, onClick: () -> Unit): TextView {
        val t = text(label, 15f, fg, true)
        t.gravity = Gravity.CENTER
        val base: Drawable =
            if (Color.alpha(bg) < 255) rounded(bg, 14)
            else if (bg == BLUE || bg == ACC2 || bg == ACCENT) rounded(ACCENT, 14)
            else rounded(bg, 14)
        t.background = pressableRound(14, base)
        t.minimumHeight = dp(52)
        t.setOnClickListener { onClick() }
        return t
    }

    fun ghostButton(label: String, onClick: () -> Unit): TextView {
        val t = text(label, 15f, TEXT, true)
        t.gravity = Gravity.CENTER
        t.background = pressableRound(14, rounded(SOFT, 14))
        t.minimumHeight = dp(52)
        t.setOnClickListener { onClick() }
        return t
    }

    fun spacer(h: Int): View {
        val v = View(ctx)
        v.layoutParams = LinearLayout.LayoutParams(MATCH, dp(h))
        return v
    }

    fun segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
        val wrap = LinearLayout(ctx)
        wrap.orientation = LinearLayout.HORIZONTAL
        wrap.background = rounded(SOFT, 11)
        wrap.setPadding(dp(3), dp(3), dp(3), dp(3))
        val tvs = ArrayList<TextView>()
        fun paint(sel: Int) {
            for ((i, t) in tvs.withIndex()) {
                if (i == sel) {
                    t.background = rounded(CARD, 9)
                    t.setTextColor(TEXT)
                    t.elevation = if (dark) 0f else dp(1).toFloat()
                } else {
                    t.background = null
                    t.setTextColor(GRAY)
                    t.elevation = 0f
                }
            }
        }
        for ((i, l) in labels.withIndex()) {
            val t = text(l, 13f, GRAY, true)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(9), 0, dp(9))
            t.setOnClickListener {
                paint(i)
                onSelect(i)
            }
            tvs.add(t)
            val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
            if (i > 0) lp.leftMargin = dp(3)
            wrap.addView(t, lp)
        }
        paint(selected)
        return wrap
    }

    fun popupMenu(anchor: View, items: List<Pair<String, String>>, current: String?, onPick: (String) -> Unit) {
        val container = LinearLayout(ctx)
        container.orientation = LinearLayout.VERTICAL
        container.background = glass(CARD, 16, LINE)
        container.clipToOutline = true
        container.setPadding(dp(6), dp(6), dp(6), dp(6))

        val popup = PopupWindow(container, dp(240), WRAP, true)
        popup.elevation = dp(18).toFloat()
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.isOutsideTouchable = true

        for (item in items) {
            val id = item.first
            val label = item.second
            val rowL = LinearLayout(ctx)
            rowL.orientation = LinearLayout.HORIZONTAL
            rowL.gravity = Gravity.CENTER_VERTICAL
            rowL.minimumHeight = dp(46)
            rowL.setPadding(dp(12), dp(8), dp(12), dp(8))
            rowL.background = pressableRound(11, ColorDrawable(Color.TRANSPARENT))
            rowL.addView(medium(label, 15f, if (id == current) ACCENT else TEXT), LinearLayout.LayoutParams(0, WRAP, 1f))
            if (id == current) {
                val chk = ImageView(ctx)
                chk.setImageDrawable(HomeIcons.drawable("check", ACCENT))
                chk.layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
                rowL.addView(chk)
            }
            rowL.setOnClickListener {
                popup.dismiss()
                onPick(id)
            }
            container.addView(rowL, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        popup.showAsDropDown(anchor, anchor.width - dp(240), -(anchor.height / 2))
    }

    fun glassWindow(w: Window?, dim: Float) {
        if (w == null) return
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        w.setLayout(MATCH, MATCH)
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.setDimAmount(dim)
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val lp = w.attributes
                lp.blurBehindRadius = dp(30)
                w.attributes = lp
            } catch (e: Exception) {
            }
        }
    }

    fun sheet(act: Activity, title: String, frac: Float): SheetHandle {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val screenH = act.resources.displayMetrics.heightPixels
        val sheetH = (screenH * frac).toInt()

        val root = FrameLayout(act)
        val panel = LinearLayout(act)
        panel.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.setColor(SHEET)
        val r = dp(24).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panel.background = bg

        val handle = View(act)
        handle.background = rounded(LINE, 3)
        val hlp = LinearLayout.LayoutParams(dp(38), dp(5))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        hlp.topMargin = dp(10)
        panel.addView(handle, hlp)

        val top = LinearLayout(act)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(22), dp(14), dp(16), dp(10))
        val tt = heavy(title, 20f)
        top.addView(tt, LinearLayout.LayoutParams(0, WRAP, 1f))
        val done = text("Done", 15f, ACCENT, true)
        done.setPadding(dp(10), dp(8), dp(6), dp(8))
        top.addView(done)
        panel.addView(top)

        val header = LinearLayout(act)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(dp(14), 0, dp(14), dp(4))
        panel.addView(header, LinearLayout.LayoutParams(MATCH, WRAP))

        val scroll = ScrollView(act)
        scroll.isVerticalScrollBarEnabled = false
        val body = LinearLayout(act)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(0, 0, 0, dp(40))
        scroll.addView(body, ViewGroup.LayoutParams(MATCH, WRAP))
        panel.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))

        root.addView(panel, FrameLayout.LayoutParams(MATCH, sheetH, Gravity.BOTTOM))
        d.setContentView(root)

        var closing = false
        val close: () -> Unit = {
            if (!closing) {
                closing = true
                panel.animate().translationY(sheetH.toFloat()).setDuration(200).withEndAction {
                    try {
                        d.dismiss()
                    } catch (e: Exception) {
                    }
                }.start()
            }
        }
        done.setOnClickListener { close() }
        root.setOnClickListener { close() }
        panel.setOnClickListener { }

        glassWindow(d.window, 0.45f)
        d.setOnShowListener {
            panel.translationY = sheetH.toFloat()
            panel.animate().translationY(0f).setDuration(280).setInterpolator(DecelerateInterpolator(1.6f)).start()
        }
        return SheetHandle(d, header, body, close)
    }

    fun alertButton(b: AlertBtn, onDone: () -> Unit): TextView {
        val bt: TextView
        if (b.kind == AlertBtn.BOLD) {
            bt = text(b.label, 15f, Color.WHITE, true)
            bt.background = pressableRound(14, rounded(ACCENT, 14))
        } else if (b.kind == AlertBtn.DESTRUCTIVE) {
            bt = text(b.label, 15f, Color.WHITE, true)
            bt.background = pressableRound(14, rounded(RED, 14))
        } else {
            bt = text(b.label, 15f, TEXT, true)
            bt.background = pressableRound(14, rounded(SOFT, 14))
        }
        bt.gravity = Gravity.CENTER
        bt.minimumHeight = dp(50)
        bt.setOnClickListener {
            onDone()
            b.action()
        }
        return bt
    }

    fun alert(act: Activity, title: String, message: String?, content: View?, buttons: List<AlertBtn>, cancelable: Boolean, icon: String? = null, iconDanger: Boolean = false): Dialog {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val dm = act.resources.displayMetrics
        val cardW = Math.min((dm.widthPixels * 0.88f).toInt(), dp(340))

        val root = FrameLayout(act)
        val card = LinearLayout(act)
        card.orientation = LinearLayout.VERTICAL
        card.background = glass(DLG, 22, LINE)
        card.clipToOutline = true
        card.elevation = dp(10).toFloat()

        val head = LinearLayout(act)
        head.orientation = LinearLayout.VERTICAL
        head.gravity = Gravity.CENTER_HORIZONTAL
        head.setPadding(dp(22), dp(24), dp(22), dp(8))
        if (icon != null) {
            val ic = ImageView(act)
            ic.setImageDrawable(HomeIcons.drawable(icon, if (iconDanger) RED else ACCENT))
            ic.background = rounded(if (iconDanger) RED_SOFT else ACCENT_SOFT, 18)
            ic.setPadding(dp(16), dp(16), dp(16), dp(16))
            val ilp = LinearLayout.LayoutParams(dp(56), dp(56))
            ilp.bottomMargin = dp(16)
            head.addView(ic, ilp)
        }
        val t = heavy(title, 17.5f)
        t.gravity = Gravity.CENTER
        head.addView(t, LinearLayout.LayoutParams(MATCH, WRAP))
        if (message != null) {
            val m = text(message, 13.5f, GRAY)
            m.gravity = Gravity.CENTER
            m.setPadding(0, dp(8), 0, 0)
            m.setLineSpacing(dp(3).toFloat(), 1f)
            head.addView(m, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        card.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

        if (content != null) {
            val sc = MaxHeightScroll(act, (dm.heightPixels * 0.46f).toInt())
            sc.isVerticalScrollBarEnabled = false
            sc.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
            val clp = LinearLayout.LayoutParams(MATCH, WRAP)
            clp.leftMargin = dp(18)
            clp.rightMargin = dp(18)
            clp.topMargin = dp(6)
            card.addView(sc, clp)
        }

        val dismiss: () -> Unit = { d.dismiss() }
        if (buttons.size <= 2) {
            val acts = LinearLayout(act)
            acts.orientation = LinearLayout.HORIZONTAL
            acts.setPadding(dp(18), dp(16), dp(18), dp(18))
            val ordered = if (buttons.size == 2 && buttons[0].kind != AlertBtn.NORMAL && buttons[1].kind == AlertBtn.NORMAL) listOf(buttons[1], buttons[0]) else buttons
            for ((i, b) in ordered.withIndex()) {
                val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
                if (i > 0) lp.leftMargin = dp(10)
                acts.addView(alertButton(b, dismiss), lp)
            }
            card.addView(acts, LinearLayout.LayoutParams(MATCH, WRAP))
        } else {
            val acts = LinearLayout(act)
            acts.orientation = LinearLayout.VERTICAL
            acts.setPadding(dp(18), dp(16), dp(18), dp(18))
            for (b in buttons) {
                val lp = LinearLayout.LayoutParams(MATCH, WRAP)
                if (acts.childCount > 0) lp.topMargin = dp(9)
                acts.addView(alertButton(b, dismiss), lp)
            }
            card.addView(acts, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        root.addView(card, FrameLayout.LayoutParams(cardW, WRAP, Gravity.CENTER))
        d.setContentView(root)
        d.setCancelable(cancelable)
        if (cancelable) root.setOnClickListener { d.dismiss() }
        card.setOnClickListener { }

        glassWindow(d.window, 0.5f)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.setOnShowListener {
            card.alpha = 0f
            card.scaleX = 0.94f
            card.scaleY = 0.94f
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
        }
        d.show()
        return d
    }
}
