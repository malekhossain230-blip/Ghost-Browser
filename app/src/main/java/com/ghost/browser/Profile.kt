package com.ghost.browser

import android.content.Context
import org.json.JSONObject

data class Field(val key: String, val label: String, val group: String, val def: String)

object Fields {
    val groupTitles: LinkedHashMap<String, String> = linkedMapOf(
        "device" to "Device & Browser",
        "screen" to "Screen",
        "locale" to "Language & Timezone",
        "location" to "Location (GPS)",
        "network" to "Network",
        "fingerprint" to "Fingerprint",
        "battery" to "Battery",
        "webrtc" to "WebRTC",
        "sensors" to "Motion Sensors",
        "clipboard" to "Clipboard",
        "media" to "Camera / Mic"
    )

    val all: List<Field> = listOf(
        Field("ua", "User-Agent", "device",
            "Mozilla/5.0 (Linux; Android 14; SM-S921B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"),
        Field("family", "OS family (android / ios / windows / mac)", "device", "android"),
        Field("model", "Device model", "device", "SM-S921B"),
        Field("androidVersion", "OS version", "device", "14"),
        Field("platform", "Platform", "device", "Linux armv8l"),
        Field("vendor", "Vendor", "device", "Google Inc."),
        Field("cores", "CPU cores", "device", "8"),
        Field("memory", "RAM (GB)", "device", "8"),
        Field("touch", "Max touch points", "device", "5"),
        Field("dnt", "Do Not Track (1 / 0 / empty)", "device", "1"),

        Field("screenW", "Screen width", "screen", "412"),
        Field("screenH", "Screen height", "screen", "915"),
        Field("dpr", "Pixel ratio", "screen", "2.625"),
        Field("depth", "Color depth", "screen", "24"),

        Field("lang", "Language", "locale", "en-SG"),
        Field("langs", "Languages (comma-separated)", "locale", "en-SG,en"),
        Field("tz", "Timezone (IANA)", "locale", "Asia/Singapore"),
        Field("tzOffset", "Timezone offset in minutes (Singapore = -480)", "locale", "-480"),

        Field("lat", "Latitude", "location", "1.3521"),
        Field("lon", "Longitude", "location", "103.8198"),
        Field("acc", "Accuracy (meters)", "location", "20"),

        Field("netType", "Network type (4g/3g/2g)", "network", "4g"),
        Field("downlink", "Downlink (Mbps)", "network", "10"),
        Field("rtt", "RTT (ms)", "network", "50"),

        Field("gpuVendor", "GPU vendor", "fingerprint", "Qualcomm"),
        Field("gpuRenderer", "GPU renderer", "fingerprint", "Adreno (TM) 750"),
        Field("canvasNoise", "Canvas noise seed (any number)", "fingerprint", "7"),

        Field("battery", "Battery level (0 to 1)", "battery", "0.87")
    )

    fun forGroup(group: String): List<Field> = all.filter { it.group == group }

    fun sanitize(m: MutableMap<String, String>): List<String> {
        val notes = ArrayList<String>()

        val nt = (m["netType"] ?: "").trim().lowercase()
        if (nt in listOf("slow-2g", "2g", "3g", "4g")) {
            m["netType"] = nt
        } else {
            m["netType"] = "4g"
            notes.add("Network type '$nt' is invalid (slow-2g/2g/3g/4g), set to 4g")
        }

        val tz = (m["tz"] ?: "").trim()
        if (java.util.TimeZone.getAvailableIDs().contains(tz)) {
            m["tz"] = tz
            val off = -java.util.TimeZone.getTimeZone(tz).getOffset(System.currentTimeMillis()) / 60000
            if (m["tzOffset"] != off.toString()) {
                m["tzOffset"] = off.toString()
                notes.add("Offset set to $off for the timezone")
            }
        } else {
            notes.add("Timezone '$tz' is invalid. Use for example America/New_York")
        }
        return notes
    }
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ghost", Context.MODE_PRIVATE)

    private val STARTER = "Apple iPhone 18 Pro Max"

