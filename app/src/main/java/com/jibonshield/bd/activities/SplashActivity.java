package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jibonshield.bd.R;
import com.jibonshield.bd.repositories.EmergencyRepository;

/**
 * Launch screen activity for JibonShield BD.
 * Verifies the local SQLite database initialization asynchronously before transitioning to the Dashboard.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 1400;

    private TextView tvSplashStatus;
    private EmergencyRepository emergencyRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvSplashStatus = findViewById(R.id.tv_splash_status);
        emergencyRepository = new EmergencyRepository(this);

        // Initialize local SQLite seed on background thread
        emergencyRepository.ensureDatabaseInitialized(new EmergencyRepository.DatabaseInitCallback() {
            @Override
            public void onDatabaseReady(int contactCount) {
                if (tvSplashStatus != null) {
                    tvSplashStatus.setText(R.string.splash_ready);
                }
                scheduleNavigation();
            }

            @Override
            public void onError(Exception error) {
                // Graceful fallback: continue to dashboard even if log error occurs
                scheduleNavigation();
            }
        });
    }

    private void scheduleNavigation() {
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
