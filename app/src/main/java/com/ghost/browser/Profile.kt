package com.ghost.browser

import android.content.Context
import org.json.JSONObject

data class Field(val key: String, val label: String, val group: String, val def: String)

object Fields {
    val groupTitles: LinkedHashMap<String, String> = linkedMapOf(
        "device" to "ডিভাইস ও ব্রাউজার",
        "screen" to "স্ক্রিন",
        "locale" to "ভাষা ও টাইমজোন",
        "location" to "লোকেশন (GPS)",
        "network" to "নেটওয়ার্ক",
        "fingerprint" to "ফিঙ্গারপ্রিন্ট (Canvas / WebGL)",
        "battery" to "ব্যাটারি",
        "webrtc" to "WebRTC (আসল IP লিক করতে পারে)",
        "sensors" to "মোশন সেন্সর",
        "clipboard" to "ক্লিপবোর্ড",
        "media" to "ক্যামেরা / মাইক"
    )

    val all: List<Field> = listOf(
        Field("ua", "User-Agent", "device",
            "Mozilla/5.0 (Linux; Android 14; SM-S921B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"),
        Field("family", "OS পরিবার (android / ios)", "device", "android"),
        Field("model", "Device model", "device", "SM-S921B"),
        Field("androidVersion", "Android version", "device", "14"),
        Field("platform", "Platform", "device", "Linux armv8l"),
        Field("vendor", "Vendor", "device", "Google Inc."),
        Field("cores", "CPU cores", "device", "8"),
        Field("memory", "RAM (GB)", "device", "8"),
        Field("touch", "Max touch points", "device", "5"),
        Field("dnt", "Do Not Track (1 / 0 / খালি)", "device", "1"),

        Field("screenW", "Screen width", "screen", "412"),
        Field("screenH", "Screen height", "screen", "915"),
        Field("dpr", "Pixel ratio", "screen", "2.625"),
        Field("depth", "Color depth", "screen", "24"),

        Field("lang", "Language", "locale", "en-US"),
        Field("langs", "Languages (কমা দিয়ে)", "locale", "en-US,en"),
        Field("tz", "Timezone (IANA)", "locale", "Asia/Dhaka"),
        Field("tzOffset", "Timezone offset মিনিটে (Dhaka = -360)", "locale", "-360"),

        Field("lat", "Latitude", "location", "23.8103"),
        Field("lon", "Longitude", "location", "90.4125"),
        Field("acc", "Accuracy (মিটার)", "location", "20"),

        Field("netType", "Network type (4g/3g/2g)", "network", "4g"),
        Field("downlink", "Downlink (Mbps)", "network", "10"),
        Field("rtt", "RTT (ms)", "network", "50"),

        Field("gpuVendor", "GPU vendor", "fingerprint", "Qualcomm"),
        Field("gpuRenderer", "GPU renderer", "fingerprint", "Adreno (TM) 750"),
        Field("canvasNoise", "Canvas noise seed (যেকোনো সংখ্যা)", "fingerprint", "7"),

        Field("battery", "Battery level (0 থেকে 1)", "battery", "0.87")
    )

    fun forGroup(group: String): List<Field> = all.filter { it.group == group }

    /** Fixes values that would make the fake profile look inconsistent. Returns notes about what changed. */
    fun sanitize(m: MutableMap<String, String>): List<String> {
        val notes = ArrayList<String>()

        val nt = (m["netType"] ?: "").trim().lowercase()
        if (nt in listOf("slow-2g", "2g", "3g", "4g")) {
            m["netType"] = nt
        } else {
            m["netType"] = "4g"
            notes.add("Network type '" + nt + "' বৈধ নয় (slow-2g/2g/3g/4g) → 4g করা হয়েছে")
        }

        val tz = (m["tz"] ?: "").trim()
        if (java.util.TimeZone.getAvailableIDs().contains(tz)) {
            m["tz"] = tz
            val off = -java.util.TimeZone.getTimeZone(tz).getOffset(System.currentTimeMillis()) / 60000
            if (m["tzOffset"] != off.toString()) {
                m["tzOffset"] = off.toString()
                notes.add("Offset টাইমজোন অনুযায়ী " + off + " করা হয়েছে")
            }
        } else {
            notes.add("টাইমজোন '" + tz + "' বৈধ নয়। এভাবে লিখুন: America/New_York")
        }
        return notes
    }
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ghost", Context.MODE_PRIVATE)

