package com.ghost.browser

object Consistency {
    private fun d(s: String?): Double? = s?.trim()?.toDoubleOrNull()

    fun check(m: Map<String, String>): List<String> {
        val out = ArrayList<String>()
        val ua = m["ua"] ?: ""
        val family = (m["family"] ?: "android").trim().lowercase()
        val platform = m["platform"] ?: ""
        val vendor = m["vendor"] ?: ""
        val gpuV = m["gpuVendor"] ?: ""
        val model = m["model"] ?: ""
        val ver = m["androidVersion"] ?: ""
        val desktop = family == "windows" || family == "mac"

        when (family) {
            "ios" -> {
                if (!ua.contains("iPhone")) out.add("OS family is ios, but the User-Agent has no 'iPhone'")
                if (platform != "iPhone") out.add("iOS should use platform 'iPhone' (now: $platform)")
                if (!vendor.contains("Apple")) out.add("iOS should use vendor 'Apple Computer, Inc.'")
                if (!gpuV.contains("Apple")) out.add("iOS should use GPU vendor 'Apple Inc.' (now: $gpuV)")
            }
            "mac" -> {
                if (!ua.contains("Macintosh")) out.add("OS family is mac, but the User-Agent has no 'Macintosh'")
                if (platform != "MacIntel") out.add("macOS should use platform 'MacIntel' (now: $platform)")
                if (!vendor.contains("Apple")) out.add("macOS should use vendor 'Apple Computer, Inc.'")
                if (!gpuV.contains("Apple")) out.add("macOS should use GPU vendor 'Apple Inc.' (now: $gpuV)")
            }
            "windows" -> {
                if (!ua.contains("Windows")) out.add("OS family is windows, but the User-Agent has no 'Windows'")
                if (platform != "Win32") out.add("Windows should use platform 'Win32' (now: $platform)")
                if (gpuV.contains("Apple")) out.add("A Windows PC should not use an Apple GPU vendor")
            }
            else -> {
                if (!ua.contains("Android")) out.add("OS family is android, but the User-Agent has no 'Android'")
                if (ua.contains("iPhone")) out.add("Android profile has 'iPhone' in the User-Agent")
                if (!platform.startsWith("Linux")) out.add("Android should use platform 'Linux ...' (now: $platform)")
                if (gpuV.contains("Apple")) out.add("An Android phone should not use an Apple GPU vendor")
                if (model.isNotBlank() && !ua.contains(model)) out.add("Model '$model' is not present in the User-Agent")
                if (ver.isNotBlank() && !ua.contains("Android $ver")) out.add("Android version $ver, but the User-Agent shows a different version")
            }
        }

        val w = d(m["screenW"])
        val h = d(m["screenH"])
        val dpr = d(m["dpr"])
        if (w == null || h == null) out.add("Screen size is not a number")
        else if (!desktop && w > h) out.add("Screen width ($w) is larger than height ($h) - phones are usually portrait")
        if (dpr == null) out.add("Pixel ratio is not a number")
        else if (dpr < 1.0 || dpr > 4.5) out.add("Pixel ratio $dpr is unusual (usually 1 to 4.5)")

        val tz = m["tz"] ?: ""
        val lang = m["lang"] ?: ""
        val langs = (m["langs"] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (langs.isNotEmpty() && langs[0] != lang) out.add("Language ($lang) and the first of Languages (${langs[0]}) do not match")

        val country = Presets.countries.firstOrNull { it.tz == tz }
        if (country != null) {
            val region = lang.substringAfter('-', "")
            val want = country.lang.substringAfter('-', "")
            if (region.isNotEmpty() && want.isNotEmpty() && region != want) {
                out.add("Timezone is for ${country.name}, but language is $lang (usually ${country.lang})")
            }
            val la = d(m["lat"])
            val lo = d(m["lon"])
            val cla = d(country.lat)
            val clo = d(country.lon)
            if (la != null && lo != null && cla != null && clo != null) {
                if (Math.abs(la - cla) > 6 || Math.abs(lo - clo) > 8) {
                    out.add("GPS location is far from the timezone country (${country.name})")
                }
            }
        } else if (tz.isNotBlank()) {
            out.add("This timezone ($tz) is not in the preset country list - check language and GPS yourself")
        }

        val nt = (m["netType"] ?: "").trim()
        val dl = d(m["downlink"])
        val rtt = d(m["rtt"])
        if (dl != null) {
            if ((nt == "2g" || nt == "slow-2g") && dl > 0.5) out.add("Network $nt, but downlink $dl Mbps - too high")
            if (nt == "3g" && dl > 3) out.add("Network 3g, but downlink $dl Mbps - high")
            if (nt == "4g" && dl < 1) out.add("Network 4g, but downlink $dl Mbps - low")
        }
        if (rtt != null && nt == "4g" && rtt > 300) out.add("On 4g, RTT $rtt ms is very high")

        val cores = d(m["cores"])
        val maxCores = if (desktop) 128.0 else 16.0
        if (cores != null && (cores < 2 || cores > maxCores)) out.add("CPU cores $cores is unusual")
        val mem = d(m["memory"])
        val maxMem = if (desktop) 128.0 else 24.0
        if (mem != null && (mem < 1 || mem > maxMem)) out.add("RAM $mem GB is unusual")
        val bat = d(m["battery"])
        if (bat != null && (bat < 0 || bat > 1)) out.add("Battery must be between 0 and 1")

        return out
    }
}
