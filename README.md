# CrumbTrails — System Requirements

CrumbTrails is a native Android (Kotlin + Jetpack Compose) app for tracking routes (car, motorcycle, or on foot) and sharing routes as well as photos with other users. Below are the requirements to build, run, and develop the project, derived directly from its Gradle configuration.

## 1. Development Environment

| Requirement | Version / Detail |
|---|---|
| OS | Windows, macOS, or Linux (Android Studio compatible OS) |
| IDE | Android Studio (recent version, compatible with AGP 9.1.1 / Gradle 9.3.1) |
| JDK | **JDK 21** |
| Android Gradle Plugin (AGP) | 9.1.1 |
| Kotlin | 2.2.10 |
| KSP | 2.2.10-2.0.2 |

## 2. Android SDK / Device Requirements

| Setting | Value |
|---|---|
| `compileSdk` | 37 |
| `targetSdk` | 36 |
| `minSdk` | 34 (Android 14+) |
| Java source/target compatibility | Java 11 |
| Compose | Enabled (`buildFeatures.compose = true`) |
| BuildConfig | Enabled |

Since `minSdk = 34`, the app only installs/runs on devices running **Android 14 (API 34) or newer**.

## 3. Required Gradle/Android Plugins

- `com.android.application` — 9.1.1
- `org.jetbrains.kotlin.plugin.compose` — 2.2.10
- `com.google.devtools.ksp` — 2.2.10-2.0.2 (annotation processing for Room)
- `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` — 2.0.1 (used for taking the Google Maps API key from `local.properties`)
- `com.google.gms.google-services` — 4.4.2 (Firebase configuration)

## 4. External Services / Necessary Acounts

Two files are deliberately excluded from the repo (`.gitignore`) and **must be supplied locally** before the project will build/run:

1. **`local.properties`** (project root) — must define a Google Maps API key, e.g.:
   ```
   GOOGLE_MAPS_API_KEY=your_key_here
   ```
   Consumed by the secrets-gradle-plugin and injected into the manifest placeholder `${GOOGLE_MAPS_API_KEY}`.
2. **`app/google-services.json`** — Firebase project configuration file, downloaded from the Firebase console. Required for Firebase Auth, Realtime Database, and Storage to function.

You'll therefore need:
- A **Google Cloud / Maps Platform** project with the Maps SDK for Android enabled (billing-enabled API key).
- A **Firebase** project with Authentication, Realtime Database, and Storage enabled.

## 5. App Permissions (Pre-declared in AndroidManifest.xml)

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `INTERNET`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION`
- `POST_NOTIFICATIONS`

(Used for GPS route tracking via a foreground `TrackingService`, and notifying the user while tracking is active.)

## 6. Library Dependencies

### AndroidX / Jetpack
| Library | Version |
|---|---|
| androidx.core:core-ktx | 1.19.0 |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.11.0 |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.2 |
| androidx.activity:activity-compose | 1.13.0 |
| androidx.compose:compose-bom (BOM) | 2024.09.00 |
| androidx.compose.ui:ui | via BOM |
| androidx.compose.ui:ui-graphics | via BOM |
| androidx.compose.ui:ui-tooling / ui-tooling-preview | via BOM |
| androidx.compose.material3:material3 | via BOM |
| androidx.compose.material:material-icons-extended | via BOM |
| androidx.navigation:navigation-compose | 2.9.8 |
| androidx.room:room-runtime, room-ktx, room-compiler (KSP) | 2.8.4 |
| androidx.exifinterface:exifinterface | 1.4.1 |
| androidx.photopicker:photopicker-compose | 1.0.0-alpha01 |

### Google / Firebase / Maps
| Library | Version |
|---|---|
| com.google.android.gms:play-services-location | 21.3.0 |
| com.google.android.gms:play-services-maps | 19.0.0 |
| com.google.maps.android:maps-compose | 6.0.0 |
| com.google.maps.android:android-maps-utils | 3.20.1 |
| com.google.firebase:firebase-bom (BOM) | 33.1.2 |
| com.google.firebase:firebase-auth-ktx | via BOM |
| com.google.firebase:firebase-storage | via BOM |
| com.google.firebase:firebase-database | via BOM |
| firebase-database-ktx (catalog entry) | 22.0.1 |

### Utilities
| Library | Version |
|---|---|
| io.coil-kt:coil-compose (image loading) | 2.7.0 |
| com.google.code.gson:gson | 2.10.1 |

## 7. Summary — Minimum Setup Checklist

1. Install **JDK 21**.
2. Install **Android Studio** 
3. Clone the repo 
4. Create a **Google Maps API key** and add it to `local.properties` as `MAPS_API_KEY`.
5. Create a **Firebase project**, download `google-services.json`, and place it in `/app`.
6. Ensure a target device/emulator runs **Android 14 (API 34) or newer**.
7. Sync Gradle and build — internet access is required for Gradle/Maven dependency downloads and for the app itself (Maps, Firebase) at runtime.

**Enjoy!!**