    init {
        if (!sp.contains("profiles")) {
            val o = JSONObject()
            o.put("Default", defaults())
            sp.edit().putString("profiles", o.toString()).putString("active", "Default").apply()
        }
        // ২০টি রেডিমেড ফোন (একবারই যোগ হয়)
        if (!sp.getBoolean("seeded_v2", false)) {
            val all = profiles()
            for ((name, ov) in Presets.phones) {
                val j = defaults()
                for ((k, v) in ov) j.put(k, v)
                all.put(name, j)
            }
            sp.edit().putString("profiles", all.toString()).putBoolean("seeded_v2", true).apply()
        }
    }

    private fun defaults(): JSONObject {
        val j = JSONObject()
        for (f in Fields.all) j.put(f.key, f.def)
        return j
    }

    private fun profiles(): JSONObject = JSONObject(sp.getString("profiles", "{}") ?: "{}")

    fun names(): List<String> = profiles().keys().asSequence().toList()

    var active: String
        get() = sp.getString("active", "Default") ?: "Default"
        set(v) {
            sp.edit().putString("active", v).apply()
        }

    fun get(name: String = active): Map<String, String> {
        val p = profiles().optJSONObject(name) ?: defaults()
        val out = LinkedHashMap<String, String>()
        for (f in Fields.all) out[f.key] = p.optString(f.key, f.def)
        return out
    }

    fun save(name: String, values: Map<String, String>) {
        val all = profiles()
        val j = JSONObject()
        for ((k, v) in values) j.put(k, v)
        all.put(name, j)
        sp.edit().putString("profiles", all.toString()).apply()
    }

    fun delete(name: String) {
        val list = names()
        if (list.size <= 1) return
        val all = profiles()
        all.remove(name)
        sp.edit().putString("profiles", all.toString()).apply()
        if (active == name) active = names().first()
    }

    // ---- app settings ----
    var workerPatch: Boolean
        get() = sp.getBoolean("worker_patch", true)
        set(v) {
            sp.edit().putBoolean("worker_patch", v).apply()
        }

    var ipAlert: Boolean
        get() = sp.getBoolean("ip_alert", true)
        set(v) {
            sp.edit().putBoolean("ip_alert", v).apply()
        }

    var preAsk: Boolean
        get() = sp.getBoolean("pre_ask", false)
        set(v) {
            sp.edit().putBoolean("pre_ask", v).apply()
        }

    // ---- per-group default mode: ask / fake / real / block ----
    fun mode(group: String): String =
        sp.getString("mode_$group", if (group == "webrtc") "block" else "ask") ?: "ask"

    fun setMode(group: String, m: String) {
        sp.edit().putString("mode_$group", m).apply()
    }

    // ---- remembered per-site decisions ----
    private fun rules(): JSONObject = JSONObject(sp.getString("rules", "{}") ?: "{}")

    fun rule(origin: String, group: String): String? {
        val v = rules().optString("$origin|$group", "")
        return if (v.isEmpty()) null else v
    }

    fun setRule(origin: String, group: String, decision: String) {
        val r = rules()
        r.put("$origin|$group", decision)
        sp.edit().putString("rules", r.toString()).apply()
    }

    fun deleteRule(key: String) {
        val r = rules()
        r.remove(key)
        sp.edit().putString("rules", r.toString()).apply()
    }

    fun clearRules() {
        sp.edit().putString("rules", "{}").apply()
    }

    fun hasRules(origin: String): Boolean =
        rules().keys().asSequence().any { it.startsWith("$origin|") }

    fun ruleList(): List<Pair<String, String>> {
        val r = rules()
        return r.keys().asSequence().map { it to r.optString(it) }.toList()
    }
}
