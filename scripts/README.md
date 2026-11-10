# JibonShield BD - Database Management

All Firebase Cloud Firestore database seeding, administrative hierarchy management, and synchronization are implemented in **100% pure Android Java**:
- **Seeder Class**: `com.jibonshield.bd.firebase.FirestoreDataSeeder`
- **Sync Engine**: `com.jibonshield.bd.sync.FirestoreSyncManager`
- **Offline Cache**: Room Database (`com.jibonshield.bd.database.AppDatabase`)

No Node.js, JavaScript, or external scripting runtimes are required.
You can seed or refresh Cloud Firestore directly from the Android app under **Settings > Data Synchronization > Seed / Upload Data to Firestore (Java)**.
