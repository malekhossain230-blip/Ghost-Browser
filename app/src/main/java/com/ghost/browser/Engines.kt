package com.ghost.browser

import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.regex.Pattern

class Engine(val key: String, val label: String, val url: String, val letter: String, val color: Int, val domain: String) {
    val icon: String get() = "eng-$key"
}

object Engines {
    val all: List<Engine> = listOf(
        Engine("google", "Google", "https://www.google.com/search?q=", "G", 0xFF4285F4.toInt(), "google.com"),
        Engine("bing", "Bing", "https://www.bing.com/search?q=", "b", 0xFF1D6CF0.toInt(), "bing.com"),
        Engine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=", "D", 0xFFDE5833.toInt(), "duckduckgo.com"),
        Engine("brave", "Brave", "https://search.brave.com/search?q=", "B", 0xFFFB542B.toInt(), "search.brave.com"),
        Engine("yahoo", "Yahoo", "https://search.yahoo.com/search?p=", "Y", 0xFF6001D2.toInt(), "yahoo.com"),
        Engine("yandex", "Yandex", "https://yandex.com/search/?text=", "Я", 0xFFFC3F1D.toInt(), "yandex.com"),
        Engine("startpage", "Startpage", "https://www.startpage.com/do/search?q=", "S", 0xFF6573FF.toInt(), "startpage.com"),
        Engine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=", "E", 0xFF3C9A5F.toInt(), "ecosia.org"),
        Engine("qwant", "Qwant", "https://www.qwant.com/?q=", "Q", 0xFF5C97FF.toInt(), "qwant.com"),
        Engine("mojeek", "Mojeek", "https://www.mojeek.com/search?q=", "M", 0xFF0FA14F.toInt(), "mojeek.com"),
        Engine("baidu", "Baidu", "https://www.baidu.com/s?wd=", "百", 0xFF2932E1.toInt(), "baidu.com"),
        Engine("perplexity", "Perplexity", "https://www.perplexity.ai/search?q=", "P", 0xFF20808D.toInt(), "perplexity.ai")
    )

    private val byKey: Map<String, Engine> = all.associateBy { it.key }

    fun has(key: String): Boolean = byKey.containsKey(key)

    fun get(key: String): Engine = byKey[key] ?: all[0]

    fun searchUrl(key: String, query: String): String = get(key).url + Uri.encode(query)
}

object UrlUtil {
    private val URL_RE = Pattern.compile("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?(/.*)?$", Pattern.CASE_INSENSITIVE)
    private val IP_RE = Pattern.compile("^(https?://)?(\\d{1,3}\\.)(\\d{1,3}\\.){2}\\d{1,3}(:\\d+)?(/.*)?$")
    private val LOCAL_RE = Pattern.compile("^localhost(:\\d+)?(/.*)?$", Pattern.CASE_INSENSITIVE)
    private val SCHEME_RE = Pattern.compile("^[a-z][a-z0-9+.-]*://", Pattern.CASE_INSENSITIVE)
    private val HTTP_RE = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE)
    private val SUB_RE = Pattern.compile("^(www|web|m|mobile|app|touch|wap)$", Pattern.CASE_INSENSITIVE)

    fun looksLikeUrl(q: String): Boolean {
        val s = q.trim()
        if (s.isEmpty() || s.any { it.isWhitespace() }) return false
        return URL_RE.matcher(s).matches() || IP_RE.matcher(s).matches() || HTTP_RE.matcher(s).matches() || LOCAL_RE.matcher(s).matches()
    }

    fun isValidSite(raw: String): Boolean {
        val u = normalize(raw)
        if (u.isEmpty() || domainOf(u).isEmpty()) return false
        val bare = u.replaceFirst(Regex("^https?://", RegexOption.IGNORE_CASE), "")
        return URL_RE.matcher(bare).matches() || IP_RE.matcher(bare).matches() || LOCAL_RE.matcher(bare).matches()
    }

    fun normalize(raw: String): String {
        var u = raw.trim()
        if (u.isEmpty()) return ""
        if (!SCHEME_RE.matcher(u).find()) u = "https://$u"
        return u
    }

    fun domainOf(url: String): String {
        var h = url.replaceFirst(Regex("^[a-z]+://", RegexOption.IGNORE_CASE), "")
        h = h.split('/', '?', '#')[0]
        h = h.replaceFirst(Regex("^www\\.", RegexOption.IGNORE_CASE), "")
        h = h.substringAfterLast('@').substringBefore(':')
        return h.lowercase()
    }

    fun hostLabel(url: String): String {
        val part = domainOf(url).split('.')[0]
        if (part.isEmpty()) return "Link"
        return part.substring(0, 1).uppercase() + part.substring(1)
    }

    fun iconDomain(domain: String): String {
        val d = domain.lowercase()
        val parts = d.split('.')
        if (parts.size > 2 && SUB_RE.matcher(parts[0]).matches()) return parts.drop(1).joinToString(".")
        return d
    }

    fun displayTerm(s: String): String {
        if (!looksLikeUrl(s)) return s
        return s.replaceFirst(Regex("^https?://", RegexOption.IGNORE_CASE), "").trimEnd('/')
    }

    fun toLoadable(q: String): String {
        val s = q.trim()
        return if (HTTP_RE.matcher(s).matches()) s else "https://$s"
    }
}

