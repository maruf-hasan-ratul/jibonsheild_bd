package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.FloodWarningAdapter;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2: Flood Warning List.
 * Displays active flood advisories and simulated offline warning drills with region
 * and active/expired status filtering. Expired warnings are strictly isolated.
 */
public class FloodWarningListActivity extends BaseActivity implements FloodWarningAdapter.OnWarningClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAction;
    private Spinner spinnerRegion;
    private Spinner spinnerStatus;

    private FloodWarningAdapter adapter;
    private EmergencyRepository repository;
    private final List<FloodWarning> warningList = new ArrayList<>();
    private final String[] regions = new String[]{"All", "Sylhet", "Sunamganj", "Kurigram", "Sirajganj", "Bogura"};
    private String[] statusOptions;

    private int currentStatusSelection = 0; // 0 = Active, 1 = Expired, 2 = All
    private String currentRegion = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_warning_list);

        statusOptions = new String[]{
                getString(R.string.filter_active_warnings),
                getString(R.string.filter_expired_warnings),
                getString(R.string.filter_all_warnings)
        };

        initViews();
        setupToolbar();
        setupSpinners();
        setupRecyclerView();
        loadWarnings();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_flood_warnings);
        progressBar = findViewById(R.id.progress_flood_warnings);
        emptyStateLayout = findViewById(R.id.empty_state_flood);
        spinnerRegion = findViewById(R.id.spinner_flood_region);
        spinnerStatus = findViewById(R.id.spinner_flood_status);

        if (emptyStateLayout != null) {
            tvEmptyTitle = emptyStateLayout.findViewById(R.id.tv_empty_title);
            tvEmptyDesc = emptyStateLayout.findViewById(R.id.tv_empty_message);
            btnEmptyAction = emptyStateLayout.findViewById(R.id.btn_empty_action);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.flood_empty_title);
            if (tvEmptyDesc != null) tvEmptyDesc.setText(R.string.flood_empty_desc);
            if (btnEmptyAction != null) {
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setText(R.string.action_clear_filters);
                btnEmptyAction.setOnClickListener(v -> {
                    spinnerRegion.setSelection(0);
                    spinnerStatus.setSelection(0);
                });
            }
        }

        repository = new EmergencyRepository(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_flood_warnings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        // Region spinner
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, regions);
        spinnerRegion.setAdapter(regionAdapter);
        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRegion = regions[position];
                loadWarnings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Status spinner (Active vs Expired vs All)
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusOptions);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStatusSelection = position;
                loadWarnings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FloodWarningAdapter(this, warningList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadWarnings() {
        progressBar.setVisibility(View.VISIBLE);

        EmergencyRepository.DataCallback<List<FloodWarning>> callback = new EmergencyRepository.DataCallback<List<FloodWarning>>() {
            @Override
            public void onDataLoaded(List<FloodWarning> data) {
                progressBar.setVisibility(View.GONE);
                warningList.clear();
                if (data != null) {
                    warningList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                if (warningList.isEmpty()) {
                    if (emptyStateLayout != null) {
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.flood_empty_title);
                        if (tvEmptyDesc != null) {
                            tvEmptyDesc.setText(getString(R.string.flood_empty_desc) + "\n\n" + getString(R.string.warning_safety_caveat_body));
                        }
                        if (btnEmptyAction != null) {
                            btnEmptyAction.setVisibility(View.VISIBLE);
                            btnEmptyAction.setText(R.string.action_view_guidelines);
                            btnEmptyAction.setOnClickListener(v -> {
                                startActivity(new Intent(FloodWarningListActivity.this, EvacuationGuidelinesActivity.class));
                            });
                        }
                    }
                    recyclerView.setVisibility(View.GONE);
                } else {
                    if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception error) {
                progressBar.setVisibility(View.GONE);
                if (emptyStateLayout != null) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    if (tvEmptyDesc != null) {
                        tvEmptyDesc.setText(getString(R.string.flood_empty_desc) + "\n\n" + getString(R.string.warning_safety_caveat_body));
                    }
                }
                recyclerView.setVisibility(View.GONE);
            }
        };

        if (currentStatusSelection == 0) {
            // Active warnings only (default)
            repository.getActiveFloodWarnings(currentRegion, callback);
        } else if (currentStatusSelection == 1) {
            // Expired warnings only
            repository.getExpiredFloodWarnings(currentRegion, callback);
        } else {
            // All warnings
            repository.getFloodWarnings(currentRegion, callback);
        }
    }

    @Override
    public void onWarningClick(FloodWarning warning) {
        Intent intent = new Intent(this, FloodWarningDetailActivity.class);
        intent.putExtra("extra_flood_warning", warning);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_sync_refresh) {
            triggerSync();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerSync() {
        progressBar.setVisibility(View.VISIBLE);
        android.widget.Toast.makeText(this, R.string.sync_in_progress, android.widget.Toast.LENGTH_SHORT).show();

        com.jibonshield.bd.sync.SyncManager.getInstance(this).syncFloodWarnings(null, new com.jibonshield.bd.sync.SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String entityName) {}

            @Override
            public void onSyncComplete(com.jibonshield.bd.sync.SyncResult result) {
                loadWarnings();
                if (result.isOffline()) {
                    android.widget.Toast.makeText(FloodWarningListActivity.this, R.string.sync_offline_toast, android.widget.Toast.LENGTH_LONG).show();
                } else if (result.isSuccess()) {
                    android.widget.Toast.makeText(FloodWarningListActivity.this, getString(R.string.sync_complete_toast, result.getMessage()), android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(FloodWarningListActivity.this, getString(R.string.sync_error_toast, result.getMessage()), android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
