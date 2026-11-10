package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
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
import com.jibonshield.bd.utils.DistrictData;
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
    private RadioGroup rgTheme;
    private RadioButton rbThemeSystem;
    private RadioButton rbThemeLight;
    private RadioButton rbThemeDark;
    private RadioButton rbThemeAmoled;
    private Spinner spinnerDistrict;
    private MaterialButton btnAbout;

    private final String[] districts = DistrictData.getDistrictsWithAll();

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
        rgTheme = findViewById(R.id.rg_settings_theme);
        rbThemeSystem = findViewById(R.id.rb_theme_system);
        rbThemeLight = findViewById(R.id.rb_theme_light);
        rbThemeDark = findViewById(R.id.rb_theme_dark);
        rbThemeAmoled = findViewById(R.id.rb_theme_amoled);
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

        // Theme
        String theme = PreferenceUtils.getThemeMode(this);
        if (PreferenceUtils.THEME_AMOLED.equalsIgnoreCase(theme)) {
            rbThemeAmoled.setChecked(true);
        } else if (PreferenceUtils.THEME_DARK.equalsIgnoreCase(theme)) {
            rbThemeDark.setChecked(true);
        } else if (PreferenceUtils.THEME_LIGHT.equalsIgnoreCase(theme)) {
            rbThemeLight.setChecked(true);
        } else {
            rbThemeSystem.setChecked(true);
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

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedTheme;
            if (checkedId == R.id.rb_theme_amoled) {
                selectedTheme = PreferenceUtils.THEME_AMOLED;
            } else if (checkedId == R.id.rb_theme_dark) {
                selectedTheme = PreferenceUtils.THEME_DARK;
            } else if (checkedId == R.id.rb_theme_light) {
                selectedTheme = PreferenceUtils.THEME_LIGHT;
            } else {
                selectedTheme = PreferenceUtils.THEME_SYSTEM;
            }

            if (!selectedTheme.equalsIgnoreCase(PreferenceUtils.getThemeMode(this))) {
                PreferenceUtils.setThemeMode(this, selectedTheme);
                applyAppTheme();
                android.widget.Toast.makeText(this, R.string.toast_theme_changed, android.widget.Toast.LENGTH_SHORT).show();
                recreate();
            }
        });

        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String dist = districts[position];
                PreferenceUtils.setSelectedDistrict(SettingsActivity.this, dist);
                if (!"All".equalsIgnoreCase(dist)) {
                    com.jibonshield.bd.firebase.FirebaseAuthManager.getInstance(SettingsActivity.this)
                            .updateHometown("", dist, "", null);
                }
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
