package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.SyncMetadata;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.utils.LocaleHelper;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen 12: Settings &amp; Configuration.
 * Manages language selection (বাংলা / English), primary location preference,
 * and live offline database storage summary &amp; sync state.
 */
public class SettingsActivity extends BaseActivity {

    private RadioGroup rgLanguage;
    private RadioButton rbBangla;
    private RadioButton rbEnglish;
    private Spinner spinnerDistrict;
    private TextView tvStorageContacts;
    private TextView tvStorageShelters;
    private TextView tvStorageDatabase;
    private MaterialButton btnAbout;

    private EmergencyRepository repository;

    private final String[] districts = new String[]{
            "All", "Dhaka", "Sylhet", "Cox's Bazar", "Sunamganj", "Kurigram", "Patuakhali", "Chittagong"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupToolbar();
        setupPreferences();
        setupListeners();
        loadDatabaseStorageInfo();
    }

    private void initViews() {
        rgLanguage = findViewById(R.id.rg_settings_language);
        rbBangla = findViewById(R.id.rb_lang_bangla);
        rbEnglish = findViewById(R.id.rb_lang_english);
        spinnerDistrict = findViewById(R.id.spinner_settings_district);
        tvStorageContacts = findViewById(R.id.tv_storage_contacts);
        tvStorageShelters = findViewById(R.id.tv_storage_shelters);
        tvStorageDatabase = findViewById(R.id.tv_storage_database);
        btnAbout = findViewById(R.id.btn_settings_about);

        repository = new EmergencyRepository(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupPreferences() {
        // Language
        String lang = PreferenceUtils.getLanguage(this);
        if (PreferenceUtils.LANG_EN.equalsIgnoreCase(lang)) {
            rbEnglish.setChecked(true);
        } else {
            rbBangla.setChecked(true);
        }

        // District
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts);
        spinnerDistrict.setAdapter(districtAdapter);
        String selectedDistrict = PreferenceUtils.getSelectedDistrict(this);
        for (int i = 0; i < districts.length; i++) {
            if (districts[i].equalsIgnoreCase(selectedDistrict)) {
                spinnerDistrict.setSelection(i);
                break;
            }
        }
    }

    private void setupListeners() {
        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String newLang = (checkedId == R.id.rb_lang_english) ? PreferenceUtils.LANG_EN : PreferenceUtils.LANG_BN;
            String currentLang = PreferenceUtils.getLanguage(this);
            if (!newLang.equalsIgnoreCase(currentLang)) {
                PreferenceUtils.setLanguage(this, newLang);
                LocaleHelper.setLocale(this, newLang);
                int toastRes = newLang.equalsIgnoreCase(PreferenceUtils.LANG_EN) ? R.string.toast_lang_changed_en : R.string.toast_lang_changed_bn;
                android.widget.Toast.makeText(this, toastRes, android.widget.Toast.LENGTH_SHORT).show();

                Intent restartIntent = new Intent(this, MainActivity.class);
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(restartIntent);
                finish();
            }
        });

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferenceUtils.setSelectedDistrict(SettingsActivity.this, districts[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }

    private void loadDatabaseStorageInfo() {
        repository.getAllSyncMetadata(new EmergencyRepository.DataCallback<List<SyncMetadata>>() {
            @Override
            public void onDataLoaded(List<SyncMetadata> metadataList) {
                if (metadataList != null && !metadataList.isEmpty()) {
                    for (SyncMetadata meta : metadataList) {
                        String timeStr = formatTimestamp(meta.getLastSyncTimestamp());
                        if (meta.getTableName().contains("contact")) {
                            tvStorageContacts.setText("Emergency Hotlines: " + meta.getRecordsCount() + " Seeded (" + meta.getSyncStatus() + ")");
                        } else if (meta.getTableName().contains("shelter")) {
                            tvStorageShelters.setText("Designated Shelters: " + meta.getRecordsCount() + " Verified (" + meta.getSyncStatus() + ")");
                        }
                    }
                    tvStorageDatabase.setText("Local Database: SQLite v3 (Offline Cache Active)");
                }
            }

            @Override
            public void onError(Exception error) {
                // Keep default baseline text
            }
        });
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "Initial Baseline";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp)) + " BST";
    }
}
