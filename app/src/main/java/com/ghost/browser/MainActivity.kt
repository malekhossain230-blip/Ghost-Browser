package com.ghost.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var store: Store
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var fab: TextView
    private lateinit var root: FrameLayout
    private val ui = Handler(Looper.getMainLooper())
    private var spoofJs = ""
    private var panel: Dialog? = null
    private var panelContent: FrameLayout? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // =====================================================================
    //  Lifecycle
    // =====================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        spoofJs = assets.open("spoof.js").bufferedReader().use { it.readText() }
        buildUi()
        setupWebView()
        web.loadUrl("https://www.google.com")
    }

    override fun onStart() {
        super.onStart()
        // floating icon always comes back when the app is opened again
        if (::fab.isInitialized) fab.visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    // =====================================================================
    //  UI: address bar + WebView + floating icon
    // =====================================================================
    private fun buildUi() {
        root = FrameLayout(this)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(Color.WHITE)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(6), dp(6), dp(6), dp(6))
        bar.setBackgroundColor(0xFFF1F3F4.toInt())

        val back = barButton("◀") { if (web.canGoBack()) web.goBack() }
        val fwd = barButton("▶") { if (web.canGoForward()) web.goForward() }

        address = EditText(this)
        address.setSingleLine(true)
        address.textSize = 14f
        address.hint = "সার্চ করুন বা ঠিকানা লিখুন"
        address.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        address.imeOptions = EditorInfo.IME_ACTION_GO
        address.setPadding(dp(16), dp(8), dp(16), dp(8))
        val pill = GradientDrawable()
        pill.cornerRadius = dp(22).toFloat()
        pill.setColor(Color.WHITE)
        pill.setStroke(dp(1), 0xFFDADCE0.toInt())
        address.background = pill
        address.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                go(address.text.toString())
                true
            } else false
        }

        val reload = barButton("⟳") { web.reload() }

        bar.addView(back, LinearLayout.LayoutParams(dp(40), dp(40)))
        bar.addView(fwd, LinearLayout.LayoutParams(dp(40), dp(40)))
        bar.addView(address, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(reload, LinearLayout.LayoutParams(dp(40), dp(40)))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = 100
        progress.visibility = View.GONE

        web = WebView(this)

        col.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        col.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        fab = TextView(this)
        fab.text = "G"
        fab.setTextColor(Color.WHITE)
        fab.textSize = 20f
        fab.setTypeface(fab.typeface, android.graphics.Typeface.BOLD)
        fab.gravity = Gravity.CENTER
        val circle = GradientDrawable()
        circle.shape = GradientDrawable.OVAL
        circle.setColor(0xDD1A73E8.toInt())
        fab.background = circle
        fab.elevation = dp(8).toFloat()

        root.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(fab, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP or Gravity.START))
        setContentView(root)

        root.post {
            fab.x = (root.width - dp(68)).toFloat()
            fab.y = root.height * 0.55f
        }
        setupFab()
    }

    private fun barButton(label: String, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = 18f
        t.gravity = Gravity.CENTER
        t.setTextColor(0xFF444444.toInt())
        t.setOnClickListener { onClick() }
        return t
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFab() {
        var dX = 0f
        var dY = 0f
        var downX = 0f
        var downY = 0f
        var moved = false
        var longDone = false
        val longRun = Runnable {
            if (!moved) {
                longDone = true
                fab.visibility = View.GONE
                Toast.makeText(this, "আইকন লুকানো হয়েছে। অ্যাপ আবার খুললে ফিরে আসবে।", Toast.LENGTH_LONG).show()
            }
        }
        fab.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - e.rawX
                    dY = v.y - e.rawY
                    downX = e.rawX
                    downY = e.rawY
                    moved = false
                    longDone = false
                    ui.postDelayed(longRun, 700)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawX - downX) > dp(8) || Math.abs(e.rawY - downY) > dp(8)) {
                        moved = true
                        ui.removeCallbacks(longRun)
                    }
                    if (moved) {
                        val maxX = Math.max(0f, (root.width - v.width).toFloat())
                        val maxY = Math.max(0f, (root.height - v.height).toFloat())
                        v.x = (e.rawX + dX).coerceIn(0f, maxX)
                        v.y = (e.rawY + dY).coerceIn(0f, maxY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    ui.removeCallbacks(longRun)
                    if (!moved && !longDone) openPanel()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    ui.removeCallbacks(longRun)
                    true
                }
                else -> false
            }
        }
    }

    // =====================================================================
    //  WebView
    // =====================================================================
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.setGeolocationEnabled(true)
        s.mediaPlaybackRequiresUserGesture = false
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        applyUserAgent()
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.addJavascriptInterface(GhostBridge(this, store), "GhostBridge")
        val docStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (docStart) {
            WebViewCompat.addDocumentStartJavaScript(web, spoofJs, setOf("*"))
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null) address.setText(url)
                if (!docStart) view?.evaluateJavascript(spoofJs, null)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url?.toString() ?: return false
                val ok = u.startsWith("http://") || u.startsWith("https://") ||
                    u.startsWith("about:") || u.startsWith("data:") || u.startsWith("blob:")
                return !ok
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            // JS-level geolocation was already approved by our popup; allow the native call
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                ensureRuntime(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
                callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val r = request ?: return
                val wants = ArrayList<String>()
                val runtime = ArrayList<String>()
                val names = ArrayList<String>()
                for (res in r.resources) {
                    if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                        wants.add(res)
                        runtime.add(Manifest.permission.CAMERA)
                        names.add("ক্যামেরা")
                    } else if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                        wants.add(res)
                        runtime.add(Manifest.permission.RECORD_AUDIO)
                        names.add("মাইক্রোফোন")
                    }
                }
                if (wants.isEmpty()) {
                    r.deny()
                    return
                }
                decide(r.origin.toString(), "media", "ব্যবহার করতে চাইছে: " + names.joinToString(", ")) { d ->
                    if (d == "real") {
                        ensureRuntime(runtime.toTypedArray())
                        r.grant(wants.toTypedArray())
                    } else {
                        r.deny()
                    }
                }
            }
        }
    }

    private fun applyUserAgent() {
        web.settings.userAgentString = store.get()["ua"]
    }

    private fun ensureRuntime(perms: Array<String>) {
        val need = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
    }

    private fun go(input: String) {
        val t = input.trim()
        if (t.isEmpty()) return
        val url = when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(" ") || !t.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(t)
            else -> "https://$t"
        }
        web.loadUrl(url)
        address.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(address.windowToken, 0)
    }

    // =====================================================================
    //  Permission decisions (called on the UI thread)
    // =====================================================================
    private fun dLabel(d: String): String = when (d) {
        "real" -> "আসল"
        "fake" -> "নকল"
        "block" -> "ব্লক"
        else -> d
    }

    private fun logLine(origin: String, title: String, detail: String, result: String): String {
        val t = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        return "$t  $origin\n$title: $detail  →  $result"
    }

    fun decide(origin: String, group: String, detail: String, cb: (String) -> Unit) {
        val title = Fields.groupTitles[group] ?: group
        val rule = store.rule(origin, group)
        if (rule != null) {
            GhostLog.add(logLine(origin, title, detail, dLabel(rule) + " (সেভ করা নিয়ম)"))
            cb(rule)
            return
        }
        val m = store.mode(group)
        if (m != "ask") {
            GhostLog.add(logLine(origin, title, detail, dLabel(m) + " (ডিফল্ট মোড)"))
            cb(if (m == "fake" && Fields.forGroup(group).isEmpty()) "block" else m)
            return
        }
        showAskDialog(origin, group, detail) { dec, remember ->
            if (remember) store.setRule(origin, group, dec)
            GhostLog.add(logLine(origin, title, detail, dLabel(dec)))
            cb(dec)
        }
    }

    private fun showAskDialog(origin: String, group: String, detail: String, done: (String, Boolean) -> Unit) {
        val fields = Fields.forGroup(group)
        val vals = store.get()

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), 0)

        val msg = TextView(this)
        msg.text = "$origin এই তথ্য চাইছে:\n$detail"
        msg.textSize = 15f
        box.addView(msg)

        val edits = LinkedHashMap<String, EditText>()
        if (fields.isNotEmpty()) {
            val hint = TextView(this)
            hint.text = "\n'নকল দিন' চাপলে নিচের মান যাবে (এডিট করা যাবে):"
            hint.textSize = 13f
            box.addView(hint)
            for (f in fields) {
                val lab = TextView(this)
                lab.text = f.label
                lab.textSize = 12f
                lab.setTextColor(0xFF666666.toInt())
                lab.setPadding(0, dp(8), 0, 0)
                box.addView(lab)
                val et = EditText(this)
                et.setText(vals[f.key])
                et.setSingleLine(true)
                et.textSize = 14f
                edits[f.key] = et
                box.addView(et)
            }
        }
        val remember = CheckBox(this)
        remember.text = "এই সাইটের জন্য মনে রাখুন"
        box.addView(remember)

        val sv = ScrollView(this)
        sv.addView(box)

        val b = AlertDialog.Builder(this)
            .setTitle(Fields.groupTitles[group] ?: group)
            .setView(sv)
            .setCancelable(false)
        if (fields.isNotEmpty()) {
            b.setPositiveButton("নকল দিন") { _, _ ->
                val merged = LinkedHashMap<String, String>(store.get())
                for ((k, e) in edits) merged[k] = e.text.toString()
                store.save(store.active, merged)
                applyUserAgent()
                done("fake", remember.isChecked)
            }
            b.setNeutralButton("আসল দিন") { _, _ -> done("real", remember.isChecked) }
        } else {
            b.setPositiveButton("অনুমতি দিন") { _, _ -> done("real", remember.isChecked) }
        }
        b.setNegativeButton("ব্লক") { _, _ -> done("block", remember.isChecked) }
        b.show()
    }

    // =====================================================================
    //  Floating-icon panel: Profile / Rules / Log
    // =====================================================================
    private fun tv(t: String, size: Float = 14f, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = size
        if (bold) v.setTypeface(v.typeface, android.graphics.Typeface.BOLD)
        return v
    }

    private fun btn(t: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.setAllCaps(false)
        b.setOnClickListener { onClick() }
        return b
    }

    private fun openPanel() {
        panel?.dismiss()
        val d = Dialog(this, android.R.style.Theme_Material_Light_NoActionBar)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(Color.WHITE)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(dp(16), dp(10), dp(8), dp(10))
        val title = tv("Ghost Panel", 18f, true)
        head.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(btn("✕ বন্ধ") { d.dismiss() })
        col.addView(head)

        val tabs = LinearLayout(this)
        tabs.orientation = LinearLayout.HORIZONTAL
        val content = FrameLayout(this)
        panelContent = content
        val labels = listOf("প্রোফাইল", "নিয়ম", "লগ")
        for ((i, l) in labels.withIndex()) {
            tabs.addView(btn(l) { showTab(i) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        col.addView(tabs)
        col.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        d.setContentView(col)
        d.setOnDismissListener {
            panelContent = null
            panel = null
        }
        panel = d
        showTab(0)
        d.show()
    }

    private fun showTab(i: Int) {
        val c = panelContent ?: return
        c.removeAllViews()
        val v = when (i) {
            0 -> profileTab()
            1 -> rulesTab()
            else -> logTab()
        }
        c.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun profileTab(): View {
        val sv = ScrollView(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(16), dp(8), dp(16), dp(24))
        sv.addView(box)

        // --- profile selector row ---
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val names = store.names()
        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        sp.setSelection(names.indexOf(store.active).coerceAtLeast(0))
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val n = names[pos]
                if (n != store.active) {
                    store.active = n
                    applyUserAgent()
                    showTab(0)
                }
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        row.addView(sp, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(btn("নতুন") { newProfileDialog() })
        row.addView(btn("মুছুন") {
            store.delete(store.active)
            applyUserAgent()
            showTab(0)
        })
        box.addView(tv("সক্রিয় প্রোফাইল", 13f))
        box.addView(row)

        // --- groups ---
        val editors = LinkedHashMap<String, EditText>()
        val vals = store.get()
        for ((g, gTitle) in Fields.groupTitles) {
            val fields = Fields.forGroup(g)
            val hdr = tv(gTitle, 16f, true)
            hdr.setPadding(0, dp(18), 0, dp(2))
            box.addView(hdr)

            val modeKeys = if (fields.isEmpty()) listOf("ask", "real", "block") else listOf("ask", "fake", "real", "block")
            val modeLabels = if (fields.isEmpty())
                listOf("প্রতিবার জিজ্ঞেস করো", "সবসময় অনুমতি", "সবসময় ব্লক")
            else
                listOf("প্রতিবার জিজ্ঞেস করো", "সবসময় নকল দাও", "সবসময় আসল দাও", "সবসময় ব্লক")
            val ms = Spinner(this)
            ms.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modeLabels)
            val cur = store.mode(g)
            ms.setSelection(modeKeys.indexOf(if (cur == "fake" && fields.isEmpty()) "block" else cur).coerceAtLeast(0))
            ms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    store.setMode(g, modeKeys[pos])
                }

                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
            box.addView(ms)

            for (f in fields) {
                val lab = tv(f.label, 12f)
                lab.setTextColor(0xFF666666.toInt())
                lab.setPadding(0, dp(6), 0, 0)
                box.addView(lab)
                val et = EditText(this)
                et.setText(vals[f.key])
                et.setSingleLine(true)
                et.textSize = 14f
                editors[f.key] = et
                box.addView(et)
            }
        }

        val save = btn("সেভ করুন ও প্রয়োগ করুন (পেজ রিলোড হবে)") {
            val m = LinkedHashMap<String, String>()
            for ((k, e) in editors) m[k] = e.text.toString()
            store.save(store.active, m)
            applyUserAgent()
            web.reload()
            Toast.makeText(this, "সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        }
        box.addView(save)
        return sv
    }

    private fun newProfileDialog() {
        val et = EditText(this)
        et.hint = "প্রোফাইলের নাম (যেমন Pixel 8)"
        AlertDialog.Builder(this)
            .setTitle("নতুন প্রোফাইল")
            .setMessage("বর্তমান প্রোফাইলের মান কপি হবে, তারপর এডিট করুন।")
            .setView(et)
            .setPositiveButton("তৈরি করুন") { _, _ ->
                val n = et.text.toString().trim()
                if (n.isNotEmpty()) {
                    store.save(n, store.get())
                    store.active = n
                    applyUserAgent()
                    showTab(0)
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun rulesTab(): View {
        val sv = ScrollView(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(16), dp(8), dp(16), dp(24))
        sv.addView(box)

        box.addView(tv("সেভ করা সাইট-নিয়ম", 16f, true))
        val rules = store.ruleList()
        if (rules.isEmpty()) {
            box.addView(tv("এখনো কোনো নিয়ম নেই। পপআপে 'মনে রাখুন' টিক দিলে এখানে আসবে।", 13f))
        }
        for ((key, dec) in rules) {
            val parts = key.split("|")
            val origin = parts.getOrElse(0) { key }
            val g = Fields.groupTitles[parts.getOrElse(1) { "" }] ?: parts.getOrElse(1) { "" }
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER_VERTICAL
            r.addView(
                tv("$origin\n$g → ${dLabel(dec)}", 13f),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            r.addView(btn("মুছুন") {
                store.deleteRule(key)
                showTab(1)
            })
            box.addView(r)
        }
        box.addView(btn("সব নিয়ম মুছুন") {
            store.clearRules()
            showTab(1)
        })

        val h2 = tv("ব্রাউজার ডেটা", 16f, true)
        h2.setPadding(0, dp(20), 0, 0)
        box.addView(h2)
        box.addView(btn("কুকি, ক্যাশ ও স্টোরেজ মুছুন") {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            web.clearCache(true)
            web.clearHistory()
            Toast.makeText(this, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
        })
        return sv
    }

    private fun logTab(): View {
        val sv = ScrollView(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(16), dp(8), dp(16), dp(24))
        sv.addView(box)

        box.addView(tv("কোন সাইট কী চেয়েছে (নতুন আগে)", 16f, true))
        box.addView(btn("লগ মুছুন") {
            GhostLog.clear()
            showTab(2)
        })
        val items = GhostLog.all()
        if (items.isEmpty()) box.addView(tv("এখনো কোনো অনুরোধ আসেনি।", 13f))
        for (line in items) {
            val t = tv(line, 12f)
            t.setPadding(0, dp(8), 0, dp(8))
            box.addView(t)
        }
        return sv
    }
}
