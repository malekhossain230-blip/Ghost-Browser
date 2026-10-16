package com.ghost.browser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object IpHosts {
    private val domains = listOf(
        "ipify.org", "ipapi.co", "ipapi.com", "ipapi.is", "ipinfo.io", "ip-api.com", "ifconfig.me",
        "ifconfig.co", "ifconfig.io", "icanhazip.com", "checkip.amazonaws.com", "myip.com",
        "ipwho.is", "ipwhois.app", "ipgeolocation.io", "geojs.io", "freegeoip.app", "ipstack.com",
        "ipdata.co", "db-ip.com", "ident.me", "wtfismyip.com", "ipecho.net", "ip.sb",
        "whatismyip.com", "whatismyipaddress.com", "ipaddress.com", "extreme-ip-lookup.com",
        "ipregistry.co", "ip.guide", "dyndns.org", "seeip.org", "jsonip.com", "ipleak.net",
        "myexternalip.com", "ip.me", "myip.wtf", "ipquery.io", "ip2location.io", "ipbase.com",
        "abstractapi.com", "browserleaks.com"
    )

    fun matches(host: String, path: String): Boolean {
        val h = host.lowercase()
        for (d in domains) {
            if (h == d || h.endsWith(".$d")) return true
        }
        if (h == "httpbin.org" && path.startsWith("/ip")) return true
        if ((h.endsWith("cloudflare.com") || h == "1.1.1.1") && path.contains("/cdn-cgi/trace")) return true
        return false
    }
}

class IpWatch(private val act: MainActivity, private val store: Store) {
    private val channel = "ip_alert"
    private val last = HashMap<String, Long>()
    private var nid = 2000

    init {
        val nm = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(channel, "IP request alert", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "Alerts you when a site tries to read your IP"
        nm.createNotificationChannel(ch)
    }

    fun report(origin: String, via: String, blocked: Boolean = false) {
        if (!store.ipAlert) return
        val key = "$origin|$via"
        val now = System.currentTimeMillis()
        var id = 0
        synchronized(this) {
            val t = last[key]
            if (t != null && now - t < 8000) return
            last[key] = now
            nid++
            id = nid
        }
        GhostLog.add(
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date()) +
                "  " + origin + "\nRequested IP (" + via + ")" + (if (blocked) " -> blocked" else "")
        )
        try {
            val i = Intent(act, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            val pi = PendingIntent.getActivity(act, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val text = "$origin -> $via" + (if (blocked) "\nBlocked" else "")
            val n = Notification.Builder(act, channel)
                .setSmallIcon(R.drawable.ic_stat_ghost)
                .setContentTitle("A site is trying to read your IP")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            val nm = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(id, n)
        } catch (e: Exception) {
        }
        act.toast(if (blocked) "IP lookup blocked: $via" else "IP was requested: $via")
    }
}
