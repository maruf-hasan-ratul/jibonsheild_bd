package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.DashboardAdapter;
import com.jibonshield.bd.databinding.ActivityMainBinding;
import com.jibonshield.bd.firebase.FirebaseAuthManager;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.DashboardCardItem;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.receivers.AirplaneModeReceiver;
import com.jibonshield.bd.receivers.BatterySaverReceiver;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.services.EmergencyMonitoringService;
import com.jibonshield.bd.sync.SyncManager;
import com.jibonshield.bd.sync.SyncResult;
import com.jibonshield.bd.utils.NetworkUtils;
import com.jibonshield.bd.utils.PermissionUtils;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Dashboard Activity for JibonShield BD.
 * Demonstrates View Binding, Airplane Mode & Battery Saver Broadcast Receivers,
 * persistent Foreground Monitoring Service, user authentication profile access,
 * and offline-first SQLite summary calculations.
 */
public class MainActivity extends BaseActivity implements DashboardAdapter.OnModuleClickListener {

    private ActivityMainBinding binding;
    private DashboardAdapter adapter;
    private List<DashboardCardItem> moduleList;
    private EmergencyRepository repository;

    private AirplaneModeReceiver airplaneModeReceiver;
    private BatterySaverReceiver batterySaverReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EmergencyRepository(this);

        setupToolbar();
        setupModules();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceivers();
        com.jibonshield.bd.firebase.RealtimeDatabaseManager.getInstance(this).listenToEmergencyStatus(new com.jibonshield.bd.firebase.RealtimeDatabaseManager.EmergencyStatusListener() {
            @Override
            public void onStatusUpdated(java.util.List<com.jibonshield.bd.models.LiveEmergencyIncident> incidents) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                if (incidents != null && !incidents.isEmpty()) {
                    com.jibonshield.bd.models.LiveEmergencyIncident top = incidents.get(0);
                    binding.tvBroadcastStatus.setVisibility(android.view.View.VISIBLE);
                    binding.tvBroadcastStatus.setText("🚨 LIVE RTDB: " + top.getMessage());
                    binding.tvBroadcastStatus.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.emergency_red));
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceivers();
        com.jibonshield.bd.firebase.RealtimeDatabaseManager.getInstance(this).stopListeningToEmergencyStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNavMain != null) {
            binding.bottomNavMain.setSelectedItemId(R.id.nav_home);
        }
        updateConnectivityState();
        updateSelectedLocation();
        loadDashboardSummary();
        updateMonitoringUi();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarMain;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void registerReceivers() {
        airplaneModeReceiver = AirplaneModeReceiver.register(this, (isAirplaneModeOn, hasInternet) -> {
            updateBroadcastIndicators();
            updateConnectivityState();
        });

        batterySaverReceiver = BatterySaverReceiver.register(this, isPowerSaveMode -> {
            updateBroadcastIndicators();
        });

        updateBroadcastIndicators();
    }

    private void unregisterReceivers() {
        AirplaneModeReceiver.unregister(this, airplaneModeReceiver);
        BatterySaverReceiver.unregister(this, batterySaverReceiver);
    }

