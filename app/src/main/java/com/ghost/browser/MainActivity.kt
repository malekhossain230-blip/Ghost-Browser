package com.ghost.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
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
import android.os.Message
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import android.widget.LinearLayout
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

    // Floating quick menu (Home / Back / Screenshot / Settings)
    private var quickMenu: LinearLayout? = null

    @Volatile
    private var pageUrl = ""

    // File upload
    private val REQ_FILE = 501
    private var fileCb: ValueCallback<Array<Uri>>? = null

    // Fullscreen video
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private var fabWasVisible = true

    // App lock
    private val REQ_LOCK = 502
    private var leftAt = 0L
    private var skipLockOnce = false
    private var unlocking = false

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
        devTools = DevTools(this, ios)
        ipWatch = IpWatch(this, store)
        dl = GhostDl(this)
        spoofJs = assets.open("spoof.js").bufferedReader().use { it.readText() }
        applyLocale()

        // Keep the status bar and the 3 navigation buttons visible at all times.
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
            toast("This phone's WebView is outdated. Please update Android System WebView.")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            withRuntime(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { }
        }
        web.loadUrl(homeUrl)
    }

    override fun onStart() {
        super.onStart()
        // The floating icon returns when the app is reopened.
        if (::fab.isInitialized && customView == null) fab.visibility = View.VISIBLE
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

    // ---- App lock ----
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

    // ---- Apply options ----
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
                if (store.proxy.isNotEmpty()) toast("This phone's WebView does not support a proxy")
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
            toast("Invalid proxy address")
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
        if (quickMenu != null) {
            closeQuickMenu()
        } else if (customView != null) {
            hideCustomView()
        } else if (web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
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

    // Fullscreen video: hide the status/navigation bars only while a video is playing.
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
        closeQuickMenu(true)
        customView = view
        customCb = cb
        view.setBackgroundColor(Color.BLACK)
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        fabWasVisible = fab.visibility == View.VISIBLE
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
        if (fabWasVisible) fab.visibility = View.VISIBLE
        setFullscreen(false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Height of the status bar, so banners sit just below it.
    private fun statusBarTop(): Int {
        val insets = ViewCompat.getRootWindowInsets(root)
        val top = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        if (top > 0) return top
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    // Short message (iOS-style banner, shown at the top).
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
            t.elevation = dp(16).toFloat()
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            lp.topMargin = statusBarTop() + dp(10)
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

    // Try to match Accept-Language to the profile's languages.
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
    //  UI: full-screen WebView + floating icon
    // =====================================================================
    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // AssistiveTouch-style icon
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
                closeQuickMenu(true)
                fab.visibility = View.GONE
                toast("Icon hidden. It will return when you reopen the app.")
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
                        if (!moved) closeQuickMenu(true)
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
                        // Snap to the nearest edge.
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

    // =====================================================================
    //  Floating quick menu
    // =====================================================================
    private fun toggleQuickMenu() {
        if (quickMenu != null) {
            closeQuickMenu()
            return
        }
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(6), dp(6), dp(6), dp(6))
        bar.background = ios.rounded(0xF21C1C1E.toInt(), 27)
        bar.elevation = dp(12).toFloat()
        bar.addView(menuIcon("home") { goHome(); closeQuickMenu() })
        bar.addView(menuIcon("back") {
            closeQuickMenu()
            if (web.canGoBack()) web.goBack() else toast("No page to go back to")
        })
        bar.addView(menuIcon("camera") {
            closeQuickMenu(true)
            takeScreenshot()
        })
        bar.addView(menuIcon("settings") {
            closeQuickMenu()
            panel.open()
        })
        root.addView(bar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        quickMenu = bar
        bar.alpha = 0f
        bar.post {
            positionQuickMenu(bar)
            bar.animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun menuIcon(name: String, onClick: () -> Unit): ImageView {
        val iv = ImageView(this)
        iv.setImageDrawable(Icons.drawable(name, Color.WHITE, 2f))
        val lp = LinearLayout.LayoutParams(dp(42), dp(42))
        lp.marginStart = dp(3)
        lp.marginEnd = dp(3)
        iv.layoutParams = lp
        iv.setPadding(dp(10), dp(10), dp(10), dp(10))
        iv.isClickable = true
        iv.setOnClickListener {
            it.animate().scaleX(0.82f).scaleY(0.82f).setDuration(70).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }.start()
            onClick()
        }
        return iv
    }

    private fun positionQuickMenu(bar: View) {
        val bw = bar.width
        val bh = bar.height
        val fabCx = fab.x + fab.width / 2f
        val onRight = fabCx > root.width / 2f
        var x = if (onRight) fab.x - bw - dp(8) else fab.x + fab.width + dp(8)
        val minX = dp(6).toFloat()
        val maxX = Math.max(minX, (root.width - bw - dp(6)).toFloat())
        x = x.coerceIn(minX, maxX)
        val minY = (statusBarTop() + dp(6)).toFloat()
        val maxY = Math.max(minY, (root.height - bh - dp(6)).toFloat())
        var y = fab.y + fab.height / 2f - bh / 2f
        y = y.coerceIn(minY, maxY)
        bar.x = x
        bar.y = y
    }

    private fun closeQuickMenu(instant: Boolean = false) {
        val bar = quickMenu ?: return
        quickMenu = null
        if (instant) {
            root.removeView(bar)
        } else {
            bar.animate().alpha(0f).setDuration(120).withEndAction { root.removeView(bar) }.start()
        }
    }

    // =====================================================================
    //  Screenshot
    // =====================================================================
    private fun takeScreenshot() {
        val shoot = {
            Screenshot.take(this, root, listOf(fab)) { ok ->
                toast(if (ok) "Screenshot saved to Gallery" else "Could not take screenshot")
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            withRuntime(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)) { granted ->
                if (granted) shoot() else toast("Storage permission is needed to save screenshots")
            }
        } else {
            shoot()
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
                // The app's own index.html
                val local = assetLoader.shouldInterceptRequest(r.url)
                if (local != null) return local

                val host = r.url.host ?: ""
                val path = r.url.path ?: ""
                Stats.req.incrementAndGet()
                var tag = ""
                var block = false
                if (!r.isForMainFrame) {
                    if (host.isNotEmpty() && IpHosts.matches(host, path)) {
                        // Request to a "what is my IP?" service: report it, block it if enabled.
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
            // For the DevTools console
            override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                val cm = m ?: return false
                val src = (cm.sourceId() ?: "").substringAfterLast('/')
                ConsoleLog.add(cm.messageLevel().name, cm.message() + (if (src.isNotEmpty()) "   ($src:${cm.lineNumber()})" else ""))
                return true
            }

            // File upload (<input type=file>)
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

            // Fullscreen video
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

            // target=_blank and window.open: open in the same tab; block popups the user didn't trigger (ad popups).
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

            // JS location is pre-approved in our popup; it is granted only once Android's permission is given too.
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

    // =====================================================================
    //  Android permissions
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
    //  Downloads (normal links, data: and blob:)
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

    // =====================================================================
    //  Opening addresses
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
    //  Permission decision (on the UI thread)
    // =====================================================================
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
        // Using WebRTC means an attempt to learn the IP — report it whatever the decision.
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
