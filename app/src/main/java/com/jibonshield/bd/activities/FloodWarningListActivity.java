package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.FloodWarningAdapter;
import com.jibonshield.bd.databinding.ActivityFloodWarningListBinding;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.sync.SyncManager;
import com.jibonshield.bd.sync.SyncResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2: Flood Warning List.
 * Displays active flood advisories and simulated offline warning drills with region
 * and active/expired status filtering. Demonstrates View Binding and dynamic filtering.
 */
public class FloodWarningListActivity extends BaseActivity implements FloodWarningAdapter.OnWarningClickListener {

    private ActivityFloodWarningListBinding binding;
    private FloodWarningAdapter adapter;
    private EmergencyRepository repository;
    private final List<FloodWarning> warningList = new ArrayList<>();
    private final String[] regions = com.jibonshield.bd.utils.DistrictData.getDistrictsWithAll();
    private String[] statusOptions;

    private int currentStatusSelection = 0; // 0 = Active, 1 = Expired, 2 = All
    private String currentRegion = "All";

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

        String hometownDistrict = com.jibonshield.bd.utils.PreferenceUtils.getHometownDistrict(this);
        if (com.jibonshield.bd.utils.PreferenceUtils.isHometownConfigured(this) && !hometownDistrict.isEmpty()) {
            for (int i = 0; i < regions.length; i++) {
                if (regions[i].equalsIgnoreCase(hometownDistrict)) {
                    currentRegion = regions[i];
                    binding.spinnerFloodRegion.setSelection(i);
                    break;
                }
            }
        }

        binding.spinnerFloodRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRegion = regions[position];
                loadWarnings();
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

    private void loadWarnings() {
        binding.progressFloodWarnings.setVisibility(View.VISIBLE);

        EmergencyRepository.DataCallback<List<FloodWarning>> callback = new EmergencyRepository.DataCallback<List<FloodWarning>>() {
            @Override
            public void onDataLoaded(List<FloodWarning> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressFloodWarnings.setVisibility(View.GONE);
                warningList.clear();
                if (data != null) {
                    warningList.addAll(data);
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
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressFloodWarnings.setVisibility(View.GONE);
                binding.emptyStateFlood.getRoot().setVisibility(View.VISIBLE);
                binding.recyclerFloodWarnings.setVisibility(View.GONE);
            }
        };

        if (currentRegion.equalsIgnoreCase("All") && com.jibonshield.bd.utils.PreferenceUtils.isHometownConfigured(this)) {
            String div = com.jibonshield.bd.utils.PreferenceUtils.getHometownDivision(this);
            String dist = com.jibonshield.bd.utils.PreferenceUtils.getHometownDistrict(this);
            String upz = com.jibonshield.bd.utils.PreferenceUtils.getHometownUpazila(this);
            repository.getDisasterUpdatesByHometown(div, dist, upz, callback);
            return;
        }

        if (currentStatusSelection == 0) {
            repository.getActiveFloodWarnings(currentRegion, callback);
        } else if (currentStatusSelection == 1) {
            repository.getExpiredFloodWarnings(currentRegion, callback);
        } else {
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
        binding.progressFloodWarnings.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show();

        SyncManager.getInstance(this).syncFloodWarnings(null, new SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String entityName) {}

            @Override
            public void onSyncComplete(SyncResult result) {
                loadWarnings();
                if (result.isOffline()) {
                    Toast.makeText(FloodWarningListActivity.this, R.string.sync_offline_toast, Toast.LENGTH_LONG).show();
                } else if (result.isSuccess()) {
                    Toast.makeText(FloodWarningListActivity.this, getString(R.string.sync_complete_toast, result.getMessage()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FloodWarningListActivity.this, getString(R.string.sync_error_toast, result.getMessage()), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
