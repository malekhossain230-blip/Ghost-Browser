package com.ghost.browser

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.LinearLayout

class Panel(private val act: MainActivity, private val store: Store, private val ui: Ios) {
    private var handle: SheetHandle? = null
    private var tab = 0

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun open() {
        val old = handle
        if (old != null) {
            try {
                old.dialog.dismiss()
            } catch (e: Exception) {
            }
        }
        val h = ui.sheet(act, "Ghost", 0.9f)
        handle = h
        h.dialog.setOnDismissListener {
            if (handle === h) handle = null
        }
        val seg = ui.segmented(listOf("Browser", "Profile", "Rules", "Log"), tab) { i ->
            tab = i
            render()
        }
        h.header.addView(seg)
        h.dialog.show()
        render()
    }

    private fun closePanel() {
        val h = handle
        if (h != null) h.close()
    }

    private fun render() {
        val h = handle ?: return
        h.body.removeAllViews()
        when (tab) {
            0 -> browserTab(h.body)
            1 -> profileTab(h.body)
            2 -> rulesTab(h.body)
            else -> logTab(h.body)
        }
    }

    private fun addPadded(parent: LinearLayout, v: View, topDp: Int = 12) {
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.leftMargin = ui.dp(16)
        lp.rightMargin = ui.dp(16)
        lp.topMargin = ui.dp(topDp)
        parent.addView(v, lp)
    }

    private fun modeLabel(k: String): String = when (k) {
        "ask" -> "Ask every time"
        "fake" -> "Always fake"
        "real" -> "Always real"
        "block" -> "Always block"
        else -> k
    }

    private fun pickList(title: String, items: List<Pair<String, String>>, current: String?, onPick: (String) -> Unit) {
        val h = ui.sheet(act, title, 0.7f)
        val c = ui.card()
        for ((id, label) in items) {
            val r = ui.row(null, 0, label, null, if (id == current) "✓" else null, false, ui.TEXT) {
                h.close()
                onPick(id)
            }
            r.value?.setTextColor(ui.BLUE)
            ui.add(c, r.view)
        }
        h.body.addView(ui.spacer(8))
        ui.addCard(h.body, c)
        h.dialog.show()
    }

