# JibonShield BD (জীবনশীল্ড বিডি)
### Offline-First Disaster Emergency Resilience, Android System Integration & Real-Time Cloud Telemetry

[![Android Build](https://img.shields.io/badge/Android-SDK%2021--34-brightgreen.svg)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-Java%208%20%2F%20Android%20Jetpack-blue.svg)](https://developer.android.com)
[![Tests](https://img.shields.io/badge/Tests-69%2F69%20Passing%20(100%25)-success.svg)](https://junit.org)
[![Offline First](https://img.shields.io/badge/Architecture-Offline--First%20Local%20Persistence-orange.svg)](#)
[![Cloud Hybrid](https://img.shields.io/badge/Cloud-Firestore%20%2B%20RTDB%20%2B%20Storage-yellow.svg)](https://firebase.google.com)
[![ViewBinding](https://img.shields.io/badge/UI-ViewBinding%20%2B%20Material3-blueviolet.svg)](https://developer.android.com/topic/libraries/view-binding)
[![License](https://img.shields.io/badge/License-MIT%20Academic-lightgrey.svg)](LICENSE)

---

## 1. Project Introduction & Mission
**JibonShield BD (জীবনশীল্ড বিডি)** is an enterprise-grade, offline-first disaster emergency resilience and citizen environmental safety Android application engineered specifically for Bangladesh's extreme climate-vulnerability profile.

During catastrophic monsoon inundations, coastal tropical cyclones, tidal surges, riverbank erosion, or acute heatwaves, traditional cellular backhauls, broadband access, and municipal power grids suffer severe brownouts or total collapse. JibonShield BD guarantees that **100% of vital life-preserving information remains immediately available on-device without internet access** using high-performance local offline persistence as the on-device source of truth.

When connectivity is detected, the application synchronizes seamlessly with **Google Firebase** using a clean separation of concerns:
- **Cloud Firestore**: Persistent master catalog of static directories (64 districts, upazilas, shelters, disaster warning bulletins, user profiles).
- **Firebase Realtime Database (RTDB)**: Low-latency dynamic operational telemetry (live shelter occupancy tracking, emergency incident broadcast ticker, volunteer squad availability).
- **Firebase Cloud Storage**: Field photo evidence storage for community water quality observations.
- **AndroidX WorkManager**: Resilient background synchronization with zero data loss.

---

## 2. Core Problem & Real-World Context
Bangladesh is one of the world's most climate-vulnerable countries. Tens of millions of residents throughout the Sylhet Haor basin, coastal belts (Barishal, Khulna, Chattogram), and Brahmaputra/Jamuna river char areas regularly confront:
1. **Network Blackouts**: Complete cellular failures during peak flood crests and cyclone landfalls.
2. **Refuge Information Gap**: Inability to locate verified cyclone or flood shelters with confirmed capacity, water supply, and disability ramps.
3. **Emergency Call Failures**: Inability to rapidly dial verified national emergency helplines without searching online or suffering clipboard failures.
4. **Post-Flood Drinking Water Crisis**: Contaminated tubewells triggering catastrophic cholera, typhoid, and acute diarrhea outbreaks.
5. **Field Reporting Inability**: Inability to record, capture photographic evidence, and queue field water safety reports while completely offline.
6. **Summer Heat Extremes**: Lethal heatstroke spikes during April–June without localized triage and cooling protocols.

---

## 3. High-Level Architectural Highlights

### 3.1 One-Tap Native System Phone Dialing
* **Direct Implicit Intent**: Tapping any hotline or shelter contact triggers `Intent.ACTION_DIAL` with `Uri.fromParts("tel", phone, null)`.
* **Android 11+ (API 30+) Package Visibility**: Fully declared `<queries>` in `AndroidManifest.xml` targeting the `tel:` URI scheme, preventing package visibility blocks and `resolveActivity()` null-pointer pitfalls.
* **Instant Dialer Launch**: Opens the device's native dialer app pre-populated with the target emergency hotline (e.g., `999`, `1090`, `16263`, `109`, `333`), allowing instant emergency calls with zero friction.
* **Non-Blocking Clipboard Sync**: Automatically and transparently places the phone number into the device clipboard in the background without blocking the dialer screen.
* **Graceful Fallback**: If running on a non-telephony tablet without a phone dialer app, catches `ActivityNotFoundException` and provides a clipboard notice.

### 3.2 Dual-Tier Database Architecture & Separation of Concerns
To ensure high performance and prevent data duplication between cloud databases:
* **Cloud Firestore (Master Directory & User Records)**:
  - `districts/`: 64 administrative districts categorized by divisions.
  - `upazilas/`: 495+ sub-districts linked to their respective districts.
  - `shelters/`: Comprehensive shelter facilities with verified geographic coordinates, capacities, water access, and accessibility features.
  - `disaster_updates/`: Official bulletins verified against Flood Forecasting and Warning Centre (FFWC) and Bangladesh Meteorological Department (BMD).
  - `water_observations/`: Community tubewell field submissions with photo links.
  - `users/`: Secure user profiles with role attributes (`CITIZEN` vs. `VOLUNTEER`).
* **Firebase Realtime Database (Dynamic Telemetry)**:
  - `/shelter_occupancy/{shelter_id}`: Real-time capacity vs. current intake count (`capacity`, `occupancy`, `lastUpdated`). Read-only for standard citizens, live-synced to `ShelterDetailActivity`.
  - `/emergency_status/`: Real-time emergency incident alerts displayed in the `MainActivity` broadcast banner.
  - `/volunteer_status/`: Real-time deployment status of emergency response squads.

### 3.3 Author-Only Edit & Delete Security for Water Observations
* **Strict Ownership Validation**: Community water observations can only be edited or removed by the author who originally logged the entry.
* **Multi-Account Integrity**: Compares current Firebase Auth UID / local reporter identifier against `observation.getReportedBy()`. Unauthorized users are barred from editing or deleting peer reports.
* **Comprehensive Observation Details Dialog**: Tapping any observation card opens an inspection modal displaying all 12 observation parameters:
  - Source type (Tubewell, Pond, River, Piped, Rainwater)
  - Color, appearance, and odor classification
  - Measured pH level and turbidity status
  - Health safety rating (Safe / Caution / Contaminated)
  - Location, district, and coordinates
  - Observer identity and reporting timestamp
  - Full photographic field evidence

### 3.4 Android System Components & Resilience
* **Airplane Mode Detection**: Dynamically registered `AirplaneModeReceiver` informs the user of active offline isolation and prevents battery drain from futile network calls.
* **Battery Saver Mode**: Dynamically registered `BatterySaverReceiver` throttles background polling during power-restricted conditions.
* **Emergency Monitoring Service**: Persistent foreground service with high-priority ongoing notification and a one-tap "Stop Monitoring" action.
* **Photo Picker Integration**: Modern Android `PickVisualMedia` contract with local stream cloning to internal storage for offline persistence.

---

## 4. Technology Stack & Dependencies

| Component | Technology | Version / Implementation |
|---|---|---|
| **Language** | Java | Java 8 (`VERSION_1_8`) |
| **Android SDK** | Min SDK 21, Target SDK 33, Compile SDK 34 | Full backward compatibility to Android 5.0 |
| **UI Framework** | XML Layouts + View Binding | Material Components 1.8.0, ConstraintLayout 2.1.4 |
| **Local Persistence** | On-Device Local Store | Thread-Safe Offline Storage & Cache |
| **Cloud Authentication**| Firebase Authentication | Email/Password with Citizen and Volunteer roles |
| **Master Cloud Data** | Cloud Firestore | Structured directory documents and user data |
| **Realtime Telemetry** | Firebase Realtime Database | Live shelter occupancy & emergency incident stream |
| **Evidence Media** | Firebase Cloud Storage | Water observation photographs |
| **Background Sync** | AndroidX WorkManager | `work-runtime:2.8.1` (`EmergencySyncWorker`) |
| **System Services** | Android Foreground Service | `EmergencyMonitoringService` (`dataSync` type) |
| **System Receivers** | Broadcast Receivers | `AirplaneModeReceiver`, `BatterySaverReceiver` |
| **Image Loading** | Glide | `glide:4.15.1` with disk caching |
| **Testing Suite** | JUnit 4 + AndroidX Test | 69 Automated Unit Tests (100% Pass Rate) |

---

## 5. System Architecture Diagram

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
    |      (On-Device Offline Store)       |                   |  (Firebase SDKs & WorkManager Sync)  |
    |                                      |                   |                                      |
    | - emergency_contacts (Verified)      | <--- Two-Way ---> | - FirebaseAuthManager (Auth & Role)  |
    | - flood_warnings (FFWC Bulletins)    |    Sync Pass      | - FirestoreManager (Static Catalog)  |
    | - shelters (64 Districts Directory)  |                   | - RealtimeDatabaseManager (Live RTDB)|
    | - water_observations (Image URI/URL) |                   | - FirebaseStorageManager (Photos)    |
    | - preparedness_checklists            |                   | - EmergencySyncWorker (Background)   |
    +--------------------------------------+                   +--------------------------------------+
                       ^                                                          ^
                       |                                                          |
    +------------------+-------------------+                   +------------------+-------------------+
    |      Android System Components       |                   |      Dialer & System Integration     |
    | - AirplaneModeReceiver (Offline mode)|                   | - Intent.ACTION_DIAL (One-Tap Call)  |
    | - BatterySaverReceiver (Power save)  |                   | - <queries> Package Visibility (API 30+)
    | - EmergencyMonitoringService (Notif) |                   | - Photo Picker (PickVisualMedia)     |
    +--------------------------------------+                   +--------------------------------------+
```

---

## 6. Project Directory Structure

```
JIBONSHEILD BD/
├── app/
│   ├── google-services.json            # Firebase configuration file
│   ├── build.gradle                    # Dependencies, ViewBinding, SDK versions
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml     # Permissions, queries, activities, services
│   │   │   ├── java/com/jibonshield/bd/
│   │   │   │   ├── JibonShieldApp.java # Application singleton & channels
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
│   │   │       ├── layout/             # 17 View Binding XML layouts
│   │   │       ├── values/strings.xml  # Complete English localization strings
│   │   │       ├── values-bn/          # Complete Bengali (বাংলা) localization
│   │   │       └── drawable/           # Vector icons & emergency styling
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

## 7. Complete Activity Map & Navigation Flow

1. **`SplashActivity`**: Verifies database readiness, validates local seeds, loads active language preferences, and launches `MainActivity`.
2. **`MainActivity`**: Core command hub. Displays real-time local cache metric counters (active warnings, shelters, emergency hotlines, emergency kit preparedness percentage), live network status banner, real-time emergency broadcast ticker from RTDB, foreground service toggle, and navigation tiles.
3. **`LoginActivity`**: Authentication portal with email and password verification, password visibility toggle, role check, and an explicit **Offline Guest Mode** for unauthenticated field access.
4. **`RegisterActivity`**: Account registration supporting **Citizen** vs **Disaster Response Volunteer** roles, home district selection, and validation.
5. **`ProfileActivity`**: Displays user identity, verified role badge, home district, sync statistics, and account management options.
6. **`FloodWarningListActivity`**: Searchable flood bulletins categorized by severity (`CRITICAL`, `WARNING`, `ADVISORY`, `NORMAL`) with offline cache staleness notices.
7. **`FloodWarningDetailActivity`**: Detailed hydrological breakdown, river basin danger marks, affected upazilas, and official FFWC source links.
8. **`ShelterDirectoryActivity`**: Instant local search across 64 districts with upazila, facility, and capacity filters, plus one-tap shelter contact calling.
9. **`ShelterDetailActivity`**: Comprehensive shelter information (lat/long, women's facilities, ramps, generator, drinking water) and **live real-time occupancy synchronization** from Firebase RTDB.
10. **`EmergencyContactsActivity`**: Pre-seeded national disaster helplines with one-tap native phone dialer launching (`ACTION_DIAL`) and safe clipboard sync.
11. **`WaterObservationFormActivity`**: Community field observation intake form with photo picker, local file stream persistence, pH validation, turbidity assessment, and safety classification.
12. **`WaterObservationHistoryActivity`**: Searchable/filterable log of community water observations with attached photo previews, author-restricted edit/delete security, and detailed observation modal dialogs.
13. **`HeatSafetyActivity`**: WHO/DGHS extreme heat index safety matrix, symptoms triage, emergency cooling procedures, and one-tap hotline calling.
14. **`EvacuationGuidelinesActivity`**: Step-by-step pre-, during-, and post-disaster guidelines in English and Bengali.
15. **`PreparednessChecklistActivity`**: Interactive disaster kit checklist tracking essential supplies and vital documents with persistent on-device progress saving.
16. **`SettingsActivity`**: Runtime language switching (English / বাংলা), default home district selection, and manual full synchronization trigger.
17. **`AboutActivity`**: Data provenance, government disclaimers, open-source attributions, and software build metadata.

---

## 8. Data Storage & Local Entity Specifications

### 8.1 Local Offline Entity Store
* **Architecture**: Thread-safe persistent on-device local storage ensuring instant resilience during total network collapse.

1. **`flood_warnings`**:
   `id` (Unique identifier), `warning_level` (TEXT), `title_en` (TEXT), `title_bn` (TEXT), `region` (TEXT), `details_en` (TEXT), `details_bn` (TEXT), `issued_at` (TIMESTAMP), `expires_at` (TIMESTAMP), `source` (TEXT), `source_reference` (TEXT), `is_active` (BOOLEAN), `is_sample` (BOOLEAN), `last_synced_at` (TIMESTAMP).
2. **`shelters`**:
   `id` (Unique identifier), `name_en` (TEXT), `name_bn` (TEXT), `district` (TEXT), `upazila` (TEXT), `address` (TEXT), `latitude` (DOUBLE), `longitude` (DOUBLE), `capacity` (INTEGER), `current_occupancy` (INTEGER), `contact_phone` (TEXT), `shelter_type` (TEXT), `accessibility_notes` (TEXT), `source` (TEXT), `verified_at` (TEXT), `is_active` (BOOLEAN).
3. **`emergency_contacts`**:
   `id` (Unique identifier), `service_name_en` (TEXT), `service_name_bn` (TEXT), `phone_number` (TEXT), `service_description_en` (TEXT), `service_description_bn` (TEXT), `category` (TEXT), `available_hours` (TEXT), `is_toll_free` (BOOLEAN), `source` (TEXT), `verification_date` (TEXT), `is_enabled` (BOOLEAN), `priority_order` (INTEGER).
4. **`water_observations`**:
   `id` (Unique identifier), `location_name` (TEXT), `district` (TEXT), `source_type` (TEXT), `appearance` (TEXT), `odor` (TEXT), `ph_level` (DOUBLE), `turbidity_status` (TEXT), `safety_status` (TEXT), `observation_time` (TIMESTAMP), `reported_by` (TEXT), `remarks` (TEXT), `image_uri` (TEXT), `image_url` (TEXT), `sync_status` (TEXT), `created_at` (TIMESTAMP), `updated_at` (TIMESTAMP).
5. **`preparedness_checklists` & `checklist_items`**:
   Disaster emergency kit items with item descriptions, categories, and completion state.
6. **`synchronization_metadata`**:
   Tracks record counts and synchronization timestamps for offline staleness detection.

---

## 9. Cloud Security Rules & Schemas

### 9.1 Cloud Firestore (`firestore.rules`)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Public directory data is read-only to all users
    match /districts/{districtId} { allow read: if true; allow write: if false; }
    match /upazilas/{upazilaId} { allow read: if true; allow write: if false; }
    match /shelters/{shelterId} { allow read: if true; allow write: if false; }
    match /disaster_updates/{updateId} { allow read: if true; allow write: if false; }

    // User profiles are readable and editable by their owner
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Water observations: readable by all, author-only modify/delete
    match /water_observations/{obsId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
        (resource.data.reported_by == request.auth.uid || resource.data.reported_by == request.auth.token.email);
    }
  }
}
```

### 9.2 Realtime Database (`database.rules.json`)
```json
{
  "rules": {
    "shelter_occupancy": {
      ".read": true,
      ".write": "auth != null"
    },
    "emergency_status": {
      ".read": true,
      ".write": "auth != null"
    },
    "volunteer_status": {
      ".read": true,
      ".write": "auth != null"
    }
  }
}
```

### 9.3 Firebase Cloud Storage (`storage.rules`)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /water_observations/{imageId} {
      allow read: if true;
      allow write: if request.auth != null &&
                   request.resource.size < 5 * 1024 * 1024 &&
                   request.resource.contentType.matches('image/.*');
    }
  }
}
```

---

## 10. Automated Testing & Verification

The test harness contains **69 comprehensive unit tests** testing business logic, persistence, and synchronization:

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
| `Day9FirebaseAuthAndModelTest` | 2 | 0 | 0 | **PASSED** |
| `Day10SyncAndStorageTest` | 2 | 0 | 0 | **PASSED** |
| **Total** | **69** | **0** | **0** | **100% PASSED** |

---

## 11. Compilation & Deployment

### Build Debug APK
```bash
./gradlew assembleDebug
```
* **Output Path**: `app/build/outputs/apk/debug/app-debug.apk`
* **Package Name**: `com.jibonshield.bd`
* **Size**: ~12 MB
* **Target OS Range**: Android 5.0 (Lollipop / API 21) through Android 14/15 (API 34)

### Install via ADB to Connected Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 12. Academic Presentation Script & Live Demonstration Workflow

### Part 1: Problem Space & Offline Core (2 minutes)
* **Opening**: "Bangladesh experiences recurrent climate extremes where cellular infrastructure and electricity collapse. JibonShield BD ensures 100% of life-critical disaster information remains available offline."
* **Live Demo**: Enable **Airplane Mode** on the phone. Launch JibonShield BD. Demonstrate that flood bulletins, the 64-district shelter directory, and disaster contacts load immediately from local offline storage.

### Part 2: One-Tap Emergency Calling & Shelters (2 minutes)
* **Hotlines Demo**: Open **Emergency Contacts**. Tap the "Call Now" button on the **999** or **1090** card. Show that it immediately launches the phone's native dialer with the number pre-filled.
* **Shelters Demo**: Open **Shelter Directory**. Filter by "Sylhet" or "Feni". Open a shelter to show its facilities, coordinates, and **live real-time occupancy synchronization** from RTDB.

### Part 3: Field Water Observation & Author Ownership (3 minutes)
* **Photo & Form**: Open **Water Observation Form**. Attach a photo of a water source using the photo picker, enter observation details (pH 7.2, clear, odorless, tubewell), and save offline.
* **Details Modal**: Tap the observation in **Observation History** to open the 12-parameter inspection dialog with photo evidence.
* **Author Security**: Switch accounts or demonstrate that non-author accounts cannot edit or delete reports filed by another volunteer.

### Part 4: Android System Services & Broadcasts (2 minutes)
* **Foreground Service**: On the dashboard, tap "Start Emergency Monitoring". Pull down the system notification shade to display the persistent notification and demonstrate the one-tap "Stop Monitoring" button.
* **Broadcast Receivers**: Demonstrate the airplane mode and battery saver state indicators.

---

## 13. License & Academic Attribution
Developed as an advanced academic and humanitarian engineering initiative for Disaster Risk Reduction (DRR) in Bangladesh. Distributed under the MIT Academic License.