    init {
        if (!sp.contains("profiles")) {
            val o = JSONObject()
            o.put("Default", defaults())
            sp.edit().putString("profiles", o.toString()).putString("active", "Default").apply()
        }
        if (!sp.getBoolean("seeded_v2", false)) {
            val all = profiles()
            for ((name, ov) in Presets.phones) {
                val j = defaults()
                for ((k, v) in ov) j.put(k, v)
                all.put(name, j)
            }
            sp.edit().putString("profiles", all.toString()).putBoolean("seeded_v2", true).apply()
        }
        if (!sp.getBoolean("seeded_laptops_v1", false)) {
            val all = profiles()
            for ((name, ov) in Presets.laptops) {
                val j = defaults()
                for ((k, v) in ov) j.put(k, v)
                all.put(name, j)
            }
            sp.edit().putString("profiles", all.toString()).putBoolean("seeded_laptops_v1", true).apply()
        }
        if (!sp.getBoolean("seeded_extra_v1", false)) {
            val all = profiles()
            for ((name, ov) in Presets.extra) {
                val j = defaults()
                for ((k, v) in ov) j.put(k, v)
                all.put(name, j)
            }
            sp.edit().putString("profiles", all.toString()).putBoolean("seeded_extra_v1", true).apply()
        }
        if (!sp.getBoolean("seeded_defaults_v6", false)) {
            val e = sp.edit()
            e.putBoolean("ip_block", true)
            e.putBoolean("random_noise", true)
            for (g in Fields.groupTitles.keys) e.putString("mode_$g", defaultMode(g))
            val all = profiles()
            val names = all.keys().asSequence().toList()
            for (n in names) {
                val p = all.optJSONObject(n) ?: continue
                if (p.optString("tz", "") == "Asia/Dhaka") {
                    p.put("tz", "Asia/Singapore")
                    p.put("tzOffset", "-480")
                    p.put("lang", "en-SG")
                    p.put("langs", "en-SG,en")
                    p.put("lat", "1.3521")
                    p.put("lon", "103.8198")
                    all.put(n, p)
                }
            }
            e.putString("profiles", all.toString())
            if (names.contains(STARTER)) e.putString("active", STARTER)
            e.putBoolean("seeded_defaults_v6", true)
            e.apply()
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

    var adBlock: Boolean
        get() = sp.getBoolean("ad_block", true)
        set(v) {
            sp.edit().putBoolean("ad_block", v).apply()
        }

    var ipBlock: Boolean
        get() = sp.getBoolean("ip_block", true)
        set(v) {
            sp.edit().putBoolean("ip_block", v).apply()
        }

    var screenSecure: Boolean
        get() = sp.getBoolean("screen_secure", false)
        set(v) {
            sp.edit().putBoolean("screen_secure", v).apply()
        }

    var appLock: Boolean
        get() = sp.getBoolean("app_lock", false)
        set(v) {
            sp.edit().putBoolean("app_lock", v).apply()
        }

    var clearOnExit: Boolean
        get() = sp.getBoolean("clear_exit", false)
        set(v) {
            sp.edit().putBoolean("clear_exit", v).apply()
        }

    var blockImages: Boolean
        get() = sp.getBoolean("block_images", false)
        set(v) {
            sp.edit().putBoolean("block_images", v).apply()
        }

    var randomNoise: Boolean
        get() = sp.getBoolean("random_noise", true)
        set(v) {
            sp.edit().putBoolean("random_noise", v).apply()
        }

    var textZoom: Int
        get() = sp.getInt("text_zoom", 100)
        set(v) {
            sp.edit().putInt("text_zoom", v).apply()
        }

    var proxy: String
        get() = sp.getString("proxy", "") ?: ""
        set(v) {
            sp.edit().putString("proxy", v).apply()
        }

    var preAsk: Boolean
        get() = sp.getBoolean("pre_ask", false)
        set(v) {
            sp.edit().putBoolean("pre_ask", v).apply()
        }

    fun defaultMode(group: String): String =
        if (group == "media") "ask"
        else if (Fields.forGroup(group).isEmpty()) "block"
        else "fake"

    fun mode(group: String): String =
        sp.getString("mode_$group", defaultMode(group)) ?: defaultMode(group)

    fun setMode(group: String, m: String) {
        sp.edit().putString("mode_$group", m).apply()
    }

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
