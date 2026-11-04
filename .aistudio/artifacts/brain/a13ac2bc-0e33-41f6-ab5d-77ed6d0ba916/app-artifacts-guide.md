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

## 🛠️ Resolved Issues in this Update
1. **Google Sign-In & Firestore Connection Bridge:**
   - Google Sign-In সফল হওয়ার সাথে সাথেই স্বয়ংক্রিয়ভাবে ক্লাউড ফায়ারস্টোর ডেটাবেজের সাথে সংযোগ যাচাই এবং লোকাল ডেটা ব্যাকআপ কার্যকর করা হয়েছে।
   - সাইন-ইন অবস্থায় সরাসরি **"কানেকশন টেস্ট"** ও **"এখনই সিঙ্ক"** বাটন যুক্ত করা হয়েছে।
2. **Firestore Security Rules Schema Update:**
   - ফায়ারস্টোর রুলসে `SALE`, `EXPENSE`, `DEBT_PAY`, `PRODUCT_BUY` সহ সকল ক্যাটাগরি সাপোর্ট অন্তর্ভুক্ত করে ডেপ্লয় করা হয়েছে।
3. **লাইভ স্ট্যাটাস ইন্ডিকেটর:**
   - ফায়ারস্টোর ডেটাবেজ আইডিতে কতটি ইনভয়েস ও আর্থিক রেকর্ড সংরক্ষিত আছে তা রিয়েল-টাইমে প্রদর্শিত হবে।

---

## 🚀 How to Run CI/CD Workflow on GitHub (If exporting project)

1. Open your repository on GitHub.
2. Go to the **Actions** tab.
3. Select **"Android CI/CD"** in the left sidebar.
4. Click **"Run workflow"** -> Select `main` -> Click **"Run workflow"**.
5. Once completed, download `app-debug-apk` directly from the **Artifacts** section at the bottom of the run.
