package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.DashboardAdapter;
import com.jibonshield.bd.models.DashboardCardItem;
import com.jibonshield.bd.utils.NetworkUtils;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Dashboard Activity for JibonShield BD.
 * Provides offline-first entry points to all disaster preparedness and environmental safety modules.
 */
public class MainActivity extends AppCompatActivity implements DashboardAdapter.OnModuleClickListener {

    private RecyclerView recyclerModules;
    private DashboardAdapter adapter;
    private List<DashboardCardItem> moduleList;
    private View bannerOffline;
    private ImageView ivConnectivityIcon;
    private TextView tvOfflineHeader;
    private TextView tvOfflineDesc;
    private TextView tvDashboardLocation;
    private MaterialButton btnEmergencyHotlines;
    private ImageView btnMainSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupToolbar();
        setupModules();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectivityState();
        updateSelectedLocation();
    }

    private void initViews() {
        recyclerModules = findViewById(R.id.recycler_dashboard_modules);
        bannerOffline = findViewById(R.id.banner_offline_status);
        ivConnectivityIcon = findViewById(R.id.iv_connectivity_icon);
        tvOfflineHeader = findViewById(R.id.tv_offline_header);
        tvOfflineDesc = findViewById(R.id.tv_offline_desc);
        tvDashboardLocation = findViewById(R.id.tv_dashboard_location);
        btnEmergencyHotlines = findViewById(R.id.btn_emergency_hotlines);
        btnMainSettings = findViewById(R.id.btn_main_settings);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void updateConnectivityState() {
        boolean isConnected = NetworkUtils.isInternetAvailable(this);
        if (isConnected) {
            tvOfflineHeader.setText(R.string.status_online_mode);
            tvOfflineDesc.setText(R.string.status_online_sub);
            tvOfflineHeader.setTextColor(ContextCompat.getColor(this, R.color.success_green));
            if (ivConnectivityIcon != null) {
                ivConnectivityIcon.setImageResource(R.drawable.ic_check_circle);
            }
        } else {
            tvOfflineHeader.setText(R.string.status_offline_mode);
            tvOfflineDesc.setText(R.string.status_offline_sub);
            tvOfflineHeader.setTextColor(ContextCompat.getColor(this, R.color.badge_offline_text));
            if (ivConnectivityIcon != null) {
                ivConnectivityIcon.setImageResource(R.drawable.ic_offline);
            }
        }
    }

    private void updateSelectedLocation() {
        String district = PreferenceUtils.getSelectedDistrict(this);
        if (district == null || district.equalsIgnoreCase("All")) {
            tvDashboardLocation.setText(R.string.dashboard_location_label);
        } else {
            tvDashboardLocation.setText(getString(R.string.label_selected_location) + " " + district);
        }
    }

    private void setupModules() {
        moduleList = new ArrayList<>();

        // 1. Flood Warnings
        moduleList.add(new DashboardCardItem(
                1,
                R.string.card_flood_title,
                R.string.card_flood_desc,
                R.drawable.ic_flood,
                R.string.badge_offline_ready,
                true
        ));

        // 2. Nearby Shelters
        moduleList.add(new DashboardCardItem(
                2,
                R.string.card_shelter_title,
                R.string.card_shelter_desc,
                R.drawable.ic_shelter,
                R.string.badge_offline_ready,
                true
        ));

        // 3. Emergency Contacts
        moduleList.add(new DashboardCardItem(
                3,
                R.string.card_emergency_title,
                R.string.card_emergency_desc,
                R.drawable.ic_call_emergency,
                R.string.badge_offline_ready,
                true
        ));

        // 4. Drinking Water Quality
        moduleList.add(new DashboardCardItem(
                4,
                R.string.card_water_title,
                R.string.card_water_desc,
                R.drawable.ic_water,
                R.string.badge_offline_ready,
                true
        ));

        // 5. Heat Safety
        moduleList.add(new DashboardCardItem(
                5,
                R.string.card_heat_title,
                R.string.card_heat_desc,
                R.drawable.ic_heat,
                R.string.badge_offline_ready,
                true
        ));

        // 6. Evacuation Guidelines
        moduleList.add(new DashboardCardItem(
                6,
                R.string.card_evacuation_title,
                R.string.card_evacuation_desc,
                R.drawable.ic_evacuation,
                R.string.badge_offline_ready,
                true
        ));

        // 7. Preparedness Checklist
        moduleList.add(new DashboardCardItem(
                7,
                R.string.card_checklist_title,
                R.string.card_checklist_desc,
                R.drawable.ic_checklist,
                R.string.badge_offline_ready,
                true
        ));

        // 8. Settings & Language
        moduleList.add(new DashboardCardItem(
                8,
                R.string.card_settings_title,
                R.string.card_settings_desc,
                R.drawable.ic_settings,
                R.string.badge_offline_ready,
                true
        ));

        recyclerModules.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DashboardAdapter(this, moduleList, this);
        recyclerModules.setAdapter(adapter);
    }

    private void setupListeners() {
        btnEmergencyHotlines.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmergencyContactsActivity.class);
            startActivity(intent);
        });

        if (btnMainSettings != null) {
            btnMainSettings.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
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
}
