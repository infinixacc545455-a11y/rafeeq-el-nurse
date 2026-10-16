package com.damanhour.nursing.rafeeq

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val messages = mutableListOf<Msg>()
    private lateinit var adapter: ChatAdapter
    private lateinit var list: RecyclerView
    private lateinit var input: EditText
    private lateinit var micBtn: ImageButton
    private lateinit var status: TextView

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var busy = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val executor = Executors.newSingleThreadExecutor()

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening()
            else Toast.makeText(this, getString(R.string.mic_denied), Toast.LENGTH_LONG).show()
        }

    private val quickQuestions = listOf(
        "🚨 مريض توقف قلبه",
        "🩺 قياس العلامات الحيوية",
        "💉 إعطاء حقنة عضل",
        "🫁 مريض عنده ضيق تنفس",
        "🧼 مكافحة العدوى",
        "🩸 التعامل مع نزيف"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list = findViewById(R.id.list)
        input = findViewById(R.id.input)
        micBtn = findViewById(R.id.mic)
        status = findViewById(R.id.status)

        adapter = ChatAdapter(messages)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        messages.add(Msg(getString(R.string.welcome), user = false, welcome = true))
        adapter.notifyItemInserted(0)

        buildChips()

        findViewById<ImageButton>(R.id.send).setOnClickListener {
            sendMessage(input.text.toString(), false)
        }
        micBtn.setOnClickListener { onMicClicked() }

        tts = TextToSpeech(this) { s ->
            if (s == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("ar"))
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildChips() {
        val box = findViewById<LinearLayout>(R.id.chips)
        for (q in quickQuestions) {
            val tv = TextView(this)
            tv.text = q
            tv.textSize = 13f
            tv.setTextColor(ContextCompat.getColor(this, R.color.teal_dark))
            tv.setPadding(dp(12), dp(8), dp(12), dp(8))
            val bg = GradientDrawable()
            bg.cornerRadius = dp(20).toFloat()
            bg.setColor(Color.WHITE)
            bg.setStroke(dp(1), ContextCompat.getColor(this, R.color.teal))
            tv.background = bg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(dp(4), 0, dp(4), 0)
            tv.layoutParams = lp
            tv.setOnClickListener { sendMessage(q.substringAfter(" "), false) }
            box.addView(tv)
        }
    }

    // ---------- الإرسال لـ Gemini ----------
    private fun sendMessage(text: String, byVoice: Boolean) {
        val q = text.trim()
        if (q.isEmpty() || busy) return
        busy = true
        tts?.stop()
        input.setText("")

        messages.add(Msg(q, user = true))
        messages.add(Msg(getString(R.string.thinking), user = false, loading = true))
        adapter.notifyItemRangeInserted(messages.size - 2, 2)
        list.scrollToPosition(messages.size - 1)

        val history = messages
            .filter { !it.loading && !it.welcome }
            .takeLast(12)
            .dropWhile { !it.user }

        executor.execute {
            val reply = try {
                GeminiClient.ask(history)
            } catch (e: Exception) {
                "مقدرتش أتصل بالسيرفر. اتأكد من الإنترنت وحاول تاني."
            }
            runOnUiThread {
                val idx = messages.indexOfLast { it.loading }
                if (idx >= 0) {
                    messages[idx] = Msg(reply, user = false)
                    adapter.notifyItemChanged(idx)
                    list.scrollToPosition(messages.size - 1)
                }
                busy = false
                if (byVoice && ttsReady) {
                    tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "answer")
                }
            }
        }
    }

    // ---------- المايك ----------
    private fun onMicClicked() {
        if (listening) {
            recognizer?.stopListening()
            setListening(false)
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, getString(R.string.mic_unavailable), Toast.LENGTH_LONG).show()
            return
        }
        tts?.stop()
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onError(error: Int) {
                    setListening(false)
                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                        error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ) {
                        Toast.makeText(this@MainActivity, "خطأ في المايك ($error)", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val t = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!t.isNullOrBlank()) input.setText(t)
                }

                override fun onResults(results: Bundle?) {
                    setListening(false)
                    val t = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!t.isNullOrBlank()) sendMessage(t, true)
                }
            })
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(i)
        setListening(true)
    }

    private fun setListening(on: Boolean) {
        listening = on
        if (on) {
            micBtn.setColorFilter(Color.RED)
            status.text = getString(R.string.listening)
        } else {
            micBtn.clearColorFilter()
            status.text = getString(R.string.disclaimer)
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        executor.shutdown()
        super.onDestroy()
    }
}
