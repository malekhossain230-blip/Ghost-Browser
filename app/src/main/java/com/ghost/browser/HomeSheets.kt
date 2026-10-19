package com.ghost.browser

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.CycleInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class DevLink(val icon: String, val label: String, val sub: String, val url: String)

class HomeSheets(private val act: Activity, private val ui: Ios, private val store: HomeStore, private val host: HomeHost) {

    companion object {
        const val REQ_ICON = 702
        const val DEV_NAME = "X ZONE BD"
        const val DEV_ROLE = "Android & Web Developer"
        const val DEV_TEXT = "Vaelo is designed and built by X ZONE BD, an independent developer from Bangladesh focused on fast, private and polished Android apps."
        val DEV_LINKS: List<DevLink> = listOf(
            DevLink("user", "Profile", "View full profile", ""),
            DevLink("brand-facebook-f", "Facebook", "Follow X ZONE BD", "https://www.facebook.com/share/18Rh8ds8om"),
            DevLink("brand-telegram", "Telegram", "Updates and support", "https://t.me/x_zone_bangladesh"),
            DevLink("brand-youtube", "YouTube", "Tutorials and demos", "https://youtube.com/@xzone_bd?si=GclYPy7xufi-LS3I"),
            DevLink("envelope", "Email", "Business and feedback", "mailto:xzone.bangladesh@gmail.com")
        )
    }

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val main = Handler(Looper.getMainLooper())

    private var editDialog: Dialog? = null
    private var editBox: IconBox? = null
    private var editBitmap: Bitmap? = null
    private var editUseCustom = false
    private var editUrl: EditText? = null
    private var editName: EditText? = null
    private var editIconTitle: TextView? = null
    private var editIconSub: TextView? = null

