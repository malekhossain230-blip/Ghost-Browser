package com.ghost.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.LocaleList
import android.os.Message
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class MainActivity : Activity() {

    lateinit var store: Store
    lateinit var web: WebView
    lateinit var ios: Ios
    lateinit var panel: Panel
    lateinit var devTools: DevTools
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

    private var quickOverlay: FrameLayout? = null

    private var volUp = false
    private var volDn = false
    private var comboArmed = false

    @Volatile
    private var pageUrl = ""

    private val REQ_FILE = 501
    private var fileCb: ValueCallback<Array<Uri>>? = null

    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null

    private val REQ_LOCK = 502
    private var leftAt = 0L
    private var skipLockOnce = false
    private var unlocking = false

    private val homeOrigin = "https://appassets.androidplatform.net"
    private val homeUrl = "https://appassets.androidplatform.net/assets/index.html"

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val prefs: SharedPreferences by lazy { getSharedPreferences("ghost", Context.MODE_PRIVATE) }

    private var fabHidden: Boolean
        get() = prefs.getBoolean("fab_hidden", false)
        set(v) {
            prefs.edit().putBoolean("fab_hidden", v).apply()
        }

    private val unhideRun = Runnable {
        comboArmed = false
        if (fabHidden) {
            fabHidden = false
            if (::fab.isInitialized) fab.visibility = View.VISIBLE
            toast("Floating button restored")
        }
    }

    private val assetLoader: WebViewAssetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        ios = Ios(this)
        panel = Panel(this, store, ios)
        devTools = DevTools(this, ios)
        ipWatch = IpWatch(this, store)
        dl = GhostDl(this)
        spoofJs = assets.open("spoof.js").bufferedReader().use { it.readText() }
        applyLocale()

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        val wic = WindowCompat.getInsetsController(window, window.decorView)
        wic?.isAppearanceLightStatusBars = true
        wic?.isAppearanceLightNavigationBars = true

        if (savedInstanceState == null) {
            if (store.randomNoise) {
                val m = LinkedHashMap<String, String>(store.get())
                m["canvasNoise"] = (1 + java.util.Random().nextInt(99998)).toString()
                store.save(store.active, m)
            }
        }

        buildUi()
        setupWebView()
        applyOptions()
        applyProxy()
        if (savedInstanceState == null && store.clearOnExit) clearAllData()
        if (store.appLock && canLock()) requestUnlock()

        approved.add(homeOrigin)
        if (!docStart) {
            toast("WebView is outdated — please update it")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            withRuntime(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { }
        }
        web.loadUrl(homeUrl)
    }

    override fun onStart() {
        super.onStart()
        if (::fab.isInitialized && customView == null) {
            fab.visibility = if (fabHidden) View.GONE else View.VISIBLE
        }
        if (store.appLock && !unlocking && leftAt > 0L && !skipLockOnce && System.currentTimeMillis() - leftAt > 15000 && canLock()) {
            requestUnlock()
        }
        skipLockOnce = false
    }

    override fun onStop() {
        super.onStop()
        leftAt = System.currentTimeMillis()
    }

    override fun onDestroy() {
        if (isFinishing && store.clearOnExit) clearAllData()
        super.onDestroy()
    }

    fun canLock(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    private fun requestUnlock() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val i = km.createConfirmDeviceCredentialIntent("Ghost Browser", "Unlock to continue")
        if (i == null) return
        root.visibility = View.INVISIBLE
        unlocking = true
        try {
            startActivityForResult(i, REQ_LOCK)
        } catch (e: Exception) {
            unlocking = false
            root.visibility = View.VISIBLE
        }
    }

    fun applyOptions() {
        if (store.screenSecure) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        web.settings.loadsImagesAutomatically = !store.blockImages
        web.settings.textZoom = store.textZoom
    }

    fun applyProxy() {
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                return
            }
            val direct = Executor { r -> r.run() }
            if (store.proxy.isEmpty()) {
                ProxyController.getInstance().clearProxyOverride(direct) { }
            } else {
                val cfg = ProxyConfig.Builder().addProxyRule(store.proxy).build()
                ProxyController.getInstance().setProxyOverride(cfg, direct) { }
            }
        } catch (e: Exception) {
        }
    }

    fun clearAllData() {
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            web.clearCache(true)
            web.clearHistory()
        } catch (e: Exception) {
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (quickOverlay != null) {
            closeQuickMenu()
        } else if (customView != null) {
            hideCustomView()
        } else if (web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) volUp = true
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) volDn = true
        if (volUp && volDn && fabHidden && !comboArmed) {
            comboArmed = true
            ui.postDelayed(unhideRun, 2000)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) volUp = false
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) volDn = false
        if (!(volUp && volDn)) {
            comboArmed = false
            ui.removeCallbacks(unhideRun)
        }
        return super.onKeyUp(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_LOCK) {
            unlocking = false
            leftAt = System.currentTimeMillis()
            if (resultCode == RESULT_OK) {
                root.visibility = View.VISIBLE
            } else {
                finish()
            }
            return
        }
        if (requestCode == REQ_FILE) {
            val cb = fileCb
            fileCb = null
            cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
        }
    }

    private fun setFullscreen(on: Boolean) {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        if (c == null) return
        if (on) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showCustomView(view: View, cb: WebChromeClient.CustomViewCallback?) {
        if (customView != null) {
            cb?.onCustomViewHidden()
            return
        }
        closeQuickMenu()
        customView = view
        customCb = cb
        view.setBackgroundColor(Color.BLACK)
        root.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
        fab.visibility = View.GONE
        setFullscreen(true)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun hideCustomView() {
        val v = customView ?: return
        root.removeView(v)
        customView = null
        val cb = customCb
        customCb = null
        cb?.onCustomViewHidden()
        fab.visibility = if (fabHidden) View.GONE else View.VISIBLE
        setFullscreen(false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun statusBarTop(): Int {
        val insets = ViewCompat.getRootWindowInsets(root)
        val top = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        if (top > 0) return top
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    fun toast(m: String) {
        runOnUiThread {
            if (!::root.isInitialized) return@runOnUiThread
            val t = TextView(this)
            t.text = m
            t.textSize = 14f
            t.setTextColor(Color.WHITE)
            t.gravity = Gravity.CENTER
            t.setSingleLine(true)
            t.maxLines = 1
            t.ellipsize = android.text.TextUtils.TruncateAt.END
            t.setPadding(dp(18), dp(11), dp(18), dp(11))
            t.background = ios.rounded(0xF22C2C2E.toInt(), 20)
            t.elevation = dp(16).toFloat()
            val lp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            lp.topMargin = statusBarTop() + dp(10)
            lp.leftMargin = dp(16)
            lp.rightMargin = dp(16)
            t.alpha = 0f
            t.translationY = -dp(20).toFloat()
            root.addView(t, lp)
            t.animate().alpha(1f).translationY(0f).setDuration(200).start()
            ui.postDelayed({
                t.animate().alpha(0f).setDuration(250).withEndAction { root.removeView(t) }.start()
            }, 3000)
        }
    }

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

    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(MATCH, MATCH))

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

        fab.visibility = if (fabHidden) View.GONE else View.VISIBLE
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
                closeQuickMenu()
                fabHidden = true
                fab.visibility = View.GONE
                toast("Button hidden · Vol +&- together to show")
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
                        val tx = if (v.x + v.width / 2f < root.width / 2f) dp(6).toFloat()
                        else (root.width - v.width - dp(6)).toFloat()
                        v.animate().x(tx).setDuration(200).start()
                    } else if (!longDone) {
                        toggleQuickMenu()
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

    private fun toggleQuickMenu() {
        if (quickOverlay != null) closeQuickMenu() else openQuickMenu()
    }

    private fun setContentBlur(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                web.setRenderEffect(
                    if (on) RenderEffect.createBlurEffect(dp(20).toFloat(), dp(20).toFloat(), Shader.TileMode.CLAMP)
                    else null
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun centerDot(): View {
        val v = View(this)
        val outer = GradientDrawable()
        outer.shape = GradientDrawable.OVAL
        outer.setColor(0xE62C2C2E.toInt())
        val dot = GradientDrawable()
        dot.shape = GradientDrawable.OVAL
        dot.setColor(0xF2FFFFFF.toInt())
        val ld = LayerDrawable(arrayOf<Drawable>(outer, dot))
        ld.setLayerInset(1, dp(19), dp(19), dp(19), dp(19))
        v.background = ld
        v.elevation = dp(10).toFloat()
        return v
    }

    private fun menuButton(icon: String, action: () -> Unit): ImageView {
        val iv = ImageView(this)
        iv.setImageDrawable(Icons.drawable(icon, Color.WHITE, 2f))
        iv.setPadding(dp(12), dp(12), dp(12), dp(12))
        iv.background = ios.rounded(0xEB2C2C2E.toInt(), 24)
        iv.elevation = dp(10).toFloat()
        iv.setOnClickListener { action() }
        return iv
    }

    private fun openQuickMenu() {
        val cx = fab.x + fab.width / 2f
        val cy = fab.y + fab.height / 2f

        val overlay = FrameLayout(this)
        val scrim = View(this)
        scrim.setBackgroundColor(0x4D000000)
        scrim.alpha = 0f
        scrim.setOnClickListener { closeQuickMenu() }
        overlay.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))

        val dot = centerDot()
        overlay.addView(dot, FrameLayout.LayoutParams(dp(52), dp(52)))
        dot.x = cx - dp(26)
        dot.y = cy - dp(26)
        dot.setOnClickListener { closeQuickMenu() }

        val items = listOf<Pair<String, () -> Unit>>(
            "home" to { goHome(); closeQuickMenu() },
            "back" to {
                closeQuickMenu()
                if (web.canGoBack()) web.goBack() else toast("No page to go back")
            },
            "refresh" to { closeQuickMenu(); web.reload() },
            "settings" to { closeQuickMenu(); panel.open() }
        )

        val onRight = cx > root.width / 2f
        val base = if (onRight) 180.0 else 0.0
        val spread = 120.0
        val radius = dp(104).toFloat()
        val half = dp(24).toFloat()
        val minX = dp(6).toFloat()
        val maxX = (root.width - dp(54)).toFloat()
        val minY = statusBarTop() + dp(6).toFloat()
        val maxY = (root.height - dp(54)).toFloat()

        for ((i, item) in items.withIndex()) {
            val frac = i.toDouble() / (items.size - 1)
            val ang = Math.toRadians(base - spread / 2 + spread * frac)
            val targetX = (cx + radius * Math.cos(ang) - half).toFloat().coerceIn(minX, maxX)
            val targetY = (cy - radius * Math.sin(ang) - half).toFloat().coerceIn(minY, maxY)
            val btn = menuButton(item.first, item.second)
            overlay.addView(btn, FrameLayout.LayoutParams(dp(48), dp(48)))
            btn.x = cx - half
            btn.y = cy - half
            btn.alpha = 0f
            btn.scaleX = 0.6f
            btn.scaleY = 0.6f
            btn.animate().x(targetX).y(targetY).alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay((i * 30).toLong()).setDuration(220)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        root.addView(overlay, FrameLayout.LayoutParams(MATCH, MATCH))
        quickOverlay = overlay
        fab.visibility = View.INVISIBLE
        setContentBlur(true)
        scrim.animate().alpha(1f).setDuration(180).start()
    }

    private fun closeQuickMenu() {
        val overlay = quickOverlay ?: return
        quickOverlay = null
        setContentBlur(false)
        if (!fabHidden && customView == null) fab.visibility = View.VISIBLE
        overlay.animate().alpha(0f).setDuration(150).withEndAction { root.removeView(overlay) }.start()
    }

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
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
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

            override fun onPageFinished(view: WebView?, url: String?) {
                if (store.adBlock) view?.evaluateJavascript(Blocker.cosmeticJs, null)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val r = request ?: return null
                val local = assetLoader.shouldInterceptRequest(r.url)
                if (local != null) return local

                val host = r.url.host ?: ""
                val path = r.url.path ?: ""
                Stats.req.incrementAndGet()
                var tag = ""
                var block = false
                if (!r.isForMainFrame) {
                    if (host.isNotEmpty() && IpHosts.matches(host, path)) {
                        if (store.ipBlock) {
                            block = true
                            tag = "IP lookup (blocked)"
                            Stats.ip.incrementAndGet()
                        } else {
                            tag = "IP lookup"
                        }
                        ipWatch.report(originOf(pageUrl) ?: pageUrl, host, block)
                    } else if (store.adBlock && Blocker.matches(host, path)) {
                        block = true
                        tag = "Ad / tracker"
                        Stats.ads.incrementAndGet()
                    }
                }
                NetLog.add(
                    NetEntry(
                        System.currentTimeMillis(), r.method ?: "GET", r.url.toString(),
                        r.isForMainFrame, tag, block, r.requestHeaders ?: emptyMap()
                    )
                )
                if (block) {
                    return WebResourceResponse(
                        "text/plain", "UTF-8", 403, "Blocked",
                        HashMap<String, String>(), ByteArrayInputStream(ByteArray(0))
                    )
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
            override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                val cm = m ?: return false
                val src = (cm.sourceId() ?: "").substringAfterLast('/')
                ConsoleLog.add(cm.messageLevel().name, cm.message() + (if (src.isNotEmpty()) "   ($src:${cm.lineNumber()})" else ""))
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                val cb = filePathCallback ?: return false
                val params = fileChooserParams ?: return false
                fileCb?.onReceiveValue(null)
                fileCb = cb
                skipLockOnce = true
                return try {
                    startActivityForResult(params.createIntent(), REQ_FILE)
                    true
                } catch (e: Exception) {
                    fileCb = null
                    cb.onReceiveValue(null)
                    toast("Could not open the file picker")
                    false
                }
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (view == null) {
                    callback?.onCustomViewHidden()
                    return
                }
                showCustomView(view, callback)
            }

            override fun onHideCustomView() {
                hideCustomView()
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val msg = resultMsg ?: return false
                if (!isUserGesture && store.adBlock) return false
                val transport = msg.obj as? WebView.WebViewTransport ?: return false
                val tmp = WebView(this@MainActivity)
                var handled = false
                fun take(u: String?) {
                    if (handled || u == null) return
                    if (!(u.startsWith("http://") || u.startsWith("https://"))) return
                    handled = true
                    ui.post {
                        openUrl(u)
                        tmp.stopLoading()
                        tmp.destroy()
                    }
                }
                tmp.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                        take(request?.url?.toString())
                        return true
                    }

                    override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                        take(url)
                    }
                }
                transport.webView = tmp
                msg.sendToTarget()
                return true
            }

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
                        names.add("Camera")
                    } else if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                        wants.add(res)
                        runtime.add(Manifest.permission.RECORD_AUDIO)
                        names.add("Microphone")
                    }
                }
                if (wants.isEmpty()) {
                    r.deny()
                    return
                }
                decide(r.origin.toString(), "media", "Wants to use: " + names.joinToString(", ")) { d ->
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
                    toast("Downloading…")
                }
                url.startsWith("data:") -> {
                    val p = Downloads.fromDataUrl(url)
                    if (p == null) {
                        toast("Download failed")
                    } else {
                        val name = URLUtil.guessFileName("https://x/download", cd, p.first)
                        val where = Downloads.saveBytes(this, name, p.first, p.second)
                        toast(if (where != null) "Downloaded: $where" else "Could not save the file")
                    }
                }
                else -> {
                    val n = Downloads.startHttp(this, url, ua, cd, mime)
                    toast("Download started: $n")
                }
            }
        } catch (e: Exception) {
            toast("Download failed: " + (e.message ?: ""))
        }
    }

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

    fun dLabel(d: String): String = when (d) {
        "real" -> "Real"
        "fake" -> "Fake"
        "block" -> "Block"
        "ask" -> "Ask every time"
        else -> d
    }

    fun logLine(origin: String, title: String, detail: String, result: String): String {
        val t = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        return "$t  $origin\n$title: $detail  →  $result"
    }

    fun decide(origin: String, group: String, detail: String, cb: (String) -> Unit) {
        val title = Fields.groupTitles[group] ?: group
        if (group == "webrtc") ipWatch.report(origin, "WebRTC")
        val rule = store.rule(origin, group)
        if (rule != null) {
            GhostLog.add(logLine(origin, title, detail, dLabel(rule) + " (saved rule)"))
            cb(rule)
            return
        }
        val m = store.mode(group)
        if (m != "ask") {
            GhostLog.add(logLine(origin, title, detail, dLabel(m) + " (default mode)"))
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
