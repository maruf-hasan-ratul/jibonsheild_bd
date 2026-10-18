package com.jibonshield.bd.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Screen 7: Water Observation Form Activity.
 * Allows field observers and citizens to record offline water quality observations.
 * Adheres strictly to water safety integrity: records are observational, NOT lab certifications.
 */
public class WaterObservationFormActivity extends BaseActivity {

    public static final String EXTRA_WATER_OBSERVATION = "extra_water_observation";
    private static final String STATE_SELECTED_TIME = "state_selected_time";

    private TextView tvDate;
    private MaterialButton btnChangeDateTime;
    private TextView tvDateError;
    private EditText etLocation;
    private TextView tvLocationError;
    private Spinner spinnerDistrict;
    private Spinner spinnerSourceType;
    private Spinner spinnerAppearance;
    private Spinner spinnerOdor;
    private EditText etPh;
    private TextView tvPhError;
    private Spinner spinnerTurbidity;
    private Spinner spinnerSafety;
    private EditText etReporter;
    private EditText etNotes;
    private TextView tvNotesError;
    private MaterialButton btnSave;
    private MaterialButton btnGotoHistory;

    private EmergencyRepository repository;
    private String currentDateFormatted;
    private WaterObservation existingObservation;
    private Calendar selectedCalendar;
    private boolean isSubmitting = false;

    private final String[] districts = new String[]{
            "Dhaka", "Sylhet", "Chandpur", "Kurigram", "Cox's Bazar", "Chittagong", "Sunamganj", "Patuakhali", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_observation_form);

