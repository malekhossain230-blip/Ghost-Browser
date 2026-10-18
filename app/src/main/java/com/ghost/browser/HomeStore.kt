package com.ghost.browser

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class Shortcut(var name: String, var url: String, var icon: String)

class HomeStore(private val ctx: Context) {

    companion object {
        const val PREFS = "vaelo_home"
        const val MAX_HISTORY = 40

        val DEFAULTS: List<Shortcut> = listOf(
            Shortcut("YouTube", "https://www.youtube.com", ""),
            Shortcut("Facebook", "https://www.facebook.com", ""),
            Shortcut("TikTok", "https://www.tiktok.com", ""),
            Shortcut("Telegram", "https://web.telegram.org", "")
        )

        fun themeMode(ctx: Context): String {
            val t = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("theme", "system") ?: "system"
            return if (t == "light" || t == "dark") t else "system"
        }
    }

    private val sp: SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var theme: String
        get() = themeMode(ctx)
        set(v) {
            sp.edit().putString("theme", if (v == "light" || v == "dark") v else "system").apply()
        }

    var engine: String
        get() {
            val k = sp.getString("engine", "google") ?: "google"
            return if (Engines.has(k)) k else "google"
        }
        set(v) {
            sp.edit().putString("engine", v).apply()
        }

    var pauseHistory: Boolean
        get() = sp.getBoolean("pause_history", false)
        set(v) {
            sp.edit().putBoolean("pause_history", v).apply()
        }

    var suggest: Boolean
        get() = sp.getBoolean("suggest", true)
        set(v) {
            sp.edit().putBoolean("suggest", v).apply()
        }

    var floatingButton: Boolean
        get() = sp.getBoolean("fab", true)
        set(v) {
            sp.edit().putBoolean("fab", v).apply()
        }

    fun hiddenDefaults(): MutableList<String> = readStrings("hidden_defaults")

    fun setHiddenDefaults(list: List<String>) {
        writeStrings("hidden_defaults", list)
    }

    fun hideDefault(url: String) {
        val h = hiddenDefaults()
        if (!h.contains(url)) h.add(url)
        setHiddenDefaults(h)
    }

    fun unhideDefault(url: String) {
        val h = hiddenDefaults()
        h.remove(url)
        setHiddenDefaults(h)
    }

    fun visibleDefaults(): List<Shortcut> {
        val h = hiddenDefaults()
        return DEFAULTS.filter { !h.contains(it.url) }
    }

    fun shortcuts(): MutableList<Shortcut> {
        val out = ArrayList<Shortcut>()
        try {
            val a = JSONArray(sp.getString("shortcuts", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val url = o.optString("url", "")
                if (url.isEmpty()) continue
                out.add(Shortcut(o.optString("name", ""), url, o.optString("icon", "")))
            }
        } catch (e: Exception) {
        }
        return out
    }

    fun saveShortcuts(list: List<Shortcut>) {
        val a = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("name", s.name)
            o.put("url", s.url)
            o.put("icon", s.icon)
            a.put(o)
        }
        sp.edit().putString("shortcuts", a.toString()).apply()
    }

    fun resetShortcuts() {
        for (s in shortcuts()) deleteIcon(s.icon)
        sp.edit().putString("shortcuts", "[]").putString("hidden_defaults", "[]").apply()
    }

    fun history(): MutableList<String> = readStrings("history")

    fun saveHistory(list: List<String>) {
        writeStrings("history", list)
    }

    fun pushHistory(q: String) {
        if (pauseHistory) return
        val t = q.trim()
        if (t.isEmpty()) return
        val h = history()
        val low = t.lowercase()
        val it = h.iterator()
        while (it.hasNext()) if (it.next().lowercase() == low) it.remove()
        h.add(0, t)
        while (h.size > MAX_HISTORY) h.removeAt(h.size - 1)
        saveHistory(h)
    }

    fun removeHistory(q: String): Int {
        val h = history()
        val idx = h.indexOf(q)
        if (idx < 0) return -1
        h.removeAt(idx)
        saveHistory(h)
        return idx
    }

    fun insertHistory(index: Int, q: String) {
        val h = history()
        h.add(Math.min(Math.max(index, 0), h.size), q)
        saveHistory(h)
    }

    fun clearHistory() {
        sp.edit().putString("history", "[]").apply()
    }

    private fun iconsDir(): File {
        val d = File(ctx.filesDir, "shortcut_icons")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun iconFile(name: String): File? {
        if (name.isEmpty()) return null
        val f = File(iconsDir(), name)
        return if (f.exists()) f else null
    }

    fun saveIcon(bmp: Bitmap): String {
        val name = UUID.randomUUID().toString().replace("-", "") + ".png"
        try {
            FileOutputStream(File(iconsDir(), name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            return ""
        }
        return name
    }

    fun deleteIcon(name: String) {
        if (name.isEmpty()) return
        try {
            File(iconsDir(), name).delete()
        } catch (e: Exception) {
        }
    }

    private fun readStrings(key: String): MutableList<String> {
        val out = ArrayList<String>()
        try {
            val a = JSONArray(sp.getString(key, "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val s = a.optString(i, "")
                if (s.isNotEmpty()) out.add(s)
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun writeStrings(key: String, list: List<String>) {
        val a = JSONArray()
        for (s in list) a.put(s)
        sp.edit().putString(key, a.toString()).apply()
    }
}
