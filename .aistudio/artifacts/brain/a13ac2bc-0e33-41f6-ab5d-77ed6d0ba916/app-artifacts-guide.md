# App Artifacts & Download Center

## 📱 App Information
- **App Name:** Bengali AI Fashion & Local Advisor
- **Package:** `com.aistudio.appareladvisor.yqvkz`
- **Build Status:** Ready & Verified

---

## 🚀 How to Upload & Generate New Artifact on GitHub

If you deleted previous artifacts on GitHub and need new artifacts uploaded right now:

### Step 1: Go to GitHub Actions
1. Open your repository on GitHub.
2. Click on the **Actions** tab at the top.

### Step 2: Run "Android CI/CD" Workflow
1. In the left sidebar, click **"Android CI/CD"**.
2. Click the **"Run workflow"** button on the right side.
3. Select branch (`main` or `master`) and click **"Run workflow"** (Green button).

### Step 3: Download Your New Artifacts
Once the build completes (takes ~2-3 minutes), scroll to the **Artifacts** section at the bottom of the run page:
- 📦 **`app-debug-apk`**: Ready-to-install debug APK for testing on any Android phone (Retention: 30 days).
- 📦 **`app-release-artifacts`**: Production release APK and Google Play AAB bundle (Retention: 30 days).
- 🧪 **`unit-test-reports`**: HTML test execution reports.

---

## 💾 Local Build Artifact Already Created
The compiled APK is also stored directly inside this project:
- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Public Path: `public/app-debug.apk`
