package com.ghost.browser

import android.content.Context
import java.util.concurrent.atomic.AtomicInteger

object Stats {
    val req = AtomicInteger(0)
    val ads = AtomicInteger(0)
    val ip = AtomicInteger(0)
}

object Blocker {
    private val builtin = hashSetOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "2mdn.net", "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net", "taboola.com",
        "outbrain.com", "moatads.com", "amazon-adsystem.com", "rubiconproject.com", "pubmatic.com",
        "openx.net", "casalemedia.com", "smartadserver.com", "adform.net", "yieldmo.com",
        "teads.tv", "mgid.com", "revcontent.com", "popads.net", "popcash.net", "propellerads.com",
        "exoclick.com", "juicyads.com", "trafficjunky.com", "highperformanceformat.com",
        "adcolony.com", "applovin.com", "unityads.unity3d.com", "ads.tiktok.com", "ads-twitter.com",
        "ads.linkedin.com", "ads.yahoo.com", "ads.pinterest.com", "advertising.com", "adition.com",
        "bidswitch.net", "contextweb.com", "lijit.com", "sharethrough.com", "triplelift.com",
        "google-analytics.com", "analytics.google.com", "scorecardresearch.com", "quantserve.com",
        "hotjar.com", "mixpanel.com", "fullstory.com", "clarity.ms", "mouseflow.com", "crazyegg.com",
        "analytics.tiktok.com", "bat.bing.com", "segment.io", "amplitude.com", "branch.io",
        "adjust.com", "appsflyer.com", "newrelic.com", "nr-data.net", "chartbeat.net",
        "inmobi.com", "smaato.net", "mopub.com", "chartboost.com", "vungle.com", "media.net",
        "serving-sys.com", "2o7.net", "demdex.net", "omtrdc.net", "everesttech.net",
        "flurry.com", "kochava.com", "tapjoy.com", "startappservice.com", "supersonicads.com",
        "zedo.com", "adtech.de", "adsafeprotected.com", "doubleverify.com", "quantcast.com",
        "bluekai.com", "krxd.net", "agkn.com", "rlcdn.com", "crwdcntrl.net", "exelator.com",
        "mathtag.com", "turn.com", "eyeota.net", "tribalfusion.com", "yieldlab.net",
        "gumgum.com", "sovrn.com", "33across.com", "districtm.io", "indexww.com",
        "ad-delivery.net", "adsco.re", "adtelligent.com", "onetag-sys.com", "sonobi.com"
    )

    private val publicSuffix = hashSetOf(
        "co.uk", "org.uk", "me.uk", "ac.uk", "gov.uk", "com.au", "net.au", "org.au",
        "com.br", "com.cn", "net.cn", "org.cn", "co.jp", "or.jp", "ne.jp", "co.kr",
        "co.in", "net.in", "org.in", "com.mx", "com.tr", "com.sg", "com.my", "co.id",
        "com.ng", "com.pk", "com.bd", "co.nz", "com.tw", "co.za", "com.ua", "com.ar",
        "com.co", "com.pe", "com.ph", "com.vn", "com.hk", "com.sa", "com.eg", "co.th",
        "com.pl", "com.es", "com.pt", "com.ru", "co.il", "com.ve", "com.ec", "com.uy"
    )

    @Volatile
    private var loaded: HashSet<String>? = null

    @Volatile
    private var total = 0

    @Volatile
    private var starting = false

    fun init(ctx: Context) {
        if (starting || loaded != null) return
        starting = true
        val app = ctx.applicationContext
        Thread {
            var set: HashSet<String>
            try {
                set = HashSet(96000)
                app.assets.open("blocklist.txt").bufferedReader().use { r ->
                    var line = r.readLine()
                    while (line != null) {
                        val d = line.trim()
                        if (d.length > 3 && d[0] != '#' && d.indexOf('.') > 0) set.add(d.lowercase())
                        line = r.readLine()
                    }
                }
                set.addAll(builtin)
            } catch (e: Throwable) {
                set = HashSet(builtin)
            }
            total = set.size
            loaded = set
            starting = false
        }.start()
    }

    fun size(): Int = if (total > 0) total else builtin.size

    fun ready(): Boolean = loaded != null

    private fun hit(set: Set<String>, h: String): Boolean {
        if (set.contains(h)) return true
        var i = h.indexOf('.')
        while (i >= 0 && i < h.length - 1) {
            val p = h.substring(i + 1)
            if (p.indexOf('.') < 0) return false
            if (!publicSuffix.contains(p) && set.contains(p)) return true
            i = h.indexOf('.', i + 1)
        }
        return false
    }

    fun matches(host: String, path: String): Boolean {
        val h = host.lowercase()
        if (h.isEmpty()) return false
        if (hit(builtin, h)) return true
        val set = loaded
        if (set != null && hit(set, h)) return true
        if (h.endsWith("facebook.com") && (path == "/tr" || path.startsWith("/tr/"))) return true
        if (h == "connect.facebook.net" && path.contains("fbevents")) return true
        if (h == "www.googletagmanager.com" && (path.startsWith("/gtm.js") || path.startsWith("/gtag/js"))) return true
        return false
    }

    const val cosmeticJs = "(function(){try{if(document.getElementById('__ghost_ads'))return;" +
        "var s=document.createElement('style');s.id='__ghost_ads';" +
        "s.textContent='ins.adsbygoogle,.adsbygoogle,[data-ad-client],[data-ad-slot]," +
        "[id^=\"google_ads_\"],[id^=\"div-gpt-ad\"],[id^=\"gpt-ad\"],[id^=\"taboola-\"]," +
        "[id^=\"carbonads\"],[id^=\"ad-slot\"],[id^=\"banner-ad\"],#ad-container,#ads-container," +
        ".ad-banner,.ad-container,.ad-wrapper,.ad-slot,.ad-unit,.ad-placeholder,.ad-placement," +
        ".advertisement,.advert-container,.sponsored-ad,.sponsor-box,.promoted-content," +
        ".trc_rbox_container,.trc_related_container,.OUTBRAIN,.ob-widget,.ob-smartfeed-wrapper," +
        "[aria-label=\"Advertisement\"],[aria-label=\"Ads\"],[aria-label=\"advertisement\"]," +
        "iframe[src*=\"doubleclick.net\"],iframe[src*=\"googlesyndication.com\"]," +
        "iframe[src*=\"googleadservices.com\"],iframe[src*=\"adnxs.com\"]," +
        "iframe[src*=\"taboola.com\"],iframe[src*=\"outbrain.com\"],iframe[src*=\"criteo\"]," +
        "iframe[src*=\"amazon-adsystem.com\"],iframe[src*=\"rubiconproject.com\"]," +
        "iframe[src*=\"pubmatic.com\"],iframe[src*=\"adsrvr.org\"],iframe[src*=\"media.net\"]," +
        "iframe[src*=\"mgid.com\"],iframe[src*=\"revcontent.com\"]" +
        "{display:none!important;height:0!important;min-height:0!important}';" +
        "(document.head||document.documentElement).appendChild(s)}catch(e){}})()"
}
