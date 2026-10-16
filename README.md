# رفيق التمريض 🩺
تطبيق أندرويد مخصص للتمريض فقط – كلية التمريض، جامعة دمنهور.
- أسئلة تمريضية وحالات طوارئ عبر Gemini (Google AI Studio)
- دعم المايك (ar-EG) + قراءة الإجابة صوتياً لما السؤال يكون بالصوت

## البناء
1. ارفع المشروع على GitHub.
2. Settings → Secrets and variables → Actions → New repository secret
   الاسم: `GEMINI_API_KEY` والقيمة: مفتاحك.
3. Actions → Build Android APK → Run workflow، وبعد ما يخلص نزّل الـ APK من Artifacts.
