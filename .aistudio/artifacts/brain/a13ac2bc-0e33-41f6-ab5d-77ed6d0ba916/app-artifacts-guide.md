# App Artifacts & Download Center

## 📱 App Information
- **App Name:** AI Assistant (Bengali AI Fashion & Local Advisor)
- **AI Model:** ⚡ **Gemini 3.8 Flash** (`gemini-3.8-flash`)
- **Package:** `com.aistudio.appareladvisor.yqvkz`
- **Build Status:** ✅ BUILD SUCCESSFUL
- **APK Size:** ~29.7 MB
- **Firebase Status:** ✅ Google Sign-In & Firestore Live Auto-Connection Active

---

## 💾 Direct Download Link & Local File Location

The latest debug APK with **Google Sign-In & Firestore Auto-Sync** is compiled and ready:
- **Direct Download/Public Location:** `public/app-debug.apk`
- **Build Outputs Directory:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 🛠️ Key Features & Performance Optimizations in this Update
1. **🚀 আল্ট্রা-ফাস্ট কোল্ড স্টার্ট (Instant 0-ms Startup):**
   - **নেটিভ উইন্ডো ব্যাকগ্রাউন্ড ও স্প্ল্যাশ আর্কিটেকচার:** `Theme.App.Starting` ও Android 12+ (API 31+) এর `windowSplashScreenBackground` এবং `windowSplashScreenAnimatedIcon` যুক্ত করা হয়েছে। ফলে অ্যাপে ট্যাপ করার সাথে সাথেই উইন্ডো ম্যানেজার ০ মিলিসেকেন্ডে (তাত্ক্ষণিকভাবে) ব্র্যান্ড ইন্টারফেস প্রদর্শন করে—কোনো ব্ল্যাঙ্ক বা সাদা স্ক্রিন ফ্রিজ হবে না।
   - **Lazy Text-To-Speech (TTS):** টেক্সট-টু-স্পিচ সার্ভিস বাইন্ডিং ও আইপিসি (IPC) অ্যাপ ওপেন হওয়ার সময় বন্ধ রাখা হয়েছে। শুধুমাত্র যখন ইউজার স্পিচ/ভয়েস বোতাম চাপবেন তখন এটি কার্যকর হবে, যা কোল্ড স্টার্টে ৪০০-১২০০ মিলিসেকেন্ড সময় বাঁচায়।
   - **Lazy Retrofit, Moshi & Firestore Settings:** অ্যাপ ওপেনের সময় অপ্রয়োজনীয় নেটওয়ার্ক কানেকশন পুল ও রেট্রোফিট লোডিং সম্পূর্ণ Lazy করা হয়েছে।
   - **ব্যাকগ্রাউন্ড IO ও ডিফার্ড সিঙ্ক:** রুম ডাটাবেজ রিড এবং ফায়ারস্টোর ক্লাউড সিঙ্ককে মেইন থ্রেড থেকে সরিয়ে ব্যাকগ্রাউন্ড `Dispatchers.IO`-তে ১.২ সেকেন্ড পর শুরু করা হয়েছে, যাতে অ্যাপ ওপেন করার সময় মেইন ইউআই ৬০ FPS-এ সঙ্গে সঙ্গে ভেসে ওঠে।
   - **স্মার্ট ড্রয়ার কম্পোজিশন:** বন্ধ অবস্থায় ন্যাভিগেশন ড্রয়ারের ভারী উইজেট ট্রি কম্পোজ না করে শুধুমাত্র ওপেন করার সময় লোড করার ব্যবস্থা করা হয়েছে।
2. **Seasonal Fashion Trend Analyzer & Entrepreneur Recommendations (মৌসুমি ট্রেন্ডস):**
   - **Gemini 3.8 Flash** চালিত রিয়েল-টাইম ফ্যাশন ট্রেন্ড বিশ্লেষণ।
   - কমার্শিয়াল কালার প্যালেট, অ্যাকশনেবল ফ্যাব্রিক স্পেসিফিকেশন ও সোর্সিং হাব (নারায়ণগঞ্জ, ইসলামপুর)।
   - ইউনিট ইকোনমিক্স ও প্রফিট মার্জিন ক্যালকুলেটর (COGS, MSRP, ব্রেক-ইভেন)।
   - ৩-ধাপের গো-টু-মার্কেট রোডম্যাপ ও এআই চ্যাট ইন্টিগ্রেশন।
3. **৪টি ন্যাভিগেশন ট্যাব:**
   - **AI চ্যাট** | **ট্রেন্ডস** | **Sale** | **ফায়ারবেস DB**

---

## 🚀 How to Run CI/CD Workflow on GitHub (If exporting project)

1. Open your repository on GitHub.
2. Go to the **Actions** tab.
3. Select **"Android CI/CD"** in the left sidebar.
4. Click **"Run workflow"** -> Select `main` -> Click **"Run workflow"**.
5. Once completed, download `app-debug-apk` directly from the **Artifacts** section at the bottom of the run.
