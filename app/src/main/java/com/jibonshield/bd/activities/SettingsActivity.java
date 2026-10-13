package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.utils.PreferenceUtils;

/**
 * Screen 12: Settings & Configuration.
 * Manages language selection (বাংলা / English), primary location preference, and offline database summary.
 */
public class SettingsActivity extends AppCompatActivity {

    private RadioGroup rgLanguage;
    private RadioButton rbBangla;
    private RadioButton rbEnglish;
    private Spinner spinnerDistrict;
    private MaterialButton btnAbout;

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
    }

    private void initViews() {
        rgLanguage = findViewById(R.id.rg_settings_language);
        rbBangla = findViewById(R.id.rb_lang_bangla);
        rbEnglish = findViewById(R.id.rb_lang_english);
        spinnerDistrict = findViewById(R.id.spinner_settings_district);
        btnAbout = findViewById(R.id.btn_settings_about);
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
            if (checkedId == R.id.rb_lang_english) {
                PreferenceUtils.setLanguage(this, PreferenceUtils.LANG_EN);
            } else {
                PreferenceUtils.setLanguage(this, PreferenceUtils.LANG_BN);
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
}
