package com.jibonshield.bd.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.DashboardAdapter;
import com.jibonshield.bd.models.DashboardCardItem;
import com.jibonshield.bd.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Dashboard Activity for JibonShield BD.
 * Provides offline-first access to disaster preparedness and environmental safety modules.
 */
public class MainActivity extends AppCompatActivity implements DashboardAdapter.OnModuleClickListener {

    private RecyclerView recyclerModules;
    private DashboardAdapter adapter;
    private List<DashboardCardItem> moduleList;
    private View bannerOffline;
    private TextView tvOfflineHeader;
    private TextView tvOfflineDesc;
    private MaterialButton btnEmergencyHotlines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupToolbar();
        setupOfflineState();
        setupModules();
        setupListeners();
    }

    private void initViews() {
        recyclerModules = findViewById(R.id.recycler_dashboard_modules);
        bannerOffline = findViewById(R.id.banner_offline_status);
        tvOfflineHeader = findViewById(R.id.tv_offline_header);
        tvOfflineDesc = findViewById(R.id.tv_offline_desc);
        btnEmergencyHotlines = findViewById(R.id.btn_emergency_hotlines);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupOfflineState() {
        boolean isConnected = NetworkUtils.isInternetAvailable(this);
        if (isConnected) {
            tvOfflineHeader.setText(R.string.status_offline_mode);
            tvOfflineDesc.setText(R.string.status_offline_sub);
        } else {
            tvOfflineHeader.setText(R.string.status_offline_mode);
            tvOfflineDesc.setText(R.string.status_offline_sub);
        }
    }

    private void setupModules() {
        moduleList = new ArrayList<>();

        // 1. Flood Warnings (Preview)
        moduleList.add(new DashboardCardItem(
                1,
                R.string.card_flood_title,
                R.string.card_flood_desc,
                R.drawable.ic_flood,
                R.string.badge_preview,
                false
        ));

        // 2. Nearby Shelters
        moduleList.add(new DashboardCardItem(
                2,
                R.string.card_shelter_title,
                R.string.card_shelter_desc,
                R.drawable.ic_shelter,
                R.string.badge_pending,
                false
        ));

        // 3. Emergency Contacts (Database Seeded)
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
                R.string.badge_pending,
                false
        ));

        // 5. Heat Safety
        moduleList.add(new DashboardCardItem(
                5,
                R.string.card_heat_title,
                R.string.card_heat_desc,
                R.drawable.ic_heat,
                R.string.badge_pending,
                false
        ));

        // 6. Evacuation Guidelines
        moduleList.add(new DashboardCardItem(
                6,
                R.string.card_evacuation_title,
                R.string.card_evacuation_desc,
                R.drawable.ic_evacuation,
                R.string.badge_pending,
                false
        ));

        // 7. Preparedness Checklist
        moduleList.add(new DashboardCardItem(
                7,
                R.string.card_checklist_title,
                R.string.card_checklist_desc,
                R.drawable.ic_checklist,
                R.string.badge_pending,
                false
        ));

        // 8. Settings & Language
        moduleList.add(new DashboardCardItem(
                8,
                R.string.card_settings_title,
                R.string.card_settings_desc,
                R.drawable.ic_settings,
                R.string.badge_pending,
                false
        ));

        recyclerModules.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DashboardAdapter(this, moduleList, this);
        recyclerModules.setAdapter(adapter);
    }

    private void setupListeners() {
        btnEmergencyHotlines.setOnClickListener(v -> showEmergencyHotlinesDialog());
    }

    private void showEmergencyHotlinesDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_emergency_hotlines_title)
                .setMessage(R.string.dialog_emergency_hotlines_body)
                .setPositiveButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_call_emergency)
                .show();
    }

    @Override
    public void onModuleClick(DashboardCardItem item) {
        if (item.getId() == 3) {
            // Emergency Contacts module is seeded in offline database
            showEmergencyHotlinesDialog();
        } else {
            // Explicitly notify user regarding the multi-day roadmap
            new AlertDialog.Builder(this)
                    .setTitle(getString(item.getTitleResId()))
                    .setMessage(R.string.dialog_module_pending_message)
                    .setPositiveButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                    .setIcon(item.getIconResId())
                    .show();
        }
    }
}
