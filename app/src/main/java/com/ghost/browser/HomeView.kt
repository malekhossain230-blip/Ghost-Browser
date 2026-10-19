package com.ghost.browser

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class HomeView(private val act: MainActivity, private val ui: Ios, private val store: HomeStore) : FrameLayout(act), HomeHost {

    override val activity: Activity = act

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val main = Handler(Looper.getMainLooper())

    private var insetT = 0
    private var insetB = 0

    val sheets = HomeSheets(act, ui, store, this)
    val voice = VoiceSearch(ui, this)
    private val search = SearchPage(ui, store, this)

    private val scroll = ScrollView(act)
    private val column = LinearLayout(act)
    private val gridHost = LinearLayout(act)
    private val hint = TextView(act)
    private val engineBox = IconBox(act, ui, 32, 10, 20)
    private val menuBtn = FrameLayout(act)

    private val menuLayer = FrameLayout(act)
    private val scrim = View(act)
    private val menuPanel = ScrollView(act)
    private val menuBody = LinearLayout(act)
    private var menuOpen = false

    private var toastView: View? = null
    private var toastHide: Runnable? = null

    init {
        setBackgroundColor(ui.BG)
        isClickable = true

        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        column.orientation = LinearLayout.VERTICAL
        column.gravity = Gravity.CENTER_HORIZONTAL
        scroll.addView(column, ViewGroup.LayoutParams(MATCH, WRAP))
        addView(scroll, LayoutParams(MATCH, MATCH))

        buildBrand()
        buildSearchCard()
        gridHost.orientation = LinearLayout.VERTICAL
        val glp = LinearLayout.LayoutParams(MATCH, WRAP)
        glp.topMargin = ui.dp(34)
        column.addView(gridHost, glp)

        menuBtn.background = ui.pressableRound(22, ui.glass(ui.CARD, 22, ui.LINE))
        menuBtn.elevation = if (ui.dark) 0f else ui.dp(2).toFloat()
        val bars = ImageView(act)
        bars.setImageDrawable(HomeIcons.drawable("bars", ui.TEXT))
        menuBtn.addView(bars, LayoutParams(ui.dp(18), ui.dp(18), Gravity.CENTER))
        menuBtn.setOnClickListener { openMenu() }
        addView(menuBtn, LayoutParams(ui.dp(44), ui.dp(44), Gravity.TOP or Gravity.END))

        addView(search.view, LayoutParams(MATCH, MATCH))
        addView(voice.view, LayoutParams(MATCH, MATCH))

        buildMenuLayer()
        addView(menuLayer, LayoutParams(MATCH, MATCH))

        applyInsetsInternal()
        paintEngine()
        paintPlaceholder()
        renderShortcuts()
    }

    private fun buildBrand() {
        val logoId = sheets.drawableId(if (ui.dark) "home_logo_dark" else "home_logo")
        val fallbackId = sheets.drawableId("home_logo")
        val useId = if (logoId != 0) logoId else fallbackId

        val logo = FrameLayout(act)
        if (useId != 0) {
            val img = ImageView(act)
            img.setImageResource(useId)
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            logo.addView(img, LayoutParams(MATCH, MATCH))
        } else {
            logo.background = ui.gradient3(24)
            logo.elevation = ui.dp(8).toFloat()
            val shield = ImageView(act)
            shield.setImageDrawable(HomeIcons.drawable("shield-halved", 0xFFFFFFFF.toInt()))
            logo.addView(shield, LayoutParams(ui.dp(34), ui.dp(34), Gravity.CENTER))
        }
        val llp = LinearLayout.LayoutParams(ui.dp(76), ui.dp(76))
        llp.topMargin = ui.dp(6)
        column.addView(logo, llp)

        val title = ui.heavy("Vaelo", 30f)
        title.gravity = Gravity.CENTER
        val tlp = LinearLayout.LayoutParams(WRAP, WRAP)
        tlp.topMargin = ui.dp(16)
        column.addView(title, tlp)

        val sub = ui.text("Private by design", 13.5f, ui.GRAY)
        sub.gravity = Gravity.CENTER
        val slp = LinearLayout.LayoutParams(WRAP, WRAP)
        slp.topMargin = ui.dp(4)
        column.addView(sub, slp)
    }

    private fun buildSearchCard() {
        val card = LinearLayout(act)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.minimumHeight = ui.dp(56)
        card.setPadding(ui.dp(16), ui.dp(4), ui.dp(8), ui.dp(4))
        card.background = ui.pressableRound(20, ui.glass(ui.CARD, 20, ui.LINE))
        card.elevation = if (ui.dark) 0f else ui.dp(4).toFloat()
        card.setOnClickListener { search.open("") }

        val icon = ImageView(act)
        icon.setImageDrawable(HomeIcons.drawable("magnifying-glass", ui.GRAY))
        val ilp = LinearLayout.LayoutParams(ui.dp(16), ui.dp(16))
        ilp.rightMargin = ui.dp(12)
        card.addView(icon, ilp)

        hint.textSize = 15.5f
        hint.setTextColor(ui.GRAY)
        hint.maxLines = 1
        hint.ellipsize = android.text.TextUtils.TruncateAt.END
        card.addView(hint, LinearLayout.LayoutParams(0, WRAP, 1f))

        val mic = ImageView(act)
        mic.setImageDrawable(HomeIcons.drawable("microphone", ui.ACCENT))
        mic.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10))
        mic.background = ui.pressableRound(20, ui.oval(0x00000000))
        mic.setOnClickListener { startVoice() }
        card.addView(mic, LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)))

        val eng = FrameLayout(act)
        eng.background = ui.pressableRound(12, ui.rounded(0x00000000, 12))
        eng.addView(engineBox, LayoutParams(ui.dp(32), ui.dp(32), Gravity.CENTER))
        eng.setOnClickListener { openEngineSheet() }
        val elp = LinearLayout.LayoutParams(ui.dp(42), ui.dp(42))
        elp.leftMargin = ui.dp(4)
        card.addView(eng, elp)

        val clp = LinearLayout.LayoutParams(MATCH, WRAP)
        clp.topMargin = ui.dp(30)
        clp.leftMargin = ui.dp(20)
        clp.rightMargin = ui.dp(20)
        column.addView(card, clp)
    }

    private fun paintEngine() {
        engineBox.bindEngine(Engines.get(store.engine))
    }

    private fun paintPlaceholder() {
        hint.text = if (store.pauseHistory) "Private search" else "Search or type a URL"
    }

    private fun renderShortcuts() {
        gridHost.removeAllViews()
        val cols = 4
        val cells = ArrayList<View>()
        for (s in store.visibleDefaults()) cells.add(shortcutCell(s, -1))
        val mine = store.shortcuts()
        for ((i, s) in mine.withIndex()) cells.add(shortcutCell(s, i))
        cells.add(addCell())

        var row: LinearLayout? = null
        for ((i, c) in cells.withIndex()) {
            if (i % cols == 0) {
                row = LinearLayout(act)
                row.orientation = LinearLayout.HORIZONTAL
                val rlp = LinearLayout.LayoutParams(MATCH, WRAP)
                if (i > 0) rlp.topMargin = ui.dp(18)
                gridHost.addView(row, rlp)
            }
            row!!.addView(c, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        val rest = (cols - cells.size % cols) % cols
        for (k in 0 until rest) row!!.addView(View(act), LinearLayout.LayoutParams(0, ui.dp(1), 1f))
    }

    private fun cellLabel(t: String): TextView {
        val l = ui.text(t, 12f, ui.GRAY, true)
        l.maxLines = 1
        l.gravity = Gravity.CENTER
        l.ellipsize = android.text.TextUtils.TruncateAt.END
        return l
    }

    private fun shortcutCell(s: Shortcut, index: Int): View {
        val cell = LinearLayout(act)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER_HORIZONTAL
        cell.setPadding(ui.dp(4), ui.dp(2), ui.dp(4), ui.dp(2))
        cell.background = ui.pressableRound(14, ui.rounded(0x00000000, 14))

        val box = IconBox(act, ui, 56, 18, 34)
        box.bind(s.name, s.url, store.iconFile(s.icon))
        cell.addView(box, LinearLayout.LayoutParams(ui.dp(56), ui.dp(56)))

        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ui.dp(8)
        val name = if (s.name.isNotEmpty()) s.name else UrlUtil.hostLabel(s.url)
        cell.addView(cellLabel(name), lp)

        cell.setOnClickListener { openSite(s.url) }
        cell.setOnLongClickListener {
            cell.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            shortcutOptions(s, index)
            true
        }
        return cell
    }

    private fun addCell(): View {
        val cell = LinearLayout(act)
        cell.orientation = LinearLayout.VERTICAL
        cell.gravity = Gravity.CENTER_HORIZONTAL
        cell.setPadding(ui.dp(4), ui.dp(2), ui.dp(4), ui.dp(2))
        cell.background = ui.pressableRound(14, ui.rounded(0x00000000, 14))

        val box = FrameLayout(act)
        box.background = ui.glass(ui.SOFT, 18, ui.LINE)
        val plus = ImageView(act)
        plus.setImageDrawable(HomeIcons.drawable("plus", ui.ACCENT))
        box.addView(plus, LayoutParams(ui.dp(20), ui.dp(20), Gravity.CENTER))
        cell.addView(box, LinearLayout.LayoutParams(ui.dp(56), ui.dp(56)))

        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = ui.dp(8)
        cell.addView(cellLabel("Add"), lp)

        cell.setOnClickListener { addShortcut() }
        return cell
    }

    private fun addShortcut() {
        sheets.editShortcut(null) { n ->
            val l = store.shortcuts()
            l.add(n)
            store.saveShortcuts(l)
            renderShortcuts()
        }
    }

    private fun shortcutOptions(s: Shortcut, index: Int) {
        val name = if (s.name.isNotEmpty()) s.name else UrlUtil.hostLabel(s.url)
        if (index < 0) {
            ui.alert(
                act, name, UrlUtil.domainOf(s.url), null,
                listOf(
                    AlertBtn("Cancel", AlertBtn.NORMAL) {},
                    AlertBtn("Hide", AlertBtn.DESTRUCTIVE) {
                        store.hideDefault(s.url)
                        renderShortcuts()
                        showToast("Shortcut hidden")
                    }
                ),
                true
            )
            return
        }
        ui.alert(
            act, name, UrlUtil.domainOf(s.url), null,
            listOf(
                AlertBtn("Cancel", AlertBtn.NORMAL) {},
                AlertBtn("Remove", AlertBtn.DESTRUCTIVE) {
                    val l = store.shortcuts()
                    if (index < l.size) {
                        val gone = l.removeAt(index)
                        store.deleteIcon(gone.icon)
                        store.saveShortcuts(l)
                        renderShortcuts()
                        showToast("Shortcut removed")
                    }
                },
                AlertBtn("Edit", AlertBtn.BOLD) {
                    sheets.editShortcut(s) { n ->
                        val l = store.shortcuts()
                        if (index < l.size) l[index] = n else l.add(n)
                        store.saveShortcuts(l)
                        renderShortcuts()
                    }
                }
            ),
            true
        )
    }

    private fun buildMenuLayer() {
        menuLayer.visibility = View.GONE
        scrim.setBackgroundColor(0x66000000)
        scrim.setOnClickListener { closeMenu() }
        menuLayer.addView(scrim, LayoutParams(MATCH, MATCH))

        val w = Math.min(ui.dp(330), (act.resources.displayMetrics.widthPixels * 0.84f).toInt())
        val bg = GradientDrawable()
        bg.setColor(ui.BG)
        val r = ui.dp(28).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
        menuPanel.background = bg
        menuPanel.isVerticalScrollBarEnabled = false
        menuPanel.overScrollMode = View.OVER_SCROLL_NEVER
        menuPanel.elevation = ui.dp(16).toFloat()
        menuPanel.isClickable = true
        menuBody.orientation = LinearLayout.VERTICAL
        menuPanel.addView(menuBody, ViewGroup.LayoutParams(MATCH, WRAP))
        menuLayer.addView(menuPanel, LayoutParams(w, MATCH, Gravity.END))
    }

    private fun themeLabel(): String = when (store.theme) {
        "light" -> "Light"
        "dark" -> "Dark"
        else -> "System"
    }

    private fun fillMenu() {
        menuBody.removeAllViews()
        menuBody.setPadding(0, insetT + ui.dp(16), 0, insetB + ui.dp(28))

        val head = LinearLayout(act)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(ui.dp(22), 0, ui.dp(14), ui.dp(6))
        val titles = LinearLayout(act)
        titles.orientation = LinearLayout.VERTICAL
        titles.addView(ui.heavy("Menu", 22f))
        val ver = sheets.versionName()
        if (ver.isNotEmpty()) titles.addView(ui.text("Vaelo $ver", 12.5f, ui.GRAY))
        head.addView(titles, LinearLayout.LayoutParams(0, WRAP, 1f))
        val close = FrameLayout(act)
        close.background = ui.pressableRound(18, ui.oval(ui.SOFT))
        val x = ImageView(act)
        x.setImageDrawable(HomeIcons.drawable("xmark", ui.GRAY))
        close.addView(x, LayoutParams(ui.dp(14), ui.dp(14), Gravity.CENTER))
        close.setOnClickListener { closeMenu() }
        head.addView(close, LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)))
        menuBody.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

        val addCard = LinearLayout(act)
        addCard.orientation = LinearLayout.HORIZONTAL
        addCard.gravity = Gravity.CENTER_VERTICAL
        addCard.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
        addCard.background = ui.pressableRound(18, ui.accent(18))
        addCard.elevation = ui.dp(4).toFloat()
        val plusWrap = FrameLayout(act)
        plusWrap.background = ui.oval(0x33FFFFFF)
        val plus = ImageView(act)
        plus.setImageDrawable(HomeIcons.drawable("plus", 0xFFFFFFFF.toInt()))
        plusWrap.addView(plus, LayoutParams(ui.dp(18), ui.dp(18), Gravity.CENTER))
        val plw = LinearLayout.LayoutParams(ui.dp(42), ui.dp(42))
        plw.rightMargin = ui.dp(14)
        addCard.addView(plusWrap, plw)
        val at = LinearLayout(act)
        at.orientation = LinearLayout.VERTICAL
        at.addView(ui.text("Add shortcut", 16f, 0xFFFFFFFF.toInt(), true))
        at.addView(ui.text("Paste a link, the name and icon fill in", 12f, 0xDDFFFFFF.toInt()))
        addCard.addView(at, LinearLayout.LayoutParams(0, WRAP, 1f))
        addCard.setOnClickListener {
            closeMenu()
            main.postDelayed({ addShortcut() }, 220)
        }
        val alp = LinearLayout.LayoutParams(MATCH, WRAP)
        alp.leftMargin = ui.dp(14)
        alp.rightMargin = ui.dp(14)
        alp.topMargin = ui.dp(12)
        menuBody.addView(addCard, alp)

        menuBody.addView(ui.header("Search"), LinearLayout.LayoutParams(MATCH, WRAP))
        val c1 = ui.card()
        ui.add(c1, ui.row("search", ui.ACCENT, "Search engine", null, Engines.get(store.engine).label, true) {
            closeMenu()
            main.postDelayed({ openEngineSheet() }, 220)
        }.view)
        ui.add(c1, ui.switchRow("clipboard", ui.INDIGO, "Save search history", "Recent searches stay on this device", !store.pauseHistory) { on ->
            store.pauseHistory = !on
            search.paintPauseState()
            paintPlaceholder()
            search.refresh()
        })
        ui.add(c1, ui.switchRow("search", ui.TEAL, "Search suggestions", "Suggestions while you type", store.suggest) { on ->
            store.suggest = on
            search.refresh()
        })
        ui.add(c1, ui.row("trash", ui.RED, "Clear history", null, null, false, ui.RED) {
            closeMenu()
            main.postDelayed({ clearHistoryPrompt() }, 220)
        }.view)
        ui.addCard(menuBody, c1)

        menuBody.addView(ui.header("Browser"), LinearLayout.LayoutParams(MATCH, WRAP))
        val c2 = ui.card()
        val theme = ui.row("monitor", ui.PURPLE, "Appearance", null, themeLabel(), true) {}
        theme.view.setOnClickListener {
            ui.popupMenu(
                theme.view,
                listOf("system" to "System", "light" to "Light", "dark" to "Dark"),
                store.theme
            ) { k ->
                if (k != store.theme) {
                    store.theme = k
                    act.applyTheme()
                }
            }
        }
        ui.add(c2, theme.view)
        ui.add(c2, ui.switchRow("compass", ui.ORANGE, "Floating button", "Quick menu on every page", store.floatingButton) { on ->
            store.floatingButton = on
            act.updateFab()
        })
        ui.add(c2, ui.row("shield", ui.GREEN, "Privacy settings", "Device profile and site rules", null, true) {
            closeMenu()
            main.postDelayed({ act.panel.open() }, 220)
        }.view)
        ui.add(c2, ui.row("refresh", ui.ORANGE, "Reset shortcuts", null, null, false) {
            closeMenu()
            main.postDelayed({
                sheets.confirm(
                    "Reset shortcuts",
                    "Custom shortcuts will be removed and the default ones restored.",
                    "Reset", true, "rotate-left"
                ) {
                    store.resetShortcuts()
                    renderShortcuts()
                    showToast("Shortcuts reset")
                }
            }, 220)
        }.view)
        ui.addCard(menuBody, c2)

        menuBody.addView(ui.header("About"), LinearLayout.LayoutParams(MATCH, WRAP))
        val c3 = ui.card()
        ui.add(c3, ui.row("code", ui.INDIGO, "Developer", "X ZONE BD", null, true) {
            closeMenu()
            main.postDelayed({ sheets.developer() }, 220)
        }.view)
        ui.add(c3, ui.row("globe", ui.BLUE, "About Vaelo", null, null, true) {
            closeMenu()
            main.postDelayed({ sheets.about() }, 220)
        }.view)
        ui.addCard(menuBody, c3)
    }

    private fun openMenu() {
        if (menuOpen) return
        menuOpen = true
        fillMenu()
        menuPanel.scrollTo(0, 0)
        menuLayer.visibility = View.VISIBLE
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(220).start()
        menuPanel.translationX = menuPanel.layoutParams.width.toFloat()
        menuPanel.animate().translationX(0f).setDuration(280).setInterpolator(DecelerateInterpolator(1.8f)).start()
    }

    private fun closeMenu() {
        if (!menuOpen) return
        menuOpen = false
        scrim.animate().alpha(0f).setDuration(200).start()
        menuPanel.animate().translationX(menuPanel.layoutParams.width.toFloat()).setDuration(220).withEndAction {
            if (!menuOpen) menuLayer.visibility = View.GONE
        }.start()
    }

    private fun applyInsetsInternal() {
        val mlp = menuBtn.layoutParams as LayoutParams
        mlp.topMargin = insetT + ui.dp(10)
        mlp.rightMargin = ui.dp(16)
        menuBtn.layoutParams = mlp
        column.setPadding(0, insetT + ui.dp(78), 0, insetB + ui.dp(28))
    }

    fun setInsets(top: Int, bottom: Int) {
        insetT = top
        insetB = bottom
        applyInsetsInternal()
    }

    fun onBack(): Boolean {
        if (voice.isOpen) {
            voice.close()
            return true
        }
        if (menuOpen) {
            closeMenu()
            return true
        }
        if (search.isOpen) {
            search.close()
            return true
        }
        return false
    }

    fun onShown() {
        paintEngine()
        paintPlaceholder()
        search.paintEngine()
        search.paintPauseState()
        renderShortcuts()
        scroll.scrollTo(0, 0)
    }

    fun closeOverlays() {
        if (menuOpen) {
            menuOpen = false
            menuLayer.visibility = View.GONE
        }
        search.close()
        voice.close()
        clearToast()
    }

    private fun clearToast() {
        toastHide?.let { main.removeCallbacks(it) }
        toastHide = null
        toastView?.let { removeView(it) }
        toastView = null
    }

    override fun showToast(message: String, action: String?, onAction: (() -> Unit)?) {
        clearToast()
        val pill = LinearLayout(act)
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.setPadding(ui.dp(18), ui.dp(11), ui.dp(18), ui.dp(11))
        pill.background = ui.rounded(ui.TOAST_BG, 22)
        pill.elevation = ui.dp(16).toFloat()
        pill.translationZ = ui.dp(40).toFloat()

        val t = TextView(act)
        t.text = message
        t.textSize = 13.5f
        t.setTextColor(ui.TOAST_FG)
        t.typeface = ui.BOLD
        t.maxLines = 1
        t.ellipsize = android.text.TextUtils.TruncateAt.END
        pill.addView(t, LinearLayout.LayoutParams(WRAP, WRAP))

        if (action != null && onAction != null) {
            val a = TextView(act)
            a.text = action
            a.textSize = 13.5f
            a.setTextColor(ui.ACCENT)
            a.typeface = ui.BOLD
            a.setPadding(ui.dp(16), 0, 0, 0)
            a.setOnClickListener {
                clearToast()
                onAction()
            }
            pill.addView(a, LinearLayout.LayoutParams(WRAP, WRAP))
        }

        val lp = LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        lp.topMargin = insetT + ui.dp(12)
        lp.leftMargin = ui.dp(16)
        lp.rightMargin = ui.dp(16)
        pill.alpha = 0f
        pill.translationY = -ui.dp(20).toFloat()
        addView(pill, lp)
        pill.animate().alpha(1f).translationY(0f).setDuration(200).start()
        toastView = pill

        val hide = Runnable {
            pill.animate().alpha(0f).setDuration(250).withEndAction {
                if (toastView === pill) toastView = null
                removeView(pill)
            }.start()
        }
        toastHide = hide
        main.postDelayed(hide, if (action != null) 4500 else 3000)
    }

    override fun openSite(url: String) {
        closeOverlays()
        act.openUrl(url)
    }

    override fun runSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        store.pushHistory(q)
        val url = if (UrlUtil.looksLikeUrl(q)) UrlUtil.toLoadable(q) else Engines.searchUrl(store.engine, q)
        closeOverlays()
        act.openUrl(url)
    }

    override fun openEngineSheet() {
        sheets.engine { k ->
            store.engine = k
            paintEngine()
            search.paintEngine()
            search.refresh()
        }
    }

    override fun startVoice() {
        voice.open()
    }

    override fun clearHistoryPrompt() {
        sheets.confirm(
            "Clear history",
            "All recent searches will be removed from this device.",
            "Clear", true, "trash"
        ) {
            store.clearHistory()
            search.refresh()
            showToast("History cleared")
        }
    }

    override fun topInset(): Int = insetT

    override fun bottomInset(): Int = insetB
}
