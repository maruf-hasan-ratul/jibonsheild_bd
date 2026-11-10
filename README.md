# JibonShield BD (জীবনশীল্ড বিডি)
### Offline-First Disaster Emergency Resilience, Android System Integration & Cloud Synchronized Application

[![Android Build](https://img.shields.io/badge/Android-SDK%2021--33-brightgreen.svg)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-Java%208%20%2F%20Android%20Jetpack-blue.svg)](https://developer.android.com)
[![Tests](https://img.shields.io/badge/Tests-69%2F69%20Passing%20(100%25)-success.svg)](https://junit.org)
[![Offline First](https://img.shields.io/badge/Architecture-Offline--First%20SQLite-orange.svg)](https://sqlite.org)
[![Firebase](https://img.shields.io/badge/Cloud-Firebase%20Auth%20%7C%20Firestore%20%7C%20RTDB%20%7C%20Storage-yellow.svg)](https://firebase.google.com)
[![ViewBinding](https://img.shields.io/badge/UI-ViewBinding%20%2B%20Material3-blueviolet.svg)](https://developer.android.com/topic/libraries/view-binding)
[![License](https://img.shields.io/badge/License-Academic%20%2F%20Open-lightgrey.svg)](LICENSE)

---

## 1. Project Introduction & Purpose
**JibonShield BD (জীবনশীল্ড বিডি)** is an offline-first disaster emergency resilience and environmental safety Android application engineered specifically for the disaster-prone geographical realities of Bangladesh.

During acute monsoon flooding, coastal cyclones, riverbank erosion, extreme heatwaves, or drinking water contamination, critical telecommunications infrastructure often experiences brownouts or congestion. JibonShield BD ensures that **100% of essential life-preserving information and field reporting tools remain accessible offline** using an embedded SQLite database as the primary local source of truth. When internet connectivity is detected, safe cloud synchronization extends the application using Google Firebase Authentication, Cloud Firestore, Firebase Realtime Database, Firebase Cloud Storage, and Android WorkManager.

---

## 2. Problem Statement
Bangladesh ranks among the most climate-vulnerable nations globally. Millions of residents across the Sylhet Haor basin, coastal belts, and river char areas regularly confront:
1. Complete breakdown of cellular communication during flash floods and cyclones.
2. Inability to locate verified cyclone or flood refuge shelters with confirmed capacity, drinking water, and accessibility.
3. Lack of rapid one-tap dialing access to national emergency helplines without searching online.
4. Post-flood drinking water contamination causing severe waterborne disease outbreaks.
5. Inability to document and capture photographic field observations of water quality without internet.
6. Heatstroke emergencies during summer heatwaves without clear triage cooling protocols.

---

## 3. Project Objectives
* **Absolute Offline Availability**: Instant access to flood bulletins, 64-district shelter directory, and disaster helplines with zero network dependency.
* **Dual Persistence Architecture**: SQLite provides the local source of truth; Firebase extends the system with cloud identity, real-time alerts, and evidence photo storage.
* **Android System Integration**: Demonstrate practical implementations of Broadcast Receivers (Airplane Mode, Battery Saver), Foreground Services (Emergency Monitoring Mode), Content Providers / Photo Picker, and WorkManager background tasks.
* **Modern Java Architecture**: Type-safe View Binding, multi-Activity navigation with explicit and implicit intents, and dynamic RecyclerView list filtering.
* **Bilingual Inclusivity**: 100% complete English and Bengali (বাংলা) localization with runtime language switching.

---

## 4. Target Users
1. **Citizens & Rural Communities**: Families living in flood-prone and cyclone-exposed districts.
2. **Disaster Management Volunteers**: Field observers and Red Crescent volunteers collecting field data.
3. **Local Emergency Response Teams**: Personnel managing shelter occupancy and evacuee intake.
4. **Community Water Quality Observers**: Volunteers monitoring tubewell safety and turbidity post-disaster.

---

## 5. Technology Stack

| Component | Technology | Version / Implementation |
|---|---|---|
| **Programming Language** | Java | Java 8 (`VERSION_1_8`) |
| **UI Architecture** | XML Layouts + View Binding | ConstraintLayout 2.1.4, Material Components 1.8.0 |
| **Local Database** | SQLite | `SQLiteOpenHelper` (Version 3 schema) |
| **Cloud Authentication** | Firebase Authentication | Email/Password with Citizen & Volunteer roles |
| **Realtime Cloud Data** | Firebase Realtime Database | Live disaster broadcasts & shelter occupancy |
| **Structured Cloud Data** | Cloud Firestore | Users, observations, and incident history |
| **Media Storage** | Firebase Storage | Water observation evidence photos (5MB max) |
| **Background Synchronization**| AndroidX WorkManager | `work-runtime:2.8.1` (`EmergencySyncWorker`) |
| **Persistent User Task** | Android Foreground Service | `EmergencyMonitoringService` with NotificationChannel |
| **System Broadcasts** | Broadcast Receivers | `AirplaneModeReceiver`, `BatterySaverReceiver` |
| **Device Media** | Photo Picker / ContentResolver | `PickVisualMedia` with local stream persistence |
| **Networking** | Native HTTP | `HttpURLConnection` with strict JSON schema validator |
| **Automated Testing** | JUnit 4 + AndroidX Test | 69 Unit Tests (100% passing) |

---

## 6. Architecture Overview

```
                                  +------------------------------------+
                                  |         User Interface             |
                                  | (17 Activities + View Binding XML) |
                                  +-----------------+------------------+
                                                    |
                                                    v
                                  +------------------------------------+
                                  |     Emergency Repository Layer     |
                                  |   (Thread-Safe Orchestration)      |
                                  +--------+------------------+--------+
                                           |                  |
                       +-------------------+                  +-------------------+
                       |                                                          |
                       v                                                          v
    +--------------------------------------+                   +--------------------------------------+
    |        Local Source of Truth         |                   |      Cloud Synchronization Layer     |
    |      (SQLite / DatabaseHelper)       |                   |  (Firebase SDKs & WorkManager Sync)  |
    |                                      |                   |                                      |
    | - emergency_contacts (Verified)      | <--- Two-Way ---> | - FirebaseAuthManager (Auth & Role)  |
    | - flood_warnings (FFWC Bulletins)    |    Sync Pass      | - FirestoreManager (Structured Data) |
    | - shelters (64 Districts Directory)  |                   | - RealtimeDatabaseManager (Live)     |
    | - water_observations (Image URI/URL) |                   | - FirebaseStorageManager (Photos)    |
    | - preparedness_checklists            |                   | - EmergencySyncWorker (Background)   |
    +--------------------------------------+                   +--------------------------------------+
                       ^                                                          ^
                       |                                                          |
    +------------------+-------------------+                   +------------------+-------------------+
    |      Android System Components       |                   |       Native HTTP Networking         |
    | - AirplaneModeReceiver (Offline mode)|                   | - NetworkHttpClient                  |
    | - BatterySaverReceiver (Power save)  |                   | - RemoteDataValidator (JSON schema)  |
    | - EmergencyMonitoringService (Notif) |                   | - HttpURLConnection (DNS timeouts)   |
    +--------------------------------------+                   +--------------------------------------+
```

---

## 7. Folder Structure

```
JIBONSHIELD BD/
├── app/
│   ├── google-services.json            # Firebase configuration
│   ├── build.gradle                    # App build features (ViewBinding, BOM, WorkManager)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml     # Manifest declarations, permissions, services
│   │   │   ├── java/com/jibonshield/bd/
│   │   │   │   ├── JibonShieldApp.java
│   │   │   │   ├── activities/
│   │   │   │   │   ├── BaseActivity.java
│   │   │   │   │   ├── SplashActivity.java
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   ├── RegisterActivity.java
│   │   │   │   │   ├── ProfileActivity.java
│   │   │   │   │   ├── FloodWarningListActivity.java
│   │   │   │   │   ├── FloodWarningDetailActivity.java
│   │   │   │   │   ├── ShelterDirectoryActivity.java
│   │   │   │   │   ├── ShelterDetailActivity.java
│   │   │   │   │   ├── EmergencyContactsActivity.java
│   │   │   │   │   ├── WaterObservationFormActivity.java
│   │   │   │   │   ├── WaterObservationHistoryActivity.java
│   │   │   │   │   ├── HeatSafetyActivity.java
│   │   │   │   │   ├── EvacuationGuidelinesActivity.java
│   │   │   │   │   ├── PreparednessChecklistActivity.java
│   │   │   │   │   ├── SettingsActivity.java
│   │   │   │   │   └── AboutActivity.java
│   │   │   │   ├── adapters/
│   │   │   │   │   ├── DashboardAdapter.java
│   │   │   │   │   ├── FloodWarningAdapter.java
│   │   │   │   │   ├── ShelterAdapter.java
│   │   │   │   │   ├── EmergencyContactAdapter.java
│   │   │   │   │   ├── WaterObservationAdapter.java
│   │   │   │   │   └── ChecklistAdapter.java
│   │   │   │   ├── database/
│   │   │   │   │   ├── DatabaseContract.java
│   │   │   │   │   └── DatabaseHelper.java
│   │   │   │   ├── firebase/
│   │   │   │   │   ├── FirebaseAuthManager.java
│   │   │   │   │   ├── FirestoreManager.java
│   │   │   │   │   ├── RealtimeDatabaseManager.java
│   │   │   │   │   └── FirebaseStorageManager.java
│   │   │   │   ├── models/
│   │   │   │   │   ├── FloodWarning.java
│   │   │   │   │   ├── Shelter.java
│   │   │   │   │   ├── EmergencyContact.java
│   │   │   │   │   ├── WaterObservation.java
│   │   │   │   │   ├── UserProfile.java
│   │   │   │   │   ├── LiveEmergencyIncident.java
│   │   │   │   │   ├── ChecklistItem.java
│   │   │   │   │   └── ChecklistProgress.java
│   │   │   │   ├── receivers/
│   │   │   │   │   ├── AirplaneModeReceiver.java
│   │   │   │   │   └── BatterySaverReceiver.java
│   │   │   │   ├── repositories/
│   │   │   │   │   └── EmergencyRepository.java
│   │   │   │   ├── services/
│   │   │   │   │   └── EmergencyMonitoringService.java
│   │   │   │   ├── sync/
│   │   │   │   │   ├── EmergencySyncWorker.java
│   │   │   │   │   ├── NetworkHttpClient.java
│   │   │   │   │   ├── RemoteDataValidator.java
│   │   │   │   │   ├── SyncManager.java
│   │   │   │   │   └── SyncResult.java
│   │   │   │   └── utils/
│   │   │   │       ├── FileUtils.java
│   │   │   │       ├── LocaleHelper.java
│   │   │   │       ├── NetworkUtils.java
│   │   │   │       ├── PermissionUtils.java
│   │   │   │       └── PreferenceUtils.java
│   │   │   └── res/
│   │   │       ├── layout/             # View Binding XML layouts
│   │   │       ├── values/strings.xml  # Complete English localization strings
│   │   │       ├── values-bn/          # Complete Bengali (বাংলা) localization
│   │   │       └── drawable/           # Vector assets & backgrounds
│   │   └── test/java/com/jibonshield/bd/
│   │       ├── Day2VerificationTest.java       (6 tests)
│   │       ├── Day3DatabaseTest.java           (13 tests)
│   │       ├── Day4FeatureTest.java            (12 tests)
│   │       ├── Day5MasterTest.java             (10 tests)
│   │       ├── Day6IntegrationTest.java        (14 tests)
│   │       ├── Day7ReleaseVerificationTest.java (7 tests)
│   │       ├── Day8SystemComponentsTest.java   (3 tests)
│   │       ├── Day9FirebaseAuthAndModelTest.java(2 tests)
│   │       └── Day10SyncAndStorageTest.java    (2 tests)
├── firestore.rules                     # Cloud Firestore Security Rules
├── database.rules.json                 # Realtime Database Security Rules
├── storage.rules                       # Firebase Cloud Storage Security Rules
├── build.gradle
└── README.md
```

---

## 8. Complete Activity Navigation Flow

1. **`SplashActivity`**: Verifies database readiness, applies selected language, and launches `MainActivity`.
2. **`MainActivity`**: Core command center. Displays live SQLite counts (alerts, shelters, helplines, kit readiness), network status banner, broadcast notices, Emergency Monitoring trigger, and navigation tiles.
3. **`LoginActivity`**: Email and password sign-in with View Binding, field validation, and guest offline access mode.
4. **`RegisterActivity`**: Account creation supporting Citizen vs Volunteer role distinction and home district selection.
5. **`ProfileActivity`**: Displays user identity, role safety badge, and secure sign-out.
6. **`FloodWarningListActivity`**: Lists flood bulletins with severity filtering (`CRITICAL`, `WARNING`, `ADVISORY`, `NORMAL`) and stale-cache warnings.
7. **`FloodWarningDetailActivity`**: Full river basin, danger levels, and official bulletin references.
8. **`ShelterDirectoryActivity`**: Instant local search across 64 districts with upazila, capacity, and facility filtering.
9. **`ShelterDetailActivity`**: Detailed shelter breakdown (ramps, women's spaces, drinking water, electricity).
10. **`EmergencyContactsActivity`**: Pre-seeded national disaster helplines with safe implicit `ACTION_DIAL` intent launching.
11. **`WaterObservationFormActivity`**: Field observation entry form with Android Photo Picker, local stream caching, and pH validation.
12. **`WaterObservationHistoryActivity`**: Searchable/filterable log of community water observations with attached photo previews.
13. **`HeatSafetyActivity`**: WHO/DGHS heat safety matrix, heatstroke emergency cooling protocols, and 999 hotline calling.
14. **`EvacuationGuidelinesActivity`**: Pre-, during-, and post-disaster guidelines in English and Bengali.
15. **`PreparednessChecklistActivity`**: Interactive checklist tracking supplies and vital documents with progress saving.
16. **`SettingsActivity`**: Runtime language switching, district preference, and manual synchronization trigger.
17. **`AboutActivity`**: Data provenance, government disclaimers, and version information.

---

## 9. Local Database Schema & Provenance

* **Database File**: `jibonshield.db`
* **Version**: `3`

### Tables
1. **`flood_warnings`**: `_id`, `warning_level`, `title_en`, `title_bn`, `region`, `details_en`, `details_bn`, `issued_at`, `expires_at`, `source`, `source_reference`, `is_active`, `is_sample`, `last_synced_at`.
2. **`shelters`**: `_id`, `name_en`, `name_bn`, `district`, `upazila`, `address`, `latitude`, `longitude`, `capacity`, `current_occupancy`, `contact_phone`, `shelter_type`, `accessibility_notes`, `source`, `verified_at`, `is_active`.
3. **`emergency_contacts`**: `_id`, `service_name_en`, `service_name_bn`, `phone_number`, `service_description_en`, `service_description_bn`, `category`, `available_hours`, `is_toll_free`, `source`, `verification_date`, `is_enabled`, `priority_order`.
4. **`water_observations`**: `_id`, `location_name`, `district`, `source_type`, `appearance`, `odor`, `ph_level`, `turbidity_status`, `safety_status`, `observation_time`, `reported_by`, `remarks`, `image_uri` (Local path), `image_url` (Cloud URL), `sync_status` (`PENDING`, `SYNCED`, `FAILED`), `created_at`, `updated_at`.
5. **`preparedness_checklists` & `checklist_items`**: Foreign-key linked checklist tables with completion flags (`is_completed`).
6. **`synchronization_metadata`**: Provenance tracking, record counts, and last synchronization timestamps.

---

## 10. Firebase Architecture & Security Rules

### 10.1 Authentication (`FirebaseAuthManager.java`)
* Email & Password registration and login.
* Role-based profiles: `CITIZEN` and `VOLUNTEER`.
* Guest Mode: Users can freely access public safety data without being forced to authenticate.

### 10.2 Realtime Database (`RealtimeDatabaseManager.java`)
* Real-time listeners for `emergency_status/` and `shelter_occupancy/`.
* Synchronizes active flood incidents without manual page refreshing.

### 10.3 Cloud Firestore (`FirestoreManager.java`)
* Collections: `users`, `water_observations`, `disaster_reports`, `volunteer_reports`, `incident_history`.
* Strict schema validation and timestamps.

### 10.4 Firebase Storage (`FirebaseStorageManager.java`)
* Uploads observation photos to `water_observations/obs_{id}.jpg`.
* Retrieves public HTTPS download URLs for synchronization with Firestore and SQLite.

### 10.5 Security Rules
Production-grade security rules are provided in `firestore.rules`, `database.rules.json`, and `storage.rules`. Unauthenticated users can read public warnings, while only authenticated users can upload field reports and photos under strict MIME and 5MB size limits.

---

## 11. Android System Components Integration

### 11.1 Airplane Mode Receiver (`AirplaneModeReceiver.java`)
* Dynamically registered to listen for `Intent.ACTION_AIRPLANE_MODE_CHANGED`.
* When enabled: Informs the user that the application is operating in 100% offline mode and suppresses unnecessary network retries.
* When disabled: Re-verifies actual internet reachability before resuming cloud synchronization.

### 11.2 Battery Saver Receiver (`BatterySaverReceiver.java`)
* Dynamically registered to monitor `PowerManager.ACTION_POWER_SAVE_MODE_CHANGED`.
* When active: Displays power-saving status and instructs background tasks to conserve battery.

### 11.3 Device Media & Content Provider (`WaterObservationFormActivity.java`)
* Integrates Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) with `ACTION_OPEN_DOCUMENT` fallback.
* Reads media streams via `ContentResolver` and persists a managed local copy via `FileUtils` to maintain offline availability.

### 11.4 Emergency Monitoring Foreground Service (`EmergencyMonitoringService.java`)
* Creates a persistent high-priority notification channel (`channel_emergency_monitoring`).
* Displays a user-visible notification with a one-tap **Stop Monitoring** action button.
* Uses `android:foregroundServiceType="dataSync"` complying with Android 13/14 requirements.

---

## 12. Offline-First Background Synchronization (`EmergencySyncWorker.java`)

1. Scheduled via WorkManager with `NetworkType.CONNECTED` constraints.
2. Identifies pending SQLite records (`sync_status != 'SYNCED'`).
3. Uploads evidence photos to Firebase Cloud Storage.
4. Saves structured observation data to Cloud Firestore.
5. Updates local SQLite status to `SYNC_SYNCED` with cloud image URL.
6. Invokes `SyncManager.syncAll()` to refresh warnings and shelter occupancy.
7. **Zero Data Loss Guarantee**: Local records are never deleted if a network request fails.

---

## 13. Security and Permission Management

Declared in `AndroidManifest.xml`:
* `android.permission.INTERNET`: Cloud synchronization with Firebase and official portals.
* `android.permission.ACCESS_NETWORK_STATE`: Pre-flight connectivity verification.
* `android.permission.FOREGROUND_SERVICE`: Persistent emergency monitoring service.
* `android.permission.POST_NOTIFICATIONS`: Android 13+ runtime notification permissions requested contextually via `PermissionUtils.java`.

---

## 14. Responsive Layouts & Orientation
* Hardcoded portrait locks removed from `AndroidManifest.xml` across all feature activities.
* Layouts wrapped in `NestedScrollView` preventing viewport clipping in landscape mode.
* Form input states and timestamps preserved across device rotation via `onSaveInstanceState`.

---

## 15. Testing & Quality Assurance

### Automated Unit Test Summary
The project contains **69 automated unit tests** passing with **zero failures**:

```bash
cmd /c "gradlew.bat testDebugUnitTest --rerun-tasks"
```

| Test Suite | Tests | Failures | Errors | Result |
|---|---|---|---|---|
| `Day2VerificationTest` | 6 | 0 | 0 | **PASSED** |
| `Day3DatabaseTest` | 13 | 0 | 0 | **PASSED** |
| `Day4FeatureTest` | 12 | 0 | 0 | **PASSED** |
| `Day5MasterTest` | 10 | 0 | 0 | **PASSED** |
| `Day6IntegrationTest` | 14 | 0 | 0 | **PASSED** |
| `Day7ReleaseVerificationTest` | 7 | 0 | 0 | **PASSED** |
| `Day8SystemComponentsTest` | 3 | 0 | 0 | **PASSED** |
| `Day9FirebaseAuthAndModelTest`| 2 | 0 | 0 | **PASSED** |
| `Day10SyncAndStorageTest` | 2 | 0 | 0 | **PASSED** |
| **Total** | **69** | **0** | **0** | **100% PASSED** |

---

## 16. APK Build & Verification

### Compiling Debug APK
```bash
gradlew.bat assembleDebug
```

* **Verified Output Path**: `app/build/outputs/apk/debug/app-debug.apk`
* **Package Name**: `com.jibonshield.bd`
* **File Size**: ~10.2 MB (10,713,870 bytes)
* **SDK Compatibility**: Android 5.0 (API 21) – Android 13 (API 33)

---

## 17. Firebase Configuration Instructions

To connect your own Firebase project:
1. Open the [Firebase Console](https://console.firebase.google.com).
2. Create a project named `jibonshield-bd`.
3. Add an Android app with package name `com.jibonshield.bd`.
4. Enable **Authentication** (Email/Password provider).
5. Enable **Cloud Firestore** and deploy `firestore.rules`.
6. Enable **Realtime Database** and deploy `database.rules.json`.
7. Enable **Cloud Storage** and deploy `storage.rules`.
8. Download `google-services.json` and replace `app/google-services.json`.

---

## 18. Academic Presentation Script & Live Demonstration Workflow

### Part 1: Application Purpose & Identity (2 minutes)
* **Opening**: "JibonShield BD is an offline-first disaster emergency resilience application for Bangladesh, maintaining life-safety information even when mobile networks and power grids collapse."
* **Offline Demonstration**: Put device into **Airplane Mode**. Show that flood warnings, 64-district shelters, and disaster helplines load instantly from SQLite without network latency.

### Part 2: Architecture & Navigation (2 minutes)
* **Activities & Intents**: Demonstrate explicit navigation from Dashboard to Shelters, Water Observations, and Settings.
* **Implicit Intent**: Click the 999 hotline in Emergency Contacts to launch the system phone dialer (`ACTION_DIAL`) without dialing without permission.
* **View Binding**: Explain how generated binding classes eliminate `findViewById` and prevent null pointer exceptions.

### Part 3: UI Layouts & Lists (2 minutes)
* **RecyclerView & Filters**: Demonstrate live filtering in the Shelter Directory by typing a district name (e.g., "Sylhet" or "Chandpur").
* **Rotation**: Rotate the device to landscape mode, showing that the layout does not clip and form data is preserved.

### Part 4: Android System Components (3 minutes)
1. **Airplane Mode Receiver**: Toggle Airplane Mode on/off in quick settings; point out the dynamic status update in the dashboard banner.
2. **Battery Saver Receiver**: Toggle Battery Saver; show the power-saving warning in the UI.
3. **Photo Picker & Media**: Open the Water Observation Form, click "Add Photo", select an image, show the preview, and save the record.
4. **Foreground Service**: Tap "Start Emergency Monitoring" on the dashboard; pull down the Android notification drawer to show the persistent ongoing notification and tap "Stop Monitoring" to stop the service.

### Part 5: Firebase & Cloud Synchronization (2 minutes)
* **Authentication**: Register a new user as an "Emergency Response Volunteer" and view their profile.
* **Storage & Firestore**: Show that saving an observation queues the image and data for synchronization to Firebase Storage and Firestore via WorkManager.

---

## 19. Known Limitations & Future Roadmap
* **Official Government APIs**: FFWC and BMD do not currently offer public unauthenticated REST APIs. JibonShield BD uses verified baseline seeds and modular HTTP client adapters ready for official API integration when released.
* **Offline Map Tiles**: Future versions will bundle offline vector map tiles for GPS shelter routing without Google Maps internet access.
* **Bluetooth Mesh**: Exploring BLE mesh for direct device-to-device alerts in communication blackout zones.

---

## 20. License
Developed for Academic Android Integration, Advanced Software Engineering & Disaster Risk Reduction Demonstration.
Distributed under the MIT Academic License.