object Suggest {
    private class Provider(val name: String, val build: (String) -> String)

    private val providers = listOf(
        Provider("google") { q -> "https://suggestqueries.google.com/complete/search?client=firefox&q=" + Uri.encode(q) },
        Provider("bing") { q -> "https://api.bing.com/osjson.aspx?query=" + Uri.encode(q) },
        Provider("yahoo") { q -> "https://search.yahoo.com/sugg/gossip/gossip-us-ura/?output=sd1&command=" + Uri.encode(q) }
    )

    private val down = HashMap<String, Long>()
    private val cache = object : LinkedHashMap<String, List<String>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean = size > 120
    }
    private val pool = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var seq = 0

    fun cached(q: String): List<String>? = synchronized(cache) { cache[q.trim().lowercase()] }

    fun cancelAll() {
        seq++
    }

    fun fetch(q: String, cb: (List<String>) -> Unit) {
        val key = q.trim().lowercase()
        if (key.isEmpty()) {
            cb(emptyList())
            return
        }
        val hit = cached(key)
        if (hit != null) {
            cb(hit)
            return
        }
        val my = ++seq
        pool.execute {
            val now = System.currentTimeMillis()
            var result: List<String> = emptyList()
            for (p in providers) {
                val until = synchronized(down) { down[p.name] ?: 0L }
                if (until > now) continue
                if (my != seq) return@execute
                val list = try {
                    parse(download(p.build(q)))
                } catch (e: Exception) {
                    null
                }
                if (list == null) {
                    synchronized(down) { down[p.name] = System.currentTimeMillis() + 60000 }
                    continue
                }
                result = list
                break
            }
            if (my != seq) return@execute
            synchronized(cache) { cache[key] = result }
            val out = result
            main.post { if (my == seq) cb(out) }
        }
    }

    private fun download(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 2500
        c.readTimeout = 2500
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36")
        c.setRequestProperty("Accept", "application/json, text/javascript, */*")
        try {
            if (c.responseCode != 200) throw IllegalStateException("http " + c.responseCode)
            val type = c.contentType ?: ""
            var charset = "UTF-8"
            val m = Regex("charset=([A-Za-z0-9_-]+)").find(type)
            if (m != null) charset = m.groupValues[1]
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(c.inputStream, charset)).use { r ->
                val buf = CharArray(4096)
                while (true) {
                    val n = r.read(buf)
                    if (n < 0) break
                    sb.append(buf, 0, n)
                    if (sb.length > 200000) break
                }
            }
            return sb.toString()
        } finally {
            c.disconnect()
        }
    }

    private fun parse(body: String): List<String> {
        val out = ArrayList<String>()
        val t = body.trim()
        if (t.isEmpty()) return out
        if (t.startsWith("[")) {
            val a = JSONArray(t)
            val inner = if (a.length() > 1 && a.optJSONArray(1) != null) a.getJSONArray(1) else a
            for (i in 0 until inner.length()) {
                val x = inner.opt(i)
                val s = when (x) {
                    is String -> x
                    is JSONArray -> if (x.length() > 0 && x.opt(0) is String) x.getString(0) else null
                    is JSONObject -> x.optString("k", x.optString("key", x.optString("phrase", x.optString("text", "")))).ifEmpty { null }
                    else -> null
                }
                if (s != null) {
                    val clean = s.replace(Regex("<[^>]+>"), "").trim()
                    if (clean.isNotEmpty()) out.add(clean)
                }
            }
            return out
        }
        if (t.startsWith("{")) {
            val o = JSONObject(t)
            val list: JSONArray? = o.optJSONArray("r") ?: o.optJSONObject("gossip")?.optJSONArray("results") ?: o.optJSONArray("suggestions")
            if (list != null) {
                for (i in 0 until list.length()) {
                    val x = list.opt(i)
                    val s = when (x) {
                        is String -> x
                        is JSONObject -> x.optString("k", x.optString("key", x.optString("phrase", x.optString("text", x.optString("query", ""))))).ifEmpty { null }
                        else -> null
                    }
                    if (s != null) {
                        val clean = s.replace(Regex("<[^>]+>"), "").trim()
                        if (clean.isNotEmpty()) out.add(clean)
                    }
                }
            }
        }
        return out
    }
}
