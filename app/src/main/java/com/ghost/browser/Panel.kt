package com.ghost.browser

import android.graphics.Color
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** iOS ধাঁচের নিচ থেকে ওঠা প্যানেল + সাইটের পপআপগুলো। */
class Panel(private val act: MainActivity, private val store: Store, private val ui: Ios) {
    private var handle: SheetHandle? = null
    private var tab = 0

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    // =====================================================================
    //  প্যানেল
    // =====================================================================
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
        val seg = ui.segmented(listOf("ব্রাউজার", "প্রোফাইল", "নিয়ম", "লগ"), tab) { i ->
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
        "ask" -> "প্রতিবার জিজ্ঞেস"
        "fake" -> "সবসময় নকল"
        "real" -> "সবসময় আসল"
        "block" -> "সবসময় ব্লক"
        else -> k
    }

    private fun pickList(title: String, items: List<Pair<String, String>>, current: String?, onPick: (String) -> Unit) {
        val h = ui.sheet(act, title, 0.7f)
        val c = ui.card()
        for ((id, label) in items) {
            val r = ui.row(null, 0, label, null, if (id == current) "✓" else null, false, Color.BLACK) {
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

    // ---------------- ব্রাউজার ট্যাব ----------------
    private fun navCell(icon: String, action: () -> Unit): android.widget.FrameLayout {
        val cell = android.widget.FrameLayout(act)
        cell.minimumHeight = ui.dp(52)
        val iv = ui.iconView(icon, ui.BLUE, 24)
        cell.addView(iv, android.widget.FrameLayout.LayoutParams(ui.dp(24), ui.dp(24), android.view.Gravity.CENTER))
        cell.setOnClickListener {
            action()
            closePanel()
        }
        return cell
    }

    private fun browserTab(b: LinearLayout) {
        if (!act.docStart) {
            val w = ui.card()
            val r = ui.row("warn", ui.RED, "ফোনের WebView পুরনো", "স্পুফিং পেজ লোডের একটু পরে প্রয়োগ হচ্ছে। Play Store থেকে Android System WebView আপডেট করুন।")
            ui.add(w, r.view, 58)
            b.addView(ui.spacer(12))
            ui.addCard(b, w)
        }

        b.addView(ui.header("ঠিকানা"))
        val c1 = ui.card()
        val et = EditText(act)
        et.setSingleLine(true)
        et.hint = "সার্চ করুন বা ঠিকানা লিখুন"
        et.textSize = 16f
        et.background = null
        et.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        et.imeOptions = EditorInfo.IME_ACTION_GO
        et.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_GO) {
                closePanel()
                act.go(et.text.toString())
                true
            } else false
        }
        ui.add(c1, et)
        ui.addCard(b, c1)
        addPadded(b, ui.bigButton("খুলুন", ui.BLUE, Color.WHITE) {
            closePanel()
            act.go(et.text.toString())
        })

        b.addView(ui.header("নেভিগেশন"))
        val nav = LinearLayout(act)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.background = ui.rounded(Color.WHITE, 12)
        nav.addView(navCell("back") { if (act.web.canGoBack()) act.web.goBack() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        nav.addView(navCell("forward") { if (act.web.canGoForward()) act.web.goForward() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        nav.addView(navCell("refresh") { act.web.reload() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        nav.addView(navCell("home") { act.goHome() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        addPadded(b, nav, 0)

        b.addView(ui.header("টুলস"))
        val c2 = ui.card()
        ui.add(c2, ui.row("code", ui.INDIGO, "DevTools", "Console, Network, কুকি, Storage, Info", null, true) {
            closePanel()
            act.devTools.open()
        }.view, 58)
        ui.add(c2, ui.row("check", ui.GREEN, "প্রোফাইল যাচাই", "ফোন, ভাষা, টাইমজোন ও GPS মেলে কিনা দেখুন", null, true) {
            consistencyDialog()
        }.view, 58)
        ui.addCard(b, c2)

        b.addView(ui.header("সেটিংস"))
        val c3 = ui.card()
        ui.add(c3, ui.switchRow("shield", ui.RED, "বিজ্ঞাপন ও ট্র্যাকার ব্লক", "পরিচিত বিজ্ঞাপন/অ্যানালিটিক্স ঠিকানা আটকায়। সাইট না চললে বন্ধ করুন", store.adBlock) {
            store.adBlock = it
            act.toast("পেজ রিলোড করলে প্রযোজ্য হবে")
        }, 58)
        ui.add(c3, ui.switchRow("block", ui.PINK, "IP-লুকআপ ব্লক", "ipify, ipinfo ইত্যাদি \"আমার IP কী?\" সার্ভিসে রিকোয়েস্ট আটকায়", store.ipBlock) {
            store.ipBlock = it
            act.toast(if (it) "IP-লুকআপ ব্লক চালু" else "শুধু নোটিফিকেশন আসবে")
        }, 58)
        ui.add(c3, ui.switchRow("search", ui.ORANGE, "IP চাইলে নোটিফিকেশন", "WebRTC ও IP-লুকআপ সার্ভিস ধরা পড়লে", store.ipAlert) { store.ipAlert = it }, 58)
        ui.add(c3, ui.switchRow("globe", ui.INDIGO, "নতুন সাইটের আগে জিজ্ঞেস", "সাইট খোলার আগে সব তথ্যের মোড বেছে নিন", store.preAsk) { store.preAsk = it }, 58)
        ui.add(c3, ui.switchRow("settings", ui.GRAY, "Web Worker-এও নকল", "কিছু সাইট না চললে বন্ধ করুন", store.workerPatch) {
            store.workerPatch = it
            act.toast("পেজ রিলোড করলে প্রযোজ্য হবে")
        }, 58)
        ui.addCard(b, c3)

        b.addView(ui.header("প্রাইভেসি ও অপশন"))
        val c5 = ui.card()
        ui.add(c5, ui.switchRow("eyeoff", ui.RED, "স্ক্রিনশট ব্লক", "স্ক্রিনশট ও স্ক্রিন রেকর্ডে অ্যাপ কালো দেখাবে", store.screenSecure) {
            store.screenSecure = it
            act.applyOptions()
        }, 58)
        ui.add(c5, ui.switchRow("lock", ui.INDIGO, "অ্যাপ লক", "খোলার সময় ফোনের পিন / প্যাটার্ন / আঙুলের ছাপ চাইবে", store.appLock) {
            store.appLock = it
            if (it && !act.canLock()) {
                store.appLock = false
                act.toast("আগে ফোনে স্ক্রিন লক (পিন/প্যাটার্ন) সেট করুন")
                render()
            }
        }, 58)
        ui.add(c5, ui.switchRow("trash", ui.ORANGE, "অ্যাপ খুললে/বন্ধ হলে ডেটা মুছুক", "কুকি, ক্যাশ ও স্টোরেজ নিজে থেকে মুছে যাবে", store.clearOnExit) {
            store.clearOnExit = it
        }, 58)
        ui.add(c5, ui.switchRow("dice", ui.PURPLE, "প্রতিবার নতুন Canvas ফিঙ্গারপ্রিন্ট", "অ্যাপ চালুর সময় নয়েজ সংখ্যা বদলায়", store.randomNoise) {
            store.randomNoise = it
        }, 58)
        ui.add(c5, ui.switchRow("image", ui.TEAL, "ছবি বন্ধ", "ডেটা বাঁচায়, পেজ দ্রুত খোলে", store.blockImages) {
            store.blockImages = it
            act.applyOptions()
            act.toast("পেজ রিলোড করলে প্রযোজ্য হবে")
        }, 58)
        ui.add(c5, ui.row("textsize", ui.BLUE, "লেখার সাইজ", null, store.textZoom.toString() + "%", true) {
            pickList("লেখার সাইজ", listOf("80", "100", "120", "150", "180").map { Pair(it, "$it%") }, store.textZoom.toString()) { id ->
                store.textZoom = id.toInt()
                act.applyOptions()
                render()
            }
        }.view, 58)
        ui.add(c5, ui.row("globe", ui.GREEN, "প্রক্সি", "আসল IP বদলাতে নিজের প্রক্সি দিন", if (store.proxy.isEmpty()) "বন্ধ" else store.proxy, true) {
            proxyDialog()
        }.view, 58)
        ui.addCard(b, c5)

        b.addView(ui.header("ডেটা"))
        val c4 = ui.card()
        val clear = ui.row("trash", ui.RED, "কুকি, ক্যাশ ও স্টোরেজ মুছুন", null, null, false, ui.RED) {
            ui.alert(
                act, "সব মুছে ফেলবেন?", "লগইন করা সাইট থেকে বেরিয়ে যাবেন।", null,
                listOf(
                    AlertBtn("মুছুন", AlertBtn.DESTRUCTIVE) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        WebStorage.getInstance().deleteAllData()
                        act.web.clearCache(true)
                        act.web.clearHistory()
                        act.toast("মুছে ফেলা হয়েছে")
                    },
                    AlertBtn("বাতিল", AlertBtn.BOLD) {}
                ),
                true
            )
        }
        ui.add(c4, clear.view, 58)
        ui.addCard(b, c4)
    }

    // ---------------- প্রোফাইল ট্যাব ----------------
    private fun fieldRow(label: String, value: String, editors: MutableMap<String, EditText>, key: String): LinearLayout {
        val l = LinearLayout(act)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(4))
        l.addView(ui.text(label, 12f, ui.GRAY))
        val et = EditText(act)
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

        b.addView(ui.header("ডিভাইস ও দেশ"))
        val c0 = ui.card()
        ui.add(c0, ui.row("phone", ui.BLUE, "ফোন", null, store.active, true) {
            pickList("ফোন বেছে নিন", store.names().map { Pair(it, it) }, store.active) { id ->
                store.active = id
                act.applyProfile()
                act.web.reload()
                act.toast("ডিভাইস: $id")
                render()
            }
        }.view, 58)
        ui.add(c0, ui.row("globe", ui.GREEN, "দেশ ও টাইমজোন", null, vals["tz"] ?: "", true) {
            pickList(
                "দেশ বেছে নিন",
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
                act.toast("দেশ: " + c.name)
                render()
            }
        }.view, 58)
        ui.add(c0, ui.row("plus", ui.ORANGE, "নতুন প্রোফাইল", null, null, true) { newProfileDialog() }.view, 58)
        ui.add(c0, ui.row("trash", ui.RED, "এই প্রোফাইল মুছুন", null, null, false, ui.RED) {
            store.delete(store.active)
            act.applyProfile()
            render()
        }.view, 58)
        ui.addCard(b, c0)
        b.addView(ui.footer("প্রতিটি ফোনে নিজস্ব UA, স্ক্রিন, GPU ও OS আছে। নিচের মান এডিটও করা যায়।"))

        val editors = LinkedHashMap<String, EditText>()
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
            val mr = ui.row(icons[g] ?: "settings", tileColor, "মোড", null, modeLabel(cur), true) {
                val now = store.mode(g)
                pickList(gTitle, modeKeys.map { Pair(it, modeLabel(it)) }, now) { id ->
                    store.setMode(g, id)
                    holder[0]?.value?.text = modeLabel(id)
                }
            }
            holder[0] = mr
            ui.add(c, mr.view, 58)

            if (fields.isNotEmpty()) {
                val box = LinearLayout(act)
                box.orientation = LinearLayout.VERTICAL
                box.visibility = View.GONE
                for (f in fields) box.addView(fieldRow(f.label, vals[f.key] ?: "", editors, f.key))
                val tg = ui.row(null, 0, "মান দেখুন / এডিট করুন", null, null, true) {
                    box.visibility = if (box.visibility == View.GONE) View.VISIBLE else View.GONE
                }
                ui.add(c, tg.view)
                ui.add(c, box)
            }
            ui.addCard(b, c)
        }

        addPadded(b, ui.bigButton("সেভ ও প্রয়োগ করুন", ui.BLUE, Color.WHITE) {
            val m = LinkedHashMap<String, String>()
            for ((k, e) in editors) m[k] = e.text.toString()
            val notes = Fields.sanitize(m)
            store.save(store.active, m)
            act.applyProfile()
            act.web.reload()
            var msg = "সেভ হয়েছে"
            if (notes.isNotEmpty()) msg += "\n" + notes.joinToString("\n")
            act.toast(msg)
            render()
        }, 24)
    }

    private fun newProfileDialog() {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        val et = EditText(act)
        et.hint = "প্রোফাইলের নাম"
        et.setSingleLine(true)
        et.textSize = 16f
        et.background = ui.rounded(Color.WHITE, 8)
        et.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        box.addView(et)
        ui.alert(
            act, "নতুন প্রোফাইল", "বর্তমান প্রোফাইলের মান কপি হবে, তারপর এডিট করুন।", box,
            listOf(
                AlertBtn("তৈরি করুন", AlertBtn.BOLD) {
                    val n = et.text.toString().trim()
                    if (n.isNotEmpty()) {
                        store.save(n, store.get())
                        store.active = n
                        act.applyProfile()
                        render()
                    }
                },
                AlertBtn("বাতিল", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    // ---------------- নিয়ম ট্যাব ----------------
    private fun rulesTab(b: LinearLayout) {
        b.addView(ui.header("সেভ করা সাইট-নিয়ম"))
        val rules = store.ruleList()
        if (rules.isEmpty()) {
            b.addView(ui.footer("এখনো কোনো নিয়ম নেই। পপআপে 'মনে রাখুন' চালু করলে এখানে আসবে।"))
            return
        }
        val c = ui.card()
        for ((key, dec) in rules) {
            val parts = key.split("|")
            val origin = parts.getOrElse(0) { key }
            val g = Fields.groupTitles[parts.getOrElse(1) { "" }] ?: parts.getOrElse(1) { "" }
            val r = ui.row(null, 0, origin, g + " → " + act.dLabel(dec), "মুছুন", false) {
                store.deleteRule(key)
                render()
            }
            r.value?.setTextColor(ui.RED)
            ui.add(c, r.view)
        }
        ui.addCard(b, c)
        addPadded(b, ui.bigButton("সব নিয়ম মুছুন", 0xFFFFFFFF.toInt(), ui.RED) {
            store.clearRules()
            render()
        }, 20)
    }

    // ---------------- লগ ট্যাব ----------------
    private fun logTab(b: LinearLayout) {
        b.addView(ui.header("কোন সাইট কী চেয়েছে (নতুন আগে)"))
        val items = GhostLog.all()
        if (items.isEmpty()) {
            b.addView(ui.footer("এখনো কোনো অনুরোধ আসেনি।"))
            return
        }
        val c = ui.card()
        for (line in items) {
            val t = ui.text(line, 12f, Color.BLACK)
            t.setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(10))
            ui.add(c, t)
        }
        ui.addCard(b, c)
        addPadded(b, ui.bigButton("লগ শেয়ার করুন", ui.BLUE, Color.WHITE) {
            Export.share(act, "Ghost Browser log", logText())
        }, 20)
        addPadded(b, ui.bigButton("ফাইলে সেভ (Download ফোল্ডার)", 0xFFFFFFFF.toInt(), ui.BLUE) {
            Export.save(act, "ghost-log", logText())
        }, 10)
        addPadded(b, ui.bigButton("লগ মুছুন", 0xFFFFFFFF.toInt(), ui.RED) {
            GhostLog.clear()
            render()
        }, 10)
    }

    private fun proxyDialog() {
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        val et = EditText(act)
        et.setText(store.proxy)
        et.hint = "http://host:8080  বা  socks5://host:1080"
        et.setSingleLine(true)
        et.textSize = 15f
        et.background = ui.rounded(Color.WHITE, 8)
        et.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10))
        box.addView(et)
        ui.alert(
            act, "প্রক্সি",
            "খালি রাখলে বন্ধ। ইউজারনেম/পাসওয়ার্ড লাগে এমন প্রক্সি এখানে চলবে না। শুধু অ্যাপের ব্রাউজিং প্রক্সি দিয়ে যাবে।", box,
            listOf(
                AlertBtn("সেভ", AlertBtn.BOLD) {
                    store.proxy = et.text.toString().trim()
                    act.applyProxy()
                    act.web.reload()
                    render()
                },
                AlertBtn("বাতিল", AlertBtn.NORMAL) {}
            ),
            true
        )
    }

    private fun logText(): String = GhostLog.all().reversed().joinToString("\n\n")

    private fun consistencyDialog() {
        val issues = Consistency.check(store.get())
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(12))
        if (issues.isEmpty()) {
            box.addView(ui.text("কোনো অমিল পাওয়া যায়নি।", 15f, ui.GREEN, true))
        } else {
            for (s in issues) {
                val t = ui.text("• $s", 14f, Color.BLACK)
                t.setPadding(0, ui.dp(4), 0, ui.dp(4))
                box.addView(t)
            }
        }
        ui.alert(
            act, "প্রোফাইল যাচাই: " + store.active,
            if (issues.isEmpty()) "সব মান পরস্পরের সাথে মিলছে" else "${issues.size}টি অমিল — সাইট এগুলো ধরে ফেলতে পারে", box,
            listOf(AlertBtn("ঠিক আছে", AlertBtn.BOLD) {}),
            true
        )
    }

    // =====================================================================
    //  সাইটের পপআপ
    // =====================================================================
    fun askDialog(origin: String, group: String, detail: String, done: (String, Boolean) -> Unit) {
        val fields = Fields.forGroup(group)
        val vals = store.get()
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(ui.dp(4), 0, ui.dp(4), ui.dp(6))

        var remember = false
        val edits = LinkedHashMap<String, EditText>()
        if (fields.isNotEmpty()) {
            val fbox = LinearLayout(act)
            fbox.orientation = LinearLayout.VERTICAL
            fbox.visibility = View.GONE
            for (f in fields) fbox.addView(fieldRow(f.label, vals[f.key] ?: "", edits, f.key))
            val tg = ui.text("নকল মান দেখুন / এডিট করুন ›", 14f, ui.BLUE)
            tg.setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(8))
            tg.setOnClickListener {
                fbox.visibility = if (fbox.visibility == View.GONE) View.VISIBLE else View.GONE
            }
            box.addView(tg)
            box.addView(fbox)
        }
        box.addView(ui.switchRow(null, 0, "এই সাইটের জন্য মনে রাখুন", null, false) { remember = it })

        val btns = ArrayList<AlertBtn>()
        if (fields.isNotEmpty()) {
            btns.add(AlertBtn("নকল দিন", AlertBtn.BOLD) {
                val merged = LinkedHashMap<String, String>(store.get())
                for ((k, e) in edits) merged[k] = e.text.toString()
                val notes = Fields.sanitize(merged)
                store.save(store.active, merged)
                act.applyProfile()
                if (notes.isNotEmpty()) act.toast(notes.joinToString("\n"))
                done("fake", remember)
            })
            btns.add(AlertBtn("আসল দিন", AlertBtn.NORMAL) { done("real", remember) })
        } else {
            btns.add(AlertBtn("অনুমতি দিন", AlertBtn.BOLD) { done("real", remember) })
        }
        btns.add(AlertBtn("ব্লক", AlertBtn.DESTRUCTIVE) { done("block", remember) })

        ui.alert(act, Fields.groupTitles[group] ?: group, "$origin এই তথ্য চাইছে:\n$detail", box, btns, false)
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
            GhostLog.add(act.logLine(origin, "সাইট মোড", "খোলার আগে", act.dLabel(choice)))
            then()
        }
        ui.alert(
            act, "নতুন সাইট", "$origin খোলার আগে এই সাইটের সব তথ্য-অনুরোধে কী হবে?", null,
            listOf(
                AlertBtn("প্রতিটায় আলাদা জিজ্ঞেস করো", AlertBtn.BOLD) { choose("ask") },
                AlertBtn("সবকিছুতে নকল দাও", AlertBtn.NORMAL) { choose("fake") },
                AlertBtn("সবকিছুতে আসল দাও", AlertBtn.NORMAL) { choose("real") },
                AlertBtn("সবকিছু ব্লক করো", AlertBtn.DESTRUCTIVE) { choose("block") },
                AlertBtn("বাতিল", AlertBtn.NORMAL) {}
            ),
            false
        )
    }
}
