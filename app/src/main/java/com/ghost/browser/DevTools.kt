package com.ghost.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetEntry(
    val time: Long,
    val method: String,
    val url: String,
    val main: Boolean,
    val tag: String,
    val blocked: Boolean,
    val headers: Map<String, String>
)

/** সব নেটওয়ার্ক রিকোয়েস্টের তালিকা (শেষ ৫০০টি)। */
object NetLog {
    private val items = ArrayList<NetEntry>()

    @Synchronized
    fun add(e: NetEntry) {
        items.add(0, e)
        if (items.size > 500) items.removeAt(items.size - 1)
    }

    @Synchronized
    fun all(): List<NetEntry> = ArrayList(items)

    @Synchronized
    fun clear() {
        items.clear()
    }
}

class ConLine(val level: String, val text: String)

/** পেজের console বার্তা + DevTools-এ চালানো কোডের ফল। */
object ConsoleLog {
    private val items = ArrayList<ConLine>()

    @Synchronized
    fun add(level: String, text: String) {
        items.add(0, ConLine(level, text))
        if (items.size > 300) items.removeAt(items.size - 1)
    }

    @Synchronized
    fun all(): List<ConLine> = ArrayList(items)

    @Synchronized
    fun clear() {
        items.clear()
    }
}

/** লগ শেয়ার ও ফাইলে সেভ করার সাহায্যকারী। */
object Export {
    fun stamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    fun share(act: MainActivity, title: String, text: String) {
        try {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, title)
            i.putExtra(Intent.EXTRA_TEXT, text)
            act.startActivity(Intent.createChooser(i, title))
        } catch (e: Exception) {
            act.toast("শেয়ার করা যায়নি")
        }
    }

    fun save(act: MainActivity, prefix: String, text: String) {
        val where = Downloads.saveBytes(act, prefix + "-" + stamp() + ".txt", "text/plain", text.toByteArray(Charsets.UTF_8))
        act.toast(if (where != null) "সেভ হয়েছে: $where" else "ফাইল সেভ করা যায়নি")
    }

    fun copy(act: MainActivity, text: String) {
        try {
            val cm = act.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("ghost", text))
            act.toast("কপি হয়েছে")
        } catch (e: Exception) {
            act.toast("কপি করা যায়নি")
        }
    }
}

/** Eruda ধাঁচের DevTools: Console, Network, কুকি, Storage, Info। */
class DevTools(private val act: MainActivity, private val ui: Ios) {
    private var handle: SheetHandle? = null
    private var tab = 0

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun open() {
        val old = handle
        if (old != null) {
            try {
                old.dialog.dismiss()
            } catch (e: Exception) {
            }
        }
        val h = ui.sheet(act, "DevTools", 0.92f)
        handle = h
        h.dialog.setOnDismissListener {
            if (handle === h) handle = null
        }
        val seg = ui.segmented(listOf("Console", "Network", "কুকি", "Storage", "Info"), tab) { i ->
            tab = i
            render()
        }
        h.header.addView(seg)
        h.dialog.show()
        render()
    }

    private fun render() {
        val h = handle ?: return
        h.body.removeAllViews()
        when (tab) {
            0 -> consoleTab(h.body)
            1 -> networkTab(h.body)
            2 -> cookiesTab(h.body)
            3 -> storageTab(h.body)
            else -> infoTab(h.body)
        }
    }

    private fun addPadded(parent: LinearLayout, v: android.view.View, topDp: Int = 12) {
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.leftMargin = ui.dp(16)
        lp.rightMargin = ui.dp(16)
        lp.topMargin = ui.dp(topDp)
        parent.addView(v, lp)
    }

