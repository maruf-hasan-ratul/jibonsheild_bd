package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jibonshield.bd.R;
import com.jibonshield.bd.databinding.ActivityProfileBinding;
import com.jibonshield.bd.firebase.FirebaseAuthManager;
import com.jibonshield.bd.models.UserProfile;
import com.jibonshield.bd.utils.PreferenceUtils;

/**
 * Profile Activity for JibonShield BD.
 * Displays authenticated profile details, role-based safety badges, and sign-out controls.
 * Supports customizable user display name and persistent email display across offline and cloud modes.
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
        binding.btnEditName.setOnClickListener(v -> showEditNameDialog());
        binding.btnChangeName.setOnClickListener(v -> showEditNameDialog());

        binding.btnActionLogout.setOnClickListener(v -> {
            if (!authManager.isUserLoggedIn()) {
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
                return;
            }
            authManager.logoutUser();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadProfile() {
        if (!authManager.isUserLoggedIn()) {
            // Guest mode display
            String guestName = PreferenceUtils.getUserName(this);
            if (guestName.isEmpty()) {
                guestName = getString(R.string.role_citizen);
            }
            binding.tvProfileName.setText(guestName);
            binding.tvProfileNameDetail.setText(guestName);
            binding.tvProfileRole.setText(R.string.role_citizen);

            String guestEmail = PreferenceUtils.getUserEmail(this);
            if (guestEmail.isEmpty()) {
                guestEmail = "guest@offline.local";
                binding.tvProfileEmailHint.setVisibility(View.VISIBLE);
                binding.tvProfileEmailHint.setText(R.string.profile_guest_email_hint);
            } else {
                binding.tvProfileEmailHint.setVisibility(View.GONE);
            }
            binding.tvProfileEmail.setText(guestEmail);

            if (PreferenceUtils.isHometownConfigured(this)) {
                String upz = PreferenceUtils.getHometownUpazila(this);
                String dist = PreferenceUtils.getHometownDistrict(this);
                binding.tvProfileDistrict.setText((!upz.isEmpty() ? upz + ", " : "") + dist);
            } else {
                binding.tvProfileDistrict.setText("National Baseline (64 Districts)");
            }
            binding.tvProfileStatus.setText(R.string.profile_guest_mode);
            binding.btnActionLogout.setText(R.string.btn_login);
            return;
        }

        // Cloud authenticated mode
        authManager.getCurrentUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (isFinishing() || isDestroyed() || profile == null) return;

                String name = profile.getName();
                if (name == null || name.isEmpty() || name.equals("User")) {
                    name = PreferenceUtils.getUserName(ProfileActivity.this);
                }
                if (name.isEmpty() && profile.getDisplayName() != null && !profile.getDisplayName().isEmpty()) {
                    name = profile.getDisplayName();
                }
                if (name.isEmpty() && authManager.getCurrentUser() != null && authManager.getCurrentUser().getDisplayName() != null) {
                    name = authManager.getCurrentUser().getDisplayName();
                }
                if (name.isEmpty()) {
                    name = "JibonShield User";
                }

                String email = profile.getEmail();
                if (email == null || email.isEmpty()) {
                    email = PreferenceUtils.getUserEmail(ProfileActivity.this);
                }
                if (email.isEmpty() && authManager.getCurrentUser() != null && authManager.getCurrentUser().getEmail() != null) {
                    email = authManager.getCurrentUser().getEmail();
                }
                if (email.isEmpty()) {
                    email = "—";
                }

                binding.tvProfileName.setText(name);
                binding.tvProfileNameDetail.setText(name);
                binding.tvProfileRole.setText(profile.isVolunteer() ? getString(R.string.role_volunteer) : getString(R.string.role_citizen));
                binding.tvProfileEmail.setText(email);
                binding.tvProfileEmailHint.setVisibility(View.GONE);

                String location = profile.getDistrict() != null ? profile.getDistrict() : "All";
                if (profile.getUpazilaId() != null && !profile.getUpazilaId().isEmpty()) {
                    location = profile.getUpazilaId() + ", " + location;
                }
                binding.tvProfileDistrict.setText(location);
                binding.tvProfileStatus.setText(R.string.profile_cloud_mode);
                binding.btnActionLogout.setText(R.string.btn_logout);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditNameDialog() {
        String currentName = binding.tvProfileNameDetail.getText() != null
                ? binding.tvProfileNameDetail.getText().toString().trim() : "";
        if (currentName.equals(getString(R.string.role_citizen)) || currentName.equals("JibonShield User")) {
            currentName = "";
        }

        final TextInputEditText input = new TextInputEditText(this);
        input.setSingleLine(true);
        input.setText(currentName);
        if (currentName.length() > 0) {
            input.setSelection(currentName.length());
        }
        input.setHint(R.string.dialog_edit_name_hint);

        TextInputLayout inputLayout = new TextInputLayout(this);
        int padH = (int) getResources().getDimension(R.dimen.spacing_large);
        int padV = (int) getResources().getDimension(R.dimen.spacing_medium);
        inputLayout.setPadding(padH, padV, padH, padV);
        inputLayout.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_edit_name_title)
                .setView(inputLayout)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(this, R.string.error_name_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveNewName(newName);
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveNewName(String newName) {
        authManager.updateUserName(newName, new FirebaseAuthManager.ActionCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                binding.tvProfileName.setText(newName);
                binding.tvProfileNameDetail.setText(newName);
                Toast.makeText(ProfileActivity.this, R.string.toast_name_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
