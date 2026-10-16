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
            act.toast("Could not share")
        }
    }

    fun save(act: MainActivity, prefix: String, text: String) {
        val where = Downloads.saveBytes(act, prefix + "-" + stamp() + ".txt", "text/plain", text.toByteArray(Charsets.UTF_8))
        act.toast(if (where != null) "Saved: $where" else "Could not save the file")
    }

    fun copy(act: MainActivity, text: String) {
        try {
            val cm = act.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("vaelo", text))
            act.toast("Copied")
        } catch (e: Exception) {
            act.toast("Could not copy")
        }
    }
}

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
        val seg = ui.segmented(listOf("Console", "Network", "Cookies", "Storage", "Info"), tab) { i ->
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
        lp.leftMargin = ui.dp(14)
        lp.rightMargin = ui.dp(14)
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

    private fun short(s: String, n: Int): String = if (s.length > n) s.substring(0, n) + "..." else s

    private fun unwrap(r: String?): String {
        if (r == null || r == "null") return ""
        return try {
            val v = JSONTokener(r).nextValue()
            if (v is String) v else r
        } catch (e: Exception) {
            r
        }
    }

    private fun consoleTab(b: LinearLayout) {
        b.addView(ui.header("Run JavaScript (on this page)"))
        val c0 = ui.card()
        val et = EditText(act)
        et.hint = "e.g. document.title"
        et.setTextColor(ui.TEXT)
        et.setHintTextColor(ui.GRAY)
        et.textSize = 14f
        et.typeface = Typeface.MONOSPACE
        et.background = null
        et.minLines = 2
        et.gravity = android.view.Gravity.TOP
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        et.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12))
        ui.add(c0, et)
        ui.addCard(b, c0)
        addPadded(b, ui.bigButton("Run", ui.BLUE, Color.WHITE) {
            val code = et.text.toString().trim()
            if (code.isNotEmpty()) {
                ConsoleLog.add("INPUT", ">> $code")
                act.web.evaluateJavascript(code) { r ->
                    ConsoleLog.add("RESULT", "<< " + (r ?: "undefined"))
                    if (handle != null && tab == 0) render()
                }
            }
        })

        b.addView(ui.header("Console (newest first)"))
        val items = ConsoleLog.all()
        if (items.isEmpty()) {
            b.addView(ui.footer("Nothing yet. The page's console.log and errors will appear here."))
        } else {
            val c = ui.card()
            for (ln in items) {
                val col = when (ln.level) {
                    "ERROR" -> ui.RED
                    "WARNING" -> ui.ORANGE
                    "INPUT" -> ui.BLUE
                    "RESULT" -> ui.GREEN
                    else -> ui.TEXT
                }
                ui.add(c, mono(short(ln.text, 1500), col))
            }
            ui.addCard(b, c)
            addPadded(b, ui.bigButton("Clear Console", 0x1AFF5A52, ui.RED) {
                ConsoleLog.clear()
                render()
            }, 16)
        }
    }

    private fun networkTab(b: LinearLayout) {
        val items = NetLog.all()
        var blocked = 0
        for (e in items) if (e.blocked) blocked++
        b.addView(ui.header("Requests (newest first)"))
        b.addView(ui.footer("Showing ${Math.min(items.size, 150)} | Total ${items.size} | Blocked $blocked"))
        if (items.isEmpty()) {
            b.addView(ui.footer("No requests yet."))
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
            val sub = tf.format(Date(e.time)) + "  " + e.method + (if (e.main) "  | page" else "") + (if (e.tag.isNotEmpty()) "  | " + e.tag else "")
            val r = ui.row(null, 0, short(title, 60), sub, if (e.blocked) "Blocked" else null, false, if (e.blocked) ui.RED else ui.TEXT) {
                netDetail(e)
            }
            r.value?.setTextColor(ui.RED)
            ui.add(c, r.view)
        }
        ui.addCard(b, c)

        addPadded(b, ui.bigButton("Share list", ui.BLUE, Color.WHITE) {
            Export.share(act, "Vaelo network", netText(items))
        }, 16)
        addPadded(b, ui.bigButton("Save to file (Download)", 0x1A33A8F5, ui.BLUE) {
            Export.save(act, "vaelo-network", netText(items))
        }, 10)
        addPadded(b, ui.bigButton("Clear Network", 0x1AFF5A52, ui.RED) {
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
        if (e.tag.isNotEmpty()) sb.append("Type: ").append(e.tag).append("\n")
        sb.append(if (e.blocked) "Result: blocked\n" else "Result: allowed\n")
        sb.append("\nRequest headers:\n")
        for ((k, v) in e.headers) sb.append(k).append(": ").append(v).append("\n")
        val t = mono(sb.toString(), ui.TEXT)
        ui.alert(
            act, "Request", null, t,
            listOf(
                AlertBtn("Copy URL", AlertBtn.BOLD) { Export.copy(act, e.url) },
                AlertBtn("Close", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    private fun currentUrl(): String? {
        val u = act.web.url ?: return null
        return if (u.startsWith("http://") || u.startsWith("https://")) u else null
    }

    private fun cookiesTab(b: LinearLayout) {
        val url = currentUrl()
        if (url == null) {
            b.addView(ui.footer("This page has no cookies (only http/https sites have cookies)."))
            return
        }
        val cm = CookieManager.getInstance()
        val raw = cm.getCookie(url) ?: ""
        val list = raw.split(";").map { it.trim() }.filter { it.contains("=") }
        b.addView(ui.header("Cookies | " + short(url, 50)))
        if (list.isEmpty()) {
            b.addView(ui.footer("This site has no cookies."))
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
        b.addView(ui.footer("Cookies on a different Path or Domain may sometimes remain."))
        addPadded(b, ui.bigButton("Clear all cookies for this site", 0x1AFF5A52, ui.RED) {
            for (kv in list) cm.setCookie(url, kv.substringBefore("=") + "=; Max-Age=0; Path=/")
            cm.flush()
            act.toast("Cookies cleared")
            render()
        }, 12)
    }

    private fun cookieEdit(url: String, name: String, value: String) {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, 0, 0, ui.dp(8))
        val et = EditText(act)
        et.setText(value)
        et.textSize = 14f
        et.setTextColor(ui.TEXT)
        et.background = ui.rounded(if (ui.dark) 0x14FFFFFF else 0x0F0A1E3C, 11)
        et.setPadding(ui.dp(12), ui.dp(11), ui.dp(12), ui.dp(11))
        box.addView(et)
        val cm = CookieManager.getInstance()
        ui.alert(
            act, name, "Change the value or delete the cookie", box,
            listOf(
                AlertBtn("Save", AlertBtn.BOLD) {
                    cm.setCookie(url, name + "=" + et.text.toString() + "; Path=/")
                    cm.flush()
                    render()
                },
                AlertBtn("Delete", AlertBtn.DESTRUCTIVE) {
                    cm.setCookie(url, "$name=; Max-Age=0; Path=/")
                    cm.flush()
                    render()
                },
                AlertBtn("Cancel", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    private val storageJs = "(function(){try{var o={l:{},s:{}};" +
        "for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o.l[k]=localStorage.getItem(k)}" +
        "for(var j=0;j<sessionStorage.length;j++){var q=sessionStorage.key(j);o.s[q]=sessionStorage.getItem(q)}" +
        "return JSON.stringify(o)}catch(e){return JSON.stringify({err:String(e)})}})()"

    private fun storageTab(b: LinearLayout) {
        if (currentUrl() == null && act.web.url?.startsWith("https://appassets") != true) {
            b.addView(ui.footer("This page has no Storage."))
            return
        }
        b.addView(ui.footer("Loading..."))
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
                body.addView(ui.footer("Could not read Storage" + (if (o != null) ": " + o.optString("err") else "")))
                return@evaluateJavascript
            }
            storageSection(body, "localStorage", "localStorage", o.optJSONObject("l"))
            storageSection(body, "sessionStorage", "sessionStorage", o.optJSONObject("s"))
        }
    }

    private fun storageSection(body: LinearLayout, title: String, store: String, obj: JSONObject?) {
        body.addView(ui.header(title))
        if (obj == null || obj.length() == 0) {
            body.addView(ui.footer("Empty"))
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
        addPadded(body, ui.bigButton("Clear $title", 0x1AFF5A52, ui.RED) {
            act.web.evaluateJavascript("try{$store.clear()}catch(e){}", null)
            act.toast("Cleared")
            render()
        }, 10)
    }

    private fun storageEdit(store: String, key: String, value: String) {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, 0, 0, ui.dp(8))
        val et = EditText(act)
        et.setText(value)
        et.textSize = 14f
        et.setTextColor(ui.TEXT)
        et.background = ui.rounded(if (ui.dark) 0x14FFFFFF else 0x0F0A1E3C, 11)
        et.setPadding(ui.dp(12), ui.dp(11), ui.dp(12), ui.dp(11))
        box.addView(et)
        ui.alert(
            act, key, "Change the value or delete", box,
            listOf(
                AlertBtn("Save", AlertBtn.BOLD) {
                    act.web.evaluateJavascript(
                        "try{" + store + ".setItem(" + JSONObject.quote(key) + "," + JSONObject.quote(et.text.toString()) + ")}catch(e){}", null
                    )
                    render()
                },
                AlertBtn("Delete", AlertBtn.DESTRUCTIVE) {
                    act.web.evaluateJavascript("try{" + store + ".removeItem(" + JSONObject.quote(key) + ")}catch(e){}", null)
                    render()
                },
                AlertBtn("Cancel", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    private fun infoTab(b: LinearLayout) {
        b.addView(ui.header("Page"))
        val c1 = ui.card()
        ui.add(c1, ui.row(null, 0, "Address", short(act.web.url ?: "-", 120)).view)
        ui.add(c1, ui.row(null, 0, "Title", short(act.web.title ?: "-", 120)).view)
        ui.addCard(b, c1)

        b.addView(ui.header("Browser"))
        val c2 = ui.card()
        ui.add(c2, ui.row(null, 0, "User-Agent (current)", short(act.web.settings.userAgentString ?: "-", 200)).view)
        val wv = try {
            androidx.webkit.WebViewCompat.getCurrentWebViewPackage(act)?.versionName ?: "-"
        } catch (e: Exception) {
            "-"
        }
        ui.add(c2, ui.row(null, 0, "WebView version", wv).view)
        ui.add(c2, ui.row(null, 0, "Active profile", act.store.active).view)
        ui.addCard(b, c2)

        b.addView(ui.header("This session's stats"))
        val c3 = ui.card()
        ui.add(c3, ui.row(null, 0, "Total requests", null, Stats.req.get().toString()).view)
        ui.add(c3, ui.row(null, 0, "Blocked ads/trackers", null, Stats.ads.get().toString()).view)
        ui.add(c3, ui.row(null, 0, "Blocked IP lookups", null, Stats.ip.get().toString()).view)
        ui.addCard(b, c3)

        b.addView(ui.header("Blocklist"))
        val c5 = ui.card()
        val bl = if (Blocker.ready()) String.format(Locale.US, "%,d", Blocker.size()) + " domains" else "loading"
        ui.add(c5, ui.row(null, 0, "Ad & tracker domains", null, bl).view)
        ui.addCard(b, c5)

        b.addView(ui.header("Tools"))
        val c4 = ui.card()
        ui.add(c4, ui.row("copy", ui.BLUE, "Copy page HTML", null, null, false) {
            act.web.evaluateJavascript("document.documentElement.outerHTML") { r ->
                val html = unwrap(r)
                if (html.isEmpty()) act.toast("Could not get HTML") else Export.copy(act, html)
            }
        }.view, 58)
        ui.add(c4, ui.row("refresh", ui.GREEN, "Reload page", null, null, false) {
            act.web.reload()
            handle?.close?.invoke()
        }.view, 58)
        ui.addCard(b, c4)
    }
}
