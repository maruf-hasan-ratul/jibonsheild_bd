package com.jibonshield.bd.sync;

import android.util.Log;

import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Validates incoming remote JSON payloads for flood warnings, shelters, and contacts.
 * Enforces schema integrity, timestamp validity, sanity bounds, and anti-corruption rules.
 */
public class RemoteDataValidator {

    private static final String TAG = "RemoteDataValidator";

    // Bangladesh Geographic Bounding Box: ~20.5°N - 26.7°N, 88.0°E - 92.7°E
    public static final double BD_MIN_LAT = 20.0;
    public static final double BD_MAX_LAT = 27.0;
    public static final double BD_MIN_LNG = 88.0;
    public static final double BD_MAX_LNG = 93.0;

    // Maximum acceptable future timestamp drift for issued_at: 7 days (in millis)
    private static final long MAX_FUTURE_DRIFT_MS = 7L * 24 * 60 * 60 * 1000L;

    public static class ValidationResult<T> {
        private final boolean valid;
        private final List<T> acceptedItems;
        private final List<String> rejectionReasons;
        private final String schemaVersion;
        private final String sourceName;
        private final long timestamp;

        public ValidationResult(boolean valid, List<T> acceptedItems, List<String> rejectionReasons,
                                String schemaVersion, String sourceName, long timestamp) {
            this.valid = valid;
            this.acceptedItems = acceptedItems != null ? acceptedItems : new ArrayList<>();
            this.rejectionReasons = rejectionReasons != null ? rejectionReasons : new ArrayList<>();
            this.schemaVersion = schemaVersion != null ? schemaVersion : "1.0";
            this.sourceName = sourceName != null ? sourceName : "Remote Feed";
            this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        }

        public static <T> ValidationResult<T> failure(String reason) {
            List<String> reasons = new ArrayList<>();
            reasons.add(reason);
            return new ValidationResult<>(false, new ArrayList<>(), reasons, "unknown", "unknown", 0);
        }

        public boolean isValid() { return valid; }
        public List<T> getAcceptedItems() { return acceptedItems; }
        public List<String> getRejectionReasons() { return rejectionReasons; }
        public String getSchemaVersion() { return schemaVersion; }
        public String getSourceName() { return sourceName; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Parses and validates a Flood Warning JSON payload.
     */
    public static ValidationResult<FloodWarning> validateFloodWarnings(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return ValidationResult.failure("Payload is empty or null");
        }

        try {
            JSONObject root = new JSONObject(jsonString);
            String version = root.optString("version", "1.0");
            String source = root.optString("source", "FFWC / BWDB");
            long timestamp = root.optLong("timestamp", System.currentTimeMillis());

            JSONArray array = root.optJSONArray("warnings");
            if (array == null) {
                array = root.optJSONArray("data");
            }

            if (array == null) {
                return ValidationResult.failure("Missing 'warnings' or 'data' array in root payload");
            }

            List<FloodWarning> accepted = new ArrayList<>();
            List<String> rejections = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    rejections.add("Index " + i + ": item is not a JSON object");
                    continue;
                }

                String level = obj.optString("warning_level", "").trim().toUpperCase();
                if (!isValidWarningLevel(level)) {
                    rejections.add("Index " + i + ": invalid warning_level '" + level + "'");
                    continue;
                }

                String titleEn = obj.optString("title_en", "").trim();
                String titleBn = obj.optString("title_bn", "").trim();
                if (titleEn.isEmpty()) {
                    rejections.add("Index " + i + ": title_en is missing");
                    continue;
                }
                if (titleBn.isEmpty()) titleBn = titleEn;

                String region = obj.optString("region", "").trim();
                if (region.isEmpty()) {
                    rejections.add("Index " + i + ": region is missing");
                    continue;
                }

                String detailsEn = obj.optString("details_en", "").trim();
                String detailsBn = obj.optString("details_bn", "").trim();
                if (detailsEn.isEmpty()) {
                    rejections.add("Index " + i + ": details_en is missing");
                    continue;
                }
                if (detailsBn.isEmpty()) detailsBn = detailsEn;

                String issuedAt = obj.optString("issued_at", "").trim();
                if (issuedAt.isEmpty() || !isValidTimestamp(issuedAt)) {
                    rejections.add("Index " + i + ": issued_at timestamp is invalid or unparseable: " + issuedAt);
                    continue;
                }

                // Check sanity: issued_at must not be > 7 days in the future
                Date parsedIssued = parseDate(issuedAt);
                if (parsedIssued != null && parsedIssued.getTime() > System.currentTimeMillis() + MAX_FUTURE_DRIFT_MS) {
                    rejections.add("Index " + i + ": issued_at timestamp is in far future: " + issuedAt);
                    continue;
                }

                String expiresAt = obj.optString("expires_at", "").trim();
                if (expiresAt.isEmpty() || !isValidTimestamp(expiresAt)) {
                    rejections.add("Index " + i + ": expires_at timestamp is missing or unparseable: " + expiresAt);
                    continue;
                }

                String itemSource = obj.optString("source", source).trim();
                String sourceRef = obj.optString("source_reference", "FFWC-" + System.currentTimeMillis()).trim();
                boolean isActive = obj.optBoolean("is_active", true);
                boolean isSample = obj.optBoolean("is_sample", false);
                long rowId = obj.optLong("id", 0);

                FloodWarning warning = new FloodWarning(
                        rowId, level, titleEn, titleBn, region, detailsEn, detailsBn,
                        issuedAt, expiresAt, itemSource, sourceRef, isActive, isSample,
                        System.currentTimeMillis()
                );
                accepted.add(warning);
            }

