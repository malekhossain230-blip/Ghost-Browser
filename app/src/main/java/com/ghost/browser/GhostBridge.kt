package com.ghost.browser

import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object GhostLog {
    private val items = ArrayList<String>()

    @Synchronized
    fun add(line: String) {
        items.add(0, line)
        if (items.size > 300) items.removeAt(items.size - 1)
    }

    @Synchronized
    fun all(): List<String> = ArrayList(items)

    @Synchronized
    fun clear() {
        items.clear()
    }
}

/**
 * Called synchronously from the page's JavaScript (spoof.js).
 * Blocks that JS call until the user answers the popup (or a saved rule applies).
 */
class GhostBridge(private val act: MainActivity, private val store: Store) {
    private val lock = Any()

    @JavascriptInterface
    fun ask(group: String, detail: String, origin: String): String {
        val result = AtomicReference("block")
        synchronized(lock) {
            val latch = CountDownLatch(1)
            act.runOnUiThread {
                act.decide(origin, group, detail) { d ->
                    result.set(d)
                    latch.countDown()
                }
            }
            latch.await(120, TimeUnit.SECONDS)
        }
        val o = JSONObject()
        o.put("mode", result.get())
        o.put("v", JSONObject(store.get()))
        return o.toString()
    }
}
