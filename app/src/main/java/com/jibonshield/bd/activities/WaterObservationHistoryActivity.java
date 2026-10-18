package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.WaterObservationAdapter;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 8: Water Observation History Activity.
 * Displays local water observation records with real-time text/district filtering,
 * full detail viewing with water safety integrity guidance, edit, and delete operations.
 */
public class WaterObservationHistoryActivity extends BaseActivity implements WaterObservationAdapter.OnObservationClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAction;
    private ExtendedFloatingActionButton fabAdd;

    private EditText etSearch;
    private ImageView btnClearFilter;
    private Spinner spinnerFilterDistrict;
    private TextView tvObsCount;

    private WaterObservationAdapter adapter;
    private EmergencyRepository repository;
    private final List<WaterObservation> observationList = new ArrayList<>();

    private final String[] filterDistricts = new String[]{
            "All Districts", "Dhaka", "Sylhet", "Chandpur", "Kurigram", "Cox's Bazar", "Chittagong", "Sunamganj", "Patuakhali", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_observation_history);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        performFilterQuery();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_water_observations);
        progressBar = findViewById(R.id.progress_obs_history);
        emptyStateLayout = findViewById(R.id.empty_state_water_obs);
        fabAdd = findViewById(R.id.fab_add_observation);
        etSearch = findViewById(R.id.et_search_water_obs);
        btnClearFilter = findViewById(R.id.btn_clear_obs_filter);
        spinnerFilterDistrict = findViewById(R.id.spinner_filter_district);
        tvObsCount = findViewById(R.id.tv_obs_count);

        if (emptyStateLayout != null) {
            tvEmptyTitle = emptyStateLayout.findViewById(R.id.tv_empty_title);
            tvEmptyDesc = emptyStateLayout.findViewById(R.id.tv_empty_message);
            btnEmptyAction = emptyStateLayout.findViewById(R.id.btn_empty_action);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.obs_empty_title);
            if (tvEmptyDesc != null) tvEmptyDesc.setText(R.string.obs_empty_desc);
            if (btnEmptyAction != null) {
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setText(R.string.action_add_observation);
                btnEmptyAction.setOnClickListener(v -> openFormActivity(null));
            }
        }

        fabAdd.setOnClickListener(v -> openFormActivity(null));
        repository = new EmergencyRepository(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_obs_history);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaterObservationAdapter(this, observationList, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterControls() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterDistricts);
        spinnerFilterDistrict.setAdapter(districtAdapter);

        spinnerFilterDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFilterQuery();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    btnClearFilter.setVisibility(View.VISIBLE);
                } else {
                    btnClearFilter.setVisibility(View.GONE);
                }
                performFilterQuery();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearFilter.setOnClickListener(v -> {
            etSearch.setText("");
            spinnerFilterDistrict.setSelection(0);
            performFilterQuery();
        });
    }

    private void performFilterQuery() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        int distPos = spinnerFilterDistrict.getSelectedItemPosition();
        String district = (distPos > 0 && distPos < filterDistricts.length) ? filterDistricts[distPos] : null;

        progressBar.setVisibility(View.VISIBLE);
        repository.filterWaterObservations(query, district, new EmergencyRepository.DataCallback<List<WaterObservation>>() {
            @Override
            public void onDataLoaded(List<WaterObservation> data) {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                observationList.clear();
                if (data != null) {
                    observationList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                int count = observationList.size();
                tvObsCount.setText(getString(R.string.obs_filter_count_format, count));

                if (observationList.isEmpty()) {
                    if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) return;
                progressBar.setVisibility(View.GONE);
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void openFormActivity(WaterObservation obs) {
        Intent intent = new Intent(this, WaterObservationFormActivity.class);
        if (obs != null) {
            intent.putExtra(WaterObservationFormActivity.EXTRA_WATER_OBSERVATION, obs);
        }
        startActivity(intent);
    }

    @Override
    public void onObservationClick(WaterObservation observation) {
        String[] options = new String[]{
                getString(R.string.obs_action_view_details),
                getString(R.string.action_edit),
                getString(R.string.action_delete)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(observation.getLocationName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showObservationDetailsDialog(observation);
                    } else if (which == 1) {
                        openFormActivity(observation);
                    } else if (which == 2) {
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
        details.append(getString(R.string.obs_field_assessment_label)).append(" ").append(observation.getSafetyStatus()).append("\n\n");

        if (observation.getRemarks() != null && !observation.getRemarks().trim().isEmpty()) {
            details.append(getString(R.string.label_obs_notes)).append(":\n").append(observation.getRemarks()).append("\n\n");
        }

        details.append("⚠️ ").append(getString(R.string.obs_disclaimer_title)).append(":\n");
        details.append(getString(R.string.obs_disclaimer_body)).append("\n\n");
        details.append(getString(R.string.obs_safe_water_guidance_title)).append(":\n");
        details.append(getString(R.string.obs_safe_water_guidance_body));

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.obs_details_title))
                .setMessage(details.toString())
                .setPositiveButton(R.string.action_edit, (dialog, which) -> openFormActivity(observation))
                .setNeutralButton(R.string.action_delete, (dialog, which) -> confirmDeleteObservation(observation))
                .setNegativeButton(R.string.obs_dialog_close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmDeleteObservation(WaterObservation observation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.dialog_delete_confirm)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    repository.deleteWaterObservation(observation.getId(), new EmergencyRepository.ActionCallback() {
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
                .setNegativeButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
