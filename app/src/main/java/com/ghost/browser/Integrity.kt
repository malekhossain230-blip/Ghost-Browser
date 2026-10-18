package com.ghost.browser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.PathParser
import java.security.MessageDigest

object Integrity {

    private const val EXPECTED = "55607433645C927149336B91CCE2B69A4A52709AA731342E6DBC0ADF45EF6590"
    private const val OFFICIAL = "https://t.me/x_zone_bangladesh"

    fun hashes(ctx: Context): List<String> {
        val out = ArrayList<String>()
        try {
            val pm = ctx.packageManager
            val sigs: Array<android.content.pm.Signature>? = if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val si = info.signingInfo
                if (si == null) null
                else if (si.hasMultipleSigners()) si.apkContentsSigners
                else si.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures
            }
            if (sigs == null) return out
            val md = MessageDigest.getInstance("SHA-256")
            for (s in sigs) {
                val d = md.digest(s.toByteArray())
                val sb = StringBuilder(64)
                for (b in d) sb.append(String.format("%02X", b))
                out.add(sb.toString())
            }
        } catch (e: Exception) {
        }
        return out
    }

    fun verify(ctx: Context): Boolean {
        val want = EXPECTED
        val have = hashes(ctx)
        if (have.isEmpty()) return false
        for (h in have) if (h == want) return true
        return false
    }

    fun blockScreen(act: Activity): View {
        val ui = Ios(act)
        val dp = { v: Int -> ui.dp(v) }
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

        val root = FrameLayout(act)
        root.setBackgroundColor(ui.BG)

        val col = LinearLayout(act)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.setPadding(dp(32), dp(24), dp(32), dp(24))

        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setColor(if (ui.dark) 0x1FFF6A62 else 0x14E5453C)
        val icon = ImageView(act)
        icon.background = ring
        val shield = PathParser.createPathFromPathData(
            "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z M9.5 9.5l5 5 M14.5 9.5l-5 5"
        )
        icon.setImageDrawable(IconDrawable(shield, ui.RED, 2f))
        icon.setPadding(dp(26), dp(26), dp(26), dp(26))
        val ilp = LinearLayout.LayoutParams(dp(104), dp(104))
        ilp.bottomMargin = dp(28)
        col.addView(icon, ilp)

        val title = ui.text("This copy of Vaelo was modified", 22f, ui.TEXT, true)
        title.gravity = Gravity.CENTER
        title.letterSpacing = -0.02f
        col.addView(title, LinearLayout.LayoutParams(match, wrap))

        val body = ui.text(
            "The app's signature does not match the official release, so it cannot be opened. " +
                "Please remove it and install Vaelo from the official source.",
            14.5f, ui.GRAY
        )
        body.gravity = Gravity.CENTER
        body.setLineSpacing(dp(4).toFloat(), 1f)
        val blp = LinearLayout.LayoutParams(match, wrap)
        blp.topMargin = dp(12)
        blp.bottomMargin = dp(30)
        col.addView(body, blp)

        val get = ui.bigButton("Get the official app", ui.ACC2, Color.WHITE) {
            try {
                act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OFFICIAL)))
            } catch (e: Exception) {
            }
        }
        col.addView(get, LinearLayout.LayoutParams(match, wrap))

        val quit = ui.ghostButton("Close") { act.finishAffinity() }
        val qlp = LinearLayout.LayoutParams(match, wrap)
        qlp.topMargin = dp(12)
        col.addView(quit, qlp)

        root.addView(col, FrameLayout.LayoutParams(match, wrap, Gravity.CENTER))
        return root
    }
}
