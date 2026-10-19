package com.ghost.browser

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
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
    lateinit var homeStore: HomeStore
    lateinit var web: WebView
    lateinit var ios: Ios
    lateinit var panel: Panel
    lateinit var devTools: DevTools
    lateinit var home: HomeView
    private lateinit var offline: OfflineView
    private lateinit var ipWatch: IpWatch
    private lateinit var dl: GhostDl
    private lateinit var fab: TextView
    private lateinit var root: FrameLayout
    private lateinit var webWrap: FrameLayout
    private lateinit var progressBar: View
    private var settingsPage: View? = null
    private var settingsClose: (() -> Unit)? = null
    private val ui = Handler(Looper.getMainLooper())
    private var spoofJs = ""
    var docStart = false
    private val approved = HashSet<String>()
    private var reqCode = 100
    private val pendingPerm = HashMap<Int, (Boolean) -> Unit>()
    private var blocked = false

    private var quickOverlay: FrameLayout? = null
    private var barColor = Color.WHITE
    private var barAnim: ValueAnimator? = null
    private var themeTick: Runnable? = null

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

    private val BLANK = "about:blank"
    var homeShown = false
        private set
    private var failedUrl = ""
    private var navError = false

    private var insetTop = 0
    private var insetBottom = 0
    private var imeBottom = 0

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun attachBaseContext(newBase: Context) {
        val mode = HomeStore.themeMode(newBase)
        if (mode == "system") {
            super.attachBaseContext(newBase)
            return
        }
        val cfg = Configuration(newBase.resources.configuration)
        val night = if (mode == "dark") Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        super.attachBaseContext(newBase.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Integrity.verify(this)) {
            blocked = true
            setContentView(Integrity.blockScreen(this))
            return
        }
        store = Store(this)
        homeStore = HomeStore(this)
        ios = Ios(this)
        panel = Panel(this, store, ios)
        devTools = DevTools(this, ios)
        ipWatch = IpWatch(this, store)
        dl = GhostDl(this)
        spoofJs = assets.open("spoof.js").bufferedReader().use { it.readText() }
        applyLocale()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        barColor = ios.BG
        setBarIcons(ios.BG)

        if (savedInstanceState == null) {
            if (store.randomNoise) {
                val m = LinkedHashMap<String, String>(store.get())
                m["canvasNoise"] = (1 + java.util.Random().nextInt(99998)).toString()
                store.save(store.active, m)
            }
        }

        Blocker.init(this)
        buildUi()
        setupWebView()
        applyOptions()
        applyProxy()
        if (savedInstanceState == null && store.clearOnExit) clearAllData()
        if (store.appLock && canLock()) requestUnlock()

        if (!docStart) {
            toast("WebView is outdated - please update it")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            withRuntime(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { }
        }
        showHome()
    }

    override fun onStart() {
        super.onStart()
        if (blocked) return
        startThemeWatch()
        updateFab()
        if (store.appLock && !unlocking && leftAt > 0L && !skipLockOnce && System.currentTimeMillis() - leftAt > 15000 && canLock()) {
            requestUnlock()
        }
        skipLockOnce = false
    }

    override fun onStop() {
        super.onStop()
        if (blocked) return
        stopThemeWatch()
        leftAt = System.currentTimeMillis()
    }

    override fun onDestroy() {
        if (!blocked && isFinishing && store.clearOnExit) clearAllData()
        super.onDestroy()
    }

    fun canLock(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    private fun requestUnlock() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val i = km.createConfirmDeviceCredentialIntent("Vaelo", "Unlock to continue")
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

    fun applyTheme() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        }
        recreate()
    }

    private fun backTargetIsBlank(): Boolean {
        val list = web.copyBackForwardList()
        val i = list.currentIndex - 1
        if (i < 0) return false
        return list.getItemAtIndex(i).url == BLANK
    }

    fun goBackOrHome() {
        if (offline.showing) {
            offline.hide()
            navError = false
        }
        if (web.canGoBack() && !backTargetIsBlank()) web.goBack() else showHome()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (blocked) {
            super.onBackPressed()
            return
        }
        if (settingsPage != null) {
            closeSettingsPage()
        } else if (quickOverlay != null) {
            closeQuickMenu()
        } else if (customView != null) {
            hideCustomView()
        } else if (homeShown) {
            if (!home.onBack()) super.onBackPressed()
        } else {
            goBackOrHome()
        }
    }

    fun retry() {
        if (homeShown) return
        offline.setBusy(true)
        web.reload()
    }

    fun showHome() {
        closeQuickMenu()
        if (homeShown) return
        homeShown = true
        navError = false
        offline.hide()
        home.onShown()
        home.visibility = View.VISIBLE
        home.alpha = 0f
        home.animate().alpha(1f).setDuration(170).start()
        webWrap.visibility = View.INVISIBLE
        hideLoadProgress()
        applyBar(ios.BG)
        updateFab()
        try {
            web.stopLoading()
            if (web.url != BLANK) web.loadUrl(BLANK)
        } catch (e: Exception) {
        }
    }

    fun showWeb() {
        if (!homeShown) return
        homeShown = false
        home.closeOverlays()
        home.visibility = View.GONE
        webWrap.visibility = View.VISIBLE
        updateFab()
    }

    fun updateFab() {
        if (!::fab.isInitialized) return
        val show = customView == null && settingsPage == null && quickOverlay == null && (homeShown || homeStore.floatingButton)
        fab.visibility = if (show) View.VISIBLE else View.GONE
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (blocked) return
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
            return
        }
        if (requestCode == HomeSheets.REQ_ICON) {
            skipLockOnce = true
            if (resultCode == RESULT_OK) home.sheets.onIconPicked(data?.data)
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
        updateFab()
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
        updateFab()
        setFullscreen(false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun statusBarTop(): Int {
        if (insetTop > 0) return insetTop
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    fun navBarBottom(): Int = insetBottom

    private fun applyInsets() {
        val bottom = if (imeBottom > 0) 0 else insetBottom
        root.setPadding(0, 0, 0, imeBottom)
        webWrap.setPadding(0, insetTop, 0, bottom)
        home.setInsets(insetTop, bottom)
        offline.setInsets(insetTop, bottom)
        progressBar.y = insetTop.toFloat()
        if (::fab.isInitialized && fab.y < insetTop) fab.y = insetTop.toFloat()
    }

    private fun setLoadProgress(p: Int) {
        if (!::progressBar.isInitialized) return
        progressBar.y = insetTop.toFloat()
        if (p < 100) {
            progressBar.visibility = View.VISIBLE
            progressBar.alpha = 1f
            progressBar.animate().scaleX(p / 100f).setDuration(120).start()
        } else {
            progressBar.animate().scaleX(1f).setDuration(120).withEndAction {
                progressBar.animate().alpha(0f).setDuration(250).withEndAction {
                    progressBar.visibility = View.GONE
                    progressBar.scaleX = 0f
                }.start()
            }.start()
        }
    }

    private fun hideLoadProgress() {
        if (!::progressBar.isInitialized) return
        progressBar.animate().cancel()
        progressBar.visibility = View.GONE
        progressBar.alpha = 1f
        progressBar.scaleX = 0f
    }

    private fun chromeColor(): Int = ios.BG

    private fun resetBar() {
        applyBar(chromeColor())
    }

    private fun setBarIcons(color: Int) {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val lum = 0.299 * r + 0.587 * g + 0.114 * b
        val wic = WindowCompat.getInsetsController(window, window.decorView)
        wic?.isAppearanceLightStatusBars = lum > 0.6
        wic?.isAppearanceLightNavigationBars = lum > 0.6
    }

    private fun applyBar(color: Int) {
        if (color == barColor) return
        barAnim?.cancel()
        val from = barColor
        barColor = color
        val a = ValueAnimator.ofObject(ArgbEvaluator(), from, color)
        a.duration = 300
        a.addUpdateListener { v ->
            val c = v.animatedValue as Int
            if (::webWrap.isInitialized) webWrap.setBackgroundColor(c)
            setBarIcons(c)
        }
        barAnim = a
        a.start()
    }

    private fun startThemeWatch() {
        stopThemeWatch()
        val r = object : Runnable {
            override fun run() {
                if (::web.isInitialized && !homeShown && settingsPage == null && customView == null && !offline.showing) readThemeColor(web)
                ui.postDelayed(this, 1100)
            }
        }
        themeTick = r
        ui.postDelayed(r, 1100)
    }

    private fun stopThemeWatch() {
        val r = themeTick
        if (r != null) ui.removeCallbacks(r)
        themeTick = null
    }

    private fun unquoteJs(r: String?): String {
        if (r == null) return ""
        var s = r.trim()
        if (s == "null") return ""
        if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        }
        return s.replace("\\\"", "\"")
    }

    private fun parseCssColor(raw: String): Int? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        try {
            if (s.startsWith("#")) return Color.parseColor(s)
            if (s.startsWith("rgb")) {
                val inside = s.substringAfter('(').substringBefore(')')
                val parts = inside.split(",").map { it.trim() }
                if (parts.size < 3) return null
                val aa = if (parts.size >= 4) parts[3].toFloat() else 1f
                if (aa < 0.5f) return null
                return Color.rgb(parts[0].toFloat().toInt(), parts[1].toFloat().toInt(), parts[2].toFloat().toInt())
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun readThemeColor(view: WebView?) {
        if (homeShown) {
            applyBar(ios.BG)
            return
        }
        val js = "(function(){try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
            "if(m&&m.content)return m.content;" +
            "return getComputedStyle(document.body).backgroundColor}catch(e){return ''}})()"
        view?.evaluateJavascript(js) { r ->
            if (homeShown) return@evaluateJavascript
            val color = parseCssColor(unquoteJs(r))
            if (color == null) resetBar() else applyBar(color)
        }
    }

    fun toast(m: String) {
        runOnUiThread {
            if (!::root.isInitialized) return@runOnUiThread
            if (homeShown && ::home.isInitialized) {
                home.showToast(m)
                return@runOnUiThread
            }
            val t = TextView(this)
            t.text = m
            t.textSize = 13.5f
            t.setTextColor(ios.TOAST_FG)
            t.typeface = ios.BOLD
            t.gravity = Gravity.CENTER
            t.setSingleLine(true)
            t.maxLines = 1
            t.ellipsize = android.text.TextUtils.TruncateAt.END
            t.setPadding(dp(18), dp(11), dp(18), dp(11))
            t.background = ios.rounded(ios.TOAST_BG, 22)
            t.elevation = dp(16).toFloat()
            val lp = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            lp.topMargin = insetTop + dp(12)
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
        showHome()
    }

    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(ios.BG)

        webWrap = FrameLayout(this)
        webWrap.setBackgroundColor(ios.BG)
        web = WebView(this)
        webWrap.addView(web, FrameLayout.LayoutParams(MATCH, MATCH))
        offline = OfflineView(this, ios, { retry() }, { showHome() })
        webWrap.addView(offline, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(webWrap, FrameLayout.LayoutParams(MATCH, MATCH))

        home = HomeView(this, ios, homeStore)
        home.visibility = View.GONE
        root.addView(home, FrameLayout.LayoutParams(MATCH, MATCH))

        fab = TextView(this)
        val outer = ios.rounded(if (ios.dark) 0xD9262A33.toInt() else 0xD90F172A.toInt(), 16)
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

        progressBar = View(this)
        progressBar.background = ios.gradient(ios.ACC1, ios.ACC2, 0)
        progressBar.elevation = dp(20).toFloat()
        progressBar.pivotX = 0f
        progressBar.scaleX = 0f
        progressBar.visibility = View.GONE
        root.addView(progressBar, FrameLayout.LayoutParams(MATCH, dp(3), Gravity.TOP))

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val nb = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            insetTop = sb.top
            insetBottom = nb.bottom
            imeBottom = ime.bottom
            applyInsets()
            insets
        }

        root.post {
            fab.x = (root.width - dp(58)).toFloat()
            fab.y = Math.max(statusBarTop().toFloat(), root.height * 0.55f)
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
        fab.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - e.rawX
                    dY = v.y - e.rawY
                    downX = e.rawX
                    downY = e.rawY
                    moved = false
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawX - downX) > dp(8) || Math.abs(e.rawY - downY) > dp(8)) {
                        moved = true
                    }
                    if (moved) {
                        val maxX = Math.max(0f, (root.width - v.width).toFloat())
                        val minY = statusBarTop().toFloat()
                        val maxY = Math.max(minY, (root.height - v.height - insetBottom - dp(16)).toFloat())
                        v.x = (e.rawX + dX).coerceIn(0f, maxX)
                        v.y = (e.rawY + dY).coerceIn(minY, maxY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    if (moved) {
                        val tx = if (v.x + v.width / 2f < root.width / 2f) dp(6).toFloat()
                        else (root.width - v.width - dp(6)).toFloat()
                        v.animate().x(tx).setDuration(200).start()
                    } else {
                        toggleQuickMenu()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
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
                val target: View = if (homeShown) home else webWrap
                target.setRenderEffect(
                    if (on) RenderEffect.createBlurEffect(dp(20).toFloat(), dp(20).toFloat(), Shader.TileMode.CLAMP)
                    else null
                )
                if (!on) {
                    home.setRenderEffect(null)
                    webWrap.setRenderEffect(null)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun centerDot(): View {
        val v = View(this)
        val outer = GradientDrawable()
        outer.shape = GradientDrawable.OVAL
        outer.setColor(if (ios.dark) 0xE6262A33.toInt() else 0xE60F172A.toInt())
        val dot = GradientDrawable()
        dot.shape = GradientDrawable.OVAL
        dot.setColor(0xF2FFFFFF.toInt())
        val ld = LayerDrawable(arrayOf<Drawable>(outer, dot))
        ld.setLayerInset(1, dp(19), dp(19), dp(19), dp(19))
        v.background = ld
        v.elevation = dp(10).toFloat()
        return v
    }

    private fun menuButton(icon: String, accent: Boolean, action: () -> Unit): ImageView {
        val iv = ImageView(this)
        iv.setImageDrawable(Icons.drawable(icon, Color.WHITE, 2f))
        iv.setPadding(dp(12), dp(12), dp(12), dp(12))
        iv.background = if (accent) ios.gradient(ios.ACC1, ios.ACC2, 24) else ios.rounded(if (ios.dark) 0xEB262A33.toInt() else 0xEB0F172A.toInt(), 24)
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
            "home" to { closeQuickMenu(); showHome() },
            "back" to {
                closeQuickMenu()
                if (homeShown) toast("You are on the home page") else goBackOrHome()
            },
            "refresh" to {
                closeQuickMenu()
                if (homeShown) home.onShown() else if (offline.showing) retry() else web.reload()
            },
            "settings" to { closeQuickMenu(); panel.open() }
        )

        val onRight = cx > root.width / 2f
        val edge = dp(150).toFloat()
        val nearTop = cy < statusBarTop() + edge
        val nearBottom = cy > root.height - edge
        val corner = nearTop || nearBottom
        val spread = if (corner) 90.0 else 120.0
        val base = if (corner) {
            if (onRight) {
                if (nearTop) 225.0 else 135.0
            } else {
                if (nearTop) -45.0 else 45.0
            }
        } else {
            if (onRight) 180.0 else 0.0
        }
        val gapNeed = (dp(48) * 1.22f * (items.size - 1)) / Math.toRadians(spread).toFloat()
        val wanted = Math.min(dp(112).toFloat(), Math.min(root.width, root.height) * 0.30f)
        val radius = Math.max(gapNeed, wanted)
        val half = dp(24).toFloat()

        for ((i, item) in items.withIndex()) {
            val frac = i.toDouble() / (items.size - 1)
            val ang = Math.toRadians(base - spread / 2 + spread * frac)
            val targetX = (cx + radius * Math.cos(ang) - half).toFloat()
            val targetY = (cy - radius * Math.sin(ang) - half).toFloat()
            val btn = menuButton(item.first, item.first == "settings", item.second)
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
        updateFab()
        overlay.animate().alpha(0f).setDuration(150).withEndAction { root.removeView(overlay) }.start()
    }

    fun openSettingsPage(title: String): PageHandle {
        closeSettingsPage()
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.background = ios.pageBg()
        page.elevation = dp(12).toFloat()
        page.isClickable = true

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(16), statusBarTop() + dp(10), dp(12), dp(10))
        val tt = ios.heavy(title, 25f)
        top.addView(tt, LinearLayout.LayoutParams(0, WRAP, 1f))
        val done = ios.text("Done", 15.5f, ios.ACCENT, true)
        done.setPadding(dp(10), dp(8), dp(6), dp(8))
        top.addView(done)
        page.addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(dp(14), 0, dp(14), dp(4))
        page.addView(header, LinearLayout.LayoutParams(MATCH, WRAP))

        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(0, 0, 0, dp(40) + insetBottom)
        scroll.addView(body, ViewGroup.LayoutParams(MATCH, WRAP))
        page.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))

        root.addView(page, FrameLayout.LayoutParams(MATCH, MATCH))
        settingsPage = page
        updateFab()
        hideLoadProgress()
        applyBar(ios.BG)

        val close: () -> Unit = {
            if (settingsPage === page) {
                root.removeView(page)
                settingsPage = null
                settingsClose = null
                updateFab()
                if (homeShown) applyBar(ios.BG) else readThemeColor(web)
            }
        }
        settingsClose = close
        done.setOnClickListener { close() }
        return PageHandle(header, body, close)
    }

    fun closeSettingsPage() {
        settingsClose?.invoke()
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
                if (pageUrl == BLANK) {
                    showHome()
                    return
                }
                navError = false
                resetBar()
                if (!docStart) view?.evaluateJavascript(spoofJs, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url == BLANK) {
                    try {
                        view?.clearHistory()
                    } catch (e: Exception) {
                    }
                    return
                }
                if (!navError && offline.showing) offline.hide()
                if (store.adBlock) view?.evaluateJavascript(Blocker.cosmeticJs, null)
                readThemeColor(view)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                val rq = request ?: return
                if (!rq.isForMainFrame) return
                val u = rq.url.toString()
                if (u == BLANK || homeShown) return
                navError = true
                failedUrl = u
                hideLoadProgress()
                applyBar(ios.BG)
                offline.show(u)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val r = request ?: return null
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
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val u = view?.url ?: ""
                if (homeShown || u == BLANK || settingsPage != null) hideLoadProgress()
                else setLoadProgress(newProgress)
            }

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

    fun withRuntime(perms: Array<String>, cb: (Boolean) -> Unit) {
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
        if (blocked) return
        if (requestCode == VoiceSearch.REQ_VOICE) {
            val ok = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            home.voice.onPermissionResult(ok)
            return
        }
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
                    toast("Downloading...")
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
            t.contains(" ") || !t.contains(".") -> Engines.searchUrl(homeStore.engine, t)
            else -> "https://$t"
        }
        openUrl(url)
    }

    fun openUrl(url: String) {
        val o = originOf(url)
        if (store.preAsk && o != null && o !in approved && !store.hasRules(o)) {
            panel.siteModeDialog(o) {
                showWeb()
                web.loadUrl(url)
            }
        } else {
            showWeb()
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
        return "$t  $origin\n$title: $detail  ->  $result"
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

class PageHandle(val header: LinearLayout, val body: LinearLayout, val close: () -> Unit)
