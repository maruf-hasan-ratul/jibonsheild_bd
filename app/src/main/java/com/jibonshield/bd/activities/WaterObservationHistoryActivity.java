package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.WaterObservationAdapter;
import com.jibonshield.bd.databinding.ActivityWaterObservationHistoryBinding;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.utils.DistrictData;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 8: Water Observation History Activity.
 * Demonstrates View Binding, RecyclerView dynamic searching/filtering,
 * full detail viewing with water safety integrity, edit, and delete operations.
 */
public class WaterObservationHistoryActivity extends BaseActivity implements WaterObservationAdapter.OnObservationClickListener {

    private ActivityWaterObservationHistoryBinding binding;
    private WaterObservationAdapter adapter;
    private EmergencyRepository repository;
    private final List<WaterObservation> observationList = new ArrayList<>();

    private final String[] filterDistricts = DistrictData.getDistrictsForHistoryFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWaterObservationHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EmergencyRepository(this);

        setupToolbar();
        setupRecyclerView();
        setupFilterControls();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        performFilterQuery();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarObsHistory;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        binding.recyclerWaterObservations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaterObservationAdapter(this, observationList, this);
        binding.recyclerWaterObservations.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.fabAddObservation.setOnClickListener(v -> openFormActivity(null));

        View emptyLayout = binding.emptyStateWaterObs.getRoot();
        View btnAction = emptyLayout.findViewById(R.id.btn_empty_action);
        if (btnAction != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> openFormActivity(null));
        }
    }

    private void setupFilterControls() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterDistricts);
        binding.spinnerFilterDistrict.setAdapter(districtAdapter);

        binding.spinnerFilterDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFilterQuery();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.etSearchWaterObs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    binding.btnClearObsFilter.setVisibility(View.VISIBLE);
                } else {
                    binding.btnClearObsFilter.setVisibility(View.GONE);
                }
                performFilterQuery();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearObsFilter.setOnClickListener(v -> {
            binding.etSearchWaterObs.setText("");
            binding.spinnerFilterDistrict.setSelection(0);
            performFilterQuery();
        });
    }

    private void performFilterQuery() {
        String query = binding.etSearchWaterObs.getText() != null ? binding.etSearchWaterObs.getText().toString().trim() : "";
        int distPos = binding.spinnerFilterDistrict.getSelectedItemPosition();
        String district = (distPos > 0 && distPos < filterDistricts.length) ? filterDistricts[distPos] : null;

        binding.progressObsHistory.setVisibility(View.VISIBLE);
        repository.filterWaterObservations(query, district, new EmergencyRepository.DataCallback<List<WaterObservation>>() {
            @Override
            public void onDataLoaded(List<WaterObservation> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressObsHistory.setVisibility(View.GONE);
                observationList.clear();
                if (data != null) {
                    observationList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                int count = observationList.size();
                binding.tvObsCount.setText(getString(R.string.obs_filter_count_format, count));

                if (observationList.isEmpty()) {
                    binding.emptyStateWaterObs.getRoot().setVisibility(View.VISIBLE);
                    binding.recyclerWaterObservations.setVisibility(View.GONE);
                } else {
                    binding.emptyStateWaterObs.getRoot().setVisibility(View.GONE);
                    binding.recyclerWaterObservations.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressObsHistory.setVisibility(View.GONE);
                binding.emptyStateWaterObs.getRoot().setVisibility(View.VISIBLE);
                binding.recyclerWaterObservations.setVisibility(View.GONE);
            }
        });
    }

    private boolean isObservationOwner(WaterObservation observation) {
        if (observation == null) return false;
        String currentUid = com.jibonshield.bd.firebase.FirebaseAuthManager.getInstance(this).getCurrentUserId();
        String currentEmail = com.jibonshield.bd.utils.PreferenceUtils.getUserEmail(this);
        String currentName = com.jibonshield.bd.utils.PreferenceUtils.getUserName(this);
        return observation.isCreatedBy(currentUid, currentName, currentEmail);
    }

    private void openFormActivity(WaterObservation obs) {
        if (obs != null && !isObservationOwner(obs)) {
            Toast.makeText(this, R.string.obs_error_unauthorized, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, WaterObservationFormActivity.class);
        if (obs != null) {
            intent.putExtra(WaterObservationFormActivity.EXTRA_WATER_OBSERVATION, obs);
        }
        startActivity(intent);
    }

    @Override
    public void onObservationClick(WaterObservation observation) {
        boolean isOwner = isObservationOwner(observation);
        List<String> optionsList = new ArrayList<>();
        optionsList.add(getString(R.string.obs_action_view_details));
        if (isOwner) {
            optionsList.add(getString(R.string.action_edit));
            optionsList.add(getString(R.string.action_delete));
        }

        String[] options = optionsList.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle(observation.getLocationName())
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (selected.equals(getString(R.string.obs_action_view_details))) {
                        showObservationDetailsDialog(observation);
                    } else if (selected.equals(getString(R.string.action_edit))) {
                        openFormActivity(observation);
                    } else if (selected.equals(getString(R.string.action_delete))) {
                        confirmDeleteObservation(observation);
                    }
                })
                .setNegativeButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showObservationDetailsDialog(WaterObservation observation) {
        StringBuilder details = new StringBuilder();
        details.append(getString(R.string.label_obs_location)).append(": ").append(observation.getLocationName()).append("\n");
        details.append(getString(R.string.label_obs_district)).append(": ").append(observation.getDistrict()).append("\n");
        details.append(getString(R.string.label_obs_source_type)).append(": ").append(observation.getSourceType()).append("\n");
        details.append(getString(R.string.label_obs_date)).append(": ").append(observation.getObservationTime()).append("\n");
        details.append(getString(R.string.label_obs_reporter)).append(": ").append(observation.getReportedBy()).append("\n\n");

        details.append("--- ").append(getString(R.string.card_water_title)).append(" ---\n");
        details.append(getString(R.string.label_obs_appearance)).append(": ").append(observation.getAppearance()).append("\n");
        details.append(getString(R.string.label_obs_odor)).append(": ").append(observation.getOdor()).append("\n");

        if (observation.isPhMeasured()) {
            details.append(getString(R.string.obs_measurement_ph_format, observation.getPhLevel())).append("\n");
        } else {
            details.append(getString(R.string.obs_measurement_ph_unmeasured)).append("\n");
        }
        details.append(getString(R.string.label_obs_turbidity)).append(": ").append(observation.getTurbidityStatus()).append("\n");
        details.append(getString(R.string.obs_field_assessment_label)).append(" ").append(observation.getSafetyStatus()).append("\n");

        if (observation.hasImage()) {
            details.append("\n📷 ").append(getString(R.string.photo_attached)).append(" (").append(observation.getSyncStatus()).append(")\n");
        }

        if (observation.getRemarks() != null && !observation.getRemarks().trim().isEmpty()) {
            details.append("\n").append(getString(R.string.label_obs_notes)).append(":\n").append(observation.getRemarks()).append("\n\n");
        }

        details.append("\n⚠️ ").append(getString(R.string.obs_disclaimer_title)).append(":\n");
        details.append(getString(R.string.obs_disclaimer_body)).append("\n\n");
        details.append(getString(R.string.obs_safe_water_guidance_title)).append(":\n");
        details.append(getString(R.string.obs_safe_water_guidance_body));

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.obs_details_title))
                .setMessage(details.toString());

        if (isObservationOwner(observation)) {
            dialogBuilder.setPositiveButton(R.string.action_edit, (dialog, which) -> openFormActivity(observation));
            dialogBuilder.setNeutralButton(R.string.action_delete, (dialog, which) -> confirmDeleteObservation(observation));
        }

        dialogBuilder.setNegativeButton(R.string.obs_dialog_close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmDeleteObservation(WaterObservation observation) {
        if (!isObservationOwner(observation)) {
            Toast.makeText(this, R.string.obs_error_unauthorized, Toast.LENGTH_LONG).show();
            return;
        }

        String currentUid = com.jibonshield.bd.firebase.FirebaseAuthManager.getInstance(this).getCurrentUserId();
        String currentEmail = com.jibonshield.bd.utils.PreferenceUtils.getUserEmail(this);
        String currentName = com.jibonshield.bd.utils.PreferenceUtils.getUserName(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.dialog_delete_confirm)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    repository.deleteWaterObservation(observation.getId(), currentUid, currentName, currentEmail, new EmergencyRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(WaterObservationHistoryActivity.this, R.string.obs_deleted_success, Toast.LENGTH_SHORT).show();
                            performFilterQuery();
                        }

                        @Override
                        public void onError(Exception error) {
                            if (isFinishing() || isDestroyed()) return;
                            String msg = error != null ? error.getMessage() : "Unknown error";
                            Toast.makeText(WaterObservationHistoryActivity.this, getString(R.string.obs_error_delete_failed, msg), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