        selectedCalendar = Calendar.getInstance();
        initViews();
        setupToolbar();
        setupSpinners();
        checkEditMode();
        setupListeners();

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_SELECTED_TIME)) {
            currentDateFormatted = savedInstanceState.getString(STATE_SELECTED_TIME);
            tvDate.setText(currentDateFormatted);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_TIME, currentDateFormatted);
    }

    private void initViews() {
        tvDate = findViewById(R.id.tv_obs_form_date);
        btnChangeDateTime = findViewById(R.id.btn_change_datetime);
        tvDateError = findViewById(R.id.tv_obs_date_error);
        etLocation = findViewById(R.id.et_obs_location);
        tvLocationError = findViewById(R.id.tv_obs_location_error);
        spinnerDistrict = findViewById(R.id.spinner_obs_district);
        spinnerSourceType = findViewById(R.id.spinner_obs_source_type);
        spinnerAppearance = findViewById(R.id.spinner_obs_appearance);
        spinnerOdor = findViewById(R.id.spinner_obs_odor);
        etPh = findViewById(R.id.et_obs_ph);
        tvPhError = findViewById(R.id.tv_obs_ph_error);
        spinnerTurbidity = findViewById(R.id.spinner_obs_turbidity);
        spinnerSafety = findViewById(R.id.spinner_obs_safety);
        etReporter = findViewById(R.id.et_obs_reporter);
        etNotes = findViewById(R.id.et_obs_notes);
        tvNotesError = findViewById(R.id.tv_obs_notes_error);
        btnSave = findViewById(R.id.btn_save_observation);
        btnGotoHistory = findViewById(R.id.btn_goto_history);

        repository = new EmergencyRepository(this);

        updateDateDisplay();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        currentDateFormatted = sdf.format(selectedCalendar.getTime()) + " BST";
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
        spinnerDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts));

        String[] sources = new String[]{
                getString(R.string.source_tubewell),
                getString(R.string.source_shallow_tubewell),
                getString(R.string.source_river),
                getString(R.string.source_pond),
                getString(R.string.source_tap),
                getString(R.string.source_rainwater)
        };
        spinnerSourceType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sources));

        String[] appearances = new String[]{
                getString(R.string.appearance_clear),
                getString(R.string.appearance_cloudy),
                getString(R.string.appearance_reddish),
                getString(R.string.appearance_greenish)
        };
        spinnerAppearance.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, appearances));

        String[] odors = new String[]{
                getString(R.string.odor_none),
                getString(R.string.odor_chlorine),
                getString(R.string.odor_earthy),
                getString(R.string.odor_sulfur)
        };
        spinnerOdor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, odors));

        String[] turbidities = new String[]{
                getString(R.string.turbidity_low),
                getString(R.string.turbidity_moderate),
                getString(R.string.turbidity_high)
        };
        spinnerTurbidity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, turbidities));

        String[] safeties = new String[]{
                getString(R.string.safety_potable),
                getString(R.string.safety_requires_boiling),
                getString(R.string.safety_unsafe)
        };
        spinnerSafety.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, safeties));
    }

    private void checkEditMode() {
        if (getIntent().hasExtra(EXTRA_WATER_OBSERVATION)) {
            existingObservation = (WaterObservation) getIntent().getSerializableExtra(EXTRA_WATER_OBSERVATION);
            if (existingObservation != null) {
                btnSave.setText(R.string.btn_update_observation);
                etLocation.setText(existingObservation.getLocationName());
                if (existingObservation.isPhMeasured()) {
                    etPh.setText(String.valueOf(existingObservation.getPhLevel()));
                } else {
                    etPh.setText("");
                }
                etReporter.setText(existingObservation.getReportedBy());
                etNotes.setText(existingObservation.getRemarks());
                currentDateFormatted = existingObservation.getObservationTime();
                tvDate.setText(currentDateFormatted);

                for (int i = 0; i < districts.length; i++) {
                    if (districts[i].equalsIgnoreCase(existingObservation.getDistrict())) {
                        spinnerDistrict.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void setupListeners() {
        btnChangeDateTime.setOnClickListener(v -> showDateTimePicker());
        tvDate.setOnClickListener(v -> showDateTimePicker());

        btnSave.setOnClickListener(v -> saveOrUpdateObservation());
        btnGotoHistory.setOnClickListener(v -> {
            Intent intent = new Intent(WaterObservationFormActivity.this, WaterObservationHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void showDateTimePicker() {
        final Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (tView, hourOfDay, minute) -> {
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedCalendar.set(Calendar.MINUTE, minute);

                                if (selectedCalendar.after(now)) {
                                    tvDateError.setVisibility(View.VISIBLE);
                                    tvDateError.setText(R.string.obs_error_future_date);
                                    selectedCalendar = Calendar.getInstance();
                                } else {
                                    tvDateError.setVisibility(View.GONE);
                                }
                                updateDateDisplay();
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true);
                    timePickerDialog.show();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveOrUpdateObservation() {
        if (isSubmitting) {
            return;
        }

        // 1. Validate Location
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        if (!WaterObservation.isValidLocation(location)) {
            tvLocationError.setVisibility(View.VISIBLE);
            tvLocationError.setText(R.string.obs_error_location_required);
            etLocation.requestFocus();
            return;
        } else {
            tvLocationError.setVisibility(View.GONE);
        }

        // 2. Validate Optional pH
        double ph = WaterObservation.UNMEASURED_PH;
        String phStr = etPh.getText() != null ? etPh.getText().toString().trim() : "";
        if (!phStr.isEmpty()) {
            if (!WaterObservation.isValidPhString(phStr)) {
                tvPhError.setVisibility(View.VISIBLE);
                tvPhError.setText(R.string.obs_error_ph_range);
                etPh.requestFocus();
                return;
            } else {
                try {
                    ph = Double.parseDouble(phStr);
                } catch (NumberFormatException e) {
                    tvPhError.setVisibility(View.VISIBLE);
                    tvPhError.setText(R.string.obs_error_ph_range);
                    etPh.requestFocus();
                    return;
                }
            }
        }
        tvPhError.setVisibility(View.GONE);

        // 3. Validate Notes Length
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
        if (!WaterObservation.isValidNotes(notes)) {
            tvNotesError.setVisibility(View.VISIBLE);
            tvNotesError.setText(R.string.obs_error_notes_too_long);
            etNotes.requestFocus();
            return;
        } else {
            tvNotesError.setVisibility(View.GONE);
        }

        String district = districts[spinnerDistrict.getSelectedItemPosition()];
        String source = spinnerSourceType.getSelectedItem().toString();
        String appearance = spinnerAppearance.getSelectedItem().toString();
        String odor = spinnerOdor.getSelectedItem().toString();
        String turbidity = spinnerTurbidity.getSelectedItem().toString();
        String safety = spinnerSafety.getSelectedItem().toString();

        String reporter = etReporter.getText() != null && !etReporter.getText().toString().trim().isEmpty()
                ? etReporter.getText().toString().trim()
                : getString(R.string.hint_obs_reporter);

        isSubmitting = true;
        btnSave.setEnabled(false);

        if (existingObservation != null) {
            // Update existing observation
            WaterObservation updatedObs = new WaterObservation(
                    existingObservation.getId(),
                    location, district, source, appearance, odor,
                    ph, turbidity, safety, currentDateFormatted,
                    reporter, notes, existingObservation.getCreatedAt(), System.currentTimeMillis()
            );

            repository.updateWaterObservation(updatedObs, new EmergencyRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    btnSave.setEnabled(true);
                    Toast.makeText(WaterObservationFormActivity.this, R.string.obs_updated_success, Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    btnSave.setEnabled(true);
                    String msg = error != null ? error.getMessage() : "Unknown";
                    Toast.makeText(WaterObservationFormActivity.this, getString(R.string.obs_error_update_failed, msg), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Insert new observation
            WaterObservation obs = new WaterObservation(0, location, district, source, appearance, odor,
                    ph, turbidity, safety, currentDateFormatted, reporter, notes);

            repository.saveWaterObservation(obs, new EmergencyRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    btnSave.setEnabled(true);
                    Toast.makeText(WaterObservationFormActivity.this, R.string.obs_saved_success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(WaterObservationFormActivity.this, WaterObservationHistoryActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    btnSave.setEnabled(true);
                    String msg = error != null ? error.getMessage() : "Unknown";
                    Toast.makeText(WaterObservationFormActivity.this, getString(R.string.obs_error_save_failed, msg), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
