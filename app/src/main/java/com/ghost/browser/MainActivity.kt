package com.ghost.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.LocaleList
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    lateinit var store: Store
    lateinit var web: WebView
    lateinit var ios: Ios
    lateinit var panel: Panel
    private lateinit var ipWatch: IpWatch
    private lateinit var dl: GhostDl
    private lateinit var fab: TextView
    private lateinit var root: FrameLayout
    private val ui = Handler(Looper.getMainLooper())
    private var spoofJs = ""
    var docStart = false
    private val approved = HashSet<String>()
    private var reqCode = 100
    private val pendingPerm = HashMap<Int, (Boolean) -> Unit>()

    @Volatile
    private var pageUrl = ""

    private val homeOrigin = "https://appassets.androidplatform.net"
    private val homeUrl = "https://appassets.androidplatform.net/assets/index.html"

    private val assetLoader: WebViewAssetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // =====================================================================
    //  Lifecycle
    // =====================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        ios = Ios(this)
        panel = Panel(this, store, ios)
        ipWatch = IpWatch(this, store)
        dl = GhostDl(this)
        spoofJs = assets.open("spoof.js").bufferedReader().use { it.readText() }
        applyLocale()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            val a = window.attributes
            a.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = a
        }

        buildUi()
        setupWebView()
        ios.hideBars(window)

        approved.add(homeOrigin)
        if (!docStart) {
            toast("⚠ এই ফোনের WebView পুরনো। Android System WebView আপডেট করুন।")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            withRuntime(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { }
        }
        web.loadUrl(homeUrl)
    }

    override fun onStart() {
        super.onStart()
        // ভাসমান আইকন অ্যাপ আবার খুললে ফিরে আসে
        if (::fab.isInitialized) fab.visibility = View.VISIBLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ios.hideBars(window)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    // সংক্ষিপ্ত বার্তা (iOS ব্যানারের মত, উপরে)
    fun toast(m: String) {
        runOnUiThread {
            if (!::root.isInitialized) return@runOnUiThread
            val t = TextView(this)
            t.text = m
            t.textSize = 14f
            t.setTextColor(Color.WHITE)
            t.gravity = Gravity.CENTER
            t.setPadding(dp(18), dp(12), dp(18), dp(12))
            t.background = ios.rounded(0xEB1C1C1E.toInt(), 16)
            t.elevation = dp(10).toFloat()
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            lp.topMargin = dp(48)
            lp.leftMargin = dp(24)
            lp.rightMargin = dp(24)
            t.alpha = 0f
            t.translationY = -dp(20).toFloat()
            root.addView(t, lp)
            t.animate().alpha(1f).translationY(0f).setDuration(200).start()
            ui.postDelayed({
                t.animate().alpha(0f).setDuration(250).withEndAction { root.removeView(t) }.start()
            }, 3200)
        }
    }

    // ফোনের লোকেল প্রোফাইলের ভাষা অনুযায়ী (Accept-Language-এর জন্য চেষ্টা)
    private fun applyLocale() {
        try {
            val tags = (store.get()["langs"] ?: "en-US").split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) {
                LocaleList.setDefault(LocaleList.forLanguageTags(tags.joinToString(",")))
            }
        } catch (e: Exception) {
        }
    }

    fun applyProfile() {
        web.settings.userAgentString = store.get()["ua"]
        applyLocale()
    }

    fun goHome() {
        web.loadUrl(homeUrl)
    }

    // =====================================================================
    //  UI: পুরো স্ক্রিন WebView + ভাসমান আইকন
    // =====================================================================
    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.setPadding(0, 0, 0, ime)
            insets
        }

        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // AssistiveTouch ধাঁচের আইকন
        fab = TextView(this)
        val outer = ios.rounded(0xBF1C1C1E.toInt(), 16)
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setColor(0x40FFFFFF)
        val inner = GradientDrawable()
        inner.shape = GradientDrawable.OVAL
        inner.setColor(0xF2FFFFFF.toInt())
        val ld = LayerDrawable(arrayOf<Drawable>(outer, ring, inner))
        ld.setLayerInset(1, dp(8), dp(8), dp(8), dp(8))
        ld.setLayerInset(2, dp(14), dp(14), dp(14), dp(14))
        fab.background = ld
        fab.elevation = dp(8).toFloat()
        fab.alpha = 0.92f
        root.addView(fab, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.TOP or Gravity.START))
        setContentView(root)

        root.post {
            fab.x = (root.width - dp(58)).toFloat()
            fab.y = root.height * 0.55f
        }
        setupFab()
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
                toast("আইকন লুকানো হয়েছে। অ্যাপ আবার খুললে ফিরে আসবে।")
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
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
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
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    if (moved) {
                        // কিনারায় লেগে যায়
                        val tx = if (v.x + v.width / 2f < root.width / 2f) dp(6).toFloat()
                        else (root.width - v.width - dp(6)).toFloat()
                        v.animate().x(tx).setDuration(200).start()
                    } else if (!longDone) {
                        panel.open()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    ui.removeCallbacks(longRun)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
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
        s.userAgentString = store.get()["ua"]
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.addJavascriptInterface(GhostBridge(this, store), "GhostBridge")
        web.addJavascriptInterface(dl, "GhostDl")
        web.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            handleDownload(url, userAgent, contentDisposition, mimetype)
        }
        docStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (docStart) {
            WebViewCompat.addDocumentStartJavaScript(web, spoofJs, setOf("*"))
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                pageUrl = url ?: ""
                if (!docStart) view?.evaluateJavascript(spoofJs, null)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val r = request ?: return null
                // অ্যাপের ভেতরের index.html
                val local = assetLoader.shouldInterceptRequest(r.url)
                if (local != null) return local
                // "আমার IP কী?" সার্ভিসে অনুরোধ গেলে জানাও
                if (!r.isForMainFrame) {
                    val host = r.url.host
                    if (host != null && IpHosts.matches(host, r.url.path ?: "")) {
                        ipWatch.report(originOf(pageUrl) ?: pageUrl, host)
                    }
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val rq = request ?: return false
                val u = rq.url.toString()
                val ok = u.startsWith("http://") || u.startsWith("https://") ||
                    u.startsWith("about:") || u.startsWith("data:") || u.startsWith("blob:")
                if (!ok) return true
                if (store.preAsk && rq.isForMainFrame && rq.method == "GET") {
                    val o = originOf(u)
                    if (o != null && o !in approved && !store.hasRules(o)) {
                        panel.siteModeDialog(o) { view?.loadUrl(u) }
                        return true
                    }
                }
                return false
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            // JS-এর লোকেশন আগেই আমাদের পপআপে অনুমোদিত; Android-এর অনুমতি পেলে তবেই দেওয়া হয়
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                withRuntime(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                ) { granted -> callback?.invoke(origin, granted, false) }
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
                        withRuntime(runtime.toTypedArray()) { granted ->
                            if (granted) r.grant(wants.toTypedArray()) else r.deny()
                        }
                    } else {
                        r.deny()
                    }
                }
            }
        }
    }

    // =====================================================================
    //  Android অনুমতি
    // =====================================================================
    private fun withRuntime(perms: Array<String>, cb: (Boolean) -> Unit) {
        val need = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isEmpty()) {
            cb(true)
            return
        }
        val code = reqCode++
        pendingPerm[code] = cb
        requestPermissions(need.toTypedArray(), code)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val cb = pendingPerm.remove(requestCode) ?: return
        val any = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        val all = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        val isLocation = permissions.any { it == Manifest.permission.ACCESS_FINE_LOCATION }
        cb(if (isLocation) any else all)
    }

    // =====================================================================
    //  ডাউনলোড (সাধারণ লিংক, data: ও blob:)
    // =====================================================================
    private fun handleDownload(url: String, ua: String?, cd: String?, mime: String?) {
        try {
            when {
                url.startsWith("blob:") -> {
                    val id = dl.expect()
                    val fb = URLUtil.guessFileName(url, cd, mime)
                    val js = "(function(u,id,fb){var n=(window.__ghn&&window.__ghn[u])||fb;" +
                        "fetch(u).then(function(r){return r.blob()}).then(function(b){" +
                        "var rd=new FileReader();" +
                        "rd.onload=function(){var s=String(rd.result);GhostDl.save(id,n,b.type,s.substring(s.indexOf(',')+1))};" +
                        "rd.onerror=function(){GhostDl.fail(id,'read')};" +
                        "rd.readAsDataURL(b)}).catch(function(e){GhostDl.fail(id,String(e))})})(" +
                        JSONObject.quote(url) + "," + JSONObject.quote(id) + "," + JSONObject.quote(fb) + ")"
                    web.evaluateJavascript(js, null)
                    toast("ডাউনলোড হচ্ছে…")
                }
                url.startsWith("data:") -> {
                    val p = Downloads.fromDataUrl(url)
                    if (p == null) {
                        toast("ডাউনলোড ব্যর্থ")
                    } else {
                        val name = URLUtil.guessFileName("https://x/download", cd, p.first)
                        val where = Downloads.saveBytes(this, name, p.first, p.second)
                        toast(if (where != null) "ডাউনলোড হয়েছে: $where" else "ফাইল সেভ করা যায়নি")
                    }
                }
                else -> {
                    val n = Downloads.startHttp(this, url, ua, cd, mime)
                    toast("ডাউনলোড শুরু: $n")
                }
            }
        } catch (e: Exception) {
            toast("ডাউনলোড ব্যর্থ: " + (e.message ?: ""))
        }
    }

    // =====================================================================
    //  ঠিকানা খোলা
    // =====================================================================
    fun originOf(url: String): String? {
        try {
            val u = Uri.parse(url)
            val sc = u.scheme ?: return null
            if (sc != "http" && sc != "https") return null
            val h = u.host ?: return null
            val port = u.port
            val def = if (sc == "https") 443 else 80
            return if (port == -1 || port == def) "$sc://$h" else "$sc://$h:$port"
        } catch (e: Exception) {
            return null
        }
    }

    fun markApproved(origin: String) {
        approved.add(origin)
    }

    fun go(input: String) {
        val t = input.trim()
        if (t.isEmpty()) return
        val url = when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(" ") || !t.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(t)
            else -> "https://$t"
        }
        openUrl(url)
    }

    fun openUrl(url: String) {
        val o = originOf(url)
        if (store.preAsk && o != null && o !in approved && !store.hasRules(o)) {
            panel.siteModeDialog(o) { web.loadUrl(url) }
        } else {
            web.loadUrl(url)
        }
    }

    // =====================================================================
    //  অনুমতির সিদ্ধান্ত (UI থ্রেডে)
    // =====================================================================
    fun dLabel(d: String): String = when (d) {
        "real" -> "আসল"
        "fake" -> "নকল"
        "block" -> "ব্লক"
        "ask" -> "প্রতিবার জিজ্ঞেস"
        else -> d
    }

    fun logLine(origin: String, title: String, detail: String, result: String): String {
        val t = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        return "$t  $origin\n$title: $detail  →  $result"
    }

    fun decide(origin: String, group: String, detail: String, cb: (String) -> Unit) {
        val title = Fields.groupTitles[group] ?: group
        // WebRTC ব্যবহার মানেই IP জানার চেষ্টা — সিদ্ধান্ত যাই হোক জানাও
        if (group == "webrtc") ipWatch.report(origin, "WebRTC")
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
        panel.askDialog(origin, group, detail) { dec, remember ->
            if (remember) store.setRule(origin, group, dec)
            GhostLog.add(logLine(origin, title, detail, dLabel(dec)))
            cb(dec)
        }
    }
}