    private fun browserTab(b: LinearLayout) {
        if (!act.docStart) {
            val w = ui.card()
            val r = ui.row("warn", ui.RED, "WebView is outdated", "The spoofing script applies a moment after a page loads. Update Android System WebView from the Play Store.")
            ui.add(w, r.view, 58)
            b.addView(ui.spacer(12))
            ui.addCard(b, w)
        }

        b.addView(ui.header("Tools"))
        val c2 = ui.card()
        ui.add(c2, ui.row("code", ui.INDIGO, "DevTools", "Console, Network, Cookies, Storage, Info", null, true) {
            closePanel()
            act.devTools.open()
        }.view, 58)
        ui.add(c2, ui.row("check", ui.GREEN, "Profile check", "Check that device, language, timezone and GPS all match", null, true) {
            consistencyDialog()
        }.view, 58)
        ui.addCard(b, c2)

        b.addView(ui.header("Settings"))
        val c3 = ui.card()
        ui.add(c3, ui.switchRow("shield", ui.RED, "Block ads & trackers", "Blocks known ad / analytics hosts. Turn off if a site breaks", store.adBlock) {
            store.adBlock = it
            act.toast("Applies after reloading the page")
        }, 58)
        ui.add(c3, ui.switchRow("block", ui.PINK, "Block IP lookup", "Blocks requests to \"what is my IP?\" services (ipify, ipinfo, etc.)", store.ipBlock) {
            store.ipBlock = it
            act.toast(if (it) "IP lookup blocking on" else "Only a notification will show")
        }, 58)
        ui.add(c3, ui.switchRow("search", ui.ORANGE, "Notify on IP request", "When WebRTC or IP-lookup services are detected", store.ipAlert) { store.ipAlert = it }, 58)
        ui.add(c3, ui.switchRow("globe", ui.INDIGO, "Ask before new sites", "Pick the mode for all data before a site opens", store.preAsk) { store.preAsk = it }, 58)
        ui.add(c3, ui.switchRow("settings", ui.GRAY, "Spoof in Web Workers too", "Turn off if some sites break", store.workerPatch) {
            store.workerPatch = it
            act.toast("Applies after reloading the page")
        }, 58)
        ui.addCard(b, c3)

        b.addView(ui.header("Privacy & Options"))
        val c5 = ui.card()
        ui.add(c5, ui.switchRow("eyeoff", ui.RED, "Screenshot block", "App shows black in screenshots and screen recording", store.screenSecure) {
            store.screenSecure = it
            act.applyOptions()
        }, 58)
        ui.add(c5, ui.switchRow("lock", ui.INDIGO, "App lock", "Asks for the phone PIN / pattern / fingerprint on open", store.appLock) {
            store.appLock = it
            if (it && !act.canLock()) {
                store.appLock = false
                act.toast("Set a screen lock on your phone first")
                render()
            }
        }, 58)
        ui.add(c5, ui.switchRow("trash", ui.ORANGE, "Clear data on open / close", "Cookies, cache and storage are cleared automatically", store.clearOnExit) {
            store.clearOnExit = it
        }, 58)
        ui.add(c5, ui.switchRow("dice", ui.PURPLE, "New canvas fingerprint each launch", "Changes the noise value when the app starts", store.randomNoise) {
            store.randomNoise = it
        }, 58)
        ui.add(c5, ui.switchRow("image", ui.TEAL, "Block images", "Saves data, pages load faster", store.blockImages) {
            store.blockImages = it
            act.applyOptions()
            act.toast("Applies after reloading the page")
        }, 58)
        ui.add(c5, ui.row("textsize", ui.BLUE, "Text size", null, store.textZoom.toString() + "%", true) {
            pickList("Text size", listOf("80", "100", "120", "150", "180").map { Pair(it, "$it%") }, store.textZoom.toString()) { id ->
                store.textZoom = id.toInt()
                act.applyOptions()
                render()
            }
        }.view, 58)
        ui.addCard(b, c5)

        b.addView(ui.header("Data"))
        val c4 = ui.card()
        val clear = ui.row("trash", ui.RED, "Clear cookies, cache & storage", null, null, false, ui.RED) {
            ui.alert(
                act, "Clear everything?", "You will be signed out of logged-in sites.", null,
                listOf(
                    AlertBtn("Clear", AlertBtn.DESTRUCTIVE) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        WebStorage.getInstance().deleteAllData()
                        act.web.clearCache(true)
                        act.web.clearHistory()
                        act.toast("Cleared")
                    },
                    AlertBtn("Cancel", AlertBtn.BOLD) {}
                ),
                true
            )
        }
        ui.add(c4, clear.view, 58)
        ui.addCard(b, c4)
    }

    private fun fieldRow(label: String, value: String, editors: MutableMap<String, android.widget.EditText>, key: String): LinearLayout {
        val l = LinearLayout(act)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(4))
        l.addView(ui.text(label, 12f, ui.GRAY))
        val et = android.widget.EditText(act)
        et.setText(value)
        et.setSingleLine(true)
        et.textSize = 15f
        et.background = null
        et.setPadding(0, ui.dp(4), 0, ui.dp(6))
        editors[key] = et
        l.addView(et)
        return l
    }

    private fun profileTab(b: LinearLayout) {
        val vals = store.get()

        b.addView(ui.header("Device & Country"))
        val c0 = ui.card()
        ui.add(c0, ui.row("phone", ui.BLUE, "Phone", null, store.active, true) {
            pickList("Choose phone", store.names().map { Pair(it, it) }, store.active) { id ->
                store.active = id
                act.applyProfile()
                act.web.reload()
                act.toast("Device: $id")
                render()
            }
        }.view, 58)
        ui.add(c0, ui.row("globe", ui.GREEN, "Country & timezone", null, vals["tz"] ?: "", true) {
            pickList(
                "Choose country",
                Presets.countries.mapIndexed { i, c -> Pair(i.toString(), c.name + "  (" + c.tz + ")") },
                null
            ) { id ->
                val c = Presets.countries[id.toInt()]
                val m = LinkedHashMap<String, String>(store.get())
                m["tz"] = c.tz
                m["lang"] = c.lang
                m["langs"] = c.langs
                m["lat"] = c.lat
                m["lon"] = c.lon
                Fields.sanitize(m)
                store.save(store.active, m)
                act.applyProfile()
                act.web.reload()
                act.toast("Country: " + c.name)
                render()
            }
        }.view, 58)
        ui.add(c0, ui.row("trash", ui.RED, "Delete this profile", null, null, false, ui.RED) {
            store.delete(store.active)
            act.applyProfile()
            render()
        }.view, 58)
        ui.addCard(b, c0)
        b.addView(ui.footer("Each phone has its own UA, screen, GPU and OS. You can also edit the values below."))

        val editors = LinkedHashMap<String, android.widget.EditText>()
        val icons = mapOf(
            "device" to "phone", "screen" to "monitor", "locale" to "globe", "location" to "pin", "network" to "signal",
            "fingerprint" to "fingerprint", "battery" to "battery", "webrtc" to "radio", "sensors" to "compass",
            "clipboard" to "clipboard", "media" to "camera"
        )
        val colors = listOf(ui.BLUE, ui.TEAL, ui.GREEN, ui.RED, ui.INDIGO, ui.PURPLE, ui.ORANGE, ui.PINK, ui.GRAY, ui.BLUE, ui.TEAL)
        var ci = 0
        for ((g, gTitle) in Fields.groupTitles) {
            val fields = Fields.forGroup(g)
            val tileColor = colors[ci % colors.size]
            ci++
            b.addView(ui.header(gTitle))
            val c = ui.card()

            val modeKeys = if (fields.isEmpty()) listOf("ask", "real", "block") else listOf("ask", "fake", "real", "block")
            var cur = store.mode(g)
            if (cur == "fake" && fields.isEmpty()) cur = "block"
            val holder = arrayOfNulls<RowView>(1)
            val mr = ui.row(icons[g] ?: "settings", tileColor, "Mode", null, modeLabel(cur), true) {
                val cv = holder[0]
                if (cv != null) {
                    ui.popupMenu(cv.view, modeKeys.map { Pair(it, modeLabel(it)) }, store.mode(g)) { id ->
                        store.setMode(g, id)
                        cv.value?.text = modeLabel(id)
                    }
                }
            }
            holder[0] = mr
            ui.add(c, mr.view, 58)

            if (fields.isNotEmpty()) {
                val box = LinearLayout(act)
                box.orientation = LinearLayout.VERTICAL
                box.visibility = View.GONE
                for (f in fields) box.addView(fieldRow(f.label, vals[f.key] ?: "", editors, f.key))
                val tg = ui.row(null, 0, "Show / edit values", null, null, true) {
                    box.visibility = if (box.visibility == View.GONE) View.VISIBLE else View.GONE
                }
                ui.add(c, tg.view)
                ui.add(c, box)
            }
            ui.addCard(b, c)
        }

        addPadded(b, ui.bigButton("Save & apply", ui.BLUE, Color.WHITE) {
            val m = LinkedHashMap<String, String>()
            for ((k, e) in editors) m[k] = e.text.toString()
            Fields.sanitize(m)
            store.save(store.active, m)
            act.applyProfile()
            act.web.reload()
            act.toast("Saved")
            render()
        }, 24)
    }

    private fun rulesTab(b: LinearLayout) {
        b.addView(ui.header("Saved site rules"))
        val rules = store.ruleList()
        if (rules.isEmpty()) {
            b.addView(ui.footer("No rules yet. Turn on 'Remember' in a popup and they will appear here."))
            return
        }
        val c = ui.card()
        for ((key, dec) in rules) {
            val parts = key.split("|")
            val origin = parts.getOrElse(0) { key }
            val g = Fields.groupTitles[parts.getOrElse(1) { "" }] ?: parts.getOrElse(1) { "" }
            val r = ui.row(null, 0, origin, g + " → " + act.dLabel(dec), "Delete", false) {
                store.deleteRule(key)
                render()
            }
            r.value?.setTextColor(ui.RED)
            ui.add(c, r.view)
        }
        ui.addCard(b, c)
        addPadded(b, ui.bigButton("Clear all rules", 0xFFFFFFFF.toInt(), ui.RED) {
            store.clearRules()
            render()
        }, 20)
    }

    private fun logTab(b: LinearLayout) {
        b.addView(ui.header("What each site asked (newest first)"))
        val items = GhostLog.all()
        if (items.isEmpty()) {
            b.addView(ui.footer("No requests yet."))
            return
        }
        val c = ui.card()
        for (line in items) {
            val t = ui.text(line, 12f, ui.TEXT)
            t.setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(10))
            ui.add(c, t)
        }
        ui.addCard(b, c)
        addPadded(b, ui.bigButton("Share log", ui.BLUE, Color.WHITE) {
            Export.share(act, "Ghost Browser log", logText())
        }, 20)
        addPadded(b, ui.bigButton("Save to file (Download folder)", 0xFFFFFFFF.toInt(), ui.BLUE) {
            Export.save(act, "ghost-log", logText())
        }, 10)
        addPadded(b, ui.bigButton("Clear log", 0xFFFFFFFF.toInt(), ui.RED) {
            GhostLog.clear()
            render()
        }, 10)
    }

    private fun logText(): String = GhostLog.all().reversed().joinToString("\n\n")

    private fun consistencyDialog() {
        val issues = Consistency.check(store.get())
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        if (issues.isEmpty()) {
            box.addView(ui.text("No mismatches found.", 15f, ui.GREEN, true))
        } else {
            for (s in issues) {
                val t = ui.text("• $s", 14f, ui.TEXT)
                t.setPadding(0, ui.dp(4), 0, ui.dp(4))
                box.addView(t)
            }
        }
        ui.alert(
            act, "Profile check: " + store.active,
            if (issues.isEmpty()) "All values are consistent" else "${issues.size} mismatch(es) — sites may detect them", box,
            listOf(AlertBtn("OK", AlertBtn.BOLD) {}),
            true
        )
    }

    fun askDialog(origin: String, group: String, detail: String, done: (String, Boolean) -> Unit) {
        val fields = Fields.forGroup(group)
        val vals = store.get()
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(4), 0, ui.dp(4), ui.dp(6))

        var remember = false
        val edits = LinkedHashMap<String, android.widget.EditText>()
        if (fields.isNotEmpty()) {
            val fbox = LinearLayout(act)
            fbox.orientation = LinearLayout.VERTICAL
            fbox.visibility = View.GONE
            for (f in fields) fbox.addView(fieldRow(f.label, vals[f.key] ?: "", edits, f.key))
            val tg = ui.text("Show / edit fake values ›", 14f, ui.BLUE)
            tg.setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(8))
            tg.setOnClickListener {
                fbox.visibility = if (fbox.visibility == View.GONE) View.VISIBLE else View.GONE
            }
            box.addView(tg)
            box.addView(fbox)
        }
        box.addView(ui.switchRow(null, 0, "Remember for this site", null, false) { remember = it })

        val btns = ArrayList<AlertBtn>()
        if (fields.isNotEmpty()) {
            btns.add(AlertBtn("Give fake", AlertBtn.BOLD) {
                val merged = LinkedHashMap<String, String>(store.get())
                for ((k, e) in edits) merged[k] = e.text.toString()
                Fields.sanitize(merged)
                store.save(store.active, merged)
                act.applyProfile()
                done("fake", remember)
            })
            btns.add(AlertBtn("Give real", AlertBtn.NORMAL) { done("real", remember) })
        } else {
            btns.add(AlertBtn("Allow", AlertBtn.BOLD) { done("real", remember) })
        }
        btns.add(AlertBtn("Block", AlertBtn.DESTRUCTIVE) { done("block", remember) })

        ui.alert(act, Fields.groupTitles[group] ?: group, "$origin is requesting:\n$detail", box, btns, false)
    }

    fun siteModeDialog(origin: String, then: () -> Unit) {
        fun choose(choice: String) {
            if (choice != "ask") {
                for (g in Fields.groupTitles.keys) {
                    val dec = if (choice == "fake" && Fields.forGroup(g).isEmpty()) "block" else choice
                    store.setRule(origin, g, dec)
                }
            }
            act.markApproved(origin)
            GhostLog.add(act.logLine(origin, "Site mode", "before open", act.dLabel(choice)))
            then()
        }
        ui.alert(
            act, "New site", "$origin — before opening, what should happen to every data request from this site?", null,
            listOf(
                AlertBtn("Ask for each", AlertBtn.BOLD) { choose("ask") },
                AlertBtn("Give fake to all", AlertBtn.NORMAL) { choose("fake") },
                AlertBtn("Give real to all", AlertBtn.NORMAL) { choose("real") },
                AlertBtn("Block everything", AlertBtn.DESTRUCTIVE) { choose("block") },
                AlertBtn("Cancel", AlertBtn.NORMAL) {}
            ),
            false
        )
    }
}
