package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.FloodWarningAdapter;
import com.jibonshield.bd.databinding.ActivityFloodWarningListBinding;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.sync.FirestoreSyncManager;
import com.jibonshield.bd.utils.DistrictData;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2: Flood Warning & Disaster Alert List.
 * Official Emergency Broadcast Viewer powered by Cloud Firestore & Room offline-first caching.
 * Real-time cloud sync, automatic hometown prioritization, and region filtering.
 */
public class FloodWarningListActivity extends BaseActivity implements FloodWarningAdapter.OnWarningClickListener {

    private ActivityFloodWarningListBinding binding;
    private FloodWarningAdapter adapter;
    private EmergencyRepository repository;
    private final List<FloodWarning> warningList = new ArrayList<>();
    private final String[] regions = DistrictData.getDistrictsWithAll();
    private String[] statusOptions;

    private int currentStatusSelection = 0; // 0 = Active, 1 = Expired, 2 = All
    private String currentRegion = "All";
    private ListenerRegistration realtimeRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFloodWarningListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        statusOptions = new String[]{
                getString(R.string.filter_active_warnings),
                getString(R.string.filter_expired_warnings),
                getString(R.string.filter_all_warnings)
        };

        repository = new EmergencyRepository(this);

        setupToolbar();
        setupSpinners();
        setupRecyclerView();
        setupEmptyStateActions();
        loadWarnings();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachRealtimeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachRealtimeListener();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarFloodWarnings;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupEmptyStateActions() {
        View emptyLayout = binding.emptyStateFlood.getRoot();
        View btnAction = emptyLayout.findViewById(R.id.btn_empty_action);
        if (btnAction != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> {
                binding.spinnerFloodRegion.setSelection(0);
                binding.spinnerFloodStatus.setSelection(0);
            });
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, regions);
        binding.spinnerFloodRegion.setAdapter(regionAdapter);
        binding.spinnerFloodRegion.setSelection(0); // Default to "All" to show nationwide alerts

        binding.spinnerFloodRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRegion = regions[position];
                loadWarnings();
                attachRealtimeListener();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusOptions);
        binding.spinnerFloodStatus.setAdapter(statusAdapter);
        binding.spinnerFloodStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStatusSelection = position;
                loadWarnings();
                attachRealtimeListener();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        binding.recyclerFloodWarnings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FloodWarningAdapter(this, warningList, this);
        binding.recyclerFloodWarnings.setAdapter(adapter);
    }

    private void attachRealtimeListener() {
        detachRealtimeListener();
        Boolean activeFilter = (currentStatusSelection == 0) ? Boolean.TRUE : (currentStatusSelection == 1 ? Boolean.FALSE : null);
        realtimeRegistration = repository.listenToDisasterUpdates(currentRegion, activeFilter, new EmergencyRepository.DataCallback<List<FloodWarning>>() {
            @Override
            public void onDataLoaded(List<FloodWarning> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressFloodWarnings.setVisibility(View.GONE);
                updateWarningList(data);
            }

            @Override
            public void onError(Exception error) {}
        });
    }

    private void detachRealtimeListener() {
        if (realtimeRegistration != null) {
            realtimeRegistration.remove();
            realtimeRegistration = null;
        }
    }

    private void loadWarnings() {
        binding.progressFloodWarnings.setVisibility(View.VISIBLE);

        EmergencyRepository.DataCallback<List<FloodWarning>> callback = new EmergencyRepository.DataCallback<List<FloodWarning>>() {
            @Override
            public void onDataLoaded(List<FloodWarning> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressFloodWarnings.setVisibility(View.GONE);
                updateWarningList(data);
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressFloodWarnings.setVisibility(View.GONE);
                if (warningList.isEmpty()) {
                    binding.emptyStateFlood.getRoot().setVisibility(View.VISIBLE);
                    binding.recyclerFloodWarnings.setVisibility(View.GONE);
                }
            }
        };

        if (currentStatusSelection == 0) {
            repository.getActiveFloodWarnings(currentRegion, callback);
        } else if (currentStatusSelection == 1) {
            repository.getExpiredFloodWarnings(currentRegion, callback);
        } else {
            repository.getFloodWarnings(currentRegion, callback);
        }
    }

    private void updateWarningList(List<FloodWarning> data) {
        warningList.clear();
        if (data != null) {
            String hometown = PreferenceUtils.getHometownDistrict(this).toLowerCase().trim();
            if (currentRegion.equalsIgnoreCase("All") && !hometown.isEmpty()) {
                List<FloodWarning> hometownList = new ArrayList<>();
                List<FloodWarning> otherList = new ArrayList<>();
                for (FloodWarning w : data) {
                    if (w.getRegion() != null && w.getRegion().toLowerCase().contains(hometown)) {
                        hometownList.add(w);
                    } else {
                        otherList.add(w);
                    }
                }
                warningList.addAll(hometownList);
                warningList.addAll(otherList);
            } else {
                warningList.addAll(data);
            }
        }
        adapter.notifyDataSetChanged();

        if (warningList.isEmpty()) {
            binding.emptyStateFlood.getRoot().setVisibility(View.VISIBLE);
            binding.recyclerFloodWarnings.setVisibility(View.GONE);
        } else {
            binding.emptyStateFlood.getRoot().setVisibility(View.GONE);
            binding.recyclerFloodWarnings.setVisibility(View.VISIBLE);
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
        binding.progressFloodWarnings.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show();

        FirestoreSyncManager.getInstance(this).syncAll(new FirestoreSyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String stage) {}

            @Override
            public void onSyncProgress(String stage, int count) {}

            @Override
            public void onSyncComplete(boolean success, int totalSynced, String message) {
                runOnUiThread(() -> {
                    loadWarnings();
                    Toast.makeText(FloodWarningListActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachRealtimeListener();
        binding = null;
    }
}
