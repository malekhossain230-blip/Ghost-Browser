package com.ghost.browser

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.BreakIterator

interface HomeHost {
    val activity: Activity
    fun openSite(url: String)
    fun runSearch(query: String)
    fun showToast(message: String, action: String? = null, onAction: (() -> Unit)? = null)
    fun openEngineSheet()
    fun startVoice()
    fun clearHistoryPrompt()
    fun topInset(): Int
    fun bottomInset(): Int
}

class SearchPage(private val ui: Ios, private val store: HomeStore, private val host: HomeHost) {

    private val ctx: Context = host.activity
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val main = Handler(Looper.getMainLooper())

    val view = FrameLayout(ctx)
    var isOpen = false
        private set

    private val head = LinearLayout(ctx)
    private val field = LinearLayout(ctx)
    private val input = EditText(ctx)
    private val clearBtn = ImageView(ctx)
    private val engineBox = IconBox(ctx, ui, 32, 10, 20)
    private val scroll = ScrollView(ctx)
    private val list = LinearLayout(ctx)
    private var seq = 0
    private var pending: Runnable? = null
    private var suppressUntil = 0L

    init {
        view.setBackgroundColor(ui.BG)
        view.visibility = View.GONE
        view.isClickable = true
        view.isFocusable = true

        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        view.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        col.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

        field.orientation = LinearLayout.HORIZONTAL
        field.gravity = Gravity.CENTER_VERTICAL
        field.minimumHeight = ui.dp(48)
        field.setPadding(ui.dp(14), 0, ui.dp(6), 0)
        field.background = ui.glass(ui.CARD, 14, ui.LINE)
        field.elevation = if (ui.dark) 0f else ui.dp(2).toFloat()

        val icon = ImageView(ctx)
        icon.setImageDrawable(HomeIcons.drawable("magnifying-glass", ui.GRAY))
        val ilp = LinearLayout.LayoutParams(ui.dp(15), ui.dp(15))
        ilp.rightMargin = ui.dp(10)
        field.addView(icon, ilp)

        input.background = null
        input.setTextColor(ui.TEXT)
        input.setHintTextColor(ui.GRAY)
        input.textSize = 16f
        input.setSingleLine(true)
        input.imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        input.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        input.setPadding(0, ui.dp(12), 0, ui.dp(12))
        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                submit()
                true
            } else false
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onInputChanged()
            }
        })
        input.setOnFocusChangeListener { _, has ->
            field.background = ui.glass(ui.CARD, 14, if (has) ui.ACCENT else ui.LINE)
        }
        field.addView(input, LinearLayout.LayoutParams(0, WRAP, 1f))

        clearBtn.setImageDrawable(HomeIcons.drawable("xmark", ui.GRAY))
        clearBtn.background = ui.oval(ui.SOFT)
        clearBtn.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
        clearBtn.visibility = View.GONE
        clearBtn.setOnClickListener {
            input.setText("")
            focusInput()
        }
        val clp = LinearLayout.LayoutParams(ui.dp(26), ui.dp(26))
        clp.rightMargin = ui.dp(4)
        field.addView(clearBtn, clp)

        val mic = ImageView(ctx)
        mic.setImageDrawable(HomeIcons.drawable("microphone", ui.ACCENT))
        mic.setPadding(ui.dp(9), ui.dp(9), ui.dp(9), ui.dp(9))
        mic.background = ui.pressableRound(18, ui.oval(Color.TRANSPARENT))
        mic.setOnClickListener { host.startVoice() }
        field.addView(mic, LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)))

        head.addView(field, LinearLayout.LayoutParams(0, WRAP, 1f))

        val eng = FrameLayout(ctx)
        eng.background = ui.pressableRound(14, ui.glass(ui.CARD, 14, ui.LINE))
        eng.elevation = if (ui.dark) 0f else ui.dp(2).toFloat()
        eng.addView(engineBox, FrameLayout.LayoutParams(ui.dp(32), ui.dp(32), Gravity.CENTER))
        eng.setOnClickListener { host.openEngineSheet() }
        val elp = LinearLayout.LayoutParams(ui.dp(48), ui.dp(48))
        elp.leftMargin = ui.dp(9)
        head.addView(eng, elp)

        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list, ViewGroup.LayoutParams(MATCH, WRAP))
        col.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))

        paintEngine()
        paintPauseState()
    }

    private fun applyInsets() {
        head.setPadding(ui.dp(14), host.topInset() + ui.dp(10), ui.dp(14), ui.dp(12))
        list.setPadding(ui.dp(14), 0, ui.dp(14), host.bottomInset() + ui.dp(24))
    }

    fun paintEngine() {
        engineBox.bindEngine(Engines.get(store.engine))
    }

    fun paintPauseState() {
        input.hint = if (store.pauseHistory) "Private search" else "Search or type a URL"
    }

    fun currentQuery(): String = input.text.toString().trim()

    fun open(prefill: String) {
        if (isOpen) return
        isOpen = true
        applyInsets()
        paintEngine()
        paintPauseState()
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = ui.dp(10).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(180).start()
        input.setText(prefill)
        input.setSelection(input.text.length)
        updateClear()
        renderList(currentQuery())
        main.postDelayed({ if (isOpen) focusInput() }, 80)
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        cancelPending()
        Suggest.cancelAll()
        hideKeyboard()
        input.clearFocus()
        view.animate().alpha(0f).translationY(ui.dp(10).toFloat()).setDuration(150).withEndAction {
            if (!isOpen) view.visibility = View.GONE
        }.start()
    }

    fun refresh() {
        if (isOpen) renderList(currentQuery())
    }

    private fun focusInput() {
        input.requestFocus()
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun submit() {
        val q = currentQuery()
        if (q.isEmpty()) return
        host.runSearch(q)
    }

    private fun updateClear() {
        clearBtn.visibility = if (input.text.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun onInputChanged() {
        updateClear()
        if (isOpen) renderList(currentQuery())
    }

    private fun cancelPending() {
        val p = pending
        if (p != null) main.removeCallbacks(p)
        pending = null
    }

    private fun sectionCard(title: String?, action: String? = null, onAction: (() -> Unit)? = null): Pair<LinearLayout, LinearLayout> {
        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.background = ui.cardBg(16)
        card.clipToOutline = true
        card.elevation = if (ui.dark) 0f else ui.dp(2).toFloat()
        if (title != null) {
            val h = LinearLayout(ctx)
            h.orientation = LinearLayout.HORIZONTAL
            h.gravity = Gravity.CENTER_VERTICAL
            h.setPadding(ui.dp(14), ui.dp(11), ui.dp(10), ui.dp(2))
            val t = ui.text(title.uppercase(), 11.5f, ui.LABEL, true)
            t.letterSpacing = 0.06f
            h.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f))
            if (action != null) {
                val a = ui.text(action, 13f, ui.ACCENT, true)
                a.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4))
                a.background = ui.pressableRound(8, ui.rounded(Color.TRANSPARENT, 8))
                a.setOnClickListener { onAction?.invoke() }
                h.addView(a)
            }
            card.addView(h, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        val body = LinearLayout(ctx)
        body.orientation = LinearLayout.VERTICAL
        card.addView(body, LinearLayout.LayoutParams(MATCH, WRAP))
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.bottomMargin = ui.dp(12)
        list.addView(card, lp)
        card.alpha = 0f
        card.translationY = ui.dp(6).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(220).start()
        return Pair(card, body)
    }

    private fun addRow(body: LinearLayout, row: View) {
        if (body.childCount > 0) {
            val sep = View(ctx)
            sep.setBackgroundColor(ui.LINE)
            body.addView(sep, LinearLayout.LayoutParams(MATCH, ui.hairPx()))
        }
        body.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun graphemeBoundary(s: String, n: Int): Int {
        if (n >= s.length) return s.length
        val it = BreakIterator.getCharacterInstance()
        it.setText(s)
        var b = it.following(n - 1)
        if (b == BreakIterator.DONE) b = s.length
        return b
    }

    private fun highlight(s: String, q: String): CharSequence {
        val low = s.lowercase()
        val ql = q.lowercase()
        if (ql.isNotEmpty() && low.startsWith(ql) && s.length > q.length) {
            val cut = graphemeBoundary(s, q.length)
            if (cut >= s.length) return s
            val sp = SpannableString(s)
            sp.setSpan(StyleSpan(Typeface.BOLD), cut, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return sp
        }
        return s
    }

    private fun termLabel(s: String, q: String): CharSequence {
        if (UrlUtil.looksLikeUrl(s)) {
            val d = UrlUtil.displayTerm(s)
            val sp = SpannableString(d)
            sp.setSpan(ForegroundColorSpan(ui.ACCENT), 0, d.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return sp
        }
        return if (q.isNotEmpty()) highlight(s, q) else s
    }

    private fun makeRow(text: String, icon: String, label: CharSequence, primary: Boolean, recent: Boolean, noFill: Boolean): LinearLayout {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.minimumHeight = ui.dp(52)
        row.setPadding(ui.dp(12), ui.dp(9), ui.dp(6), ui.dp(9))
        row.background = ui.pressable()

        val ic = ImageView(ctx)
        ic.setImageDrawable(HomeIcons.drawable(icon, if (primary) Color.WHITE else ui.GRAY))
        ic.background = ui.rounded(if (primary) ui.ACCENT else ui.SOFT, 10)
        ic.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
        val ilp = LinearLayout.LayoutParams(ui.dp(32), ui.dp(32))
        ilp.rightMargin = ui.dp(12)
        row.addView(ic, ilp)

        val tv = ui.text("", 15f, ui.TEXT)
        tv.text = label
        tv.maxLines = 1
        tv.ellipsize = android.text.TextUtils.TruncateAt.END
        row.addView(tv, LinearLayout.LayoutParams(0, WRAP, 1f))

        if (!noFill) {
            val display = UrlUtil.displayTerm(text)
            val fill = ImageView(ctx)
            fill.setImageDrawable(HomeIcons.drawable("arrow-up", ui.GRAY))
            fill.rotation = -45f
            fill.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10))
            fill.background = ui.pressableRound(10, ui.rounded(Color.TRANSPARENT, 10))
            fill.setOnClickListener {
                input.setText(display)
                input.setSelection(input.text.length)
                focusInput()
            }
            row.addView(fill, LinearLayout.LayoutParams(ui.dp(34), ui.dp(34)))
        }

        row.setOnClickListener {
            if (System.currentTimeMillis() < suppressUntil) return@setOnClickListener
            host.runSearch(text)
        }
        if (recent) {
            row.setOnLongClickListener {
                suppressUntil = System.currentTimeMillis() + 600
                row.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                row.animate().translationX(-ui.dp(16).toFloat()).alpha(0f).setDuration(190).withEndAction {
                    val parent = row.parent as? ViewGroup
                    parent?.removeView(row)
                    val idx = store.removeHistory(text)
                    host.showToast("Removed from history", "Undo") {
                        store.insertHistory(idx, text)
                        refresh()
                    }
                    if (parent != null && parent.childCount == 0) refresh()
                }.start()
                true
            }
        }
        return row
    }

    private fun skeletonRows(body: LinearLayout, n: Int) {
        val widths = floatArrayOf(0.62f, 0.78f, 0.48f, 0.7f)
        for (i in 0 until n) {
            val r = LinearLayout(ctx)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER_VERTICAL
            r.setPadding(ui.dp(12), ui.dp(11), ui.dp(12), ui.dp(11))
            val ic = View(ctx)
            ic.background = ui.rounded(ui.SOFT, 10)
            val ilp = LinearLayout.LayoutParams(ui.dp(32), ui.dp(32))
            ilp.rightMargin = ui.dp(12)
            r.addView(ic, ilp)
            val line = View(ctx)
            line.background = ui.rounded(ui.SOFT, 6)
            r.addView(line, LinearLayout.LayoutParams(0, ui.dp(12), widths[i % widths.size]))
            val pad = View(ctx)
            r.addView(pad, LinearLayout.LayoutParams(0, 1, 1f - widths[i % widths.size]))
            r.alpha = 0.55f
            r.animate().alpha(1f).setDuration(650).setStartDelay((i * 80).toLong()).withEndAction {
                r.animate().alpha(0.55f).setDuration(650).start()
            }.start()
            addRow(body, r)
        }
    }

    private fun renderRecents() {
        val recents = store.history()
        if (store.pauseHistory) {
            val chip = LinearLayout(ctx)
            chip.orientation = LinearLayout.HORIZONTAL
            chip.gravity = Gravity.CENTER_VERTICAL
            chip.background = ui.rounded(ui.ACCENT_SOFT, 999)
            chip.setPadding(ui.dp(13), ui.dp(8), ui.dp(14), ui.dp(8))
            val ic = ImageView(ctx)
            ic.setImageDrawable(HomeIcons.drawable("user-secret", ui.ACCENT))
            val ilp = LinearLayout.LayoutParams(ui.dp(13), ui.dp(13))
            ilp.rightMargin = ui.dp(8)
            chip.addView(ic, ilp)
            chip.addView(ui.text("History paused — searches won't be saved", 12.5f, ui.ACCENT, true))
            val lp = LinearLayout.LayoutParams(WRAP, WRAP)
            lp.bottomMargin = ui.dp(12)
            list.addView(chip, lp)
        }
        if (recents.isEmpty()) {
            val (card, body) = sectionCard(null)
            val empty = LinearLayout(ctx)
            empty.orientation = LinearLayout.VERTICAL
            empty.gravity = Gravity.CENTER_HORIZONTAL
            empty.setPadding(ui.dp(20), ui.dp(34), ui.dp(20), ui.dp(30))
            val big = ImageView(ctx)
            big.setImageDrawable(HomeIcons.drawable("clock-rotate-left", ui.LABEL))
            big.alpha = 0.55f
            big.background = ui.oval(ui.SOFT)
            big.setPadding(ui.dp(22), ui.dp(22), ui.dp(22), ui.dp(22))
            val blp = LinearLayout.LayoutParams(ui.dp(76), ui.dp(76))
            blp.bottomMargin = ui.dp(16)
            empty.addView(big, blp)
            val t = ui.text("No searches yet", 15f, ui.TEXT, true)
            t.gravity = Gravity.CENTER
            empty.addView(t)
            val s = ui.text("What you search for will show up here", 13f, ui.GRAY)
            s.gravity = Gravity.CENTER
            s.setPadding(0, ui.dp(4), 0, 0)
            empty.addView(s)
            body.addView(empty, LinearLayout.LayoutParams(MATCH, WRAP))
            card.tag = "empty"
            return
        }
        val (_, body) = sectionCard("Recent", "Clear") { host.clearHistoryPrompt() }
        var n = 0
        for (r in recents) {
            if (n >= 10) break
            addRow(body, makeRow(r, "clock-rotate-left", termLabel(r, ""), false, true, false))
            n++
        }
        scroll.scrollTo(0, 0)
    }

    fun renderList(q: String) {
        seq++
        cancelPending()
        list.removeAllViews()
        if (q.isEmpty()) {
            renderRecents()
            return
        }
        val isUrl = UrlUtil.looksLikeUrl(q)
        val seen = HashSet<String>()
        seen.add(q.lowercase())

        val (_, primary) = sectionCard(null)
        if (isUrl) {
            val d = UrlUtil.displayTerm(q)
            val sp = SpannableString("$d — Open site")
            sp.setSpan(ForegroundColorSpan(ui.ACCENT), 0, d.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(ui.GRAY), d.length, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            addRow(primary, makeRow(q, "link", sp, true, false, true))
        } else {
            val label = Engines.get(store.engine).label
            val sp = SpannableString("$q — $label Search")
            sp.setSpan(StyleSpan(Typeface.BOLD), 0, q.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(ui.GRAY), q.length, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            addRow(primary, makeRow(q, "magnifying-glass", sp, true, false, true))
        }

        val low = q.lowercase()
        val matches = ArrayList<String>()
        for (r in store.history()) {
            if (matches.size >= 3) break
            val rk = r.lowercase()
            if (seen.contains(rk) || !rk.contains(low)) continue
            seen.add(rk)
            matches.add(r)
        }
        if (matches.isNotEmpty()) {
            val (_, body) = sectionCard("From recent")
            for (m in matches) addRow(body, makeRow(m, "clock-rotate-left", termLabel(m, q), false, true, false))
        }

        if (store.suggest && !isUrl) {
            val (card, body) = sectionCard("Suggestions")
            val hit = Suggest.cached(q)
            if (hit != null) {
                fillSuggest(card, body, q, hit, seen)
            } else {
                skeletonRows(body, 4)
                val my = seq
                val r = Runnable {
                    Suggest.fetch(q) { result ->
                        if (my != seq || !isOpen) return@fetch
                        body.removeAllViews()
                        fillSuggest(card, body, q, result, seen)
                    }
                }
                pending = r
                main.postDelayed(r, 80)
            }
        }
        scroll.scrollTo(0, 0)
    }

    private fun fillSuggest(card: LinearLayout, body: LinearLayout, q: String, items: List<String>, seen: HashSet<String>) {
        var n = 0
        for (s in items) {
            if (n >= 8) break
            val k = s.lowercase()
            if (k.isEmpty() || seen.contains(k)) continue
            seen.add(k)
            addRow(body, makeRow(s, if (UrlUtil.looksLikeUrl(s)) "globe" else "magnifying-glass", termLabel(s, q), false, false, false))
            n++
        }
        if (n == 0) list.removeView(card)
    }
}
