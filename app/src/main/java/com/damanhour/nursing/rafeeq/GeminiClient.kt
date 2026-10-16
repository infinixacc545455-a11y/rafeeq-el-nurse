package com.damanhour.nursing.rafeeq

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    // ترتيب الموديلات: لو الأول مزدحم (503) أو مش متاح (404) بنجرب اللي بعده
    private val MODELS = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite"
    )
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    private class Result(val code: Int, val text: String)

    private const val SYSTEM_PROMPT = """
أنت "رفيق التمريض"، مساعد ذكي مخصص حصرياً لطلاب وخريجي وأفراد هيئة التمريض، ومرتبط بكلية التمريض - جامعة دمنهور.

القواعد:
1) أجب فقط عن الأسئلة المتعلقة بالتمريض والرعاية الصحية: الإجراءات التمريضية، التعامل مع المرضى، العلامات الحيوية، الأدوية وطرق إعطائها (من منظور التمريض)، مكافحة العدوى، الإسعافات الأولية، التوثيق، وحالات الطوارئ.
2) لو السؤال خارج المجال (سياسة، ترفيه، برمجة، إلخ) اعتذر بلطف وقل إنك متخصص في التمريض فقط، واعرض المساعدة في سؤال تمريضي.
3) في حالات الطوارئ: ابدأ فوراً بخطوات مرتبة (تقييم ABCDE، تأمين المكان، استدعاء الطبيب/فريق الطوارئ/الإنعاش، الإجراءات الفورية، المتابعة والتوثيق) بشكل مختصر وواضح، وبعدها التفاصيل.
4) نبّه دائماً أن الإجابة تعليمية، وأن الأولوية لبروتوكول المستشفى وأوامر الطبيب المعالج، ولا تعطِ جرعات دوائية قاطعة بدون التأكيد على مراجعة النشرة/الصيدلي/الطبيب.
5) رد بنفس لغة المستخدم (الافتراضي العربي بلهجة مبسطة وواضحة)، وسيب المصطلحات الطبية بالإنجليزي بجانب الشرح.
6) خلي الإجابة منظمة ومختصرة: نقاط قصيرة، بدون جداول ولا رموز Markdown معقدة.
"""

    fun ask(history: List<Msg>): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) return "مفتاح الـ API مش متضاف في التطبيق. راجع إعدادات GitHub Secrets."

        val contents = JSONArray()
        for (m in history) {
            contents.put(
                JSONObject()
                    .put("role", if (m.user) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", m.text)))
            )
        }
        val body = JSONObject()
            .put("system_instruction", JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT.trim()))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.4))
            .toString()

        var last: Result? = null
        for (model in MODELS) {
            for (attempt in 1..2) {
                val r = try {
                    post(model, key, body)
                } catch (e: Exception) {
                    Result(-1, "")
                }
                last = r
                if (r.code in 200..299) return parse(r.text)
                // مزدحم/ضغط: جرّب تاني بعد ثانية ونص
                if (r.code == 503 || r.code == 429 || r.code == 500 || r.code == -1) {
                    if (attempt == 1) { Thread.sleep(1500); continue }
                }
                break
            }
            // 404 أو ضغط مستمر: كمّل على الموديل اللي بعده، أي خطأ تاني اطلع منه
            val c = last?.code ?: -1
            if (c != 404 && c != 503 && c != 429 && c != 500 && c != -1) break
        }

        return when (last?.code) {
            503, 429, 500 -> "السيرفر مشغول دلوقتي. استنى ثواني وجرّب تاني."
            -1 -> "مقدرتش أتصل بالسيرفر. اتأكد من الإنترنت وحاول تاني."
            else -> "حصل خطأ (${last?.code}): " + errorMessage(last?.text ?: "")
        }
    }

    private fun post(model: String, key: String, body: String): Result {
        val conn = URL("$BASE$model:generateContent").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return Result(code, text)
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(raw: String): String =
        try { JSONObject(raw).getJSONObject("error").getString("message") } catch (e: Exception) { raw }

    private fun parse(raw: String): String {
        val parts = JSONObject(raw)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
        return clean(sb.toString())
    }

    // تنضيف بسيط لرموز Markdown
    private fun clean(s: String): String =
        s.replace("**", "")
            .replace(Regex("(?m)^\\s*[*-]\\s+"), "• ")
            .replace(Regex("(?m)^#+\\s*"), "")
            .trim()
}
