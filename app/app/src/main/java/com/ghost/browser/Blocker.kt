package com.ghost.browser

import java.util.concurrent.atomic.AtomicInteger

/** চলতি সেশনের গণনা (Info ট্যাবে দেখায়)। */
object Stats {
    val req = AtomicInteger(0)
    val ads = AtomicInteger(0)
    val ip = AtomicInteger(0)
}

/** বিজ্ঞাপন ও ট্র্যাকারের পরিচিত ঠিকানার তালিকা। */
object Blocker {
    private val domains = listOf(
        // বিজ্ঞাপন নেটওয়ার্ক
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "2mdn.net", "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net", "taboola.com",
        "outbrain.com", "moatads.com", "amazon-adsystem.com", "rubiconproject.com", "pubmatic.com",
        "openx.net", "casalemedia.com", "smartadserver.com", "adform.net", "yieldmo.com",
        "teads.tv", "mgid.com", "revcontent.com", "popads.net", "popcash.net", "propellerads.com",
        "exoclick.com", "juicyads.com", "trafficjunky.com", "highperformanceformat.com",
        "adcolony.com", "applovin.com", "unityads.unity3d.com", "ads.tiktok.com", "ads-twitter.com",
        "ads.linkedin.com", "ads.yahoo.com", "ads.pinterest.com", "advertising.com", "adition.com",
        "bidswitch.net", "contextweb.com", "lijit.com", "sharethrough.com", "triplelift.com",
        // অ্যানালিটিক্স ও ট্র্যাকার
        "google-analytics.com", "analytics.google.com", "scorecardresearch.com", "quantserve.com",
        "hotjar.com", "mixpanel.com", "fullstory.com", "clarity.ms", "mouseflow.com", "crazyegg.com",
        "analytics.tiktok.com", "bat.bing.com", "segment.io", "amplitude.com", "branch.io",
        "adjust.com", "appsflyer.com", "newrelic.com", "nr-data.net", "chartbeat.net"
    )

    fun matches(host: String, path: String): Boolean {
        val h = host.lowercase()
        if (h.isEmpty()) return false
        for (d in domains) {
            if (h == d || h.endsWith(".$d")) return true
        }
        if (h.endsWith("facebook.com") && (path == "/tr" || path.startsWith("/tr/"))) return true
        if (h == "connect.facebook.net" && path.contains("fbevents")) return true
        if (h == "www.googletagmanager.com" && (path.startsWith("/gtm.js") || path.startsWith("/gtag/js"))) return true
        return false
    }

    /** পেজ লোড শেষে বিজ্ঞাপনের খালি জায়গা লুকাতে। */
    const val cosmeticJs = "(function(){try{if(document.getElementById('__ghost_ads'))return;" +
        "var s=document.createElement('style');s.id='__ghost_ads';" +
        "s.textContent='ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads_\"],[id^=\"div-gpt-ad\"]," +
        "iframe[src*=\"doubleclick.net\"],iframe[src*=\"googlesyndication.com\"]," +
        "iframe[src*=\"adnxs.com\"],iframe[src*=\"taboola.com\"],iframe[src*=\"outbrain.com\"]" +
        "{display:none!important}';" +
        "(document.head||document.documentElement).appendChild(s)}catch(e){}})()"
}
