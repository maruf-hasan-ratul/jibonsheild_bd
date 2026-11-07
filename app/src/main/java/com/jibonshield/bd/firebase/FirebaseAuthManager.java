package com.jibonshield.bd.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.jibonshield.bd.models.UserProfile;

/**
 * Authentication Manager wrapping Firebase Authentication with offline-first resilience.
 * Supports email/password registration, login, profile caching, role distinction (Citizen vs Volunteer),
 * and defensive fallback if Firebase cloud services have not yet been provisioned.
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

    private final Context context;
    private FirebaseAuth auth;

    public static synchronized FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context.getApplicationContext());
        }
        return instance;
    }

    private FirebaseAuthManager(Context context) {
        this.context = context;
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
        return user != null ? user.getUid() : "guest_offline_user";
    }

    public void registerUser(String email, String password, String displayName,
                             String role, String district, String phoneNumber,
                             AuthCallback callback) {
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
                        // Update display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build();
                        user.updateProfile(profileUpdates);

                        // Save profile in Firestore
                        UserProfile profile = new UserProfile(
                                user.getUid(), email, displayName, role, district, phoneNumber, System.currentTimeMillis()
                        );
                        FirestoreManager.getInstance(context).saveUserProfile(profile, null);
                    }
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Registration failed", e);
                    if (callback != null) {
                        callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage());
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
                    if (callback != null) {
                        callback.onSuccess(authResult.getUser());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed", e);
                    if (callback != null) {
                        callback.onFailure(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage());
                    }
                });
    }

    public void logoutUser() {
        if (auth != null) {
            auth.signOut();
        }
    }

    public void getCurrentUserProfile(ProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onFailure("No user is currently logged in.");
            }
            return;
        }

        FirestoreManager.getInstance(context).getUserProfile(user.getUid(), new FirestoreManager.ProfileResultCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (callback != null) callback.onSuccess(profile);
            }

            @Override
            public void onFailure(String error) {
                // Fallback to basic FirebaseUser metadata
                UserProfile fallback = new UserProfile(
                        user.getUid(),
                        user.getEmail(),
                        user.getDisplayName() != null ? user.getDisplayName() : "JibonShield User",
                        UserProfile.ROLE_CITIZEN,
                        "Dhaka",
                        user.getPhoneNumber() != null ? user.getPhoneNumber() : "—",
                        System.currentTimeMillis()
                );
                if (callback != null) callback.onSuccess(fallback);
            }
        });
    }
}

