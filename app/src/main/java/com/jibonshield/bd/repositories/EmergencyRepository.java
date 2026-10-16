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
import com.jibonshield.bd.models.ChecklistProgress;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.PreparednessChecklist;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.SyncMetadata;
import com.jibonshield.bd.models.WaterObservation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer managing local persistent SQLite database operations for JibonShield BD.
 * Guarantees zero main thread blocking, thread safety, parameterized queries,
 * exception containment, and clean memory resource management.
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

    // =========================================================================
    // 1. DATABASE INITIALIZATION & VERIFICATION
    // =========================================================================

    public void ensureDatabaseInitialized(DatabaseInitCallback callback) {
        executorService.execute(() -> {
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.EmergencyContactEntry.TABLE_NAME, null);
                int count = 0;
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
                final int totalCount = count;
                mainHandler.post(() -> {
                    if (callback != null) callback.onDatabaseReady(totalCount);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // =========================================================================
    // 2. EMERGENCY CONTACTS REPOSITORY
    // =========================================================================

    public void getEmergencyContacts(DataCallback<List<EmergencyContact>> callback) {
        getEmergencyContactsInternal(false, callback);
    }

    public void getVerifiedEmergencyContacts(DataCallback<List<EmergencyContact>> callback) {
        getEmergencyContactsInternal(true, callback);
    }

    private void getEmergencyContactsInternal(boolean enabledOnly, DataCallback<List<EmergencyContact>> callback) {
        executorService.execute(() -> {
            List<EmergencyContact> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String selection = enabledOnly ? DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED + " = 1" : null;
                String sortOrder = DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER + " ASC";
                cursor = db.query(
                        DatabaseContract.EmergencyContactEntry.TABLE_NAME,
                        null, selection, null, null, null, sortOrder
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        list.add(cursorToEmergencyContact(cursor));
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void insertEmergencyContact(EmergencyContact contact, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = emergencyContactToContentValues(contact);
                db.insertOrThrow(DatabaseContract.EmergencyContactEntry.TABLE_NAME, null, cv);
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private EmergencyContact cursorToEmergencyContact(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry._ID));
        String nameEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN));
        String nameBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN));
        String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER));
        String descEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN));
        String descBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN));
        String cat = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY));
        String hours = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS));
        boolean tollFree = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE)) == 1;
        String source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE));
        String verDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE));
        boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED)) == 1;
        int priority = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER));

        return new EmergencyContact(id, nameEn, nameBn, phone, descEn, descBn, cat, hours, tollFree, source, verDate, enabled, priority);
    }

    private ContentValues emergencyContactToContentValues(EmergencyContact contact) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN, contact.getServiceNameEn());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN, contact.getServiceNameBn());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER, contact.getPhoneNumber());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN, contact.getServiceDescriptionEn());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN, contact.getServiceDescriptionBn());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY, contact.getCategory());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS, contact.getAvailableHours());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE, contact.isTollFree() ? 1 : 0);
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE, contact.getSource());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE, contact.getVerificationDate());
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED, contact.isEnabled() ? 1 : 0);
        cv.put(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER, contact.getPriorityOrder());
        return cv;
    }

    // =========================================================================
    // 3. FLOOD WARNINGS REPOSITORY
    // =========================================================================

    public void getFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        getFloodWarningsInternal(regionFilter, null, callback);
    }

    public void getActiveFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        getFloodWarningsInternal(regionFilter, Boolean.TRUE, callback);
    }

    public void getExpiredFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        getFloodWarningsInternal(regionFilter, Boolean.FALSE, callback);
    }

    private void getFloodWarningsInternal(String regionFilter, Boolean activeFilter, DataCallback<List<FloodWarning>> callback) {
        executorService.execute(() -> {
            List<FloodWarning> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                StringBuilder where = new StringBuilder();
                List<String> args = new ArrayList<>();

                if (regionFilter != null && !regionFilter.isEmpty() && !regionFilter.equalsIgnoreCase("All")) {
                    where.append(DatabaseContract.FloodWarningEntry.COLUMN_REGION).append(" LIKE ?");
                    args.add("%" + regionFilter + "%");
                }

                String selection = where.length() > 0 ? where.toString() : null;
                String[] selectionArgs = args.isEmpty() ? null : args.toArray(new String[0]);

                cursor = db.query(
                        DatabaseContract.FloodWarningEntry.TABLE_NAME,
                        null, selection, selectionArgs, null, null,
                        DatabaseContract.FloodWarningEntry._ID + " DESC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        FloodWarning warning = cursorToFloodWarning(cursor);
                        if (activeFilter != null) {
                            if (activeFilter && !warning.isExpired()) {
                                list.add(warning);
                            } else if (!activeFilter && warning.isExpired()) {
                                list.add(warning);
                            }
                        } else {
                            list.add(warning);
                        }
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void getFloodWarningById(long id, DataCallback<FloodWarning> callback) {
        executorService.execute(() -> {
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.FloodWarningEntry.TABLE_NAME,
                        null,
                        DatabaseContract.FloodWarningEntry._ID + " = ?",
                        new String[]{String.valueOf(id)},
                        null, null, null
                );
                FloodWarning warning = null;
                if (cursor != null && cursor.moveToFirst()) {
                    warning = cursorToFloodWarning(cursor);
                }
                final FloodWarning result = warning;
                mainHandler.post(() -> callback.onDataLoaded(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    private FloodWarning cursorToFloodWarning(Cursor cursor) {
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
        String sourceRef = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE));
        boolean isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE)) == 1;
        boolean isSample = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE)) == 1;
        long lastSynced = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FloodWarningEntry.COLUMN_LAST_SYNC_TIMESTAMP));

        return new FloodWarning(id, level, titleEn, titleBn, region, detailsEn, detailsBn, issued, expires, source, sourceRef, isActive, isSample, lastSynced);
    }

    // =========================================================================
    // 4. SHELTERS REPOSITORY
    // =========================================================================

    public void getShelters(String query, String district, DataCallback<List<Shelter>> callback) {
        searchShelters(query, district, null, callback);
    }

    public void searchShelters(String query, String district, String upazila, DataCallback<List<Shelter>> callback) {
        executorService.execute(() -> {
            List<Shelter> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                StringBuilder where = new StringBuilder();
                List<String> args = new ArrayList<>();

                if (district != null && !district.isEmpty() && !district.equalsIgnoreCase("All") && !district.equalsIgnoreCase("All Districts")) {
                    where.append(DatabaseContract.ShelterEntry.COLUMN_DISTRICT).append(" = ?");
                    args.add(district);
                }

                if (upazila != null && !upazila.isEmpty() && !upazila.equalsIgnoreCase("All")) {
                    if (where.length() > 0) where.append(" AND ");
                    where.append(DatabaseContract.ShelterEntry.COLUMN_UPAZILA).append(" = ?");
                    args.add(upazila);
                }

                if (query != null && !query.trim().isEmpty()) {
                    if (where.length() > 0) where.append(" AND ");
                    where.append("(")
                            .append(DatabaseContract.ShelterEntry.COLUMN_NAME_EN).append(" LIKE ? OR ")
                            .append(DatabaseContract.ShelterEntry.COLUMN_NAME_BN).append(" LIKE ? OR ")
                            .append(DatabaseContract.ShelterEntry.COLUMN_UPAZILA).append(" LIKE ? OR ")
                            .append(DatabaseContract.ShelterEntry.COLUMN_ADDRESS).append(" LIKE ?")
                            .append(")");
                    String pattern = "%" + query.trim() + "%";
                    args.add(pattern);
                    args.add(pattern);
                    args.add(pattern);
                    args.add(pattern);
                }

                String selection = where.length() > 0 ? where.toString() : null;
                String[] selectionArgs = args.isEmpty() ? null : args.toArray(new String[0]);

                cursor = db.query(
                        DatabaseContract.ShelterEntry.TABLE_NAME,
                        null, selection, selectionArgs, null, null,
                        DatabaseContract.ShelterEntry.COLUMN_DISTRICT + " ASC, " + DatabaseContract.ShelterEntry.COLUMN_NAME_EN + " ASC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        list.add(cursorToShelter(cursor));
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void getUpazilasForDistrict(String district, DataCallback<List<String>> callback) {
        executorService.execute(() -> {
            List<String> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String selection = (district != null && !district.isEmpty() && !district.equalsIgnoreCase("All") && !district.equalsIgnoreCase("All Districts"))
                        ? DatabaseContract.ShelterEntry.COLUMN_DISTRICT + " = ?" : null;
                String[] selectionArgs = selection != null ? new String[]{district} : null;
                cursor = db.query(
                        true,
                        DatabaseContract.ShelterEntry.TABLE_NAME,
                        new String[]{DatabaseContract.ShelterEntry.COLUMN_UPAZILA},
                        selection, selectionArgs, null, null,
                        DatabaseContract.ShelterEntry.COLUMN_UPAZILA + " ASC", null
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String upazila = cursor.getString(0);
                        if (upazila != null && !upazila.trim().isEmpty() && !list.contains(upazila)) {
                            list.add(upazila);
                        }
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void getShelterById(long id, DataCallback<Shelter> callback) {
        executorService.execute(() -> {
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.ShelterEntry.TABLE_NAME,
                        null,
                        DatabaseContract.ShelterEntry._ID + " = ?",
                        new String[]{String.valueOf(id)},
                        null, null, null
                );
                Shelter shelter = null;
                if (cursor != null && cursor.moveToFirst()) {
                    shelter = cursorToShelter(cursor);
                }
                final Shelter result = shelter;
                mainHandler.post(() -> callback.onDataLoaded(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void insertShelter(Shelter shelter, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = shelterToContentValues(shelter);
                db.insertOrThrow(DatabaseContract.ShelterEntry.TABLE_NAME, null, cv);
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private Shelter cursorToShelter(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry._ID));
        String nameEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_NAME_EN));
        String nameBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_NAME_BN));
        String dist = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_DISTRICT));
        String upazila = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_UPAZILA));
        String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_ADDRESS));
        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_LATITUDE));
        double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE));
        int cap = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CAPACITY));
        int occ = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY));
        String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE));
        String shelterType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE));
        String accessNotes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES));
        String source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_SOURCE));
        boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE)) == 1;
        String verified = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_CREATED_AT));
        long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT));

        return new Shelter(id, nameEn, nameBn, dist, upazila, address, lat, lng, cap, occ, phone, shelterType, accessNotes, source, active, verified, createdAt, updatedAt);
    }

    private ContentValues shelterToContentValues(Shelter s) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseContract.ShelterEntry.COLUMN_NAME_EN, s.getNameEn());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_NAME_BN, s.getNameBn());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_DISTRICT, s.getDistrict());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_UPAZILA, s.getUpazila());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_ADDRESS, s.getAddress());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_LATITUDE, s.getLatitude());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE, s.getLongitude());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_CAPACITY, s.getCapacity());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY, s.getCurrentOccupancy());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE, s.getContactPhone());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE, s.getShelterType());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES, s.getAccessibilityNotes());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_SOURCE, s.getSource());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE, s.isActive() ? 1 : 0);
        cv.put(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT, s.getVerifiedAt());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_CREATED_AT, s.getCreatedAt());
        cv.put(DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT, s.getUpdatedAt());
        return cv;
    }

    // =========================================================================
    // 5. WATER OBSERVATIONS REPOSITORY (FULL CRUD)
    // =========================================================================

    public void saveWaterObservation(WaterObservation obs, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = waterObservationToContentValues(obs);
                long rowId = db.insertOrThrow(DatabaseContract.WaterObservationEntry.TABLE_NAME, null, cv);
                if (rowId == -1) {
                    throw new RuntimeException("Failed to insert water observation");
                }
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void updateWaterObservation(WaterObservation obs, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                if (obs == null || obs.getId() <= 0) {
                    throw new IllegalArgumentException("Invalid water observation ID for update");
                }
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = waterObservationToContentValues(obs);
                cv.put(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
                int rows = db.update(
                        DatabaseContract.WaterObservationEntry.TABLE_NAME,
                        cv,
                        DatabaseContract.WaterObservationEntry._ID + " = ?",
                        new String[]{String.valueOf(obs.getId())}
                );
                if (rows == 0) {
                    throw new RuntimeException("No record found to update with ID: " + obs.getId());
                }
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void deleteWaterObservation(long id, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                if (id <= 0) {
                    throw new IllegalArgumentException("Invalid water observation ID for deletion");
                }
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                int rows = db.delete(
                        DatabaseContract.WaterObservationEntry.TABLE_NAME,
                        DatabaseContract.WaterObservationEntry._ID + " = ?",
                        new String[]{String.valueOf(id)}
                );
                if (rows == 0) {
                    throw new RuntimeException("No record found to delete with ID: " + id);
                }
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getWaterObservationById(long id, DataCallback<WaterObservation> callback) {
        executorService.execute(() -> {
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.WaterObservationEntry.TABLE_NAME,
                        null,
                        DatabaseContract.WaterObservationEntry._ID + " = ?",
                        new String[]{String.valueOf(id)},
                        null, null, null
                );
                WaterObservation obs = null;
                if (cursor != null && cursor.moveToFirst()) {
                    obs = cursorToWaterObservation(cursor);
                }
                final WaterObservation result = obs;
                mainHandler.post(() -> callback.onDataLoaded(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void getWaterObservations(DataCallback<List<WaterObservation>> callback) {
        executorService.execute(() -> {
            List<WaterObservation> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.WaterObservationEntry.TABLE_NAME,
                        null, null, null, null, null,
                        DatabaseContract.WaterObservationEntry._ID + " DESC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        list.add(cursorToWaterObservation(cursor));
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    private WaterObservation cursorToWaterObservation(Cursor cursor) {
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
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT));
        long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT));

        return new WaterObservation(id, loc, dist, src, app, odor, ph, turb, safe, time, rep, rem, createdAt, updatedAt);
    }

    private ContentValues waterObservationToContentValues(WaterObservation obs) {
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
        cv.put(DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT, obs.getCreatedAt());
        cv.put(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT, obs.getUpdatedAt());
        return cv;
    }

    // =========================================================================
    // 6. CHECKLISTS & CHECKLIST ITEMS REPOSITORY
    // =========================================================================

    public void getChecklistCategories(DataCallback<List<PreparednessChecklist>> callback) {
        executorService.execute(() -> {
            List<PreparednessChecklist> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.PreparednessChecklistEntry.TABLE_NAME,
                        null, null, null, null, null,
                        DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER + " ASC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry._ID));
                        String key = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY));
                        String en = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN));
                        String bn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN));
                        String descEn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_EN));
                        String descBn = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_BN));
                        String icon = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_ICON_NAME));
                        int order = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER));
                        list.add(new PreparednessChecklist(id, key, en, bn, descEn, descBn, icon, order));
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void getChecklistItems(DataCallback<List<ChecklistItem>> callback) {
        getChecklistItemsByCategory(-1, callback);
    }

    public void getChecklistItemsByCategory(long categoryId, DataCallback<List<ChecklistItem>> callback) {
        executorService.execute(() -> {
            List<ChecklistItem> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String selection = categoryId > 0 ? DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID + " = ?" : null;
                String[] selectionArgs = categoryId > 0 ? new String[]{String.valueOf(categoryId)} : null;

                cursor = db.query(
                        DatabaseContract.ChecklistItemEntry.TABLE_NAME,
                        null, selection, selectionArgs, null, null,
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
                        long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT));
                        list.add(new ChecklistItem(id, catId, en, bn, completed, essential, updatedAt));
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void updateChecklistItem(long itemId, boolean completed, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                if (itemId <= 0) {
                    throw new IllegalArgumentException("Invalid checklist item ID");
                }
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, completed ? 1 : 0);
                cv.put(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
                int rows = db.update(DatabaseContract.ChecklistItemEntry.TABLE_NAME, cv,
                        DatabaseContract.ChecklistItemEntry._ID + " = ?", new String[]{String.valueOf(itemId)});
                if (rows == 0) {
                    throw new RuntimeException("Checklist item with ID " + itemId + " not found to update");
                }
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void resetAllChecklistItems(ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, 0);
                cv.put(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
                db.update(DatabaseContract.ChecklistItemEntry.TABLE_NAME, cv, null, null);
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getChecklistProgress(DataCallback<ChecklistProgress> callback) {
        executorService.execute(() -> {
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.ChecklistItemEntry.TABLE_NAME,
                        new String[]{
                                DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED,
                                DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL
                        },
                        null, null, null, null, null
                );
                int total = 0;
                int completed = 0;
                int essTotal = 0;
                int essCompleted = 0;
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        total++;
                        boolean isComp = cursor.getInt(0) == 1;
                        boolean isEss = cursor.getInt(1) == 1;
                        if (isComp) completed++;
                        if (isEss) {
                            essTotal++;
                            if (isComp) essCompleted++;
                        }
                    }
                }
                ChecklistProgress progress = new ChecklistProgress(total, completed, essTotal, essCompleted);
                mainHandler.post(() -> callback.onDataLoaded(progress));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    // =========================================================================
    // 7. SYNCHRONIZATION METADATA REPOSITORY
    // =========================================================================

    public void getAllSyncMetadata(DataCallback<List<SyncMetadata>> callback) {
        executorService.execute(() -> {
            List<SyncMetadata> list = new ArrayList<>();
            Cursor cursor = null;
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                cursor = db.query(
                        DatabaseContract.SyncMetadataEntry.TABLE_NAME,
                        null, null, null, null, null,
                        DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME + " ASC"
                );
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String table = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME));
                        String src = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME));
                        long time = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP));
                        int count = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT));
                        String status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS));
                        String hash = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH));
                        String tag = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG));
                        list.add(new SyncMetadata(table, src, time, count, status, hash, tag));
                    }
                }
                mainHandler.post(() -> callback.onDataLoaded(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (cursor != null) cursor.close();
            }
        });
    }

    public void updateSyncMetadata(SyncMetadata meta, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME, meta.getTableName());
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME, meta.getSourceName());
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP, meta.getLastSyncTimestamp());
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT, meta.getRecordsCount());
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS, meta.getSyncStatus());
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH, meta.getSyncHash());
                cv.put(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG, meta.getVersionTag());
                db.insertWithOnConflict(DatabaseContract.SyncMetadataEntry.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (callback != null) mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }
}
