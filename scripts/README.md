# JibonShield BD - Firebase Cloud Firestore Data Import

This Node.js script uploads the complete Bangladesh administrative dataset (8 Divisions, 64 Districts, 495 Upazilas), designated disaster shelters, and official disaster alerts into your Firebase Cloud Firestore database.

## Prerequisites
- Node.js (v16+)
- Firebase Admin Service Account credentials (`serviceAccountKey.json`)

## Steps to Run

1. **Obtain Service Account Key:**
   - Open [Firebase Console](https://console.firebase.google.com/)
   - Go to **Project Settings** > **Service accounts**
   - Click **Generate new private key**
   - Save the downloaded JSON file into this folder as `serviceAccountKey.json`
     *(Note: `serviceAccountKey.json` is ignored by Git and must NEVER be committed to version control or included in the Android APK)*

2. **Install Dependencies:**
   ```bash
   cd scripts
   npm install
   ```

3. **Run Import:**
   ```bash
   npm start
   # or
   node import_bangladesh_data.js
   ```

4. **Verify:**
   - Open Firestore Database in the Firebase Console.
   - You will see the collections populated:
     - `divisions` (8 documents)
     - `districts` (64 documents)
     - `upazilas` (495 documents)
     - `shelters` (initial verified shelters)
     - `disaster_updates` (active advisories)

