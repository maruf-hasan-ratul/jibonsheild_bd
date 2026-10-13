package com.jibonshield.bd.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.ChipGroup;
import com.jibonshield.bd.R;

/**
 * Screen 10: Evacuation Guidelines.
 * Displays disaster response instructions categorized by disaster type (Flood, Cyclone, Earthquake, Fire)
 * and preparation stage (Before, During, After).
 */
public class EvacuationGuidelinesActivity extends AppCompatActivity {

    private ChipGroup chipGroupDisasters;
    private TextView tvStageBeforeBody;
    private TextView tvStageDuringBody;
    private TextView tvStageAfterBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evacuation_guidelines);

        setupToolbar();
        initViews();
        setupListeners();
        showDisasterGuidance(R.id.chip_disaster_flood);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_evacuation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        chipGroupDisasters = findViewById(R.id.chip_group_disasters);
        tvStageBeforeBody = findViewById(R.id.tv_stage_before_body);
        tvStageDuringBody = findViewById(R.id.tv_stage_during_body);
        tvStageAfterBody = findViewById(R.id.tv_stage_after_body);
    }

    private void setupListeners() {
        chipGroupDisasters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                showDisasterGuidance(checkedId);
            }
        });
    }

    private void showDisasterGuidance(int chipId) {
        if (chipId == R.id.chip_disaster_cyclone) {
            tvStageBeforeBody.setText(R.string.evac_cyclone_before);
            tvStageDuringBody.setText(R.string.evac_cyclone_during);
            tvStageAfterBody.setText(R.string.evac_cyclone_after);
        } else if (chipId == R.id.chip_disaster_earthquake) {
            tvStageBeforeBody.setText(R.string.evac_earthquake_before);
            tvStageDuringBody.setText(R.string.evac_earthquake_during);
            tvStageAfterBody.setText(R.string.evac_earthquake_after);
        } else if (chipId == R.id.chip_disaster_fire) {
            tvStageBeforeBody.setText(R.string.evac_fire_before);
            tvStageDuringBody.setText(R.string.evac_fire_during);
            tvStageAfterBody.setText(R.string.evac_fire_after);
        } else {
            // Default: Flood
            tvStageBeforeBody.setText(R.string.evac_flood_before);
            tvStageDuringBody.setText(R.string.evac_flood_during);
            tvStageAfterBody.setText(R.string.evac_flood_after);
        }
    }
}