    private void updateBroadcastIndicators() {
        boolean isPowerSave = BatterySaverReceiver.isPowerSaveModeActive(this);
        // Check system airplane mode setting
        boolean isAirplane = false;
        try {
            isAirplane = android.provider.Settings.Global.getInt(
                    getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        } catch (Exception ignored) {}

        if (isAirplane) {
            binding.tvBroadcastStatus.setVisibility(View.VISIBLE);
            binding.tvBroadcastStatus.setText(R.string.status_airplane_mode_active);
            binding.tvBroadcastStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
        } else if (isPowerSave) {
            binding.tvBroadcastStatus.setVisibility(View.VISIBLE);
            binding.tvBroadcastStatus.setText(R.string.status_battery_saver_active);
            binding.tvBroadcastStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
        } else {
            binding.tvBroadcastStatus.setVisibility(View.GONE);
        }
    }

    private void updateConnectivityState() {
        boolean isConnected = NetworkUtils.isInternetAvailable(this);
        if (isConnected) {
            binding.tvOfflineHeader.setText(R.string.status_online_mode);
            binding.tvOfflineDesc.setText(R.string.status_online_sub);
            binding.tvOfflineHeader.setTextColor(ContextCompat.getColor(this, R.color.success_green));
            binding.ivConnectivityIcon.setImageResource(R.drawable.ic_check_circle);
        } else {
            binding.tvOfflineHeader.setText(R.string.status_offline_mode);
            binding.tvOfflineDesc.setText(R.string.status_offline_sub);
            binding.tvOfflineHeader.setTextColor(ContextCompat.getColor(this, R.color.badge_offline_text));
            binding.ivConnectivityIcon.setImageResource(R.drawable.ic_offline);
        }
    }

    private void updateSelectedLocation() {
        if (PreferenceUtils.isHometownConfigured(this)) {
            String upz = PreferenceUtils.getHometownUpazila(this);
            String dist = PreferenceUtils.getHometownDistrict(this);
            String locationStr = (!upz.isEmpty() ? upz + ", " : "") + (!dist.isEmpty() ? dist : "");
            binding.tvDashboardLocation.setText("🏠 " + locationStr);
        } else {
            String district = PreferenceUtils.getSelectedDistrict(this);
            if (district == null || district.equalsIgnoreCase("All")) {
                binding.tvDashboardLocation.setText(R.string.dashboard_location_label);
            } else {
                binding.tvDashboardLocation.setText(getString(R.string.label_selected_location) + " " + district);
            }
        }
    }

    private void updateMonitoringUi() {
        boolean isMonitoring = EmergencyMonitoringService.isMonitoringActive();
        if (isMonitoring) {
            binding.tvMonitoringTitle.setText(R.string.monitoring_active_title);
            binding.tvMonitoringTitle.setTextColor(ContextCompat.getColor(this, R.color.emergency_red));
            binding.btnToggleMonitoring.setText(R.string.action_stop_monitoring);
            binding.btnToggleMonitoring.setTextColor(ContextCompat.getColor(this, R.color.emergency_red));
            binding.cardMonitoringSession.setStrokeColor(ContextCompat.getColor(this, R.color.emergency_red));
        } else {
            binding.tvMonitoringTitle.setText(R.string.channel_emergency_monitoring_name);
            binding.tvMonitoringTitle.setTextColor(ContextCompat.getColor(this, R.color.primary));
            binding.btnToggleMonitoring.setText(R.string.action_start_monitoring);
            binding.btnToggleMonitoring.setTextColor(ContextCompat.getColor(this, R.color.primary));
            binding.cardMonitoringSession.setStrokeColor(ContextCompat.getColor(this, R.color.divider_color));
        }
    }

    private void toggleEmergencyMonitoring() {
        PermissionUtils.requestNotificationPermission(this);

        if (EmergencyMonitoringService.isMonitoringActive()) {
            EmergencyMonitoringService.stopMonitoring(this);
            Toast.makeText(this, R.string.action_stop_monitoring, Toast.LENGTH_SHORT).show();
        } else {
            EmergencyMonitoringService.startMonitoring(this);
            Toast.makeText(this, R.string.monitoring_active_title, Toast.LENGTH_SHORT).show();
        }
        updateMonitoringUi();
    }

    private void loadDashboardSummary() {
        String div = PreferenceUtils.getHometownDivision(this);
        String dist = PreferenceUtils.getHometownDistrict(this);
        String upz = PreferenceUtils.getHometownUpazila(this);

        // Tile 1: Active disaster warnings prioritized by hometown
        repository.getDisasterUpdatesByHometown(div, dist, upz, new EmergencyRepository.DataCallback<List<FloodWarning>>() {
            @Override
            public void onDataLoaded(List<FloodWarning> data) {
                if (binding == null) return;
                int count = data != null ? data.size() : 0;
                binding.tvSummaryWarningsCount.setText(String.valueOf(count));
                binding.tvSummaryWarningsCount.setTextColor(ContextCompat.getColor(MainActivity.this,
                        count > 0 ? R.color.emergency_red : R.color.success_green));
                binding.tvSummarySafetyCaveat.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Exception error) {
                if (binding != null) binding.tvSummaryWarningsCount.setText("—");
            }
        });

        // Tile 2: Designated shelters (prioritized by hometown district if configured)
        String shelterDistrict = (dist != null && !dist.isEmpty()) ? dist : "All";
        repository.getShelters("", shelterDistrict, new EmergencyRepository.DataCallback<List<Shelter>>() {
            @Override
            public void onDataLoaded(List<Shelter> data) {
                if (binding != null) {
                    binding.tvSummarySheltersCount.setText(data != null ? String.valueOf(data.size()) : "—");
                }
            }

            @Override
            public void onError(Exception error) {
                if (binding != null) binding.tvSummarySheltersCount.setText("—");
            }
        });

        // Tile 3: Emergency contacts
        repository.getEmergencyContacts(new EmergencyRepository.DataCallback<List<EmergencyContact>>() {
            @Override
            public void onDataLoaded(List<EmergencyContact> data) {
                if (binding != null) {
                    binding.tvSummaryContactsCount.setText(data != null ? String.valueOf(data.size()) : "—");
                }
            }

            @Override
            public void onError(Exception error) {
                if (binding != null) binding.tvSummaryContactsCount.setText("—");
            }
        });

        // Tile 4: Preparedness checklist progress
        repository.getChecklistItems(new EmergencyRepository.DataCallback<List<ChecklistItem>>() {
            @Override
            public void onDataLoaded(List<ChecklistItem> data) {
                if (binding == null || data == null) return;
                int total = data.size();
                int done = 0;
                for (ChecklistItem item : data) {
                    if (item.isCompleted()) done++;
                }
                int pct = total > 0 ? (done * 100) / total : 0;
                binding.tvSummaryChecklistCount.setText(pct + "%");
            }

            @Override
            public void onError(Exception error) {
                if (binding != null) binding.tvSummaryChecklistCount.setText("—");
            }
        });
    }

    private void setupModules() {
        moduleList = new ArrayList<>();

        // 1. Flood Warnings
        moduleList.add(new DashboardCardItem(
                1, R.string.card_flood_title, R.string.card_flood_desc,
                R.drawable.ic_flood, R.string.badge_offline_ready, true
        ));

        // 2. Nearby Shelters
        moduleList.add(new DashboardCardItem(
                2, R.string.card_shelter_title, R.string.card_shelter_desc,
                R.drawable.ic_shelter, R.string.badge_offline_ready, true
        ));

        // 3. Emergency Contacts
        moduleList.add(new DashboardCardItem(
                3, R.string.card_emergency_title, R.string.card_emergency_desc,
                R.drawable.ic_call_emergency, R.string.badge_offline_ready, true
        ));

        // 4. Drinking Water Quality
        moduleList.add(new DashboardCardItem(
                4, R.string.card_water_title, R.string.card_water_desc,
                R.drawable.ic_water, R.string.badge_offline_ready, true
        ));

        // 5. Heat Safety
        moduleList.add(new DashboardCardItem(
                5, R.string.card_heat_title, R.string.card_heat_desc,
                R.drawable.ic_heat, R.string.badge_offline_ready, true
        ));

        // 6. Evacuation Guidelines
        moduleList.add(new DashboardCardItem(
                6, R.string.card_evacuation_title, R.string.card_evacuation_desc,
                R.drawable.ic_evacuation, R.string.badge_offline_ready, true
        ));

        // 7. Preparedness Checklist
        moduleList.add(new DashboardCardItem(
                7, R.string.card_checklist_title, R.string.card_checklist_desc,
                R.drawable.ic_checklist, R.string.badge_offline_ready, true
        ));

        // 8. Settings & Language
        moduleList.add(new DashboardCardItem(
                8, R.string.card_settings_title, R.string.card_settings_desc,
                R.drawable.ic_settings, R.string.badge_offline_ready, true
        ));

        binding.recyclerDashboardModules.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DashboardAdapter(this, moduleList, this);
        binding.recyclerDashboardModules.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnEmergencyHotlines.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmergencyContactsActivity.class);
            startActivity(intent);
        });

        if (binding.btnActionShelters != null) {
            binding.btnActionShelters.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ShelterDirectoryActivity.class));
            });
        }

        if (binding.btnActionWater != null) {
            binding.btnActionWater.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, WaterObservationFormActivity.class));
            });
        }

        if (binding.bottomNavMain != null) {
            binding.bottomNavMain.setSelectedItemId(R.id.nav_home);
            binding.bottomNavMain.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_shelters) {
                    startActivity(new Intent(MainActivity.this, ShelterDirectoryActivity.class));
                    return false;
                } else if (itemId == R.id.nav_alerts) {
                    startActivity(new Intent(MainActivity.this, FloodWarningListActivity.class));
                    return false;
                } else if (itemId == R.id.nav_help) {
                    startActivity(new Intent(MainActivity.this, EmergencyContactsActivity.class));
                    return false;
                }
                return false;
            });
        }

        binding.btnMainAccount.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        binding.btnMainSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnToggleMonitoring.setOnClickListener(v -> toggleEmergencyMonitoring());

        binding.bannerOfflineStatus.setOnClickListener(v -> {
            boolean isConnected = NetworkUtils.isInternetAvailable(MainActivity.this);
            if (!isConnected) {
                Toast.makeText(MainActivity.this, R.string.sync_offline_toast, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show();
                SyncManager.getInstance(MainActivity.this).syncAll(new SyncManager.SyncCallback() {
                    @Override
                    public void onSyncStarted(String entityName) {}

                    @Override
                    public void onSyncComplete(SyncResult result) {
                        loadDashboardSummary();
                        updateConnectivityState();
                        if (result.isSuccess()) {
                            Toast.makeText(MainActivity.this, getString(R.string.sync_complete_toast, result.getMessage()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.sync_error_toast, result.getMessage()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onModuleClick(DashboardCardItem item) {
        Intent intent;
        switch (item.getId()) {
            case 1:
                intent = new Intent(this, FloodWarningListActivity.class);
                break;
            case 2:
                intent = new Intent(this, ShelterDirectoryActivity.class);
                break;
            case 3:
                intent = new Intent(this, EmergencyContactsActivity.class);
                break;
            case 4:
                intent = new Intent(this, WaterObservationFormActivity.class);
                break;
            case 5:
                intent = new Intent(this, HeatSafetyActivity.class);
                break;
            case 6:
                intent = new Intent(this, EvacuationGuidelinesActivity.class);
                break;
            case 7:
                intent = new Intent(this, PreparednessChecklistActivity.class);
                break;
            case 8:
                intent = new Intent(this, SettingsActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