    fun versionName(): String {
        return try {
            act.packageManager.getPackageInfo(act.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun drawableId(name: String): Int = act.resources.getIdentifier(name, "drawable", act.packageName)

    private fun sheet(build: (LinearLayout, () -> Unit) -> Unit): Dialog {
        val d = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
        val root = FrameLayout(act)
        val panel = LinearLayout(act)
        panel.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.setColor(ui.SHEET)
        val r = ui.dp(24).toFloat()
        bg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        panel.background = bg
        panel.setPadding(0, ui.dp(10), 0, ui.dp(26))

        val handle = View(act)
        handle.background = ui.rounded(ui.LINE, 3)
        val hlp = LinearLayout.LayoutParams(ui.dp(38), ui.dp(5))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        hlp.bottomMargin = ui.dp(18)
        panel.addView(handle, hlp)

        val scroll = MaxHeightScroll(act, (act.resources.displayMetrics.heightPixels * 0.86f).toInt())
        scroll.isVerticalScrollBarEnabled = false
        val content = LinearLayout(act)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(ui.dp(22), 0, ui.dp(22), 0)
        scroll.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
        panel.addView(scroll, LinearLayout.LayoutParams(MATCH, WRAP))

        root.addView(panel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
        d.setContentView(root)

        var closing = false
        val close: () -> Unit = {
            if (!closing) {
                closing = true
                panel.animate().translationY(panel.height.toFloat()).setDuration(200).withEndAction {
                    try {
                        d.dismiss()
                    } catch (e: Exception) {
                    }
                }.start()
            }
        }
        root.setOnClickListener { close() }
        panel.setOnClickListener { }
        build(content, close)

        ui.glassWindow(d.window, 0.5f)
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.setOnShowListener {
            panel.translationY = ui.dp(600).toFloat()
            panel.animate().translationY(0f).setDuration(300).setInterpolator(DecelerateInterpolator(1.8f)).start()
        }
        d.show()
        return d
    }

    private fun title(parent: LinearLayout, t: String, sub: String? = null) {
        val tv = ui.heavy(t, 18f)
        parent.addView(tv, LinearLayout.LayoutParams(MATCH, WRAP))
        if (sub != null) {
            val s = ui.text(sub, 13f, ui.GRAY)
            val lp = LinearLayout.LayoutParams(MATCH, WRAP)
            lp.topMargin = ui.dp(4)
            parent.addView(s, lp)
        }
        parent.addView(ui.spacer(18))
    }

    private fun fieldLabel(parent: LinearLayout, t: String) {
        val l = ui.text(t.uppercase(), 11.5f, ui.LABEL, true)
        l.letterSpacing = 0.06f
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.bottomMargin = ui.dp(7)
        parent.addView(l, lp)
    }

    private fun field(parent: LinearLayout, hint: String, value: String, uri: Boolean): EditText {
        val et = EditText(act)
        et.setText(value)
        et.hint = hint
        et.setTextColor(ui.TEXT)
        et.setHintTextColor(ui.tint(ui.GRAY, 0.8f))
        et.textSize = 15.5f
        et.setSingleLine(true)
        et.background = ui.glass(ui.SOFT, 13, ui.LINE)
        et.setPadding(ui.dp(14), ui.dp(13), ui.dp(14), ui.dp(13))
        et.inputType = if (uri) EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI
        else EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS
        et.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        et.setOnFocusChangeListener { _, has -> et.background = ui.glass(ui.SOFT, 13, if (has) ui.ACCENT else ui.LINE) }
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.bottomMargin = ui.dp(16)
        parent.addView(et, lp)
        return et
    }

    private fun shake(v: View) {
        v.background = ui.glass(ui.SOFT, 13, ui.RED)
        val a = ObjectAnimator.ofFloat(v, "translationX", 0f, ui.dp(8).toFloat())
        a.duration = 380
        a.interpolator = CycleInterpolator(3f)
        a.start()
    }

    private fun buttonRow(parent: LinearLayout, cancel: String, ok: String, danger: Boolean, onCancel: () -> Unit, onOk: () -> Unit) {
        val row = LinearLayout(act)
        row.orientation = LinearLayout.HORIZONTAL
        val c = ui.ghostButton(cancel) { onCancel() }
        c.minimumHeight = ui.dp(50)
        row.addView(c, LinearLayout.LayoutParams(0, WRAP, 1f))
        val o = ui.bigButton(ok, if (danger) ui.RED else ui.ACCENT, Color.WHITE) { onOk() }
        o.minimumHeight = ui.dp(50)
        val olp = LinearLayout.LayoutParams(0, WRAP, 1f)
        olp.leftMargin = ui.dp(10)
        row.addView(o, olp)
        parent.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    fun engine(onPick: (String) -> Unit) {
        sheet { c, close ->
            title(c, "Search engine", "Used for searches and suggestions")
            val current = store.engine
            val cols = 4
            var row: LinearLayout? = null
            for ((i, e) in Engines.all.withIndex()) {
                if (i % cols == 0) {
                    row = LinearLayout(act)
                    row.orientation = LinearLayout.HORIZONTAL
                    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
                    if (i > 0) lp.topMargin = ui.dp(10)
                    c.addView(row, lp)
                }
                val active = e.key == current
                val opt = LinearLayout(act)
                opt.orientation = LinearLayout.VERTICAL
                opt.gravity = Gravity.CENTER_HORIZONTAL
                opt.setPadding(ui.dp(4), ui.dp(13), ui.dp(4), ui.dp(11))
                val base = GradientDrawable()
                base.setColor(ui.SOFT)
                base.cornerRadius = ui.dp(16).toFloat()
                base.setStroke(if (active) ui.dp(2) else ui.hairPx(), if (active) ui.ACCENT else ui.LINE)
                opt.background = ui.pressableRound(16, base)
                val box = IconBox(act, ui, 38, 12, 24)
                box.bindEngine(e)
                opt.addView(box, LinearLayout.LayoutParams(ui.dp(38), ui.dp(38)))
                val lab = ui.text(e.label, 11.5f, if (active) ui.TEXT else ui.GRAY, true)
                lab.maxLines = 1
                lab.ellipsize = android.text.TextUtils.TruncateAt.END
                lab.gravity = Gravity.CENTER
                val llp = LinearLayout.LayoutParams(MATCH, WRAP)
                llp.topMargin = ui.dp(8)
                opt.addView(lab, llp)
                opt.setOnClickListener {
                    close()
                    onPick(e.key)
                }
                val olp = LinearLayout.LayoutParams(0, WRAP, 1f)
                if (i % cols != 0) olp.leftMargin = ui.dp(10)
                row!!.addView(opt, olp)
            }
            c.addView(ui.spacer(6))
        }
    }

    private fun paintEditIcon() {
        val box = editBox ?: return
        val url = UrlUtil.normalize(editUrl?.text?.toString() ?: "")
        val dom = UrlUtil.domainOf(url)
        val typed = editName?.text?.toString()?.trim() ?: ""
        val label = if (typed.isNotEmpty()) typed else if (dom.isNotEmpty()) UrlUtil.hostLabel(url) else ""
        editName?.hint = if (dom.isNotEmpty()) UrlUtil.hostLabel(url) else "Auto from link"
        val bmp = editBitmap
        if (editUseCustom && bmp != null) {
            editIconTitle?.text = "Custom icon"
            editIconSub?.text = "Chosen from your gallery"
            box.setBitmap(bmp)
            return
        }
        editIconTitle?.text = "Automatic"
        editIconSub?.text = if (dom.isNotEmpty()) "Icon from $dom" else "The site's icon is fetched for you"
        box.bind(label, url, null)
    }

    fun editShortcut(existing: Shortcut?, onSave: (Shortcut) -> Unit) {
        val isNew = existing == null
        editUseCustom = false
        editBitmap = null
        if (existing != null && existing.icon.isNotEmpty()) {
            val f = store.iconFile(existing.icon)
            if (f != null) {
                editBitmap = try {
                    BitmapFactory.decodeFile(f.absolutePath)
                } catch (e: Exception) {
                    null
                }
                editUseCustom = editBitmap != null
            }
        }
        var pickedNew = false
        onIconPickedHook = { bmp ->
            editBitmap = bmp
            editUseCustom = true
            pickedNew = true
            paintEditIcon()
        }

        editDialog = sheet { c, close ->
            title(c, if (isNew) "Add shortcut" else "Edit shortcut", if (isNew) "Paste a link, Vaelo does the rest" else null)

            fieldLabel(c, "Link")
            val url = field(c, "youtube.com", existing?.url ?: "", true)
            editUrl = url

            if (!isNew) {
                fieldLabel(c, "Name")
                val name = field(c, "Auto from link", existing?.name ?: "", false)
                editName = name

                fieldLabel(c, "Icon")
                val row = LinearLayout(act)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.background = ui.glass(ui.SOFT, 14, ui.LINE)
                row.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
                val box = IconBox(act, ui, 52, 15, 32)
                editBox = box
                row.addView(box, LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)))
                val texts = LinearLayout(act)
                texts.orientation = LinearLayout.VERTICAL
                val t1 = ui.text("Automatic", 14.5f, ui.TEXT, true)
                val t2 = ui.text("", 12f, ui.GRAY)
                t2.maxLines = 1
                t2.ellipsize = android.text.TextUtils.TruncateAt.END
                texts.addView(t1)
                texts.addView(t2)
                editIconTitle = t1
                editIconSub = t2
                val tlp = LinearLayout.LayoutParams(0, WRAP, 1f)
                tlp.leftMargin = ui.dp(14)
                tlp.rightMargin = ui.dp(8)
                row.addView(texts, tlp)
                val pick = ImageView(act)
                pick.setImageDrawable(HomeIcons.drawable("image", ui.ACCENT))
                pick.setPadding(ui.dp(11), ui.dp(11), ui.dp(11), ui.dp(11))
                pick.background = ui.pressableRound(11, ui.glass(ui.CARD, 11, ui.LINE))
                pick.setOnClickListener {
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.type = "image/*"
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    try {
                        act.startActivityForResult(Intent.createChooser(i, "Choose an icon"), REQ_ICON)
                    } catch (e: Exception) {
                        host.showToast("No gallery app found")
                    }
                }
                row.addView(pick, LinearLayout.LayoutParams(ui.dp(38), ui.dp(38)))
                val reset = ImageView(act)
                reset.setImageDrawable(HomeIcons.drawable("rotate-left", ui.ACCENT))
                reset.setPadding(ui.dp(11), ui.dp(11), ui.dp(11), ui.dp(11))
                reset.background = ui.pressableRound(11, ui.glass(ui.CARD, 11, ui.LINE))
                reset.setOnClickListener {
                    if (!editUseCustom) {
                        host.showToast("Automatic icon is already in use")
                    } else {
                        editUseCustom = false
                        editBitmap = null
                        paintEditIcon()
                    }
                }
                val rlp = LinearLayout.LayoutParams(ui.dp(38), ui.dp(38))
                rlp.leftMargin = ui.dp(6)
                row.addView(reset, rlp)
                val rowLp = LinearLayout.LayoutParams(MATCH, WRAP)
                rowLp.bottomMargin = ui.dp(20)
                c.addView(row, rowLp)

                var previewRun: Runnable? = null
                url.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        url.background = ui.glass(ui.SOFT, 13, if (url.hasFocus()) ui.ACCENT else ui.LINE)
                        val p = previewRun
                        if (p != null) main.removeCallbacks(p)
                        val r = Runnable { paintEditIcon() }
                        previewRun = r
                        main.postDelayed(r, 350)
                    }
                })
                name.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (!editUseCustom) paintEditIcon()
                    }
                })
                paintEditIcon()
            } else {
                editName = null
                editBox = null
                editIconTitle = null
                editIconSub = null
                url.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        url.background = ui.glass(ui.SOFT, 13, if (url.hasFocus()) ui.ACCENT else ui.LINE)
                    }
                })
            }

            buttonRow(c, "Cancel", "Save", false, { close() }) {
                val raw = url.text.toString()
                if (!UrlUtil.isValidSite(raw)) {
                    shake(url)
                    url.requestFocus()
                    return@buttonRow
                }
                val u = UrlUtil.normalize(raw)
                val typed = editName?.text?.toString()?.trim() ?: ""
                val nm = if (isNew) UrlUtil.hostLabel(u) else if (typed.isNotEmpty()) typed else UrlUtil.hostLabel(u)
                var icon = existing?.icon ?: ""
                val bmp = editBitmap
                if (!isNew) {
                    if (editUseCustom && bmp != null) {
                        if (pickedNew) {
                            if (icon.isNotEmpty()) store.deleteIcon(icon)
                            icon = store.saveIcon(bmp)
                        }
                    } else {
                        if (icon.isNotEmpty()) store.deleteIcon(icon)
                        icon = ""
                    }
                }
                close()
                onSave(Shortcut(nm, u, icon))
                host.showToast(if (isNew) "Shortcut added" else "Shortcut updated")
            }
            c.addView(ui.spacer(4))

            main.postDelayed({
                url.requestFocus()
                val imm = act.getSystemService(Activity.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(url, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }, 260)
        }
        editDialog?.setOnDismissListener {
            editDialog = null
            editBox = null
            editUrl = null
            editName = null
            editIconTitle = null
            editIconSub = null
            onIconPickedHook = null
        }
    }

    private var onIconPickedHook: ((Bitmap) -> Unit)? = null

    fun onIconPicked(uri: Uri?) {
        if (uri == null || editDialog == null) return
        val bmp = try {
            act.contentResolver.openInputStream(uri)?.use { input ->
                val opts = BitmapFactory.Options()
                opts.inJustDecodeBounds = true
                BitmapFactory.decodeStream(input, null, opts)
                var sample = 1
                val w = opts.outWidth
                val h = opts.outHeight
                while (w / sample > 512 || h / sample > 512) sample *= 2
                act.contentResolver.openInputStream(uri)?.use { second ->
                    val o2 = BitmapFactory.Options()
                    o2.inSampleSize = sample
                    BitmapFactory.decodeStream(second, null, o2)
                }
            }
        } catch (e: Exception) {
            null
        }
        if (bmp == null) {
            host.showToast("Couldn't read that image")
            return
        }
        val s = Math.min(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - s) / 2, (bmp.height - s) / 2, s, s)
        val scaled = Bitmap.createScaledBitmap(square, 128, 128, true)
        onIconPickedHook?.invoke(scaled)
    }

    fun confirm(title: String, text: String, ok: String, danger: Boolean, icon: String, onOk: () -> Unit) {
        ui.alert(
            act, title, text, null,
            listOf(
                AlertBtn("Cancel", AlertBtn.NORMAL) {},
                AlertBtn(ok, if (danger) AlertBtn.DESTRUCTIVE else AlertBtn.BOLD) { onOk() }
            ),
            true, icon, danger
        )
    }

    private fun linkRow(l: DevLink, close: () -> Unit): LinearLayout {
        val row = LinearLayout(act)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(ui.dp(13), ui.dp(12), ui.dp(13), ui.dp(12))
        row.background = ui.pressableRound(14, ui.glass(ui.SOFT, 14, ui.LINE))
        val badge = ImageView(act)
        badge.setImageDrawable(HomeIcons.drawable(l.icon, ui.ACCENT))
        badge.background = ui.rounded(ui.ACCENT_SOFT, 10)
        badge.setPadding(ui.dp(9), ui.dp(9), ui.dp(9), ui.dp(9))
        val blp = LinearLayout.LayoutParams(ui.dp(34), ui.dp(34))
        blp.rightMargin = ui.dp(13)
        row.addView(badge, blp)
        val texts = LinearLayout(act)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(ui.text(l.label, 14.5f, ui.TEXT, true))
        val sub = ui.text(l.sub, 12f, ui.GRAY)
        sub.maxLines = 1
        sub.ellipsize = android.text.TextUtils.TruncateAt.END
        texts.addView(sub)
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
        val ext = ImageView(act)
        ext.setImageDrawable(HomeIcons.drawable("arrow-up-right-from-square", ui.LABEL))
        row.addView(ext, LinearLayout.LayoutParams(ui.dp(12), ui.dp(12)))
        row.setOnClickListener {
            close()
            if (l.url.startsWith("mailto:")) {
                try {
                    act.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(l.url)))
                } catch (e: Exception) {
                    host.showToast("No email app found")
                }
            } else {
                host.openSite(l.url)
            }
        }
        return row
    }

    fun developer() {
        sheet { c, close ->
            val head = LinearLayout(act)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            val avatar = FrameLayout(act)
            avatar.background = ui.gradient3(22)
            avatar.clipToOutline = true
            avatar.elevation = ui.dp(6).toFloat()
            val id = drawableId("developer")
            if (id != 0) {
                val img = ImageView(act)
                img.setImageResource(id)
                img.scaleType = ImageView.ScaleType.CENTER_CROP
                avatar.addView(img, FrameLayout.LayoutParams(MATCH, MATCH))
            } else {
                val ic = ImageView(act)
                ic.setImageDrawable(HomeIcons.drawable("code", Color.WHITE))
                avatar.addView(ic, FrameLayout.LayoutParams(ui.dp(28), ui.dp(28), Gravity.CENTER))
            }
            val alp = LinearLayout.LayoutParams(ui.dp(68), ui.dp(68))
            alp.rightMargin = ui.dp(16)
            head.addView(avatar, alp)
            val texts = LinearLayout(act)
            texts.orientation = LinearLayout.VERTICAL
            val n = ui.heavy(DEV_NAME, 20f)
            n.letterSpacing = 0.08f
            texts.addView(n)
            val r = ui.text(DEV_ROLE, 13f, ui.GRAY)
            val rlp = LinearLayout.LayoutParams(WRAP, WRAP)
            rlp.topMargin = ui.dp(3)
            texts.addView(r, rlp)
            head.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
            val hlp = LinearLayout.LayoutParams(MATCH, WRAP)
            hlp.bottomMargin = ui.dp(18)
            c.addView(head, hlp)

            val about = ui.text(DEV_TEXT, 14f, ui.GRAY)
            about.setLineSpacing(ui.dp(4).toFloat(), 1f)
            val tlp = LinearLayout.LayoutParams(MATCH, WRAP)
            tlp.bottomMargin = ui.dp(18)
            c.addView(about, tlp)

            var any = false
            for (l in DEV_LINKS) {
                if (l.url.isEmpty()) continue
                any = true
                val lp = LinearLayout.LayoutParams(MATCH, WRAP)
                lp.bottomMargin = ui.dp(8)
                c.addView(linkRow(l, close), lp)
            }
            if (!any) {
                val t = ui.text("Contact links coming soon", 13f, ui.LABEL)
                t.gravity = Gravity.CENTER
                t.setPadding(0, ui.dp(14), 0, ui.dp(14))
                c.addView(t, LinearLayout.LayoutParams(MATCH, WRAP))
            }
            c.addView(ui.spacer(6))
        }
    }

    private fun feature(parent: LinearLayout, icon: String, title: String, sub: String) {
        val sep = View(act)
        sep.setBackgroundColor(ui.LINE)
        parent.addView(sep, LinearLayout.LayoutParams(MATCH, ui.hairPx()))
        val row = LinearLayout(act)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, ui.dp(12), 0, ui.dp(12))
        val ic = ImageView(act)
        ic.setImageDrawable(HomeIcons.drawable(icon, ui.ACCENT))
        val ilp = LinearLayout.LayoutParams(ui.dp(18), ui.dp(18))
        ilp.rightMargin = ui.dp(13)
        ilp.topMargin = ui.dp(2)
        row.addView(ic, ilp)
        val texts = LinearLayout(act)
        texts.orientation = LinearLayout.VERTICAL
        texts.addView(ui.medium(title, 14.5f, ui.TEXT))
        val s = ui.text(sub, 12.5f, ui.GRAY)
        s.setPadding(0, ui.dp(2), 0, 0)
        texts.addView(s)
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP, 1f))
        parent.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    fun about() {
        sheet { c, _ ->
            val logoId = drawableId(if (ui.dark) "home_logo_dark" else "home_logo")
            val fallbackId = drawableId("home_logo")
            val useId = if (logoId != 0) logoId else fallbackId
            if (useId != 0) {
                val img = ImageView(act)
                img.setImageResource(useId)
                img.scaleType = ImageView.ScaleType.FIT_START
                img.adjustViewBounds = true
                c.addView(img, LinearLayout.LayoutParams(WRAP, ui.dp(48)))
            } else {
                val head = LinearLayout(act)
                head.orientation = LinearLayout.HORIZONTAL
                head.gravity = Gravity.CENTER_VERTICAL
                val logo = ImageView(act)
                logo.setImageDrawable(HomeIcons.drawable("shield-halved", Color.WHITE))
                logo.setPadding(ui.dp(15), ui.dp(15), ui.dp(15), ui.dp(15))
                logo.background = ui.gradient3(16)
                val llp = LinearLayout.LayoutParams(ui.dp(56), ui.dp(56))
                llp.rightMargin = ui.dp(14)
                head.addView(logo, llp)
                head.addView(ui.heavy("Vaelo", 22f))
                c.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))
            }
            val v = versionName()
            val sub = ui.text("Private browsing, your way" + (if (v.isNotEmpty()) " - v$v" else ""), 13f, ui.GRAY)
            val slp = LinearLayout.LayoutParams(MATCH, WRAP)
            slp.topMargin = ui.dp(11)
            slp.bottomMargin = ui.dp(16)
            c.addView(sub, slp)

            val text = ui.text(
                "Vaelo is a privacy-first browser that controls exactly what every website can learn about your device. " +
                    "Spoof your identity, block trackers, and browse without leaving a fingerprint.", 14f, ui.GRAY
            )
            text.setLineSpacing(ui.dp(4).toFloat(), 1f)
            val tlp = LinearLayout.LayoutParams(MATCH, WRAP)
            tlp.bottomMargin = ui.dp(16)
            c.addView(text, tlp)

            feature(c, "user-secret", "Device spoofing", "Send fake or edited device, screen, GPU and locale data")
            feature(c, "shield-halved", "Permission control", "Decide per site what to share: real, fake, or blocked")
            feature(c, "ban", "Ad and tracker blocking", "Blocks over 73,000 known ad, tracker and scam domains")
            feature(c, "fingerprint", "Anti-fingerprint", "Canvas, WebGL and WebRTC protection built in")
            c.addView(ui.spacer(6))
        }
    }
}