            boolean overallValid = !accepted.isEmpty() || rejections.isEmpty();
            return new ValidationResult<>(overallValid, accepted, rejections, version, source, timestamp);

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed for flood warnings: " + e.getMessage());
            return ValidationResult.failure("Malformed JSON: " + e.getMessage());
        }
    }

    /**
     * Parses and validates a Shelter JSON payload.
     */
    public static ValidationResult<Shelter> validateShelters(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return ValidationResult.failure("Payload is empty or null");
        }

        try {
            JSONObject root = new JSONObject(jsonString);
            String version = root.optString("version", "1.0");
            String source = root.optString("source", "MoDMR / CPP");
            long timestamp = root.optLong("timestamp", System.currentTimeMillis());

            JSONArray array = root.optJSONArray("shelters");
            if (array == null) {
                array = root.optJSONArray("data");
            }

            if (array == null) {
                return ValidationResult.failure("Missing 'shelters' or 'data' array in root payload");
            }

            List<Shelter> accepted = new ArrayList<>();
            List<String> rejections = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    rejections.add("Index " + i + ": item is not a JSON object");
                    continue;
                }

                String nameEn = obj.optString("name_en", "").trim();
                String nameBn = obj.optString("name_bn", "").trim();
                if (nameEn.isEmpty()) {
                    rejections.add("Index " + i + ": name_en is missing");
                    continue;
                }
                if (nameBn.isEmpty()) nameBn = nameEn;

                String district = obj.optString("district", "").trim();
                String upazila = obj.optString("upazila", "").trim();
                if (district.isEmpty() || upazila.isEmpty()) {
                    rejections.add("Index " + i + ": district or upazila is missing");
                    continue;
                }

                double lat = obj.optDouble("latitude", Double.NaN);
                double lng = obj.optDouble("longitude", Double.NaN);
                if (Double.isNaN(lat) || Double.isNaN(lng) || lat < BD_MIN_LAT || lat > BD_MAX_LAT || lng < BD_MIN_LNG || lng > BD_MAX_LNG) {
                    rejections.add("Index " + i + ": coordinates (" + lat + ", " + lng + ") out of Bangladesh bounds");
                    continue;
                }

                int capacity = obj.optInt("capacity", -1);
                if (capacity <= 0) {
                    rejections.add("Index " + i + ": capacity must be greater than zero");
                    continue;
                }

                int occupancy = obj.optInt("current_occupancy", 0);
                String phone = obj.optString("contact_phone", "").trim();
                String shelterType = obj.optString("shelter_type", "Cyclone & Flood Shelter").trim();
                String accessNotes = obj.optString("accessibility_notes", "Standard ground refuge").trim();
                String itemSource = obj.optString("source", source).trim();
                boolean isActive = obj.optBoolean("is_active", true);
                String verifiedAt = obj.optString("verified_at", "2026-01-01").trim();
                String address = obj.optString("address", upazila + ", " + district).trim();
                long id = obj.optLong("id", 0);

                Shelter shelter = new Shelter(
                        id, nameEn, nameBn, district, upazila, address, lat, lng,
                        capacity, occupancy, phone, shelterType, accessNotes, itemSource,
                        isActive, verifiedAt, System.currentTimeMillis(), System.currentTimeMillis()
                );
                accepted.add(shelter);
            }

            boolean overallValid = !accepted.isEmpty() || rejections.isEmpty();
            return new ValidationResult<>(overallValid, accepted, rejections, version, source, timestamp);

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed for shelters: " + e.getMessage());
            return ValidationResult.failure("Malformed JSON: " + e.getMessage());
        }
    }

    /**
     * Parses and validates an Emergency Contacts JSON payload.
     */
    public static ValidationResult<EmergencyContact> validateEmergencyContacts(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return ValidationResult.failure("Payload is empty or null");
        }

        try {
            JSONObject root = new JSONObject(jsonString);
            String version = root.optString("version", "1.0");
            String source = root.optString("source", "BTRC / MoDMR");
            long timestamp = root.optLong("timestamp", System.currentTimeMillis());

            JSONArray array = root.optJSONArray("contacts");
            if (array == null) {
                array = root.optJSONArray("data");
            }

            if (array == null) {
                return ValidationResult.failure("Missing 'contacts' or 'data' array in root payload");
            }

            List<EmergencyContact> accepted = new ArrayList<>();
            List<String> rejections = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    rejections.add("Index " + i + ": item is not a JSON object");
                    continue;
                }

                String nameEn = obj.optString("service_name_en", "").trim();
                String nameBn = obj.optString("service_name_bn", "").trim();
                if (nameEn.isEmpty()) {
                    rejections.add("Index " + i + ": service_name_en is missing");
                    continue;
                }
                if (nameBn.isEmpty()) nameBn = nameEn;

                String phone = obj.optString("phone_number", "").trim();
                if (phone.length() < 3 || !phone.matches("^[0-9+\\-\\s()]+$")) {
                    rejections.add("Index " + i + ": phone_number is invalid: " + phone);
                    continue;
                }

                String descEn = obj.optString("service_description_en", "").trim();
                String descBn = obj.optString("service_description_bn", "").trim();
                String category = obj.optString("category", "DISASTER").trim().toUpperCase();
                String hours = obj.optString("available_hours", "24/7").trim();
                boolean tollFree = obj.optBoolean("is_toll_free", false);
                String itemSource = obj.optString("source", source).trim();
                String verDate = obj.optString("verification_date", "2026-01-01").trim();
                boolean isEnabled = obj.optBoolean("is_enabled", true);
                int priority = obj.optInt("priority_order", 100);
                long id = obj.optLong("id", 0);

                EmergencyContact contact = new EmergencyContact(
                        id, nameEn, nameBn, phone, descEn, descBn, category,
                        hours, tollFree, itemSource, verDate, isEnabled, priority
                );
                accepted.add(contact);
            }

            boolean overallValid = !accepted.isEmpty() || rejections.isEmpty();
            return new ValidationResult<>(overallValid, accepted, rejections, version, source, timestamp);

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed for emergency contacts: " + e.getMessage());
            return ValidationResult.failure("Malformed JSON: " + e.getMessage());
        }
    }

    private static boolean isValidWarningLevel(String level) {
        return "CRITICAL".equalsIgnoreCase(level) ||
                "WARNING".equalsIgnoreCase(level) ||
                "ADVISORY".equalsIgnoreCase(level) ||
                "NORMAL".equalsIgnoreCase(level);
    }

    public static boolean isValidTimestamp(String dateStr) {
        return parseDate(dateStr) != null;
    }

    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String cleanDate = dateStr.trim();
        if (cleanDate.endsWith(" BST")) {
            cleanDate = cleanDate.substring(0, cleanDate.length() - 4).trim();
        }

        String[] formats = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(cleanDate);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
