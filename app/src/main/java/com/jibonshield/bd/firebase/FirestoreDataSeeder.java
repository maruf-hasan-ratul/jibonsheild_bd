package com.jibonshield.bd.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.jibonshield.bd.utils.DistrictData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pure Java Cloud Firestore Data Seeder for JibonShield BD.
 * Replaces any need for Node.js / JavaScript scripts by allowing direct
 * database population from within the Android application.
 */
public class FirestoreDataSeeder {

    private static final String TAG = "FirestoreDataSeeder";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface SeedCallback {
        void onProgress(String currentCollection, int currentCount, int totalCount);
        void onSuccess(String summary);
        void onFailure(String errorMessage);
    }

    public static void seedAllDataToFirestore(Context context, SeedCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                long now = System.currentTimeMillis();

                // 1. Divisions (8 official divisions)
                List<Map<String, Object>> divisions = getDivisions();
                postProgress(callback, "divisions", 0, divisions.size());
                WriteBatch divBatch = db.batch();
                for (Map<String, Object> div : divisions) {
                    String id = (String) div.get("id");
                    div.put("updatedAt", FieldValue.serverTimestamp());
                    divBatch.set(db.collection("divisions").document(id), div);
                }
                divBatch.commit();
                postProgress(callback, "divisions", divisions.size(), divisions.size());

                // 2. Districts (64 official districts)
                List<Map<String, Object>> districts = getDistricts();
                postProgress(callback, "districts", 0, districts.size());
                WriteBatch distBatch = db.batch();
                for (Map<String, Object> dist : districts) {
                    String id = (String) dist.get("id");
                    dist.put("updatedAt", FieldValue.serverTimestamp());
                    distBatch.set(db.collection("districts").document(id), dist);
                }
                distBatch.commit();
                postProgress(callback, "districts", districts.size(), districts.size());

                // 3. Upazilas (representative sub-districts from DistrictData)
                List<Map<String, Object>> upazilas = getUpazilas();
                postProgress(callback, "upazilas", 0, upazilas.size());
                // Commit upazilas in chunks of 400 (Firestore batch limit is 500)
                int upazilaChunkSize = 400;
                for (int i = 0; i < upazilas.size(); i += upazilaChunkSize) {
                    WriteBatch upBatch = db.batch();
                    int end = Math.min(i + upazilaChunkSize, upazilas.size());
                    for (int j = i; j < end; j++) {
                        Map<String, Object> upz = upazilas.get(j);
                        String id = (String) upz.get("id");
                        upz.put("updatedAt", FieldValue.serverTimestamp());
                        upBatch.set(db.collection("upazilas").document(id), upz);
                    }
                    upBatch.commit();
                    postProgress(callback, "upazilas", end, upazilas.size());
                }

                // 4. Shelters
                List<Map<String, Object>> shelters = getShelters();
                postProgress(callback, "shelters", 0, shelters.size());
                WriteBatch shelterBatch = db.batch();
                for (Map<String, Object> shelter : shelters) {
                    String id = (String) shelter.get("id");
                    shelter.put("updatedAt", FieldValue.serverTimestamp());
                    shelter.put("createdAt", FieldValue.serverTimestamp());
                    shelterBatch.set(db.collection("shelters").document(id), shelter);
                }
                shelterBatch.commit();
                postProgress(callback, "shelters", shelters.size(), shelters.size());

                // 5. Disaster Updates
                List<Map<String, Object>> updates = getDisasterUpdates();
                postProgress(callback, "disaster_updates", 0, updates.size());
                WriteBatch updateBatch = db.batch();
                for (Map<String, Object> alert : updates) {
                    String id = (String) alert.get("id");
                    alert.put("updatedAt", FieldValue.serverTimestamp());
                    updateBatch.set(db.collection("disaster_updates").document(id), alert);
                }
                updateBatch.commit();
                postProgress(callback, "disaster_updates", updates.size(), updates.size());

                String summary = "Successfully seeded " + divisions.size() + " Divisions, "
                        + districts.size() + " Districts, " + upazilas.size() + " Upazilas, "
                        + shelters.size() + " Shelters, and " + updates.size() + " Alerts to Firestore!";
                Log.i(TAG, summary);

                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onSuccess(summary);
                });

            } catch (Exception e) {
                Log.e(TAG, "Seeding failed", e);
                MAIN_HANDLER.post(() -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
            }
        });
    }

    private static void postProgress(SeedCallback callback, String collection, int current, int total) {
        MAIN_HANDLER.post(() -> {
            if (callback != null) callback.onProgress(collection, current, total);
        });
    }

    private static List<Map<String, Object>> getDivisions() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(createDivision("barisal", "Barisal", "বরিশাল", "BARISAL"));
        list.add(createDivision("chittagong", "Chittagong", "চট্টগ্রাম", "CHITTAGONG"));
        list.add(createDivision("dhaka", "Dhaka", "ঢাকা", "DHAKA"));
        list.add(createDivision("khulna", "Khulna", "খুলনা", "KHULNA"));
        list.add(createDivision("mymensingh", "Mymensingh", "ময়মনসিংহ", "MYMENSINGH"));
        list.add(createDivision("rajshahi", "Rajshahi", "রাজশাহী", "RAJSHAHI"));
        list.add(createDivision("rangpur", "Rangpur", "রংপুর", "RANGPUR"));
        list.add(createDivision("sylhet", "Sylhet", "সিলেট", "SYLHET"));
        return list;
    }

    private static Map<String, Object> createDivision(String id, String name, String nameBn, String code) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("nameBn", nameBn);
        map.put("code", code);
        map.put("isActive", true);
        return map;
    }

    private static List<Map<String, Object>> getDistricts() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String districtName : DistrictData.ALL_64_DISTRICTS) {
            String id = districtName.toLowerCase().replace(" ", "_").replace("'", "");
            String divisionId = inferDivision(districtName);
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", districtName);
            map.put("nameBn", districtName);
            map.put("divisionId", divisionId);
            map.put("code", id.toUpperCase());
            map.put("isActive", true);
            list.add(map);
        }
        return list;
    }

    private static String inferDivision(String district) {
        if (Arrays.asList("Barisal", "Barguna", "Bhola", "Jhalokati", "Patuakhali", "Pirojpur").contains(district)) return "barisal";
        if (Arrays.asList("Chittagong", "Bandarban", "Brahmanbaria", "Chandpur", "Comilla", "Cox's Bazar", "Feni", "Khagrachhari", "Lakshmipur", "Noakhali", "Rangamati").contains(district)) return "chittagong";
        if (Arrays.asList("Dhaka", "Faridpur", "Gazipur", "Gopalganj", "Kishoreganj", "Madaripur", "Manikganj", "Munshiganj", "Narayanganj", "Narsingdi", "Rajbari", "Shariatpur", "Tangail").contains(district)) return "dhaka";
        if (Arrays.asList("Khulna", "Bagerhat", "Chuadanga", "Jashore", "Jhenaidah", "Kushtia", "Magura", "Meherpur", "Narail", "Satkhira").contains(district)) return "khulna";
        if (Arrays.asList("Mymensingh", "Jamalpur", "Netrokona", "Sherpur").contains(district)) return "mymensingh";
        if (Arrays.asList("Rajshahi", "Bogura", "Joypurhat", "Naogaon", "Natore", "Chapainawabganj", "Pabna", "Sirajganj").contains(district)) return "rajshahi";
        if (Arrays.asList("Rangpur", "Dinajpur", "Gaibandha", "Kurigram", "Lalmonirhat", "Nilphamari", "Panchagarh", "Thakurgaon").contains(district)) return "rangpur";
        return "sylhet";
    }

    private static List<Map<String, Object>> getUpazilas() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String dist : DistrictData.ALL_64_DISTRICTS) {
            String distId = dist.toLowerCase().replace(" ", "_").replace("'", "");
            List<String> upList = DistrictData.getUpazilasForDistrict(dist);
            for (String upName : upList) {
                String id = distId + "_" + upName.toLowerCase().replace(" ", "_").replace("'", "");
                Map<String, Object> map = new HashMap<>();
                map.put("id", id);
                map.put("name", upName);
                map.put("nameBn", upName);
                map.put("districtId", distId);
                map.put("code", id.toUpperCase());
                map.put("isActive", true);
                list.add(map);
            }
        }
        return list;
    }

    private static List<Map<String, Object>> getShelters() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(createShelter("khulna_shelter_001", "Khulna Government Cyclone Shelter", "খুলনা সরকারি আশ্রয়কেন্দ্র",
                "khulna", "khulna", "khulna_dumuria", "Dumuria, Khulna", 22.815, 89.55, 500, 120, "Cyclone Shelter",
                Arrays.asList("Drinking Water", "First Aid", "Sanitation", "Emergency Power"), "01711000001"));

        list.add(createShelter("sylhet_shelter_001", "Sylhet Sadar Flood Relief Center", "সিলেট সদর বন্যা আশ্রয় কেন্দ্র",
                "sylhet", "sylhet", "sylhet_sylhet_sadar", "Sadar Upazila, Sylhet", 24.8949, 91.8687, 800, 310, "Flood Shelter",
                Arrays.asList("Purified Water", "Medical Support", "Community Kitchen"), "01711000002"));

        list.add(createShelter("cox_shelter_001", "Chakaria Multipurpose Cyclone Shelter", "চকরিয়া বহুমুখী ঘূর্ণিঝড় আশ্রয়কেন্দ্র",
                "chittagong", "coxs_bazar", "coxs_bazar_chakaria", "Chakaria, Cox's Bazar", 21.7865, 92.0772, 1200, 450, "Multipurpose Cyclone Shelter",
                Arrays.asList("Helipad", "Solar Generator", "Separate Women Ward", "First Aid"), "01711000003"));

        list.add(createShelter("kurigram_shelter_001", "Rajarhat River Erosion & Flood Shelter", "রাজারহাট নদীভাঙন ও বন্যা আশ্রয়কেন্দ্র",
                "rangpur", "kurigram", "kurigram_rajarhat", "Rajarhat, Kurigram", 25.795, 89.552, 600, 180, "Flood Shelter",
                Arrays.asList("Drinking Water", "Livestock Ground (Killa)", "Sanitary Toilet"), "01711000004"));

        list.add(createShelter("barisal_shelter_001", "Mehendiganj Coastal Cyclone Center", "মেহেন্দীগঞ্জ উপকূলীয় ঘূর্ণিঝড় কেন্দ্র",
                "barisal", "barisal", "barisal_mehendiganj", "Mehendiganj, Barisal", 22.825, 90.533, 750, 95, "Cyclone Shelter",
                Arrays.asList("High Ground Elevation", "Solar Power", "First Aid Kit"), "01711000005"));

        return list;
    }

    private static Map<String, Object> createShelter(String id, String name, String nameBn,
                                                     String divId, String distId, String upzId,
                                                     String address, double lat, double lng,
                                                     int capacity, int occupancy, String type,
                                                     List<String> facilities, String phone) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("nameBn", nameBn);
        map.put("description", "Designated emergency disaster evacuation shelter.");
        map.put("descriptionBn", "জরুরি দুর্যোগকালীন নিরাপদ আশ্রয়কেন্দ্র।");
        map.put("divisionId", divId);
        map.put("districtId", distId);
        map.put("upazilaId", upzId);
        map.put("address", address);
        map.put("addressBn", address);
        map.put("latitude", lat);
        map.put("longitude", lng);
        map.put("capacity", capacity);
        map.put("currentOccupancy", occupancy);
        map.put("shelterType", type);
        map.put("facilities", facilities);
        map.put("contactNumber", phone);
        map.put("isActive", true);
        return map;
    }

    private static List<Map<String, Object>> getDisasterUpdates() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(createAlert("alert_surma_001",
                "Surma-Kushiyara Severe Flash Flood",
                "সুরমা-কুশিয়ারা তীব্র আকস্মিক বন্যা সতর্কবার্তা",
                "Surma River is flowing 68 cm above danger level at Kanaighat and 28 cm at Sylhet Sadar. Inundation spreading across low-lying unions.",
                "মেঘালয় ও আসামে অতিভারী বর্ষণে সুরমা নদী কানাইঘাটে ৬৮ সেমি এবং সিলেট সদরে ২৮ সেমি বিপৎসীমার উপর দিয়ে প্রবাহিত হচ্ছে। নিম্নাঞ্চল প্লাবিত।",
                "FLOOD", "CRITICAL", "ACTIVE", "sylhet", "sylhet", "sylhet_kanaighat",
                Arrays.asList("Kanaighat", "Zakiganj", "Golapganj", "Sylhet Sadar", "Beanibazar"), "FFWC Bangladesh",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 4))));

        list.add(createAlert("alert_jamuna_002",
                "Brahmaputra-Jamuna Basin Major Flood Alert",
                "ব্রহ্মপুত্র-যমুনা অববাহিকায় ভয়াবহ বন্যা সতর্কতা",
                "Jamuna water level rising at 18 cm/24h at Sirajganj and Sariakandi. Riverbank erosion and embankment overtopping imminent.",
                "যমুনা নদীর পানি সিরাজগঞ্জ ও সারিয়াকান্দি পয়েন্টে দ্রুত বাড়ছে। নদী তীরবর্তী নিম্নাঞ্চল ও চরে বন্যা পরিস্থিতি অবনতিশীল।",
                "FLOOD", "CRITICAL", "ACTIVE", "rajshahi", "sirajganj", "sirajganj_kazipur",
                Arrays.asList("Kazipur", "Sirajganj Sadar", "Belkuchi", "Chauhali", "Shahjadpur"), "Flood Forecasting & Warning Centre",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 5))));

        list.add(createAlert("alert_kurigram_003",
                "Dharla & Dudhkumar Flood Inundation",
                "ধরলা ও দুধকুমার নদীর পানি বৃদ্ধি ও প্লাবন",
                "Dharla river flowing 35 cm above danger level at Kurigram. Over 40,000 households across char areas stranded.",
                "কুড়িগ্রাম পয়েন্টে ধরলা নদী বিপৎসীমার ৩৫ সেমি উপরে। রৌমারী, রাজিবপুর ও চিলমারী চরাঞ্চল প্লাবিত হয়েছে।",
                "FLOOD", "CRITICAL", "ACTIVE", "rangpur", "kurigram", "kurigram_rajarhat",
                Arrays.asList("Rajarhat", "Chilmari", "Ulipur", "Rowmari", "Nageshwari"), "BWDB & FFWC",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 3))));

        list.add(createAlert("alert_teesta_004",
                "Teesta River High Water Advisory",
                "তিস্তা নদীর পানি বৃদ্ধি সতর্কবার্তা",
                "Teesta Barrage point at Dalia recorded discharge at maximum capacity. Gated flumes fully opened to release upstream run-off.",
                "ডালিয়া পয়েন্টে তিস্তা নদীর পানি বিপৎসীমার কাছাকাছি। ডিমলা, হাতীবান্ধা ও পাটগ্রাম চরে সতর্কবার্তা জারি।",
                "FLOOD", "WARNING", "ACTIVE", "rangpur", "lalmonirhat", "lalmonirhat_hatibandha",
                Arrays.asList("Hatibandha", "Patgram", "Dimla", "Kaliganj"), "BWDB Dalia Division",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 3))));

        list.add(createAlert("alert_cyclone_005",
                "Bay of Bengal Coastal Cyclone & Surge Warning",
                "বঙ্গোপসাগরে ঘূর্ণিঝড় ও জলোচ্ছ্বাস সতর্কবার্তা",
                "Deep tropical depression intensifying into cyclonic storm. Maritime signal 4 hoisted. 4-6 ft storm surge expected along coastlines.",
                "বঙ্গোপসাগরে সৃষ্ট গভীর নিম্নচাপটি ঘূর্ণিঝড়ে রূপ নিচ্ছে। ৪ নম্বর স্থানীয় হুঁশিয়ারি সংকেত জারি। ৪-৬ ফুট উচ্চতার জলোচ্ছ্বাসের আশঙ্কা।",
                "CYCLONE", "WARNING", "ACTIVE", "chittagong", "coxs_bazar", "coxs_bazar_chakaria",
                Arrays.asList("Cox's Bazar Sadar", "Chakaria", "Moheshkhali", "Teknaf", "Kutubdia", "Sandwip"), "Bangladesh Meteorological Department (BMD)",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 2))));

        list.add(createAlert("alert_muhuri_006",
                "Muhuri River Embankment Breach Alert",
                "মুহুরী নদীর বাঁধ ভাঙন ও প্লাবন সতর্কতা",
                "Heavy transboundary hill torrents caused multiple dyke breaches along Muhuri river in Parshuram and Fulgazi upazilas.",
                "ভারতের ত্রিপুরা থেকে নেমে আসা পাহাড়ি ঢলে পরশুরাম ও ফুলগাজীতে মুহুরী নদীর বন্যা নিয়ন্ত্রণ বাঁধ ভেঙে বেশ কয়েকটি গ্রাম প্লাবিত।",
                "FLOOD", "CRITICAL", "ACTIVE", "chittagong", "feni", "feni_parshuram",
                Arrays.asList("Parshuram", "Fulgazi", "Chhagalnaiya"), "Feni District Administration & BWDB",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 4))));

        list.add(createAlert("alert_sundarbans_007",
                "Coastal Tidal Inundation Advisory",
                "উপকূলীয় জোয়ারের পানি বৃদ্ধি ও বাঁধ সতর্কতা",
                "Spring tide combined with southerly wind forcing saline water through weak embankments across Dacope, Koyra, and Shyamnagar.",
                "অমাবস্যার জোয়ার ও বাতাসের প্রভাবে দাকোপ, কয়রা ও শ্যামনগরের দুর্বল বেড়িবাঁধ উপচে লবণাক্ত পানি প্রবেশের আশঙ্কা।",
                "FLOOD", "ADVISORY", "ACTIVE", "khulna", "khulna", "khulna_koyra",
                Arrays.asList("Koyra", "Dacope", "Paikgachha", "Shyamnagar", "Assasuni"), "CPP & Local Disaster Management Committee",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 3))));

        list.add(createAlert("alert_dhaka_008",
                "Urban Torrential Rain & Waterlogging Advisory",
                "ভারী বর্ষণজনিত জলাবদ্ধতা ও সতর্কতা",
                "Consecutive cloudbursts forecast over Greater Dhaka. Urban road waterlogging and slow drainage expected in low-lying canals.",
                "ঢাকায় টানা ভারী বৃষ্টির পূর্বাভাস। মিরপুর, ডেমরা, ও টঙ্গী খাল এলাকায় জলাবদ্ধতার কারণে যান চলাচলে বিঘ্ন ঘটতে পারে।",
                "FLOOD", "ADVISORY", "ACTIVE", "dhaka", "dhaka", "dhaka_mirpur",
                Arrays.asList("Mirpur", "Demra", "Keraniganj", "Tongi"), "Dhaka North & South City Corporation Disaster Cells",
                new Timestamp(new Date()),
                new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 2))));

        return list;
    }

    private static Map<String, Object> createAlert(String id, String title, String titleBn,
                                                   String desc, String descBn,
                                                   String type, String severity, String status,
                                                   String divId, String distId, String upzId,
                                                   List<String> affected, String issuedBy,
                                                   Timestamp issuedAt, Timestamp expiresAt) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("titleBn", titleBn);
        map.put("description", desc);
        map.put("descriptionBn", descBn);
        map.put("disasterType", type);
        map.put("severity", severity);
        map.put("status", status);
        map.put("divisionId", divId);
        map.put("districtId", distId);
        map.put("upazilaId", upzId);
        map.put("affectedAreas", affected);
        map.put("issuedBy", issuedBy);
        map.put("issuedAt", issuedAt);
        map.put("expiresAt", expiresAt);
        map.put("isActive", true);
        return map;
    }
}

