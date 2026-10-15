package com.ghost.browser

/** প্রোফাইলের মানগুলো একে অপরের সাথে মেলে কিনা যাচাই করে। যা মেলে না তার তালিকা ফেরত দেয়। */
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

        // ---- ডিভাইস পরিবার ----
        if (family == "ios") {
            if (!ua.contains("iPhone")) out.add("OS পরিবার ios, কিন্তু User-Agent-এ 'iPhone' নেই")
            if (platform != "iPhone") out.add("iOS হলে Platform 'iPhone' হওয়া উচিত (এখন: $platform)")
            if (!vendor.contains("Apple")) out.add("iOS হলে Vendor 'Apple Computer, Inc.' হওয়া উচিত")
            if (!gpuV.contains("Apple")) out.add("iOS হলে GPU vendor 'Apple Inc.' হওয়া উচিত (এখন: $gpuV)")
        } else {
            if (!ua.contains("Android")) out.add("OS পরিবার android, কিন্তু User-Agent-এ 'Android' নেই")
            if (ua.contains("iPhone")) out.add("Android প্রোফাইলে User-Agent-এ 'iPhone' আছে")
            if (!platform.startsWith("Linux")) out.add("Android হলে Platform 'Linux ...' হওয়া উচিত (এখন: $platform)")
            if (gpuV.contains("Apple")) out.add("Android ফোনে GPU vendor 'Apple' মানায় না")
            if (model.isNotBlank() && !ua.contains(model)) out.add("Model '$model' User-Agent-এর ভেতরে নেই")
            if (ver.isNotBlank() && !ua.contains("Android $ver")) out.add("Android version $ver, কিন্তু User-Agent-এ অন্য সংস্করণ")
        }

        // ---- স্ক্রিন ----
        val w = d(m["screenW"])
        val h = d(m["screenH"])
        val dpr = d(m["dpr"])
        if (w == null || h == null) out.add("স্ক্রিনের মাপ সংখ্যা নয়")
        else if (w > h) out.add("Screen width ($w) উচ্চতার ($h) চেয়ে বড় — ফোন সাধারণত উল্টো")
        if (dpr == null) out.add("Pixel ratio সংখ্যা নয়")
        else if (dpr < 1.0 || dpr > 4.5) out.add("Pixel ratio $dpr অস্বাভাবিক (সাধারণত 1.5 থেকে 4)")

        // ---- ভাষা / টাইমজোন / লোকেশন ----
        val tz = m["tz"] ?: ""
        val lang = m["lang"] ?: ""
        val langs = (m["langs"] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (langs.isNotEmpty() && langs[0] != lang) out.add("Language ($lang) আর Languages-এর প্রথমটি (${langs[0]}) এক নয়")

        val country = Presets.countries.firstOrNull { it.tz == tz }
        if (country != null) {
            val region = lang.substringAfter('-', "")
            val want = country.lang.substringAfter('-', "")
            if (region.isNotEmpty() && want.isNotEmpty() && region != want) {
                out.add("টাইমজোন ${country.name}-এর, কিন্তু ভাষা $lang (সাধারণত ${country.lang})")
            }
            val la = d(m["lat"])
            val lo = d(m["lon"])
            val cla = d(country.lat)
            val clo = d(country.lon)
            if (la != null && lo != null && cla != null && clo != null) {
                if (Math.abs(la - cla) > 6 || Math.abs(lo - clo) > 8) {
                    out.add("GPS লোকেশন টাইমজোনের দেশ (${country.name}) থেকে অনেক দূরে")
                }
            }
        } else if (tz.isNotBlank()) {
            out.add("এই টাইমজোন ($tz) প্রিসেট দেশের তালিকায় নেই — ভাষা আর GPS নিজে মিলিয়ে নিন")
        }

        // ---- নেটওয়ার্ক ----
        val nt = (m["netType"] ?: "").trim()
        val dl = d(m["downlink"])
        val rtt = d(m["rtt"])
        if (dl != null) {
            if ((nt == "2g" || nt == "slow-2g") && dl > 0.5) out.add("Network $nt, কিন্তু downlink $dl Mbps — অনেক বেশি")
            if (nt == "3g" && dl > 3) out.add("Network 3g, কিন্তু downlink $dl Mbps — বেশি")
            if (nt == "4g" && dl < 1) out.add("Network 4g, কিন্তু downlink $dl Mbps — কম")
        }
        if (rtt != null && nt == "4g" && rtt > 300) out.add("4g নেটওয়ার্কে RTT $rtt ms অনেক বেশি")

        // ---- হার্ডওয়্যার ----
        val cores = d(m["cores"])
        if (cores != null && (cores < 2 || cores > 16)) out.add("CPU cores $cores অস্বাভাবিক")
        val mem = d(m["memory"])
        if (mem != null && (mem < 1 || mem > 24)) out.add("RAM $mem GB অস্বাভাবিক")
        val bat = d(m["battery"])
        if (bat != null && (bat < 0 || bat > 1)) out.add("Battery 0 থেকে 1 এর মধ্যে হতে হবে")

        return out
    }
}