    private fun mono(t: String, color: Int): TextView {
        val v = ui.text(t, 12f, color)
        v.typeface = Typeface.MONOSPACE
        v.setTextIsSelectable(true)
        v.setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(8))
        return v
    }

    private fun short(s: String, n: Int): String = if (s.length > n) s.substring(0, n) + "…" else s

    /** evaluateJavascript-এর ফলাফল (JSON-এনকোড করা) থেকে আসল স্ট্রিং বের করে। */
    private fun unwrap(r: String?): String {
        if (r == null || r == "null") return ""
        return try {
            val v = JSONTokener(r).nextValue()
            if (v is String) v else r
        } catch (e: Exception) {
            r
        }
    }

    // ---------------- Console ----------------
    private fun consoleTab(b: LinearLayout) {
        b.addView(ui.header("JavaScript চালান (এই পেজে)"))
        val c0 = ui.card()
        val et = EditText(act)
        et.hint = "যেমন: document.title"
        et.textSize = 14f
        et.typeface = Typeface.MONOSPACE
        et.background = null
        et.minLines = 2
        et.gravity = android.view.Gravity.TOP
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        et.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
        ui.add(c0, et)
        ui.addCard(b, c0)
        addPadded(b, ui.bigButton("চালান", ui.BLUE, Color.WHITE) {
            val code = et.text.toString().trim()
            if (code.isNotEmpty()) {
                ConsoleLog.add("INPUT", ">> $code")
                act.web.evaluateJavascript(code) { r ->
                    ConsoleLog.add("RESULT", "<< " + (r ?: "undefined"))
                    if (handle != null && tab == 0) render()
                }
            }
        })

        b.addView(ui.header("Console (নতুন আগে)"))
        val items = ConsoleLog.all()
        if (items.isEmpty()) {
            b.addView(ui.footer("এখনো কিছু নেই। পেজের console.log ও এরর এখানে আসবে।"))
        } else {
            val c = ui.card()
            for (ln in items) {
                val col = when (ln.level) {
                    "ERROR" -> ui.RED
                    "WARNING" -> ui.ORANGE
                    "INPUT" -> ui.BLUE
                    "RESULT" -> ui.GREEN
                    else -> Color.BLACK
                }
                ui.add(c, mono(short(ln.text, 1500), col))
            }
            ui.addCard(b, c)
            addPadded(b, ui.bigButton("Console মুছুন", Color.WHITE, ui.RED) {
                ConsoleLog.clear()
                render()
            }, 16)
        }
    }

    // ---------------- Network ----------------
    private fun networkTab(b: LinearLayout) {
        val items = NetLog.all()
        var blocked = 0
        for (e in items) if (e.blocked) blocked++
        b.addView(ui.header("রিকোয়েস্ট (নতুন আগে)"))
        b.addView(ui.footer("দেখানো হচ্ছে ${Math.min(items.size, 150)}টি • মোট ${items.size} • ব্লক $blocked"))
        if (items.isEmpty()) {
            b.addView(ui.footer("এখনো কোনো রিকোয়েস্ট নেই।"))
            return
        }
        val tf = SimpleDateFormat("HH:mm:ss", Locale.US)
        val c = ui.card()
        for (e in items.take(150)) {
            val u = try {
                android.net.Uri.parse(e.url)
            } catch (x: Exception) {
                null
            }
            val title = (u?.host ?: e.url) + (u?.path ?: "")
            val sub = tf.format(Date(e.time)) + "  " + e.method + (if (e.main) "  • পেজ" else "") + (if (e.tag.isNotEmpty()) "  • " + e.tag else "")
            val r = ui.row(null, 0, short(title, 60), sub, if (e.blocked) "ব্লক" else null, false, if (e.blocked) ui.RED else Color.BLACK) {
                netDetail(e)
            }
            r.value?.setTextColor(ui.RED)
            ui.add(c, r.view)
        }
        ui.addCard(b, c)

        addPadded(b, ui.bigButton("তালিকা শেয়ার করুন", ui.BLUE, Color.WHITE) {
            Export.share(act, "Ghost Browser network", netText(items))
        }, 16)
        addPadded(b, ui.bigButton("ফাইলে সেভ (Download)", Color.WHITE, ui.BLUE) {
            Export.save(act, "ghost-network", netText(items))
        }, 10)
        addPadded(b, ui.bigButton("Network মুছুন", Color.WHITE, ui.RED) {
            NetLog.clear()
            render()
        }, 10)
    }

    private fun netText(items: List<NetEntry>): String {
        val tf = SimpleDateFormat("HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        for (e in items.reversed()) {
            sb.append(tf.format(Date(e.time))).append("  ").append(e.method).append("  ").append(e.url)
            if (e.tag.isNotEmpty()) sb.append("  [").append(e.tag).append("]")
            if (e.blocked) sb.append("  BLOCKED")
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun netDetail(e: NetEntry) {
        val sb = StringBuilder()
        sb.append(e.method).append("  ").append(e.url).append("\n\n")
        if (e.tag.isNotEmpty()) sb.append("ধরন: ").append(e.tag).append("\n")
        sb.append(if (e.blocked) "ফলাফল: ব্লক করা হয়েছে\n" else "ফলাফল: যেতে দেওয়া হয়েছে\n")
        sb.append("\nRequest headers:\n")
        for ((k, v) in e.headers) sb.append(k).append(": ").append(v).append("\n")
        val t = mono(sb.toString(), Color.BLACK)
        ui.alert(
            act, "রিকোয়েস্ট", null, t,
            listOf(
                AlertBtn("URL কপি", AlertBtn.BOLD) { Export.copy(act, e.url) },
                AlertBtn("বন্ধ", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    // ---------------- কুকি ----------------
    private fun currentUrl(): String? {
        val u = act.web.url ?: return null
        return if (u.startsWith("http://") || u.startsWith("https://")) u else null
    }

    private fun cookiesTab(b: LinearLayout) {
        val url = currentUrl()
        if (url == null) {
            b.addView(ui.footer("এই পেজের কুকি নেই (শুধু http/https সাইটে কুকি থাকে)।"))
            return
        }
        val cm = CookieManager.getInstance()
        val raw = cm.getCookie(url) ?: ""
        val list = raw.split(";").map { it.trim() }.filter { it.contains("=") }
        b.addView(ui.header("কুকি • " + short(url, 50)))
        if (list.isEmpty()) {
            b.addView(ui.footer("এই সাইটের কোনো কুকি নেই।"))
            return
        }
        val c = ui.card()
        for (kv in list) {
            val name = kv.substringBefore("=")
            val value = kv.substringAfter("=")
            val r = ui.row(null, 0, name, short(value, 80), null, true) { cookieEdit(url, name, value) }
            ui.add(c, r.view)
        }
        ui.addCard(b, c)
        b.addView(ui.footer("আলাদা Path বা Domain-এর কুকি মুছলে কখনো কখনো থেকে যায়।"))
        addPadded(b, ui.bigButton("এই সাইটের সব কুকি মুছুন", Color.WHITE, ui.RED) {
            for (kv in list) cm.setCookie(url, kv.substringBefore("=") + "=; Max-Age=0; Path=/")
            cm.flush()
            act.toast("কুকি মুছেছে")
            render()
        }, 12)
    }

    private fun cookieEdit(url: String, name: String, value: String) {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        val et = EditText(act)
        et.setText(value)
        et.textSize = 14f
        et.background = ui.rounded(Color.WHITE, 8)
        et.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        box.addView(et)
        val cm = CookieManager.getInstance()
        ui.alert(
            act, name, "মান বদলান বা কুকিটি মুছুন", box,
            listOf(
                AlertBtn("সেভ", AlertBtn.BOLD) {
                    cm.setCookie(url, name + "=" + et.text.toString() + "; Path=/")
                    cm.flush()
                    render()
                },
                AlertBtn("মুছুন", AlertBtn.DESTRUCTIVE) {
                    cm.setCookie(url, "$name=; Max-Age=0; Path=/")
                    cm.flush()
                    render()
                },
                AlertBtn("বাতিল", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    // ---------------- Storage ----------------
    private val storageJs = "(function(){try{var o={l:{},s:{}};" +
        "for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o.l[k]=localStorage.getItem(k)}" +
        "for(var j=0;j<sessionStorage.length;j++){var q=sessionStorage.key(j);o.s[q]=sessionStorage.getItem(q)}" +
        "return JSON.stringify(o)}catch(e){return JSON.stringify({err:String(e)})}})()"

    private fun storageTab(b: LinearLayout) {
        if (currentUrl() == null && act.web.url?.startsWith("https://appassets") != true) {
            b.addView(ui.footer("এই পেজে Storage নেই।"))
            return
        }
        b.addView(ui.footer("লোড হচ্ছে…"))
        val myHandle = handle
        act.web.evaluateJavascript(storageJs) { r ->
            if (handle == null || handle !== myHandle || tab != 3) return@evaluateJavascript
            val body = handle!!.body
            body.removeAllViews()
            val o: JSONObject? = try {
                JSONObject(unwrap(r))
            } catch (e: Exception) {
                null
            }
            if (o == null || o.has("err")) {
                body.addView(ui.footer("Storage পড়া যায়নি" + (if (o != null) ": " + o.optString("err") else "")))
                return@evaluateJavascript
            }
            storageSection(body, "localStorage", "localStorage", o.optJSONObject("l"))
            storageSection(body, "sessionStorage", "sessionStorage", o.optJSONObject("s"))
        }
    }

    private fun storageSection(body: LinearLayout, title: String, store: String, obj: JSONObject?) {
        body.addView(ui.header(title))
        if (obj == null || obj.length() == 0) {
            body.addView(ui.footer("খালি"))
            return
        }
        val c = ui.card()
        val keys = obj.keys().asSequence().toList()
        for (k in keys) {
            val v = obj.optString(k)
            val r = ui.row(null, 0, short(k, 40), short(v, 90), null, true) { storageEdit(store, k, v) }
            ui.add(c, r.view)
        }
        ui.addCard(body, c)
        addPadded(body, ui.bigButton("$title খালি করুন", Color.WHITE, ui.RED) {
            act.web.evaluateJavascript("try{$store.clear()}catch(e){}", null)
            act.toast("খালি করা হয়েছে")
            render()
        }, 10)
    }

    private fun storageEdit(store: String, key: String, value: String) {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        val et = EditText(act)
        et.setText(value)
        et.textSize = 14f
        et.background = ui.rounded(Color.WHITE, 8)
        et.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        box.addView(et)
        ui.alert(
            act, key, "মান বদলান বা মুছুন", box,
            listOf(
                AlertBtn("সেভ", AlertBtn.BOLD) {
                    act.web.evaluateJavascript(
                        "try{" + store + ".setItem(" + JSONObject.quote(key) + "," + JSONObject.quote(et.text.toString()) + ")}catch(e){}", null
                    )
                    render()
                },
                AlertBtn("মুছুন", AlertBtn.DESTRUCTIVE) {
                    act.web.evaluateJavascript("try{" + store + ".removeItem(" + JSONObject.quote(key) + ")}catch(e){}", null)
                    render()
                },
                AlertBtn("বাতিল", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    // ---------------- Info ----------------
    private fun infoTab(b: LinearLayout) {
        b.addView(ui.header("পেজ"))
        val c1 = ui.card()
        ui.add(c1, ui.row(null, 0, "ঠিকানা", short(act.web.url ?: "-", 120)).view)
        ui.add(c1, ui.row(null, 0, "শিরোনাম", short(act.web.title ?: "-", 120)).view)
        ui.addCard(b, c1)

        b.addView(ui.header("ব্রাউজার"))
        val c2 = ui.card()
        ui.add(c2, ui.row(null, 0, "User-Agent (এখন)", short(act.web.settings.userAgentString ?: "-", 200)).view)
        val wv = try {
            androidx.webkit.WebViewCompat.getCurrentWebViewPackage(act)?.versionName ?: "-"
        } catch (e: Exception) {
            "-"
        }
        ui.add(c2, ui.row(null, 0, "WebView সংস্করণ", wv).view)
        ui.add(c2, ui.row(null, 0, "সক্রিয় প্রোফাইল", act.store.active).view)
        ui.addCard(b, c2)

        b.addView(ui.header("এই সেশনের পরিসংখ্যান"))
        val c3 = ui.card()
        ui.add(c3, ui.row(null, 0, "মোট রিকোয়েস্ট", null, Stats.req.get().toString()).view)
        ui.add(c3, ui.row(null, 0, "ব্লক হওয়া বিজ্ঞাপন/ট্র্যাকার", null, Stats.ads.get().toString()).view)
        ui.add(c3, ui.row(null, 0, "ব্লক হওয়া IP-লুকআপ", null, Stats.ip.get().toString()).view)
        ui.addCard(b, c3)

        b.addView(ui.header("টুলস"))
        val c4 = ui.card()
        ui.add(c4, ui.row("copy", ui.BLUE, "পেজের HTML কপি করুন", null, null, false) {
            act.web.evaluateJavascript("document.documentElement.outerHTML") { r ->
                val html = unwrap(r)
                if (html.isEmpty()) act.toast("HTML পাওয়া যায়নি") else Export.copy(act, html)
            }
        }.view, 58)
        ui.add(c4, ui.row("refresh", ui.GREEN, "পেজ রিলোড", null, null, false) {
            act.web.reload()
            handle?.close?.invoke()
        }.view, 58)
        ui.addCard(b, c4)
    }
}
