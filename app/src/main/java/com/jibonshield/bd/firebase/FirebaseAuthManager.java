package com.jibonshield.bd.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.jibonshield.bd.database.AppDatabase;
import com.jibonshield.bd.database.entity.UserProfileEntity;
import com.jibonshield.bd.models.UserProfile;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Authentication Manager wrapping Firebase Authentication with offline-first resilience.
 * Supports email/password registration, login, profile caching, role distinction (Citizen vs Volunteer),
 * and automatic hometown synchronization between Firestore and Room local database.
 */
public class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthManager";
    private static FirebaseAuthManager instance;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    public interface ProfileCallback {
        void onSuccess(UserProfile profile);
        void onFailure(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private final Context context;
    private final AppDatabase appDatabase;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private FirebaseAuth auth;

    public static synchronized FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context.getApplicationContext());
        }
        return instance;
    }

    private FirebaseAuthManager(Context context) {
        this.context = context;
        this.appDatabase = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                this.auth = FirebaseAuth.getInstance();
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization warning: " + e.getMessage());
        }
    }

    public boolean isFirebaseConfigured() {
        return auth != null;
    }

    public boolean isUserLoggedIn() {
        return auth != null && auth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return auth != null ? auth.getCurrentUser() : null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
            return user.getUid();
        }
        return PreferenceUtils.getOrCreateUserUuid(context);
    }

    public void registerUser(String email, String password, String displayName,
                             String role, String district, String phoneNumber,
                             AuthCallback callback) {
        registerUser(email, password, displayName, role, "", district, "", phoneNumber, callback);
    }

    public void registerUser(String email, String password, String displayName,
                             String role, String divisionId, String districtId, String upazilaId,
                             String phoneNumber, AuthCallback callback) {
        if (!isFirebaseConfigured()) {
            if (callback != null) {
                callback.onFailure("Firebase Authentication is not configured. Please supply valid google-services.json.");
            }
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        PreferenceUtils.setUserName(context, displayName);
                        PreferenceUtils.setUserEmail(context, email);

                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();
                        user.updateProfile(profileUpdates);

                        UserProfile profile = new UserProfile(
                                user.getUid(),
                                displayName,
                                email,
                                phoneNumber,
                                divisionId,
                                districtId,
                                upazilaId,
                                districtId != null && !districtId.isEmpty(),
                                System.currentTimeMillis()
                        );
                        profile.setRole(role);

                        // Save in local Room DB and Preferences
                        saveProfileLocally(profile);

                        // Save in Firestore
                        FirestoreManager.getInstance(context).saveUserProfile(profile, null);
                    }
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Registration failed", e);
                    if (callback != null) {
                        callback.onFailure(getFriendlyErrorMessage(e));
                    }
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        if (!isFirebaseConfigured()) {
            if (callback != null) {
                callback.onFailure("Firebase Authentication is not configured. Please supply valid google-services.json.");
            }
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        PreferenceUtils.setUserEmail(context, user.getEmail() != null ? user.getEmail() : email);
                        String displayName = user.getDisplayName();
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')) : "User";
                        }
                        PreferenceUtils.setUserName(context, displayName);
                        syncUserProfileFromFirestore(user.getUid());
                    }
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed", e);
                    if (callback != null) {
                        callback.onFailure(getFriendlyErrorMessage(e));
                    }
                });
    }

    /**
     * Downloads user profile from Firestore, caches it in Room DB and Preferences.
     */
    public void syncUserProfileFromFirestore(String uid) {
        if (uid == null) return;
        FirestoreManager.getInstance(context).getUserProfile(uid, new FirestoreManager.ProfileResultCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (profile != null) {
                    saveProfileLocally(profile);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.w(TAG, "Could not fetch profile from Firestore: " + errorMessage);
            }
        });
    }

    /**
     * Updates user hometown both locally (Room + Preferences) and in Firestore if connected.
     */
    public void updateHometown(String divisionId, String districtId, String upazilaId, ActionCallback callback) {
        PreferenceUtils.setHometown(context, divisionId, districtId, upazilaId);

        String uid = getCurrentUserId();
        long now = System.currentTimeMillis();

        executor.execute(() -> {
            UserProfileEntity current = appDatabase.userProfileDao().getByUid(uid);
            if (current != null) {
                appDatabase.userProfileDao().updateHometown(uid, divisionId, districtId, upazilaId, now);
            } else {
                String existingName = PreferenceUtils.getUserName(context);
                if (existingName.isEmpty() && getCurrentUser() != null && getCurrentUser().getDisplayName() != null) {
                    existingName = getCurrentUser().getDisplayName();
                }
                if (existingName.isEmpty()) existingName = "JibonShield Citizen";

                String existingEmail = PreferenceUtils.getUserEmail(context);
                if (existingEmail.isEmpty() && getCurrentUser() != null && getCurrentUser().getEmail() != null) {
                    existingEmail = getCurrentUser().getEmail();
                }

                UserProfileEntity newProfile = new UserProfileEntity(
                        uid, existingName, existingEmail, "", divisionId, districtId, upazilaId, true, now
                );
                appDatabase.userProfileDao().insert(newProfile);
            }

            // Sync to Firestore if user is authenticated
            if (isFirebaseConfigured() && isUserLoggedIn()) {
                try {
                    Map<String, Object> update = new HashMap<>();
                    update.put("divisionId", divisionId != null ? divisionId.toLowerCase().trim() : "");
                    update.put("districtId", districtId != null ? districtId.toLowerCase().trim() : "");
                    update.put("upazilaId", upazilaId != null ? upazilaId.toLowerCase().trim() : "");
                    update.put("hometownConfigured", true);
                    update.put("updatedAt", FieldValue.serverTimestamp());

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(update, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.i(TAG, "Hometown synced to Firestore"))
                            .addOnFailureListener(e -> Log.w(TAG, "Failed syncing hometown to Firestore: " + e.getMessage()));
                } catch (Exception e) {
                    Log.w(TAG, "Firestore update exception", e);
                }
            }

            if (callback != null) {
                mainHandler.post(callback::onSuccess);
            }
        });
    }

    private void saveProfileLocally(UserProfile profile) {
        if (profile == null) return;
        if (profile.getName() != null && !profile.getName().trim().isEmpty() && !profile.getName().equals("User")) {
            PreferenceUtils.setUserName(context, profile.getName().trim());
        }
        if (profile.getEmail() != null && !profile.getEmail().trim().isEmpty()) {
            PreferenceUtils.setUserEmail(context, profile.getEmail().trim());
        }
        if (profile.isHometownConfigured() || (profile.getDistrictId() != null && !profile.getDistrictId().isEmpty())) {
            PreferenceUtils.setHometown(context, profile.getDivisionId(), profile.getDistrictId(), profile.getUpazilaId());
        }

        executor.execute(() -> {
            UserProfileEntity entity = new UserProfileEntity(
                    profile.getUid(),
                    profile.getName(),
                    profile.getEmail(),
                    profile.getPhone(),
                    profile.getDivisionId(),
                    profile.getDistrictId(),
                    profile.getUpazilaId(),
                    profile.isHometownConfigured(),
                    profile.getUpdatedAt()
            );
            appDatabase.userProfileDao().insert(entity);
        });
    }

    public static String getFriendlyErrorMessage(Exception e) {
        if (e == null) {
            return "Authentication request failed.";
        }
        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(errorCode)) {
                return "Email/Password sign-in is disabled in your Firebase console. Please go to Firebase Console > Authentication > Sign-in method and enable 'Email/Password'.";
            }
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email address already exists. Please sign in instead.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password is too weak. Please use at least 6 characters.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Invalid email address or credentials provided.";
        }
        if (e instanceof FirebaseNetworkException) {
            return "Network connection failed. Please check your internet connection.";
        }
        String msg = e.getMessage();
        if (msg != null && (msg.toLowerCase().contains("operation is not allowed") || msg.contains("ERROR_OPERATION_NOT_ALLOWED"))) {
            return "Email/Password sign-in is disabled in your Firebase console. Please go to Firebase Console > Authentication > Sign-in method and enable 'Email/Password'.";
        }
        return e.getLocalizedMessage() != null ? e.getLocalizedMessage() : (msg != null ? msg : "Authentication failed.");
    }

    public void logoutUser() {
        if (auth != null) {
            auth.signOut();
        }
        PreferenceUtils.setUserName(context, "");
        PreferenceUtils.setUserEmail(context, "");
        PreferenceUtils.resetUserUuid(context);
    }

    public void getCurrentUserProfile(ProfileCallback callback) {
        String uid = getCurrentUserId();
        FirebaseUser user = getCurrentUser();

        // Check local Room cache first (instant response)
        executor.execute(() -> {
            UserProfileEntity cached = appDatabase.userProfileDao().getByUid(uid);

            // Determine best name from all available sources
            String bestName = "";
            if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                bestName = user.getDisplayName().trim();
            }
            if (bestName.isEmpty() && cached != null && cached.getName() != null && !cached.getName().trim().isEmpty() && !cached.getName().equals("User")) {
                bestName = cached.getName().trim();
            }
            if (bestName.isEmpty()) {
                bestName = PreferenceUtils.getUserName(context);
            }
            if (bestName.isEmpty() && user != null && user.getEmail() != null && user.getEmail().contains("@")) {
                bestName = user.getEmail().substring(0, user.getEmail().indexOf('@'));
            }
            if (bestName.isEmpty()) {
                bestName = isUserLoggedIn() ? "JibonShield User" : "Guest Citizen";
            }

            // Determine best email from all available sources
            String bestEmail = "";
            if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                bestEmail = user.getEmail().trim();
            }
            if (bestEmail.isEmpty() && cached != null && cached.getEmail() != null && !cached.getEmail().trim().isEmpty()) {
                bestEmail = cached.getEmail().trim();
            }
            if (bestEmail.isEmpty()) {
                bestEmail = PreferenceUtils.getUserEmail(context);
            }
            if (bestEmail.isEmpty()) {
                bestEmail = (user != null) ? "—" : "guest@offline.local";
            }

            // Update preferences with resolved details
            if (!bestName.isEmpty()) {
                PreferenceUtils.setUserName(context, bestName);
            }
            if (!bestEmail.isEmpty() && !bestEmail.equals("—") && !bestEmail.equals("guest@offline.local")) {
                PreferenceUtils.setUserEmail(context, bestEmail);
            }

            if (cached != null) {
                // Ensure cached entity keeps best details
                boolean needsUpdate = false;
                if ((cached.getName() == null || cached.getName().isEmpty() || cached.getName().equals("User")) && !bestName.isEmpty()) {
                    cached.setName(bestName);
                    needsUpdate = true;
                }
                if ((cached.getEmail() == null || cached.getEmail().isEmpty()) && !bestEmail.isEmpty() && !bestEmail.equals("—") && !bestEmail.equals("guest@offline.local")) {
                    cached.setEmail(bestEmail);
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    appDatabase.userProfileDao().insert(cached);
                }

                UserProfile profile = new UserProfile(
                        cached.getUid(),
                        bestName,
                        bestEmail,
                        cached.getPhone() != null ? cached.getPhone() : "",
                        cached.getDivisionId(),
                        cached.getDistrictId(),
                        cached.getUpazilaId(),
                        cached.isHometownConfigured(),
                        cached.getUpdatedAt()
                );
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(profile);
                });
                return;
            }

            // Fallback to Firestore if no cache
            if (user == null) {
                UserProfile guestProfile = new UserProfile(
                        uid,
                        bestName,
                        bestEmail,
                        "",
                        PreferenceUtils.getHometownDivision(context),
                        PreferenceUtils.getHometownDistrict(context),
                        PreferenceUtils.getHometownUpazila(context),
                        PreferenceUtils.isHometownConfigured(context),
                        System.currentTimeMillis()
                );
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(guestProfile);
                });
                return;
            }

            final String finalBestName = bestName;
            final String finalBestEmail = bestEmail;
            FirestoreManager.getInstance(context).getUserProfile(user.getUid(), new FirestoreManager.ProfileResultCallback() {
                @Override
                public void onSuccess(UserProfile profile) {
                    if (profile != null) {
                        if (profile.getName() == null || profile.getName().isEmpty() || profile.getName().equals("User")) {
                            profile.setName(finalBestName);
                        }
                        if (profile.getEmail() == null || profile.getEmail().isEmpty()) {
                            profile.setEmail(finalBestEmail);
                        }
                        saveProfileLocally(profile);
                    }
                    if (callback != null) callback.onSuccess(profile);
                }

                @Override
                public void onFailure(String error) {
                    UserProfile fallback = new UserProfile(
                            user.getUid(),
                            finalBestName,
                            finalBestEmail,
                            user.getPhoneNumber() != null ? user.getPhoneNumber() : "—",
                            PreferenceUtils.getHometownDivision(context),
                            PreferenceUtils.getHometownDistrict(context),
                            PreferenceUtils.getHometownUpazila(context),
                            PreferenceUtils.isHometownConfigured(context),
                            System.currentTimeMillis()
                    );
                    saveProfileLocally(fallback);
                    if (callback != null) callback.onSuccess(fallback);
                }
            });
        });
    }

    /**
     * Updates user display name locally (Preferences + Room) and synchronizes to Firebase Auth and Firestore.
     */
    public void updateUserName(String newName, ActionCallback callback) {
        if (newName == null || newName.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Name cannot be empty");
            return;
        }
        String cleanName = newName.trim();
        PreferenceUtils.setUserName(context, cleanName);

        String uid = getCurrentUserId();
        long now = System.currentTimeMillis();

        executor.execute(() -> {
            UserProfileEntity current = appDatabase.userProfileDao().getByUid(uid);
            if (current != null) {
                appDatabase.userProfileDao().updateName(uid, cleanName, now);
            } else {
                UserProfileEntity newProfile = new UserProfileEntity(
                        uid,
                        cleanName,
                        PreferenceUtils.getUserEmail(context),
                        "",
                        PreferenceUtils.getHometownDivision(context),
                        PreferenceUtils.getHometownDistrict(context),
                        PreferenceUtils.getHometownUpazila(context),
                        PreferenceUtils.isHometownConfigured(context),
                        now
                );
                appDatabase.userProfileDao().insert(newProfile);
            }

            // Sync to Firebase Auth & Firestore if logged in
            if (isFirebaseConfigured() && isUserLoggedIn()) {
                FirebaseUser user = getCurrentUser();
                if (user != null) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build();
                    user.updateProfile(profileUpdates);
                }

                try {
                    Map<String, Object> update = new HashMap<>();
                    update.put("name", cleanName);
                    update.put("displayName", cleanName);
                    update.put("updatedAt", FieldValue.serverTimestamp());

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(update, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.i(TAG, "User name synced to Firestore"))
                            .addOnFailureListener(e -> Log.w(TAG, "Failed syncing name to Firestore: " + e.getMessage()));
                } catch (Exception e) {
                    Log.w(TAG, "Firestore name update exception", e);
                }
            }

            if (callback != null) {
                mainHandler.post(callback::onSuccess);
            }
        });
    }
}
