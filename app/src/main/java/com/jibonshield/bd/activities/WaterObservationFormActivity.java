package com.jibonshield.bd.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.jibonshield.bd.R;
import com.jibonshield.bd.databinding.ActivityWaterObservationFormBinding;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.sync.EmergencySyncWorker;
import com.jibonshield.bd.utils.DistrictData;
import com.jibonshield.bd.utils.FileUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Screen 7: Water Observation Form Activity.
 * Demonstrates View Binding, Android Photo Picker integration, local content caching,
 * field validation, and offline-first SQLite persistence with WorkManager cloud sync.
 */
public class WaterObservationFormActivity extends BaseActivity {

    public static final String EXTRA_WATER_OBSERVATION = "extra_water_observation";
    private static final String STATE_SELECTED_TIME = "state_selected_time";
    private static final String STATE_IMAGE_URI = "state_image_uri";

    private ActivityWaterObservationFormBinding binding;
    private EmergencyRepository repository;
    private String currentDateFormatted;
    private WaterObservation existingObservation;
    private Calendar selectedCalendar;
    private boolean isSubmitting = false;
    private String selectedImageUriString = null;
    private String cloudImageUrl = null;

    private final String[] districts = DistrictData.getDistrictsForRegistration();

    // Modern Photo Picker Launcher
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                handleSelectedImageUri(uri);
            });

    // Fallback document/content picker launcher for older Android devices
    private final ActivityResultLauncher<String> getContentLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                handleSelectedImageUri(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWaterObservationFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        selectedCalendar = Calendar.getInstance();
        repository = new EmergencyRepository(this);

        setupToolbar();
        setupSpinners();
        updateDateDisplay();
        checkEditMode();
        setupListeners();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_SELECTED_TIME)) {
                currentDateFormatted = savedInstanceState.getString(STATE_SELECTED_TIME);
                binding.tvObsFormDate.setText(currentDateFormatted);
            }
            if (savedInstanceState.containsKey(STATE_IMAGE_URI)) {
                selectedImageUriString = savedInstanceState.getString(STATE_IMAGE_URI);
                displaySelectedImage(selectedImageUriString);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_TIME, currentDateFormatted);
        outState.putString(STATE_IMAGE_URI, selectedImageUriString);
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarObsForm;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        currentDateFormatted = sdf.format(selectedCalendar.getTime()) + " BST";
        binding.tvObsFormDate.setText(currentDateFormatted);
    }

    private void setupSpinners() {
        binding.spinnerObsDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts));

        String[] sources = new String[]{
                getString(R.string.source_tubewell),
                getString(R.string.source_shallow_tubewell),
                getString(R.string.source_river),
                getString(R.string.source_pond),
                getString(R.string.source_tap),
                getString(R.string.source_rainwater)
        };
        binding.spinnerObsSourceType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sources));

        String[] appearances = new String[]{
                getString(R.string.appearance_clear),
                getString(R.string.appearance_cloudy),
                getString(R.string.appearance_reddish),
                getString(R.string.appearance_greenish)
        };
        binding.spinnerObsAppearance.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, appearances));

        String[] odors = new String[]{
                getString(R.string.odor_none),
                getString(R.string.odor_chlorine),
                getString(R.string.odor_earthy),
                getString(R.string.odor_sulfur)
        };
        binding.spinnerObsOdor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, odors));

        String[] turbidities = new String[]{
                getString(R.string.turbidity_low),
                getString(R.string.turbidity_moderate),
                getString(R.string.turbidity_high)
        };
        binding.spinnerObsTurbidity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, turbidities));

        String[] safeties = new String[]{
                getString(R.string.safety_potable),
                getString(R.string.safety_requires_boiling),
                getString(R.string.safety_unsafe)
        };
        binding.spinnerObsSafety.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, safeties));
    }

    private void checkEditMode() {
        if (getIntent().hasExtra(EXTRA_WATER_OBSERVATION)) {
            existingObservation = (WaterObservation) getIntent().getSerializableExtra(EXTRA_WATER_OBSERVATION);
            if (existingObservation != null) {
                binding.btnSaveObservation.setText(R.string.btn_update_observation);
                binding.etObsLocation.setText(existingObservation.getLocationName());
                if (existingObservation.isPhMeasured()) {
                    binding.etObsPh.setText(String.valueOf(existingObservation.getPhLevel()));
                } else {
                    binding.etObsPh.setText("");
                }
                binding.etObsReporter.setText(existingObservation.getReportedBy());
                binding.etObsNotes.setText(existingObservation.getRemarks());
                currentDateFormatted = existingObservation.getObservationTime();
                binding.tvObsFormDate.setText(currentDateFormatted);

                for (int i = 0; i < districts.length; i++) {
                    if (districts[i].equalsIgnoreCase(existingObservation.getDistrict())) {
                        binding.spinnerObsDistrict.setSelection(i);
                        break;
                    }
                }

                if (existingObservation.getImageUri() != null) {
                    selectedImageUriString = existingObservation.getImageUri();
                    displaySelectedImage(selectedImageUriString);
                }
                cloudImageUrl = existingObservation.getImageUrl();
            }
        }
    }

    private void setupListeners() {
        binding.btnChangeDatetime.setOnClickListener(v -> showDateTimePicker());
        binding.tvObsFormDate.setOnClickListener(v -> showDateTimePicker());

        binding.btnAddPhoto.setOnClickListener(v -> launchPhotoPicker());
        binding.btnRemovePhoto.setOnClickListener(v -> removeSelectedPhoto());

        binding.btnSaveObservation.setOnClickListener(v -> saveOrUpdateObservation());
        binding.btnGotoHistory.setOnClickListener(v -> {
            Intent intent = new Intent(WaterObservationFormActivity.this, WaterObservationHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void launchPhotoPicker() {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable()) {
                pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            } else {
                getContentLauncher.launch("image/*");
            }
        } catch (Exception e) {
            // Fallback for custom ROMs / legacy Android versions
            getContentLauncher.launch("image/*");
        }
    }

    private void handleSelectedImageUri(Uri uri) {
        if (uri != null) {
            // Persist a managed local copy into app private storage to ensure safe offline access
            String localUriString = FileUtils.saveImageToInternalStorage(this, uri);
            if (localUriString != null) {
                selectedImageUriString = localUriString;
            } else {
                selectedImageUriString = uri.toString();
            }
            displaySelectedImage(selectedImageUriString);
        }
    }

    private void displaySelectedImage(String uriString) {
        if (uriString != null && !uriString.trim().isEmpty()) {
            try {
                binding.ivPhotoPreview.setImageURI(Uri.parse(uriString));
                binding.layoutPhotoPreviewContainer.setVisibility(View.VISIBLE);
                binding.btnAddPhoto.setVisibility(View.GONE);
            } catch (Exception e) {
                binding.layoutPhotoPreviewContainer.setVisibility(View.GONE);
                binding.btnAddPhoto.setVisibility(View.VISIBLE);
            }
        } else {
            binding.layoutPhotoPreviewContainer.setVisibility(View.GONE);
            binding.btnAddPhoto.setVisibility(View.VISIBLE);
        }
    }

    private void removeSelectedPhoto() {
        selectedImageUriString = null;
        cloudImageUrl = null;
        binding.ivPhotoPreview.setImageURI(null);
        binding.layoutPhotoPreviewContainer.setVisibility(View.GONE);
        binding.btnAddPhoto.setVisibility(View.VISIBLE);
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
                                    binding.tvObsDateError.setVisibility(View.VISIBLE);
                                    binding.tvObsDateError.setText(R.string.obs_error_future_date);
                                    selectedCalendar = Calendar.getInstance();
                                } else {
                                    binding.tvObsDateError.setVisibility(View.GONE);
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
        if (isSubmitting) return;

        // 1. Validate Location
        String location = binding.etObsLocation.getText() != null ? binding.etObsLocation.getText().toString().trim() : "";
        if (!WaterObservation.isValidLocation(location)) {
            binding.tvObsLocationError.setVisibility(View.VISIBLE);
            binding.tvObsLocationError.setText(R.string.obs_error_location_required);
            binding.etObsLocation.requestFocus();
            return;
        } else {
            binding.tvObsLocationError.setVisibility(View.GONE);
        }

        // 2. Validate Optional pH
        double ph = WaterObservation.UNMEASURED_PH;
        String phStr = binding.etObsPh.getText() != null ? binding.etObsPh.getText().toString().trim() : "";
        if (!phStr.isEmpty()) {
            if (!WaterObservation.isValidPhString(phStr)) {
                binding.tvObsPhError.setVisibility(View.VISIBLE);
                binding.tvObsPhError.setText(R.string.obs_error_ph_range);
                binding.etObsPh.requestFocus();
                return;
            } else {
                try {
                    ph = Double.parseDouble(phStr);
                } catch (NumberFormatException e) {
                    binding.tvObsPhError.setVisibility(View.VISIBLE);
                    binding.tvObsPhError.setText(R.string.obs_error_ph_range);
                    binding.etObsPh.requestFocus();
                    return;
                }
            }
        }
        binding.tvObsPhError.setVisibility(View.GONE);

        // 3. Validate Notes Length
        String notes = binding.etObsNotes.getText() != null ? binding.etObsNotes.getText().toString().trim() : "";
        if (!WaterObservation.isValidNotes(notes)) {
            binding.tvObsNotesError.setVisibility(View.VISIBLE);
            binding.tvObsNotesError.setText(R.string.obs_error_notes_too_long);
            binding.etObsNotes.requestFocus();
            return;
        } else {
            binding.tvObsNotesError.setVisibility(View.GONE);
        }

        String district = districts[binding.spinnerObsDistrict.getSelectedItemPosition()];
        String source = binding.spinnerObsSourceType.getSelectedItem().toString();
        String appearance = binding.spinnerObsAppearance.getSelectedItem().toString();
        String odor = binding.spinnerObsOdor.getSelectedItem().toString();
        String turbidity = binding.spinnerObsTurbidity.getSelectedItem().toString();
        String safety = binding.spinnerObsSafety.getSelectedItem().toString();

        String reporter = binding.etObsReporter.getText() != null && !binding.etObsReporter.getText().toString().trim().isEmpty()
                ? binding.etObsReporter.getText().toString().trim()
                : getString(R.string.hint_obs_reporter);

        final double finalPh = ph;
        isSubmitting = true;
        binding.btnSaveObservation.setEnabled(false);

        if (selectedImageUriString != null && !selectedImageUriString.trim().isEmpty() && cloudImageUrl == null) {
            try {
                Uri imageUri = Uri.parse(selectedImageUriString);
                com.jibonshield.bd.firebase.FirebaseStorageManager.getInstance(this)
                        .uploadWaterObservationImage(imageUri, String.valueOf(System.currentTimeMillis()), new com.jibonshield.bd.firebase.FirebaseStorageManager.UploadCallback() {
                            @Override
                            public void onProgress(double progressPercent) {
                            }

                            @Override
                            public void onSuccess(String downloadUrl) {
                                cloudImageUrl = downloadUrl;
                                executeSaveOrUpdate(location, district, source, appearance, odor, finalPh, turbidity, safety, reporter, notes);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                executeSaveOrUpdate(location, district, source, appearance, odor, finalPh, turbidity, safety, reporter, notes);
                            }
                        });
            } catch (Exception e) {
                executeSaveOrUpdate(location, district, source, appearance, odor, finalPh, turbidity, safety, reporter, notes);
            }
        } else {
            executeSaveOrUpdate(location, district, source, appearance, odor, finalPh, turbidity, safety, reporter, notes);
        }
    }

    private void executeSaveOrUpdate(String location, String district, String source, String appearance,
                                    String odor, double ph, String turbidity, String safety,
                                    String reporter, String notes) {
        if (existingObservation != null) {
            // Update existing observation in Firestore
            WaterObservation updatedObs = new WaterObservation(
                    existingObservation.getId(),
                    location, district, source, appearance, odor,
                    ph, turbidity, safety, currentDateFormatted,
                    reporter, notes, selectedImageUriString, cloudImageUrl, WaterObservation.SYNC_SYNCED,
                    existingObservation.getCreatedAt(), System.currentTimeMillis()
            );

            repository.updateWaterObservation(updatedObs, new EmergencyRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    binding.btnSaveObservation.setEnabled(true);
                    Toast.makeText(WaterObservationFormActivity.this, R.string.obs_updated_success, Toast.LENGTH_SHORT).show();
                    EmergencySyncWorker.triggerImmediateSync(WaterObservationFormActivity.this);
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    binding.btnSaveObservation.setEnabled(true);
                    String msg = error != null ? error.getMessage() : "Unknown";
                    Toast.makeText(WaterObservationFormActivity.this, getString(R.string.obs_error_update_failed, msg), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Insert new observation directly into Firestore
            WaterObservation obs = new WaterObservation(
                    0, location, district, source, appearance, odor,
                    ph, turbidity, safety, currentDateFormatted, reporter, notes,
                    selectedImageUriString, cloudImageUrl, WaterObservation.SYNC_SYNCED,
                    System.currentTimeMillis(), System.currentTimeMillis()
            );

            repository.saveWaterObservation(obs, new EmergencyRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    binding.btnSaveObservation.setEnabled(true);
                    Toast.makeText(WaterObservationFormActivity.this, R.string.obs_saved_success, Toast.LENGTH_SHORT).show();
                    EmergencySyncWorker.triggerImmediateSync(WaterObservationFormActivity.this);

                    Intent intent = new Intent(WaterObservationFormActivity.this, WaterObservationHistoryActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing() || isDestroyed()) return;
                    isSubmitting = false;
                    binding.btnSaveObservation.setEnabled(true);
                    String msg = error != null ? error.getMessage() : "Unknown";
                    Toast.makeText(WaterObservationFormActivity.this, getString(R.string.obs_error_save_failed, msg), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
