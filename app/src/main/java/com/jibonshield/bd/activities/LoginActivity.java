package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseUser;
import com.jibonshield.bd.R;
import com.jibonshield.bd.databinding.ActivityLoginBinding;
import com.jibonshield.bd.firebase.FirebaseAuthManager;

/**
 * Login Activity for JibonShield BD.
 * Demonstrates View Binding, Firebase Authentication, input validation,
 * and seamless offline guest access.
 */
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = FirebaseAuthManager.getInstance(this);

        setupToolbar();
        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarLogin;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        binding.btnActionLogin.setOnClickListener(v -> performLogin());

        binding.tvGotoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnGuestAccess.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, R.string.profile_guest_mode, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void performLogin() {
        String email = binding.etLoginEmail.getText() != null ? binding.etLoginEmail.getText().toString().trim() : "";
        String password = binding.etLoginPassword.getText() != null ? binding.etLoginPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, R.string.auth_error_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressLogin.setVisibility(View.VISIBLE);
        binding.btnActionLogin.setEnabled(false);

        authManager.loginUser(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressLogin.setVisibility(View.GONE);
                binding.btnActionLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressLogin.setVisibility(View.GONE);
                binding.btnActionLogin.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
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

