package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen 7: Water Observation Form.
 * Allows field volunteers and citizens to record drinking water source condition, appearance, and tests offline.
 */
public class WaterObservationFormActivity extends AppCompatActivity {

    private TextView tvDate;
    private EditText etLocation;
    private TextView tvLocationError;
    private Spinner spinnerDistrict;
    private Spinner spinnerSourceType;
    private Spinner spinnerAppearance;
    private Spinner spinnerOdor;
    private EditText etPh;
    private Spinner spinnerTurbidity;
    private Spinner spinnerSafety;
    private EditText etReporter;
    private EditText etNotes;
    private MaterialButton btnSave;
    private MaterialButton btnGotoHistory;

    private EmergencyRepository repository;
    private String currentDateFormatted;

    private final String[] districts = new String[]{
            "Dhaka", "Sylhet", "Chandpur", "Kurigram", "Cox's Bazar", "Chittagong", "Sunamganj", "Patuakhali", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_observation_form);

        initViews();
        setupToolbar();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        tvDate = findViewById(R.id.tv_obs_form_date);
        etLocation = findViewById(R.id.et_obs_location);
        tvLocationError = findViewById(R.id.tv_obs_location_error);
        spinnerDistrict = findViewById(R.id.spinner_obs_district);
        spinnerSourceType = findViewById(R.id.spinner_obs_source_type);
        spinnerAppearance = findViewById(R.id.spinner_obs_appearance);
        spinnerOdor = findViewById(R.id.spinner_obs_odor);
        etPh = findViewById(R.id.et_obs_ph);
        spinnerTurbidity = findViewById(R.id.spinner_obs_turbidity);
        spinnerSafety = findViewById(R.id.spinner_obs_safety);
        etReporter = findViewById(R.id.et_obs_reporter);
        etNotes = findViewById(R.id.et_obs_notes);
        btnSave = findViewById(R.id.btn_save_observation);
        btnGotoHistory = findViewById(R.id.btn_goto_history);

        repository = new EmergencyRepository(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        currentDateFormatted = sdf.format(new Date()) + " BST";
        tvDate.setText(currentDateFormatted);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_obs_form);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        // District
        spinnerDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts));

        // Source Types
        String[] sources = new String[]{
                getString(R.string.source_tubewell),
                getString(R.string.source_shallow_tubewell),
                getString(R.string.source_river),
                getString(R.string.source_pond),
                getString(R.string.source_tap),
                getString(R.string.source_rainwater)
        };
        spinnerSourceType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sources));

        // Appearance
        String[] appearances = new String[]{
                getString(R.string.appearance_clear),
                getString(R.string.appearance_cloudy),
                getString(R.string.appearance_reddish),
                getString(R.string.appearance_greenish)
        };
        spinnerAppearance.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, appearances));

        // Odor
        String[] odors = new String[]{
                getString(R.string.odor_none),
                getString(R.string.odor_chlorine),
                getString(R.string.odor_earthy),
                getString(R.string.odor_sulfur)
        };
        spinnerOdor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, odors));

        // Turbidity
        String[] turbidities = new String[]{
                getString(R.string.turbidity_low),
                getString(R.string.turbidity_moderate),
                getString(R.string.turbidity_high)
        };
        spinnerTurbidity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, turbidities));

        // Safety Assessment
        String[] safeties = new String[]{
                getString(R.string.safety_potable),
                getString(R.string.safety_requires_boiling),
                getString(R.string.safety_unsafe)
        };
        spinnerSafety.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, safeties));
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveObservation());
        btnGotoHistory.setOnClickListener(v -> {
            Intent intent = new Intent(WaterObservationFormActivity.this, WaterObservationHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void saveObservation() {
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        if (location.isEmpty()) {
            tvLocationError.setVisibility(View.VISIBLE);
            tvLocationError.setText(R.string.obs_validation_error);
            etLocation.requestFocus();
            return;
        } else {
            tvLocationError.setVisibility(View.GONE);
        }

        String district = districts[spinnerDistrict.getSelectedItemPosition()];
        String source = spinnerSourceType.getSelectedItem().toString();
        String appearance = spinnerAppearance.getSelectedItem().toString();
        String odor = spinnerOdor.getSelectedItem().toString();
        String turbidity = spinnerTurbidity.getSelectedItem().toString();
        String safety = spinnerSafety.getSelectedItem().toString();

        double ph = 7.0;
        String phStr = etPh.getText() != null ? etPh.getText().toString().trim() : "";
        if (!phStr.isEmpty()) {
            try {
                ph = Double.parseDouble(phStr);
            } catch (NumberFormatException ignored) {}
        }

        String reporter = etReporter.getText() != null && !etReporter.getText().toString().trim().isEmpty()
                ? etReporter.getText().toString().trim()
                : "Community Observer";

        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        WaterObservation obs = new WaterObservation(0, location, district, source, appearance, odor,
                ph, turbidity, safety, currentDateFormatted, reporter, notes);

        btnSave.setEnabled(false);
        repository.saveWaterObservation(obs, new EmergencyRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(WaterObservationFormActivity.this, R.string.obs_saved_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(WaterObservationFormActivity.this, WaterObservationHistoryActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception error) {
                btnSave.setEnabled(true);
                Toast.makeText(WaterObservationFormActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
