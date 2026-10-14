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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** একটা ScrollView যার উচ্চতা সীমা আছে (পপআপে বড় তালিকার জন্য)। */
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

/** iOS ধাঁচের ছোট UI টুলকিট (প্রোগ্রাম দিয়ে আঁকা)। */
class Ios(val ctx: Context) {
    val BG = 0xFFF2F2F7.toInt()
    val LINE = 0xFFD1D1D6.toInt()
    val BLUE = 0xFF007AFF.toInt()
    val RED = 0xFFFF3B30.toInt()
    val GREEN = 0xFF34C759.toInt()
    val GRAY = 0xFF8E8E93.toInt()
    val ORANGE = 0xFFFF9500.toInt()
    val PURPLE = 0xFFAF52DE.toInt()
    val TEAL = 0xFF30B0C7.toInt()
    val INDIGO = 0xFF5856D6.toInt()
    val PINK = 0xFFFF2D55.toInt()

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    fun text(t: String, size: Float = 16f, color: Int = Color.BLACK, bold: Boolean = false): TextView {
        val v = TextView(ctx)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.setTypeface(v.typeface, Typeface.BOLD)
        return v
    }

    fun hideBars(w: Window?) {
        if (w == null) return
        try {
            val c = WindowCompat.getInsetsController(w, w.decorView) ?: return
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } catch (e: Exception) {
        }
    }

    private fun pressable(): StateListDrawable {
        val s = StateListDrawable()
        s.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(0xFFE5E5EA.toInt()))
        s.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        return s
    }

    // ---------------- grouped list ----------------
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
        c.background = rounded(Color.WHITE, 12)
        c.clipToOutline = true
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
            val lp = LinearLayout.LayoutParams(MATCH, 1)
            lp.leftMargin = dp(inset)
            card.addView(sep, lp)
        }
        card.addView(v, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun tile(icon: String, color: Int): TextView {
        val t = TextView(ctx)
        t.text = icon
        t.textSize = 15f
        t.gravity = Gravity.CENTER
        t.setTextColor(Color.WHITE)
        t.background = rounded(color, 7)
        val lp = LinearLayout.LayoutParams(dp(30), dp(30))
        lp.rightMargin = dp(12)
        t.layoutParams = lp
        return t
    }

    fun row(
        icon: String?, tileColor: Int, title: String, sub: String? = null,
        value: String? = null, chevron: Boolean = false, titleColor: Int = Color.BLACK,
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
            val c = text("›", 22f, 0xFFC7C7CC.toInt())
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
        sw.trackTintList = ColorStateList(states, intArrayOf(GREEN, 0xFFE5E5EA.toInt()))
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
        t.background = rounded(bg, 13)
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
        wrap.background = rounded(0xFFE3E3E8.toInt(), 10)
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
            val t = text(l, 13f, Color.BLACK)
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

    // ---------------- bottom sheet ----------------
    fun sheet(act: Activity, title: String, frac: Float): SheetHandle {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val screenH = act.resources.displayMetrics.heightPixels
        val sheetH = (screenH * frac).toInt()

        val root = FrameLayout(act)
        val panel = LinearLayout(act)
        panel.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.setColor(BG)
        val r = dp(18).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panel.background = bg

        val handle = View(act)
        handle.background = rounded(0xFFC7C7CC.toInt(), 3)
        val hlp = LinearLayout.LayoutParams(dp(36), dp(5))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        hlp.topMargin = dp(8)
        panel.addView(handle, hlp)

        val top = LinearLayout(act)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(20), dp(10), dp(16), dp(6))
        val tt = text(title, 22f, Color.BLACK, true)
        top.addView(tt, LinearLayout.LayoutParams(0, WRAP, 1f))
        val done = text("পূর্ণ", 17f, BLUE, true)
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

        val w = d.window
        if (w != null) {
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(MATCH, MATCH)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setDimAmount(0.4f)
        }
        d.setOnShowListener {
            hideBars(d.window)
            panel.translationY = sheetH.toFloat()
            panel.animate().translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
        }
        return SheetHandle(d, header, body, close)
    }

    // ---------------- alert (iOS style) ----------------
    fun alert(act: Activity, title: String, message: String?, content: View?, buttons: List<AlertBtn>, cancelable: Boolean): Dialog {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val dm = act.resources.displayMetrics
        val cardW = Math.min((dm.widthPixels * 0.84f).toInt(), dp(340))

        val root = FrameLayout(act)
        val card = LinearLayout(act)
        card.orientation = LinearLayout.VERTICAL
        card.background = rounded(0xFFF9F9F9.toInt(), 16)
        card.clipToOutline = true

        val head = LinearLayout(act)
        head.orientation = LinearLayout.VERTICAL
        head.setPadding(dp(18), dp(18), dp(18), dp(12))
        val t = text(title, 17f, Color.BLACK, true)
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
            card.addView(sep, LinearLayout.LayoutParams(MATCH, 1))
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

        val w = d.window
        if (w != null) {
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(MATCH, MATCH)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setDimAmount(0.4f)
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        d.setOnShowListener {
            hideBars(d.window)
            card.alpha = 0f
            card.scaleX = 0.92f
            card.scaleY = 0.92f
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
        }
        d.show()
        return d
    }
}
