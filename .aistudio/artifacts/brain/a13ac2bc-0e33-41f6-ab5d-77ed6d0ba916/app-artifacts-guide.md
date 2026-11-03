# App Artifacts & Download Center

## 📱 App Information
- **App Name:** AI Assistant (Bengali AI Fashion & Local Advisor)
- **Package:** `com.aistudio.appareladvisor.yqvkz`
- **Build Status:** ✅ BUILD SUCCESSFUL (All Debug APK & Unit Test errors solved)
- **APK Size:** ~29.7 MB (31,135,288 bytes)
- **Firebase Status:** Firestore & Auth provisioned & integrated with secure rules

---

## 💾 Direct Download Link & Local File Location

The latest debug APK has been compiled and is ready:
- **Direct Download/Public Location:** `public/app-debug.apk`
- **Build Outputs Directory:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 🛠️ Errors Resolved in this Build

1. **Gradle Build & Dependency Resolution:**
   - AGP 9.1.1 + Gradle 9.3.1 compatibility verified with Java 21.
   - Missing vector icon import (`Icons.Filled.Logout` replaced with modern `Icons.AutoMirrored.Filled.Logout`).
   - AndroidX Credentials & Play Services Auth integrated without dependency conflicts.

2. **Google Services & Firestore Integration:**
   - `google-services.json` properly configured with package `com.aistudio.appareladvisor.yqvkz`.
   - Firestore security rules compiled, tested (6/6 unit tests passed), and deployed to Cloud Firestore database `ai-studio-android-threadcr-a13ac2bc-0e33-41f6-ab5d-77ed6d0ba916`.
   - Android resources and layout strings synchronized.

3. **Unit Tests & Roborazzi Screenshot Tests:**
   - `testDebugUnitTest` executed and passed cleanly without HTTP 429 or SDK mismatch errors.

---

## 🚀 How to Run CI/CD Workflow on GitHub (If exporting project)

1. Open your repository on GitHub.
2. Go to the **Actions** tab.
3. Select **"Android CI/CD"** in the left sidebar.
4. Click **"Run workflow"** -> Select `main` -> Click **"Run workflow"**.
5. Once completed, download `app-debug-apk` directly from the **Artifacts** section at the bottom of the run.
