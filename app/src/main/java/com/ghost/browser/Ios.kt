package com.ghost.browser

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
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
    val BG = 0xF2F3F3F7.toInt()
    val LINE = 0x16000000
    val BLUE = 0xFF007AFF.toInt()
    val RED = 0xFFFF3B30.toInt()
    val GREEN = 0xFF34C759.toInt()
    val GRAY = 0xFF8E8E93.toInt()
    val ORANGE = 0xFFFF9500.toInt()
    val PURPLE = 0xFFAF52DE.toInt()
    val TEAL = 0xFF30B0C7.toInt()
    val INDIGO = 0xFF5856D6.toInt()
    val PINK = 0xFFFF2D55.toInt()
    val TEXT = 0xFF0B0B0D.toInt()

    private val CARD = 0xF2FFFFFF.toInt()
    private val CARD_LINE = 0x1F000000
    private val PRESS = 0x12000000

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    private fun hairPx(): Int = Math.max(1, (ctx.resources.displayMetrics.density * 0.9f).toInt())

    fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
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

    @Suppress("UNUSED_PARAMETER")
    fun hideBars(w: Window?) {
    }

    private fun pressable(): StateListDrawable {
        val s = StateListDrawable()
        s.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(PRESS))
        s.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        return s
    }

    fun header(t: String): TextView {
        val v = text(t, 13f, GRAY)
        v.setPadding(dp(32), dp(20), dp(16), dp(6))
        return v
    }

    fun footer(t: String): TextView {
        val v = text(t, 12f, GRAY)
        v.setPadding(dp(32), dp(6), dp(24), dp(4))
        return v
    }

    fun card(): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.background = glass(CARD, 15, CARD_LINE)
        c.clipToOutline = true
        c.elevation = dp(1).toFloat()
        return c
    }

    fun addCard(parent: LinearLayout, card: LinearLayout) {
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.leftMargin = dp(16)
        lp.rightMargin = dp(16)
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

    private fun tile(icon: String, color: Int): ImageView {
        val t = ImageView(ctx)
        t.setImageDrawable(Icons.drawable(icon, Color.WHITE))
        t.setPadding(dp(5), dp(5), dp(5), dp(5))
        t.background = rounded(color, 8)
        val lp = LinearLayout.LayoutParams(dp(30), dp(30))
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
        r.minimumHeight = dp(48)
        r.setPadding(dp(16), dp(9), dp(14), dp(9))
        if (icon != null) r.addView(tile(icon, tileColor))

        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        mid.addView(text(title, 16f, titleColor))
        if (sub != null) mid.addView(text(sub, 12f, GRAY))
        r.addView(mid, LinearLayout.LayoutParams(0, WRAP, 1f))

        var vv: TextView? = null
        if (value != null) {
            val t = text(value, 15f, GRAY)
            t.maxWidth = dp(170)
            t.maxLines = 1
            t.ellipsize = android.text.TextUtils.TruncateAt.END
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = dp(8)
            r.addView(t, lp)
            vv = t
        }
        if (chevron) {
            val c = text("›", 22f, 0xFFC2C2C7.toInt())
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.leftMargin = dp(8)
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
        sw.trackTintList = ColorStateList(states, intArrayOf(GREEN, 0x33000000))
        sw.thumbTintList = ColorStateList.valueOf(Color.WHITE)
        sw.setOnCheckedChangeListener { _, c -> onChange(c) }
        rv.view.addView(sw)
        rv.view.background = pressable()
        rv.view.setOnClickListener { sw.isChecked = !sw.isChecked }
        return rv.view
    }

    fun bigButton(label: String, bg: Int, fg: Int, onClick: () -> Unit): TextView {
        val t = text(label, 17f, fg, true)
        t.gravity = Gravity.CENTER
        t.background = rounded(bg, 14)
        t.minimumHeight = dp(50)
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
        wrap.background = rounded(0x14000000, 10)
        wrap.setPadding(dp(2), dp(2), dp(2), dp(2))
        val tvs = ArrayList<TextView>()
        fun paint(sel: Int) {
            for ((i, t) in tvs.withIndex()) {
                if (i == sel) {
                    t.background = rounded(Color.WHITE, 8)
                    t.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    t.background = null
                    t.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                }
            }
        }
        for ((i, l) in labels.withIndex()) {
            val t = text(l, 13f, TEXT)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(7), 0, dp(7))
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
        container.background = glass(0xF7FFFFFF.toInt(), 15, CARD_LINE)
        container.clipToOutline = true
        container.setPadding(0, dp(4), 0, dp(4))

        val popup = PopupWindow(container, dp(234), WRAP, true)
        popup.elevation = dp(16).toFloat()
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
            rowL.minimumHeight = dp(46)
            rowL.setPadding(dp(16), dp(8), dp(14), dp(8))
            rowL.background = pressable()
            rowL.addView(text(label, 16f, TEXT, id == current), LinearLayout.LayoutParams(0, WRAP, 1f))
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
        popup.showAsDropDown(anchor, anchor.width - dp(234), -(anchor.height / 2))
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
                lp.blurBehindRadius = dp(34)
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
        bg.setColor(BG)
        val r = dp(22).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panel.background = bg

        val handle = View(act)
        handle.background = rounded(0x33000000, 3)
        val hlp = LinearLayout.LayoutParams(dp(36), dp(5))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        hlp.topMargin = dp(8)
        panel.addView(handle, hlp)

        val top = LinearLayout(act)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(20), dp(10), dp(16), dp(6))
        val tt = text(title, 22f, TEXT, true)
        top.addView(tt, LinearLayout.LayoutParams(0, WRAP, 1f))
        val done = text("Done", 17f, BLUE, true)
        done.setPadding(dp(10), dp(6), dp(4), dp(6))
        top.addView(done)
        panel.addView(top)

        val header = LinearLayout(act)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(dp(16), 0, dp(16), dp(4))
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

        glassWindow(d.window, 0.28f)
        d.setOnShowListener {
            panel.translationY = sheetH.toFloat()
            panel.animate().translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
        }
        return SheetHandle(d, header, body, close)
    }

    fun alert(act: Activity, title: String, message: String?, content: View?, buttons: List<AlertBtn>, cancelable: Boolean): Dialog {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val dm = act.resources.displayMetrics
        val cardW = Math.min((dm.widthPixels * 0.84f).toInt(), dp(340))

        val root = FrameLayout(act)
        val card = LinearLayout(act)
        card.orientation = LinearLayout.VERTICAL
        card.background = glass(0xF7F7F7FA.toInt(), 18, CARD_LINE)
        card.clipToOutline = true

        val head = LinearLayout(act)
        head.orientation = LinearLayout.VERTICAL
        head.setPadding(dp(18), dp(18), dp(18), dp(12))
        val t = text(title, 17f, TEXT, true)
        t.gravity = Gravity.CENTER
        head.addView(t)
        if (message != null) {
            val m = text(message, 13f, 0xFF3C3C43.toInt())
            m.gravity = Gravity.CENTER
            m.setPadding(0, dp(6), 0, 0)
            head.addView(m)
        }
        card.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

        if (content != null) {
            val sc = MaxHeightScroll(act, (dm.heightPixels * 0.42f).toInt())
            sc.isVerticalScrollBarEnabled = false
            sc.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
            card.addView(sc, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        for (b in buttons) {
            val sep = View(act)
            sep.setBackgroundColor(LINE)
            card.addView(sep, LinearLayout.LayoutParams(MATCH, hairPx()))
            val bt = text(b.label, 17f, if (b.kind == AlertBtn.DESTRUCTIVE) RED else BLUE, b.kind == AlertBtn.BOLD)
            bt.gravity = Gravity.CENTER
            bt.minimumHeight = dp(48)
            bt.background = pressable()
            bt.setOnClickListener {
                d.dismiss()
                b.action()
            }
            card.addView(bt, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        root.addView(card, FrameLayout.LayoutParams(cardW, WRAP, Gravity.CENTER))
        d.setContentView(root)
        d.setCancelable(cancelable)
        if (cancelable) root.setOnClickListener { d.dismiss() }
        card.setOnClickListener { }

        glassWindow(d.window, 0.32f)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.setOnShowListener {
            card.alpha = 0f
            card.scaleX = 0.92f
            card.scaleY = 0.92f
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
        }
        d.show()
        return d
    }
}
