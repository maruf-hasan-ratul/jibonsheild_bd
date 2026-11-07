package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.jibonshield.bd.R;
import com.jibonshield.bd.databinding.ActivityProfileBinding;
import com.jibonshield.bd.firebase.FirebaseAuthManager;
import com.jibonshield.bd.models.UserProfile;

/**
 * Profile Activity for JibonShield BD.
 * Displays authenticated profile details, role-based safety badges, and sign-out controls.
 */
public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = FirebaseAuthManager.getInstance(this);

        setupToolbar();
        setupListeners();
        loadProfile();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarProfile;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        binding.btnActionLogout.setOnClickListener(v -> {
            authManager.logoutUser();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadProfile() {
        if (!authManager.isUserLoggedIn()) {
            // Guest mode display
            binding.tvProfileName.setText(R.string.profile_guest_mode);
            binding.tvProfileRole.setText(R.string.role_citizen);
            binding.tvProfileEmail.setText("guest@offline.local");
            binding.tvProfileDistrict.setText("National Baseline (64 Districts)");
            binding.tvProfileStatus.setText(R.string.profile_guest_mode);
            binding.btnActionLogout.setText(R.string.btn_login);
            binding.btnActionLogout.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
            });
            return;
        }

        authManager.getCurrentUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (isFinishing() || isDestroyed() || profile == null) return;

                binding.tvProfileName.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "JibonShield User");
                binding.tvProfileRole.setText(profile.isVolunteer() ? getString(R.string.role_volunteer) : getString(R.string.role_citizen));
                binding.tvProfileEmail.setText(profile.getEmail() != null ? profile.getEmail() : "—");
                binding.tvProfileDistrict.setText(profile.getDistrict() != null ? profile.getDistrict() : "All");
                binding.tvProfileStatus.setText(R.string.profile_cloud_mode);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

