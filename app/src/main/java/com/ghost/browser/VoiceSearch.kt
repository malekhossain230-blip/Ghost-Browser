package com.ghost.browser

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class VoiceSearch(private val ui: Ios, private val host: HomeHost) {

    companion object {
        const val REQ_VOICE = 701
        private const val BAR_COUNT = 24
        private const val MSG_LISTEN = "Listening..."
        private const val MSG_RETRY = "Didn't catch that. Tap the mic to try again."
        private const val MSG_NONE = "No speech detected. Tap the mic to try again."
    }

    private val ctx: Context = host.activity
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val main = Handler(Looper.getMainLooper())

    val view = FrameLayout(ctx)
    var isOpen = false
        private set

    private val closeBtn = ImageView(ctx)
    private val pulse = View(ctx)
    private val circle = FrameLayout(ctx)
    private val micIcon = ImageView(ctx)
    private val bars = ArrayList<View>()
    private val status = TextView(ctx)
    private val result = TextView(ctx)

    private var rec: SpeechRecognizer? = null
    private var listening = false
    private var level = 0f
    private var pulseAnim: ValueAnimator? = null
    private var tickStart = 0L
    private var busyRetries = 0

    private val tick = object : Runnable {
        override fun run() {
            if (!listening) return
            val t = (System.currentTimeMillis() - tickStart) / 1000.0
            for ((i, b) in bars.withIndex()) {
                val wave = Math.sin(t * 3 + i * 0.6) * 0.5 + 0.5
                val jitter = Math.random() * 0.3
                val v = Math.min(1.0, wave * (0.35 + level * 0.55) + jitter * (0.4 + level))
                val lp = b.layoutParams
                lp.height = ui.dp(6) + (ui.dpf(46f) * v).toInt()
                b.layoutParams = lp
            }
            main.postDelayed(this, 48)
        }
    }

    init {
        view.setBackgroundColor(ui.BG)
        view.visibility = View.GONE
        view.isClickable = true
        view.isFocusable = true

        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.setPadding(ui.dp(24), 0, ui.dp(24), 0)
        view.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        val micWrap = FrameLayout(ctx)
        pulse.background = ui.oval(ui.ACCENT)
        pulse.alpha = 0f
        micWrap.addView(pulse, FrameLayout.LayoutParams(ui.dp(104), ui.dp(104), Gravity.CENTER))
        circle.background = ui.oval(ui.ACCENT)
        circle.elevation = ui.dp(8).toFloat()
        micIcon.setImageDrawable(HomeIcons.drawable("microphone", Color.WHITE))
        circle.addView(micIcon, FrameLayout.LayoutParams(ui.dp(30), ui.dp(30), Gravity.CENTER))
        circle.setOnClickListener { if (isOpen) begin() }
        micWrap.addView(circle, FrameLayout.LayoutParams(ui.dp(76), ui.dp(76), Gravity.CENTER))
        col.addView(micWrap, LinearLayout.LayoutParams(ui.dp(104), ui.dp(104)))

        val barRow = LinearLayout(ctx)
        barRow.orientation = LinearLayout.HORIZONTAL
        barRow.gravity = Gravity.CENTER
        for (i in 0 until BAR_COUNT) {
            val b = View(ctx)
            b.background = ui.rounded(ui.ACCENT, 3)
            val lp = LinearLayout.LayoutParams(ui.dp(4), ui.dp(6))
            if (i > 0) lp.leftMargin = ui.dp(4)
            barRow.addView(b, lp)
            bars.add(b)
        }
        val blp = LinearLayout.LayoutParams(WRAP, ui.dp(52))
        blp.topMargin = ui.dp(22)
        col.addView(barRow, blp)

        status.textSize = 15f
        status.setTextColor(ui.GRAY)
        status.typeface = ui.MEDIUM
        status.gravity = Gravity.CENTER
        status.setLineSpacing(ui.dp(3).toFloat(), 1f)
        status.text = MSG_LISTEN
        val slp = LinearLayout.LayoutParams(ui.dp(320), WRAP)
        slp.topMargin = ui.dp(22)
        col.addView(status, slp)

        result.textSize = 18f
        result.setTextColor(ui.TEXT)
        result.typeface = ui.BOLD
        result.gravity = Gravity.CENTER
        result.minHeight = ui.dp(28)
        result.setLineSpacing(ui.dp(3).toFloat(), 1f)
        val rlp = LinearLayout.LayoutParams(ui.dp(320), WRAP)
        rlp.topMargin = ui.dp(10)
        col.addView(result, rlp)

        closeBtn.setImageDrawable(HomeIcons.drawable("xmark", ui.TEXT))
        closeBtn.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
        closeBtn.background = ui.pressableRound(19, ui.glass(ui.CARD, 19, ui.LINE))
        closeBtn.setOnClickListener { close() }
        view.addView(closeBtn, FrameLayout.LayoutParams(ui.dp(38), ui.dp(38), Gravity.TOP or Gravity.END))
    }

    private fun placeClose() {
        val lp = closeBtn.layoutParams as FrameLayout.LayoutParams
        lp.topMargin = host.topInset() + ui.dp(16)
        lp.rightMargin = ui.dp(16)
        closeBtn.layoutParams = lp
    }

    fun open() {
        if (!isOpen) {
            isOpen = true
            placeClose()
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.scaleX = 0.96f
            view.scaleY = 0.96f
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
        }
        begin()
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        stopRecognition()
        setIdle(true)
        result.text = ""
        view.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(180).withEndAction {
            if (!isOpen) view.visibility = View.GONE
        }.start()
    }

    fun onPermissionResult(granted: Boolean) {
        if (!isOpen) return
        if (granted) begin() else {
            status.text = "Microphone permission is needed for voice search."
            setIdle(true)
        }
    }

    private fun languageTag(): String {
        try {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val st = imm.currentInputMethodSubtype
            if (st != null) {
                val tag = st.languageTag
                if (tag.isNotEmpty()) return tag
                @Suppress("DEPRECATION")
                val loc = st.locale
                if (loc.isNotEmpty()) return loc.replace('_', '-')
            }
        } catch (e: Exception) {
        }
        return Locale.getDefault().toLanguageTag()
    }

    private fun setIdle(idle: Boolean) {
        listening = !idle
        main.removeCallbacks(tick)
        if (idle) {
            pulseAnim?.cancel()
            pulseAnim = null
            pulse.alpha = 0f
            circle.background = ui.oval(ui.GRAY)
            circle.elevation = 0f
            for (b in bars) {
                val lp = b.layoutParams
                lp.height = ui.dp(6)
                b.layoutParams = lp
            }
        } else {
            circle.background = ui.oval(ui.ACCENT)
            circle.elevation = ui.dp(8).toFloat()
            if (pulseAnim == null) {
                val a = ValueAnimator.ofFloat(0f, 1f)
                a.duration = 1600
                a.repeatCount = ValueAnimator.INFINITE
                a.interpolator = LinearInterpolator()
                a.addUpdateListener { v ->
                    val f = v.animatedValue as Float
                    val s = 0.7f + 0.9f * f
                    pulse.scaleX = s
                    pulse.scaleY = s
                    pulse.alpha = 0.35f * (1f - f)
                }
                a.start()
                pulseAnim = a
            }
            tickStart = System.currentTimeMillis()
            main.post(tick)
        }
    }

    private fun stopRecognition() {
        val r = rec
        rec = null
        if (r != null) {
            try {
                r.cancel()
            } catch (e: Exception) {
            }
            try {
                r.destroy()
            } catch (e: Exception) {
            }
        }
    }

    private fun begin() {
        stopRecognition()
        result.text = ""
        level = 0f
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            status.text = "Voice search isn't available on this phone."
            setIdle(true)
            return
        }
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status.text = "Allow microphone access to search by voice."
            setIdle(true)
            host.activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_VOICE)
            return
        }
        status.text = MSG_LISTEN
        setIdle(false)
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                status.text = MSG_LISTEN
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val v = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                level = level * 0.6f + v * 0.4f
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                status.text = "Processing..."
            }

            override fun onError(error: Int) {
                if (rec !== r) return
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && busyRetries < 2) {
                    busyRetries++
                    main.postDelayed({ if (isOpen && rec === r) begin() }, 350)
                    return
                }
                busyRetries = 0
                status.text = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> MSG_NONE
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice search."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice search needs an internet connection."
                    else -> MSG_RETRY
                }
                setIdle(true)
            }

            override fun onResults(results: Bundle?) {
                if (rec !== r) return
                busyRetries = 0
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull()?.trim() ?: ""
                if (text.isEmpty()) {
                    status.text = MSG_NONE
                    setIdle(true)
                    return
                }
                finish(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull() ?: return
                if (text.isNotBlank()) result.text = text
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag())
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        try {
            r.startListening(intent)
        } catch (e: Exception) {
            status.text = MSG_RETRY
            setIdle(true)
        }
    }

    private fun finish(text: String) {
        status.text = "Searching..."
        result.text = text
        setIdle(true)
        circle.background = ui.oval(ui.ACCENT)
        main.postDelayed({
            if (!isOpen) return@postDelayed
            host.runSearch(text)
        }, 450)
    }
}
