package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseUser;
import com.jibonshield.bd.R;
import com.jibonshield.bd.databinding.ActivityRegisterBinding;
import com.jibonshield.bd.firebase.FirebaseAuthManager;
import com.jibonshield.bd.utils.DistrictData;
import com.jibonshield.bd.models.UserProfile;

/**
 * Registration Activity for JibonShield BD.
 * Demonstrates View Binding, role distinction (Citizen vs Volunteer),
 * form validation, and cloud registration with offline fallback.
 */
public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuthManager authManager;

    private final String[] districts = DistrictData.getDistrictsForRegistration();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = FirebaseAuthManager.getInstance(this);

        setupToolbar();
        setupSpinners();
        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarRegister;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        binding.spinnerRegDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts));
    }

    private void setupListeners() {
        binding.btnActionRegister.setOnClickListener(v -> performRegistration());

        binding.tvGotoLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = binding.etRegName.getText() != null ? binding.etRegName.getText().toString().trim() : "";
        String email = binding.etRegEmail.getText() != null ? binding.etRegEmail.getText().toString().trim() : "";
        String password = binding.etRegPassword.getText() != null ? binding.etRegPassword.getText().toString().trim() : "";
        String phone = binding.etRegPhone.getText() != null ? binding.etRegPhone.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, R.string.auth_error_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        String role = binding.rbRoleVolunteer.isChecked() ? UserProfile.ROLE_VOLUNTEER : UserProfile.ROLE_CITIZEN;
        String district = districts[binding.spinnerRegDistrict.getSelectedItemPosition()];

        binding.progressRegister.setVisibility(View.VISIBLE);
        binding.btnActionRegister.setEnabled(false);

        authManager.registerUser(email, password, name, role, district, phone, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressRegister.setVisibility(View.GONE);
                binding.btnActionRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, R.string.auth_success, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressRegister.setVisibility(View.GONE);
                binding.btnActionRegister.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                        .setTitle(R.string.auth_failed_title)
                        .setMessage(errorMessage)
                        .setPositiveButton(R.string.dialog_close, null)
                        .show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

