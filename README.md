# JibonShield BD (জীবনশীল্ড বিডি)
### Offline-First Disaster Emergency Resilience & Life Safety Android Application

[![Android Build](https://img.shields.io/badge/Android-SDK%2021--33-brightgreen.svg)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-Java%208%20%2F%20Android%20Jetpack-blue.svg)](https://developer.android.com)
[![Tests](https://img.shields.io/badge/Tests-62%2F62%20Passing%20(100%25)-success.svg)](https://junit.org)
[![Offline First](https://img.shields.io/badge/Architecture-Offline--First%20SQLite-orange.svg)](https://sqlite.org)
[![License](https://img.shields.io/badge/License-Academic%20%2F%20Open-lightgrey.svg)](LICENSE)

---

## 1. Project Purpose & Problem Statement

### Problem Statement
Bangladesh is one of the most climate-vulnerable countries on Earth, regularly facing devastating flash floods in the Sylhet and Haor basins, monsoon inundation along the Jamuna, Padma, and Meghna river basins, coastal cyclones along the Bay of Bengal, and extreme heatwaves across the northern districts. 

During acute disaster onset, mobile cellular towers, electrical grids, and internet gateways frequently collapse or experience severe congestion. Vulnerable citizens, local responders, and families find themselves isolated without access to:
1. Active flood alerts and river danger level warnings.
2. Verified physical shelter locations with capacity and accessibility details.
3. Critical emergency helpline numbers (disaster response, fire service, medical ambulances).
4. Potable drinking water safety guidelines and field test observations.
5. Evidence-based extreme heatwave safety and heatstroke response protocols.
6. Offline actionable family evacuation checklists.

### Project Purpose
**JibonShield BD** is built from the ground up as an **offline-first Android life-safety utility**. It ensures that 100% of essential life-preserving information is permanently stored, queried, and rendered locally on the user's smartphone without requiring an active internet connection. When network connectivity is intermittent or briefly restored, JibonShield BD provides safe, schema-validated, rollback-protected synchronization without ever erasing or corrupting existing cached safety records.

---

## 2. Target Users
* **Frontline Citizens & Rural Communities**: Families living in riverine char areas, low-lying Haor basins, and cyclone-prone coastal belts.
* **Community Emergency Volunteers**: Red Crescent volunteers, Cyclone Preparedness Programme (CPP) workers, and local youth responders.
* **Local Upazila & Union Disaster Committees**: Local field workers recording water quality field observations and verifying shelter capacities.
* **Urban Residents**: Vulnerable urban populations confronting severe urban heat island waves and flash waterlogging.

---

## 3. Implemented vs. Planned Features

### ✅ Implemented Features (Day 1 – Day 7)
* **Offline-First SQLite Database Architecture**: Zero-network dependency with SQLite database migrations, transactional atomic writes, indices, and seed data.
* **Flood Warning & Alert System**: Real-time severity classification (`CRITICAL`, `WARNING`, `ADVISORY`, `NORMAL`), river basin tracking, official bulletin references, expiry status tracking (`CURRENT`, `CACHED`, `EXPIRED`), and stale-data messaging.
* **Shelter Directory & Geographic Finder**: Local multi-parameter search (by name, district, upazila, shelter type), capacity vs. occupancy tracking, emergency contact dialing, and accessibility indicators (ramps, electricity, clean water, medical post, dedicated women's spaces).
* **Emergency Helplines & Safe Dialer**: One-tap direct dialer launching for national services (999, 1090, 109, 333, Red Crescent, Fire Service, Naval Police, Child Helpline) with input sanitization and zero auto-dial risks.
* **Drinking Water Observation Module**: Local field logging of water source type, physical appearance, odor, measured pH (0.0–14.0), turbidity, and non-certified community observation integrity constraints.
* **Heat Safety & First Aid Module**: Educational triage matrix for Heat Exhaustion vs. Life-Threatening Heat Stroke, cooling protocols, vulnerable group warnings (infants, elderly, pregnant women), and hydration guidance.
* **Educational Evacuation Guidance**: Step-by-step pre-flood, during-flood, and post-flood survival guidelines formatted in bilingual Bengali and English.
* **Family Preparedness Checklist**: Interactive checklist categorized by Supplies, Documents, and Family Safety with real-time percentage progress tracking and database persistence.
* **Bilingual Localization (English & বাংলা)**: Full Unicode Bengali script support with seamless in-app language switching and runtime preference persistence.
* **Resilient Network Synchronization Client**: Transactional HTTP sync engine with DNS timeout handling, 4xx/5xx HTTP error containment, schema validation, and rollback protection guaranteeing zero data loss.
* **62 Comprehensive Automated Unit Tests**: 100% passing test suite spanning model integrity, database constraints, UI logic, sync validation, and safety disclaimers.

### ⏳ Planned Future Features (Post-Release Roadmap)
* **Official Government API Integration**: Direct machine-to-machine REST integration once the Flood Forecasting and Warning Centre (FFWC/BWDB) and Bangladesh Meteorological Department (BMD) launch public authenticated JSON endpoints.
* **Peer-to-Peer Bluetooth Mesh Sync**: Direct device-to-device synchronization of river observations and shelter occupancy in communication blackout zones.
* **Interactive Offline Map Tiles**: Pre-bundled vector or MBTiles offline mapping for visual shelter navigation without Google Play Services.
* **SMS-Triggered Warning Broadcasts**: Automatic alert parsing via verified carrier-grade SMS gateway broadcasts.

---

## 4. Technical Stack

| Layer | Technology |
|---|---|
| **Platform** | Android OS (API 21 Android 5.0 Lollipop through API 33 Android 13) |
| **Language** | Java 8 (`sourceCompatibility` & `targetCompatibility` 1.8) |
| **UI Components** | Android Jetpack AppCompat, Material Components 1.8.0, ConstraintLayout 2.1.4, RecyclerView 1.3.0, CardView 1.0.0 |
| **Local Storage** | Android SQLite (`SQLiteOpenHelper`) with explicit Foreign Keys, Indices, and Transactions |
| **Networking** | Standard Android `HttpURLConnection` wrapped in `NetworkHttpClient` with socket timeout enforcement |
| **Data Parsing** | Android Platform `org.json` with strict schema validation |
| **Testing** | JUnit 4.13.2, AndroidX Test Runner, Espresso Core |
| **Build System** | Gradle 8.0 / Android Gradle Plugin 8.1.0 |

---

## 5. Architecture Overview

```
                      +----------------------------------+
                      |         User Interface           |
                      |  (Activities, Adapters, Views)   |
                      +-----------------+----------------+
                                        |
                                        v
                      +----------------------------------+
                      |       Emergency Repository       |
                      |   (Thread-Safe Orchestration)    |
                      +--------+----------------+--------+
                               |                |
                +--------------+                +---------------+
                |                                               |
                v                                               v
+-------------------------------+               +-------------------------------+
|     Local SQLite Database     |               |      Sync & Remote Network    |
|   (EmergencyDbHelper.java)    |               |       (SyncManager.java)      |
|                               |               |                               |
| - flood_warnings              |               | - NetworkHttpClient           |
| - shelters                    |               | - RemoteDataValidator         |
| - emergency_contacts          |               | - Transactional Rollback      |
| - water_observations          |               +---------------+---------------+
| - checklist_items             |                               |
| - app_metadata                |                               v
+-------------------------------+               +-------------------------------+
                                                |     External Web Endpoints    |
                                                |   (FFWC, BMD, Open-Meteo)     |
                                                +-------------------------------+
```

The application strictly adheres to the **Offline-First Repository Architecture**:
1. **The Database is the Single Source of Truth**: UI components observe and query the local SQLite database via `EmergencyRepository`. Activities never block on network sockets or wait for remote responses to render.
2. **Network Operations Are Non-Destructive**: Network sync operates on a background thread. Incoming data is passed through `RemoteDataValidator`. If valid, it is committed to SQLite inside an atomic transaction (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`). If network calls fail, timeout, or return corrupt data, the transaction rolls back cleanly, leaving local cache completely untouched.
3. **Data Provenance & Safety Tagging**: Every record tracks `is_active`, `is_sample`, `source`, `source_reference`, and `last_synced_at`. Citizen observations are explicitly marked as community records to prevent confusion with accredited laboratory test results.

---

## 6. Project Directory Structure

```
JIBONSHEILD BD/
├── app/
│   ├── build.gradle
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/jibonshield/bd/
│   │   │   │   ├── activities/
│   │   │   │   │   ├── SplashActivity.java
│   │   │   │   │   ├── MainActivity.java
│   │   │   │   │   ├── FloodWarningListActivity.java
│   │   │   │   │   ├── ShelterDirectoryActivity.java
│   │   │   │   │   ├── EmergencyContactsActivity.java
│   │   │   │   │   ├── WaterObservationFormActivity.java
│   │   │   │   │   ├── WaterObservationHistoryActivity.java
│   │   │   │   │   ├── HeatSafetyActivity.java
│   │   │   │   │   ├── EvacuationGuidelinesActivity.java
│   │   │   │   │   ├── PreparednessChecklistActivity.java
│   │   │   │   │   └── SettingsActivity.java
│   │   │   │   ├── adapters/
│   │   │   │   │   ├── FloodWarningAdapter.java
│   │   │   │   │   ├── ShelterAdapter.java
│   │   │   │   │   ├── EmergencyContactAdapter.java
│   │   │   │   │   ├── WaterObservationAdapter.java
│   │   │   │   │   └── ChecklistAdapter.java
│   │   │   │   ├── database/
│   │   │   │   │   └── EmergencyDbHelper.java
│   │   │   │   ├── models/
│   │   │   │   │   ├── FloodWarning.java
│   │   │   │   │   ├── Shelter.java
│   │   │   │   │   ├── EmergencyContact.java
│   │   │   │   │   ├── WaterObservation.java
│   │   │   │   │   ├── ChecklistItem.java
│   │   │   │   │   └── ChecklistProgress.java
│   │   │   │   ├── repositories/
│   │   │   │   │   └── EmergencyRepository.java
│   │   │   │   ├── sync/
│   │   │   │   │   ├── NetworkHttpClient.java
│   │   │   │   │   ├── RemoteDataValidator.java
│   │   │   │   │   ├── SyncManager.java
│   │   │   │   │   └── SyncResult.java
│   │   │   │   └── utils/
│   │   │   │       ├── LocaleHelper.java
│   │   │   │       └── PreferenceUtils.java
│   │   │   └── res/
│   │   │       ├── layout/             # All XML layouts with responsive constraints
│   │   │       ├── values/strings.xml  # Complete English localization strings
│   │   │       ├── values-bn/strings.xml # Complete Bengali (বাংলা) strings
│   │   │       ├── drawable/           # Icons and vector graphics
│   │   │       └── menu/               # Sync refresh and overflow menus
│   │   └── test/java/com/jibonshield/bd/
│   │       ├── Day2VerificationTest.java       (6 tests)
│   │       ├── Day3DatabaseTest.java           (13 tests)
│   │       ├── Day4FeatureTest.java            (12 tests)
│   │       ├── Day5MasterTest.java             (10 tests)
│   │       ├── Day6IntegrationTest.java        (14 tests)
│   │       └── Day7ReleaseVerificationTest.java (7 tests)
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 7. Database Overview & Schema Integrity

Database Name: `jibonshield.db`  
Version: `1`

### Tables
1. **`flood_warnings`**: Stores official alerts, warning severity (`CRITICAL`, `WARNING`, `ADVISORY`, `NORMAL`), issued timestamps, expiry timestamps, affected river basin/region, bulletin reference, and verification status.
2. **`shelters`**: Stores designated cyclone/flood shelters with geographic coordinates (latitude, longitude), total capacity, current occupancy, district, upazila, shelter type, accessibility features (ramps, drinking water, electricity, women-only facilities), and contact numbers.
3. **`emergency_contacts`**: Pre-seeded official helplines (National 999, Disaster 1090, Child Helpline 1098, Women & Children Helpline 109, Fire Service, Red Crescent).
4. **`water_observations`**: User-recorded community drinking water field observations (pH, turbidity, odor, appearance, location, date, observer name, remarks).
5. **`checklist_items`**: Pre-populated emergency preparedness checklist items (drinking water, dry food, ORS, flashlight, first aid, documents) with completion states.
6. **`app_metadata`**: Key-value storage tracking sync timestamps, app version, and last refresh status.

---

## 8. Data Sources, Realities, and Limitations

### Official Government Portals in Bangladesh
* **Flood Forecasting and Warning Centre (FFWC / BWDB)**: `http://www.ffwc.gov.bd/` — FFWC publishes river stage bulletins, hydrographs, and flood outlooks primarily via web tables and PDF bulletins. **There is currently no public unauthenticated open REST API**.
* **Bangladesh Meteorological Department (BMD)**: `https://www.bmd.gov.bd/` — BMD provides weather forecasts and cyclone warning signals through web updates and official press releases.
* **Ministry of Disaster Management and Relief (MoDMR)**: Pre-seeded cyclone shelter registers are sourced from MoDMR and CPP public census records.

### Safety Disclaimers & Integrity
> [!IMPORTANT]
> **Academic Project & Demonstration Context**: JibonShield BD is an advanced academic and engineering demonstration. It is **not** an officially endorsed national civil defense communication channel.
> * In an imminent life-threatening emergency, always follow official radio/television broadcasts and directions issued by the Cyclone Preparedness Programme (CPP), local administration, and national emergency operators (999 / 1090).
> * **Water Observation Disclaimer**: Citizen water observations recorded in this app reflect localized, unverified visual/pH indicator field observations. They must **never** be treated as certified laboratory test results or absolute safety certifications.

---

## 9. Testing & Quality Assurance

### Automated Unit Test Summary
The project contains **62 automated unit tests** executing without mocking failures across 6 test suites:

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
| **Total** | **62** | **0** | **0** | **100% PASSED** |

### Offline & Runtime Scenarios Verified
1. **Fresh Installation Without Internet**: App boots cleanly from SQLite seed data without network exceptions.
2. **Airplane Mode Execution**: Full navigation across all 10 activities works seamlessly without crashes.
3. **Offline Water Observation Entry**: Record saved to local database, viewable immediately in observation history.
4. **Shelter Search & Filter**: Instant filtering by district/upazila without network latency.
5. **Checklist Persistence**: Checkbox states persist across complete application restart.
6. **Failed Sync Containment**: Network timeouts or unreachable host errors display non-intrusive feedback without altering cached database records.

---

## 10. APK Build & Installation Guide

### Building Debug APK via Gradle
To build the debug APK from the command line:

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

### Verified Output Location
Upon completion, the generated APK is verified at:
```
app/build/outputs/apk/debug/app-debug.apk
```
* **Binary Size**: ~5.65 MB (5,925,818 bytes)
* **Minimum Android Version**: Android 5.0 (API 21)
* **Target Android Version**: Android 13 (API 33)

### Installing on Device / Emulator
Ensure USB Debugging is enabled on your Android smartphone:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 11. Release & Signing Workflow

### Debug APK vs. Signed Release APK
* **Debug APK (`app-debug.apk`)**: Automatically signed with Android SDK's default debug keystore (`androiddebugkey`). Intended exclusively for developers, QA testers, and local demonstration. Not suitable for Google Play distribution.
* **Signed Release APK (`app-release.apk`)**: Compiled with code shrinking (ProGuard/R8) and cryptographically signed with a secure, private developer keystore (`.jks` / `.keystore`).

### Release Signing Guidelines
1. In Android Studio: Navigate to **Build > Generate Signed Bundle / APK...**
2. Select **APK** and click **Next**.
3. Create or select your private keystore file.
4. **Security Notice**: Never commit keystore files (`*.jks`) or passwords to GitHub or version control. Store signing keys in a secure secrets vault or local `gradle.properties` outside source tracking.

---

## 12. License & Acknowledgments
* **Project**: JibonShield BD (জীবনশীল্ড বিডি)
* **Author / Developer**: Senior Android Integration & QA Release Team
* **Data Provenance**: Bangladesh Water Development Board (BWDB), Flood Forecasting and Warning Centre (FFWC), Bangladesh Meteorological Department (BMD), Ministry of Disaster Management and Relief (MoDMR).
