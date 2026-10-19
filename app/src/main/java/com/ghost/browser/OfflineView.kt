package com.ghost.browser

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class OfflineView(
    ctx: Context,
    private val ui: Ios,
    private val onRetry: () -> Unit,
    private val onHome: () -> Unit
) : FrameLayout(ctx) {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val title: TextView
    private val message: TextView
    private val retryBtn: TextView
    private var busy = false

    val showing: Boolean
        get() = visibility == View.VISIBLE

    init {
        setBackgroundColor(ui.BG)
        visibility = View.GONE
        isClickable = true
        isFocusable = true

        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.setPadding(ui.dp(32), 0, ui.dp(32), ui.dp(24))

        val badge = FrameLayout(ctx)
        badge.background = ui.oval(ui.SOFT)
        val icon = ImageView(ctx)
        icon.setImageDrawable(HomeIcons.drawable("cloud-moon", ui.GRAY))
        badge.addView(icon, LayoutParams(ui.dp(40), ui.dp(40), Gravity.CENTER))
        col.addView(badge, LinearLayout.LayoutParams(ui.dp(96), ui.dp(96)))

        title = ui.heavy("Can't reach this page", 21f)
        title.gravity = Gravity.CENTER
        val tlp = LinearLayout.LayoutParams(MATCH, WRAP)
        tlp.topMargin = ui.dp(26)
        col.addView(title, tlp)

        message = ui.text("Check your connection and try again.", 14f, ui.GRAY)
        message.gravity = Gravity.CENTER
        message.setLineSpacing(ui.dp(3).toFloat(), 1f)
        val mlp = LinearLayout.LayoutParams(MATCH, WRAP)
        mlp.topMargin = ui.dp(8)
        col.addView(message, mlp)

        retryBtn = ui.bigButton("Try again", ui.ACCENT, 0xFFFFFFFF.toInt()) {
            if (!busy) onRetry()
        }
        val rlp = LinearLayout.LayoutParams(MATCH, WRAP)
        rlp.topMargin = ui.dp(30)
        col.addView(retryBtn, rlp)

        val home = ui.ghostButton("Go to home") { onHome() }
        val hlp = LinearLayout.LayoutParams(MATCH, WRAP)
        hlp.topMargin = ui.dp(10)
        col.addView(home, hlp)

        val lp = LayoutParams(MATCH, WRAP, Gravity.CENTER)
        addView(col, lp)
    }

    fun show(url: String) {
        val domain = UrlUtil.domainOf(url)
        message.text = if (domain.isNotEmpty()) "$domain could not be loaded. Check your connection and try again." else "Check your connection and try again."
        setBusy(false)
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(160).start()
        }
    }

    fun hide() {
        animate().cancel()
        alpha = 1f
        visibility = View.GONE
        setBusy(false)
    }

    fun setBusy(on: Boolean) {
        busy = on
        retryBtn.text = if (on) "Trying..." else "Try again"
        retryBtn.alpha = if (on) 0.6f else 1f
    }

    fun setInsets(top: Int, bottom: Int) {
    }
}
