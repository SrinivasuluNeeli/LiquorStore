# Liquor Inventory & Daily Sales Management

A production-ready native Android application built with modern Android development standards using **Kotlin** and **Jetpack Compose**. The app provides end-to-end liquor store inventory reconciliation, real-time daily stock register tracking, multi-category movements (Whisky, Beer, Brandy, Rum, Vodka, Wine), expense logging, profit calculation, cash & digital settlements, and exportable PDF & Excel/CSV daily reports.

---

## 🛠️ Technology Stack & Versions

- **Platform:** Native Android (Kotlin)
- **UI Toolkit:** Jetpack Compose with Material Design 3 (M3)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Language:** Kotlin `2.2.10`
- **Android Gradle Plugin (AGP):** `9.1.1`
- **Gradle:** `8.11.1`
- **JDK Requirement:** Java 17 (Eclipse Temurin / OpenJDK 17)
- **Minimum SDK (`minSdk`):** `24` (Android 7.0 Nougat)
- **Target SDK (`targetSdk`):** `36`
- **Compile SDK (`compileSdk`):** `36` (Android 16 preview / release 36.1)
- **Application ID:** `com.aistudio.liquorinventory.mgr`
- **Local Persistence:** Room Database `2.7.0` (Offline-first SQLite via Kotlin Symbol Processing - KSP)
- **Async & Reactive Flow:** Kotlin Coroutines `1.10.2` & StateFlow
- **Serialization / Network:** Moshi `1.15.2` & Retrofit `2.12.0` / OkHttp `4.10.0`
- **Dependency Management:** Gradle Version Catalog (`gradle/libs.versions.toml`)

---

## 📁 Repository Structure

```text
├── .github/
│   └── workflows/
│       └── build-apk.yml           # Automated CI workflow for building & uploading APK
├── app/
│   ├── build.gradle.kts           # App-level module configuration & dependencies
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # App manifest & FileProvider configuration
│   │   ├── java/com/example/      # Kotlin source code (MVVM, Room, UI Screens, Utils)
│   │   └── res/                   # Drawables, mipmaps, strings, XML configs
├── gradle/
│   ├── libs.versions.toml         # Central version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── gradlew                        # Gradle execution wrapper (POSIX)
├── gradlew.bat                    # Gradle execution wrapper (Windows)
├── .env.example                   # Environment configuration template
├── .gitignore                     # Git ignore rules
├── build.gradle.kts               # Root build script
├── gradle.properties              # JVM & compiler arguments
├── metadata.json                  # Platform metadata
└── settings.gradle.kts            # Project settings & repositories
```

---

## 🚀 How to Clone and Open in Android Studio

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   cd <your-repo-name>
   ```

2. **Open in Android Studio:**
   - Launch **Android Studio Ladybug (or newer)**.
   - Select **Open...** and select the root directory of this cloned project.
   - Ensure your Project JDK is set to **JDK 17** (*Settings / Preferences $\to$ Build, Execution, Deployment $\to$ Build Tools $\to$ Gradle $\to$ Gradle JDK*).
   - Allow Gradle to sync dependencies.

---

## 💻 How to Build Locally from Command Line

To build without Android Studio directly from the terminal:

```bash
# Make gradlew executable (macOS/Linux)
chmod +x ./gradlew

# Build the Debug APK
./gradlew assembleDebug
```

The generated APK will be available at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

To run unit and Robolectric tests:
```bash
./gradlew testDebugUnitTest
```

---

## ⚙️ Automated GitHub Actions CI / APK Generation

Every commit or pull request pushed to `main` (or triggered via `workflow_dispatch`) automatically runs the workflow defined in `.github/workflows/build-apk.yml`.

### Workflow Steps:
1. Checks out the repository.
2. Sets up JDK 17 (Temurin distribution) with Gradle caching.
3. Configures Android SDK command-line tools.
4. Generates the debug APK using `./gradlew assembleDebug`.
5. Uploads the generated APK as a downloadable artifact.

### How to Download the Built APK from GitHub:
1. Open your repository on **GitHub**.
2. Click on the **Actions** tab at the top.
3. Click on the latest workflow run under **Build Android APK**.
4. Scroll down to the **Artifacts** section at the bottom.
5. Click on **`android-debug-apk`** to download the ZIP file containing `app-debug.apk`.

---

## 🔐 Creating a Signed Release Build (Google Play / AAB / Release APK)

The `app/build.gradle.kts` file is pre-configured to read signing parameters from environment variables.

### 1. Generate a Release Keystore (if you don't have one):
```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

### 2. Configure Environment Variables / GitHub Secrets:
For local builds or automated release workflows, configure:
- `KEYSTORE_PATH`: Path to your `.jks` file (e.g. `/path/to/my-upload-key.jks`)
- `STORE_PASSWORD`: Your keystore password
- `KEY_PASSWORD`: Your private key password

### 3. Build Signed Bundle / APK:
```bash
./gradlew bundleRelease    # Generates .aab for Google Play Store upload
./gradlew assembleRelease  # Generates signed release .apk
```

---

## 📝 How to Update App Name, Application ID & Versioning

- **Application ID:** Edit `applicationId` in `app/build.gradle.kts` (`defaultConfig { applicationId = "..." }`).
- **App Launcher Name:** Edit `<string name="app_name">` in `app/src/main/res/values/strings.xml`.
- **Version Code & Name:** Increment `versionCode` and update `versionName` in `app/build.gradle.kts` before submitting new releases to Google Play.
- **Launcher Icons:** Located in `app/src/main/res/mipmap-*` and `app/src/main/res/drawable/ic_launcher_*`.
