package com.damanhour.nursing.rafeeq

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

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

        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val msg = try {
                    JSONObject(text).getJSONObject("error").getString("message")
                } catch (e: Exception) { text }
                return "حصل خطأ ($code): $msg"
            }

            val parts = JSONObject(text)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            val sb = StringBuilder()
            for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
            return clean(sb.toString())
        } finally {
            conn.disconnect()
        }
    }

    // تنضيف بسيط لرموز Markdown
    private fun clean(s: String): String =
        s.replace("**", "")
            .replace(Regex("(?m)^\\s*[*-]\\s+"), "• ")
            .replace(Regex("(?m)^#+\\s*"), "")
            .trim()
}
