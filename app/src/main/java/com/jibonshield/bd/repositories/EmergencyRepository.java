package com.jibonshield.bd.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.jibonshield.bd.database.DatabaseContract;
import com.jibonshield.bd.database.DatabaseHelper;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.WaterObservation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer abstracting offline SQLite database operations across worker threads.
 */
public class EmergencyRepository {

    public interface DatabaseInitCallback {
        void onDatabaseReady(int contactCount);
        void onError(Exception error);
    }

    public interface DataCallback<T> {
        void onDataLoaded(T data);
        void onError(Exception error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception error);
    }

    private final DatabaseHelper dbHelper;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public EmergencyRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Ensures the database is initialized and seed data is verified asynchronously.
     */
    public void ensureDatabaseInitialized(DatabaseInitCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.EmergencyContactEntry.TABLE_NAME, null);
                int count = 0;
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                    cursor.close();
                }
                final int totalCount = count;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDatabaseReady(totalCount);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }

    /**
     * Loads all verified national emergency hotlines.
     */
    public void getEmergencyContacts(DataCallback<List<EmergencyContact>> callback) {
        executorService.execute(() -> {
            List<EmergencyContact> list = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String sortOrder = DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER + " ASC";
                Cursor cursor = db.query(
                        DatabaseContract.EmergencyContactEntry.TABLE_NAME,
                        null, null, null, null, null, sortOrder
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry._ID));
                        String nameEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN));
                        String nameBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN));
                        String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER));
                        String cat = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY));
                        String hours = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS));
                        boolean tollFree = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE)) == 1;
                        int priority = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER));
                        list.add(new EmergencyContact(id, nameEn, nameBn, phone, cat, hours, tollFree, priority));
                    }
                    cursor.close();
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Loads flood warnings, optionally filtered by region.
     */
    public void getFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        executorService.execute(() -> {
            List<FloodWarning> list = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String selection = null;
                String[] selectionArgs = null;
                if (regionFilter != null && !regionFilter.isEmpty() && !regionFilter.equalsIgnoreCase("All")) {
                    selection = DatabaseContract.FloodWarningEntry.COLUMN_REGION + " LIKE ?";
                    selectionArgs = new String[]{"%" + regionFilter + "%"};
                }
                Cursor cursor = db.query(
                        DatabaseContract.FloodWarningEntry.TABLE_NAME,
                        null, selection, selectionArgs, null, null,
                        DatabaseContract.FloodWarningEntry._ID + " ASC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry._ID));
                        String level = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_WARNING_LEVEL));
                        String titleEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_EN));
                        String titleBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_BN));
                        String region = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_REGION));
                        String detailsEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_EN));
                        String detailsBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_BN));
                        String issued = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_ISSUED_AT));
                        String expires = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_EXPIRES_AT));
                        String source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE));
                        boolean isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE)) == 1;
                        boolean isSample = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE)) == 1;
                        list.add(new FloodWarning(id, level, titleEn, titleBn, region, detailsEn, detailsBn, issued, expires, source, isActive, isSample));
                    }
                    cursor.close();
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Loads shelters with optional query search and district filtering.
     */
    public void getShelters(String query, String district, DataCallback<List<Shelter>> callback) {
        executorService.execute(() -> {
            List<Shelter> list = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                StringBuilder where = new StringBuilder();
                List<String> args = new ArrayList<>();

                if (district != null && !district.isEmpty() && !district.equalsIgnoreCase("All") && !district.equalsIgnoreCase("All Districts")) {
                    where.append(DatabaseContract.ShelterEntry.COLUMN_DISTRICT).append(" = ?");
                    args.add(district);
                }

                if (query != null && !query.trim().isEmpty()) {
                    if (where.length() > 0) where.append(" AND ");
                    where.append("(")
                            .append(DatabaseContract.ShelterEntry.COLUMN_NAME_EN).append(" LIKE ? OR ")
                            .append(DatabaseContract.ShelterEntry.COLUMN_NAME_BN).append(" LIKE ? OR ")
                            .append(DatabaseContract.ShelterEntry.COLUMN_UPAZILA).append(" LIKE ?")
                            .append(")");
                    String pattern = "%" + query.trim() + "%";
                    args.add(pattern);
                    args.add(pattern);
                    args.add(pattern);
                }

                String selection = where.length() > 0 ? where.toString() : null;
                String[] selectionArgs = args.isEmpty() ? null : args.toArray(new String[0]);

                Cursor cursor = db.query(
                        DatabaseContract.ShelterEntry.TABLE_NAME,
                        null, selection, selectionArgs, null, null,
                        DatabaseContract.ShelterEntry.COLUMN_DISTRICT + " ASC, " + DatabaseContract.ShelterEntry.COLUMN_NAME_EN + " ASC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry._ID));
                        String nameEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_NAME_EN));
                        String nameBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_NAME_BN));
                        String dist = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_DISTRICT));
                        String upazila = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_UPAZILA));
                        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_LATITUDE));
                        double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE));
                        int cap = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CAPACITY));
                        int occ = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY));
                        String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE));
                        boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE)) == 1;
                        String verified = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT));
                        list.add(new Shelter(id, nameEn, nameBn, dist, upazila, lat, lng, cap, occ, phone, active, verified));
                    }
                    cursor.close();
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Inserts a user-recorded water observation into SQLite.
     */
    public void saveWaterObservation(WaterObservation obs, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_LOCATION_NAME, obs.getLocationName());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_DISTRICT, obs.getDistrict());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_SOURCE_TYPE, obs.getSourceType());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_APPEARANCE, obs.getAppearance());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_ODOR, obs.getOdor());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_PH_LEVEL, obs.getPhLevel());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_TURBIDITY_STATUS, obs.getTurbidityStatus());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_SAFETY_STATUS, obs.getSafetyStatus());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_OBSERVATION_TIME, obs.getObservationTime());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_REPORTED_BY, obs.getReportedBy());
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_REMARKS, obs.getRemarks());
                db.insert(DatabaseContract.WaterObservationEntry.TABLE_NAME, null, cv);
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Loads history of water observations.
     */
    public void getWaterObservations(DataCallback<List<WaterObservation>> callback) {
        executorService.execute(() -> {
            List<WaterObservation> list = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.query(
                        DatabaseContract.WaterObservationEntry.TABLE_NAME,
                        null, null, null, null, null,
                        DatabaseContract.WaterObservationEntry._ID + " DESC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry._ID));
                        String loc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_LOCATION_NAME));
                        String dist = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_DISTRICT));
                        String src = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_SOURCE_TYPE));
                        String app = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_APPEARANCE));
                        String odor = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_ODOR));
                        double ph = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_PH_LEVEL));
                        String turb = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_TURBIDITY_STATUS));
                        String safe = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_SAFETY_STATUS));
                        String time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_OBSERVATION_TIME));
                        String rep = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_REPORTED_BY));
                        String rem = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_REMARKS));
                        list.add(new WaterObservation(id, loc, dist, src, app, odor, ph, turb, safe, time, rep, rem));
                    }
                    cursor.close();
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Loads all checklist items.
     */
    public void getChecklistItems(DataCallback<List<ChecklistItem>> callback) {
        executorService.execute(() -> {
            List<ChecklistItem> list = new ArrayList<>();
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.query(
                        DatabaseContract.ChecklistItemEntry.TABLE_NAME,
                        null, null, null, null, null,
                        DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID + " ASC, " + DatabaseContract.ChecklistItemEntry._ID + " ASC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry._ID));
                        long catId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID));
                        String en = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_EN));
                        String bn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_BN));
                        boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED)) == 1;
                        boolean essential = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL)) == 1;
                        list.add(new ChecklistItem(id, catId, en, bn, completed, essential));
                    }
                    cursor.close();
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Updates completion status of a checklist item.
     */
    public void updateChecklistItem(long itemId, boolean completed, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, completed ? 1 : 0);
                db.update(DatabaseContract.ChecklistItemEntry.TABLE_NAME, cv,
                        DatabaseContract.ChecklistItemEntry._ID + " = ?", new String[]{String.valueOf(itemId)});
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Resets all checklist items to unchecked state.
     */
    public void resetAllChecklistItems(ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, 0);
                db.update(DatabaseContract.ChecklistItemEntry.TABLE_NAME, cv, null, null);
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }
}
