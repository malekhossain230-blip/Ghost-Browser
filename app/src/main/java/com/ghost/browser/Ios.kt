package com.ghost.browser

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
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

    val ACC1 = 0xFF2AB2F0.toInt()
    val ACC2 = 0xFF2A7FF0.toInt()
    val BLUE = 0xFF33A8F5.toInt()
    val GREEN = 0xFF2FD27A.toInt()
    val ORANGE = 0xFFFFA733.toInt()
    val TEAL = 0xFF30B0C7.toInt()
    val INDIGO = 0xFF5B6CF0.toInt()
    val PURPLE = 0xFF8B5CF6.toInt()
    val PINK = 0xFFFF4D8D.toInt()
    val RED = if (dark) 0xFFFF6A62.toInt() else 0xFFE5453C.toInt()

    val BG = if (dark) 0xFF0B0D12.toInt() else 0xFFF2F5FA.toInt()
    val TEXT = if (dark) 0xFFF2F4F8.toInt() else 0xFF141A26.toInt()
    val GRAY = if (dark) 0xFF8B93A5.toInt() else 0xFF7D8697.toInt()
    val LABEL = if (dark) 0xFF737C8F.toInt() else 0xFF8A93A6.toInt()
    val LINE = if (dark) 0x12FFFFFF else 0x0F0A1E3C
    val CHEV = if (dark) 0xFF5E677A.toInt() else 0xFFA8B0BE.toInt()

    private val CARD = if (dark) 0x0FFFFFFF else 0xFFFFFFFF.toInt()
    private val CARD_LINE = if (dark) 0x14FFFFFF else 0x120A1E3C
    private val PRESS = if (dark) 0x18FFFFFF else 0x0E0A1E3C
    private val SW_OFF = if (dark) 0xFF3A3F4B.toInt() else 0xFFD6DBE4.toInt()
    private val SHEET = if (dark) 0xFF15171E.toInt() else 0xFFF6F8FC.toInt()
    private val DLG = if (dark) 0xFF191C24.toInt() else 0xFFFFFFFF.toInt()
    private val SOFT = if (dark) 0x0BFFFFFF else 0x090A1E3C

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    private fun hairPx(): Int = Math.max(1, (ctx.resources.displayMetrics.density * 0.9f).toInt())

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

    fun gradient(c1: Int, c2: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    fun grad2(base: Int, radiusDp: Int): GradientDrawable = gradient(lighten(base, 0.28f), base, radiusDp)

    fun accent(radiusDp: Int): GradientDrawable = gradient(ACC1, ACC2, radiusDp)

    fun pageBg(): GradientDrawable {
        val c = if (dark) intArrayOf(0xFF141C2B.toInt(), 0xFF0B0D12.toInt(), 0xFF08090D.toInt())
        else intArrayOf(0xFFF8FAFE.toInt(), 0xFFF0F4FA.toInt(), 0xFFE9EEF6.toInt())
        val g = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, c)
        g.gradientType = GradientDrawable.LINEAR_GRADIENT
        return g
    }

    private fun glass(fill: Int, radiusDp: Int, stroke: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dp(radiusDp).toFloat()
        g.setStroke(hairPx(), stroke)
        return g
    }

    fun text(t: String, size: Float = 16f, color: Int = TEXT, bold: Boolean = false): TextView {
        val v = TextView(ctx)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.setTypeface(v.typeface, Typeface.BOLD)
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

    @Suppress("UNUSED_PARAMETER")
    fun hideBars(w: Window?) {
    }

    private fun pressable(): StateListDrawable {
        val s = StateListDrawable()
        s.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(PRESS))
        s.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        return s
    }

    private fun pressableRound(radiusDp: Int, base: android.graphics.drawable.Drawable): StateListDrawable {
        val s = StateListDrawable()
        val p = GradientDrawable()
        p.setColor(PRESS)
        p.cornerRadius = dp(radiusDp).toFloat()
        s.addState(intArrayOf(android.R.attr.state_pressed), p)
        s.addState(intArrayOf(), base)
        return s
    }

    fun header(t: String): TextView {
        val v = TextView(ctx)
        v.text = t.uppercase()
        v.textSize = 11.5f
        v.setTextColor(LABEL)
        v.typeface = MEDIUM
        v.letterSpacing = 0.09f
        v.setPadding(dp(28), dp(22), dp(18), dp(9))
        return v
    }

    fun footer(t: String): TextView {
        val v = text(t, 12f, GRAY)
        v.setPadding(dp(28), dp(9), dp(24), dp(2))
        v.setLineSpacing(dp(2).toFloat(), 1f)
        return v
    }

    fun card(): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.background = glass(CARD, 20, CARD_LINE)
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

    private fun tile(icon: String, color: Int): ImageView {
        val t = ImageView(ctx)
        t.setImageDrawable(Icons.drawable(icon, Color.WHITE, 2.1f))
        t.setPadding(dp(8), dp(8), dp(8), dp(8))
        t.background = grad2(color, 11)
        val lp = LinearLayout.LayoutParams(dp(36), dp(36))
        lp.rightMargin = dp(13)
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
        r.minimumHeight = dp(56)
        r.setPadding(dp(15), dp(11), dp(14), dp(11))
        if (icon != null) r.addView(tile(icon, tileColor))

        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        val tt = medium(title, 15.5f, titleColor)
        tt.maxLines = 2
        mid.addView(tt)
        if (sub != null) {
            val s = text(sub, 12f, GRAY)
            s.maxLines = 2
            s.ellipsize = android.text.TextUtils.TruncateAt.END
            val slp = LinearLayout.LayoutParams(MATCH, WRAP)
            slp.topMargin = dp(1)
            mid.addView(s, slp)
        }
        r.addView(mid, LinearLayout.LayoutParams(0, WRAP, 1f))

        var vv: TextView? = null
        if (value != null) {
            val t = medium(value, 14.5f, GRAY)
            t.maxWidth = dp(165)
            t.maxLines = 1
            t.ellipsize = android.text.TextUtils.TruncateAt.END
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = dp(8)
            r.addView(t, lp)
            vv = t
        }
        if (chevron) {
            val c = text("\u203A", 21f, CHEV)
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = dp(7)
            r.addView(c, lp)
        }
        if (onClick != null) {
            r.background = pressable()
            r.setOnClickListener { onClick() }
        }
        return RowView(r, vv)
    }

    fun switchRow(icon: String?, tileColor: Int, title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val rv = row(icon, tileColor, title, sub)
        val sw = Switch(ctx)
        sw.isChecked = checked
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        sw.trackTintList = ColorStateList(states, intArrayOf(ACC2, SW_OFF))
        sw.thumbTintList = ColorStateList.valueOf(Color.WHITE)
        sw.splitTrack = false
        sw.setOnCheckedChangeListener { _, c -> onChange(c) }
        val lp = LinearLayout.LayoutParams(WRAP, WRAP)
        lp.leftMargin = dp(6)
        rv.view.addView(sw, lp)
        rv.view.background = pressable()
        rv.view.setOnClickListener { sw.isChecked = !sw.isChecked }
        return rv.view
    }

    fun hero(icon: String, title: String, sub: String, tag: String?): LinearLayout {
        val h = LinearLayout(ctx)
        h.orientation = LinearLayout.HORIZONTAL
        h.gravity = Gravity.CENTER_VERTICAL
        h.setPadding(dp(16), dp(15), dp(16), dp(15))
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF2AABEE.toInt(), 0xFF5B5CF0.toInt(), 0xFF8B3DF0.toInt()))
        g.cornerRadius = dp(22).toFloat()
        h.background = g
        h.elevation = dp(4).toFloat()

        val box = ImageView(ctx)
        box.setImageDrawable(Icons.drawable(icon, Color.WHITE, 2.1f))
        box.setPadding(dp(11), dp(11), dp(11), dp(11))
        box.background = rounded(0x33FFFFFF, 14)
        val blp = LinearLayout.LayoutParams(dp(46), dp(46))
        blp.rightMargin = dp(14)
        h.addView(box, blp)

        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        mid.addView(text(title, 16.5f, Color.WHITE, true))
        mid.addView(text(sub, 12.5f, 0xE6FFFFFF.toInt()))
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
        b.background = glass(SOFT, 14, CARD_LINE)
        b.clipToOutline = true
        return b
    }

    fun bullet(t: String, dotColor: Int): LinearLayout {
        val r = LinearLayout(ctx)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(dp(13), dp(10), dp(13), dp(10))
        val d = View(ctx)
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(dotColor)
        d.background = g
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
        val t = medium(label, 16f, fg)
        t.gravity = Gravity.CENTER
        val base: android.graphics.drawable.Drawable =
            if (Color.alpha(bg) < 255) rounded(bg, 15)
            else if (bg == BLUE || bg == ACC2) accent(15)
            else grad2(bg, 15)
        t.background = pressableRound(15, base)
        t.minimumHeight = dp(52)
        t.setOnClickListener { onClick() }
        return t
    }

    fun ghostButton(label: String, onClick: () -> Unit): TextView {
        val t = medium(label, 16f, TEXT)
        t.gravity = Gravity.CENTER
        t.background = pressableRound(15, glass(if (dark) 0x12FFFFFF else 0xFFF1F4F9.toInt(), 15, CARD_LINE))
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
        wrap.background = glass(if (dark) 0x0FFFFFFF else 0x0D0A1E3C, 16, CARD_LINE)
        wrap.setPadding(dp(5), dp(5), dp(5), dp(5))
        val tvs = ArrayList<TextView>()
        fun paint(sel: Int) {
            for ((i, t) in tvs.withIndex()) {
                if (i == sel) {
                    t.background = accent(12)
                    t.setTextColor(Color.WHITE)
                    t.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                } else {
                    t.background = null
                    t.setTextColor(LABEL)
                    t.typeface = MEDIUM
                }
            }
        }
        for ((i, l) in labels.withIndex()) {
            val t = text(l, 13f, LABEL)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(9), 0, dp(9))
            t.setOnClickListener {
                paint(i)
                onSelect(i)
            }
            tvs.add(t)
            wrap.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        paint(selected)
        return wrap
    }

    fun popupMenu(anchor: View, items: List<Pair<String, String>>, current: String?, onPick: (String) -> Unit) {
        val container = LinearLayout(ctx)
        container.orientation = LinearLayout.VERTICAL
        container.background = glass(DLG, 18, CARD_LINE)
        container.clipToOutline = true
        container.setPadding(0, dp(5), 0, dp(5))

        val popup = PopupWindow(container, dp(240), WRAP, true)
        popup.elevation = dp(18).toFloat()
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.isOutsideTouchable = true

        for ((index, item) in items.withIndex()) {
            val id = item.first
            val label = item.second
            if (index > 0) {
                val sep = View(ctx)
                sep.setBackgroundColor(LINE)
                val lp = LinearLayout.LayoutParams(MATCH, hairPx())
                lp.leftMargin = dp(16)
                container.addView(sep, lp)
            }
            val rowL = LinearLayout(ctx)
            rowL.orientation = LinearLayout.HORIZONTAL
            rowL.gravity = Gravity.CENTER_VERTICAL
            rowL.minimumHeight = dp(48)
            rowL.setPadding(dp(16), dp(8), dp(14), dp(8))
            rowL.background = pressable()
            rowL.addView(medium(label, 15.5f, if (id == current) BLUE else TEXT), LinearLayout.LayoutParams(0, WRAP, 1f))
            if (id == current) {
                val chk = ImageView(ctx)
                chk.setImageDrawable(Icons.drawable("check", BLUE))
                chk.layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
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

    private fun glassWindow(w: Window?, dim: Float) {
        if (w == null) return
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        w.setLayout(MATCH, MATCH)
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.setDimAmount(dim)
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val lp = w.attributes
                lp.blurBehindRadius = dp(38)
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
        val r = dp(26).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panel.background = bg

        val handle = View(act)
        handle.background = rounded(if (dark) 0x3DFFFFFF else 0x30000000, 3)
        val hlp = LinearLayout.LayoutParams(dp(38), dp(5))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        hlp.topMargin = dp(9)
        panel.addView(handle, hlp)

        val top = LinearLayout(act)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(20), dp(12), dp(16), dp(8))
        val tt = text(title, 25f, TEXT, true)
        tt.letterSpacing = -0.02f
        top.addView(tt, LinearLayout.LayoutParams(0, WRAP, 1f))
        val done = medium("Done", 16f, BLUE)
        done.setPadding(dp(10), dp(8), dp(4), dp(8))
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

        glassWindow(d.window, 0.3f)
        d.setOnShowListener {
            panel.translationY = sheetH.toFloat()
            panel.animate().translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
        }
        return SheetHandle(d, header, body, close)
    }

    fun alert(act: Activity, title: String, message: String?, content: View?, buttons: List<AlertBtn>, cancelable: Boolean): Dialog {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val dm = act.resources.displayMetrics
        val cardW = Math.min((dm.widthPixels * 0.88f).toInt(), dp(348))

        val root = FrameLayout(act)
        val card = LinearLayout(act)
        card.orientation = LinearLayout.VERTICAL
        card.background = glass(DLG, 26, CARD_LINE)
        card.clipToOutline = true
        card.elevation = dp(10).toFloat()

        val head = LinearLayout(act)
        head.orientation = LinearLayout.VERTICAL
        head.setPadding(dp(20), dp(22), dp(20), dp(10))
        val t = text(title, 17.5f, TEXT, true)
        t.gravity = Gravity.CENTER
        head.addView(t)
        if (message != null) {
            val m = text(message, 13f, GRAY)
            m.gravity = Gravity.CENTER
            m.setPadding(0, dp(7), 0, 0)
            m.setLineSpacing(dp(3).toFloat(), 1f)
            head.addView(m)
        }
        card.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

        if (content != null) {
            val sc = MaxHeightScroll(act, (dm.heightPixels * 0.46f).toInt())
            sc.isVerticalScrollBarEnabled = false
            sc.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
            val clp = LinearLayout.LayoutParams(MATCH, WRAP)
            clp.leftMargin = dp(18)
            clp.rightMargin = dp(18)
            card.addView(sc, clp)
        }

        val acts = LinearLayout(act)
        acts.orientation = LinearLayout.VERTICAL
        acts.setPadding(dp(16), dp(14), dp(16), dp(18))
        for (b in buttons) {
            val bt: TextView
            if (b.kind == AlertBtn.BOLD) {
                bt = medium(b.label, 16f, Color.WHITE)
                bt.background = pressableRound(15, accent(15))
            } else if (b.kind == AlertBtn.DESTRUCTIVE) {
                bt = medium(b.label, 16f, RED)
                bt.background = pressableRound(15, rounded(if (dark) 0x1AFF5A52 else 0x17E5453C, 15))
            } else {
                bt = medium(b.label, 16f, TEXT)
                bt.background = pressableRound(15, glass(if (dark) 0x12FFFFFF else 0xFFF1F4F9.toInt(), 15, CARD_LINE))
            }
            bt.gravity = Gravity.CENTER
            bt.minimumHeight = dp(50)
            bt.setOnClickListener {
                d.dismiss()
                b.action()
            }
            val lp = LinearLayout.LayoutParams(MATCH, WRAP)
            if (acts.childCount > 0) lp.topMargin = dp(9)
            acts.addView(bt, lp)
        }
        card.addView(acts, LinearLayout.LayoutParams(MATCH, WRAP))

        root.addView(card, FrameLayout.LayoutParams(cardW, WRAP, Gravity.CENTER))
        d.setContentView(root)
        d.setCancelable(cancelable)
        if (cancelable) root.setOnClickListener { d.dismiss() }
        card.setOnClickListener { }

        glassWindow(d.window, 0.38f)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.setOnShowListener {
            card.alpha = 0f
            card.scaleX = 0.93f
            card.scaleY = 0.93f
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(190).start()
        }
        d.show()
        return d
    }
}
