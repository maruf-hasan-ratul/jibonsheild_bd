package com.jibonshield.bd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.WaterObservationAdapter;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 8: Water Observation History.
 * Shows historical field observations with parameter indicators, safety verdicts, and empty state.
 */
public class WaterObservationHistoryActivity extends AppCompatActivity implements WaterObservationAdapter.OnObservationClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAction;
    private ExtendedFloatingActionButton fabAdd;

    private WaterObservationAdapter adapter;
    private EmergencyRepository repository;
    private final List<WaterObservation> observationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_observation_history);

        initViews();
        setupToolbar();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadObservations();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_water_observations);
        progressBar = findViewById(R.id.progress_obs_history);
        emptyStateLayout = findViewById(R.id.empty_state_water_obs);
        fabAdd = findViewById(R.id.fab_add_observation);

        if (emptyStateLayout != null) {
            tvEmptyTitle = emptyStateLayout.findViewById(R.id.tv_empty_title);
            tvEmptyDesc = emptyStateLayout.findViewById(R.id.tv_empty_message);
            btnEmptyAction = emptyStateLayout.findViewById(R.id.btn_empty_action);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.obs_empty_title);
            if (tvEmptyDesc != null) tvEmptyDesc.setText(R.string.obs_empty_desc);
            if (btnEmptyAction != null) {
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setText(R.string.action_add_observation);
                btnEmptyAction.setOnClickListener(v -> openFormActivity());
            }
        }

        fabAdd.setOnClickListener(v -> openFormActivity());
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

    private void loadObservations() {
        progressBar.setVisibility(View.VISIBLE);
        repository.getWaterObservations(new EmergencyRepository.DataCallback<List<WaterObservation>>() {
            @Override
            public void onDataLoaded(List<WaterObservation> data) {
                progressBar.setVisibility(View.GONE);
                observationList.clear();
                if (data != null) {
                    observationList.addAll(data);
                }
                adapter.notifyDataSetChanged();

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
                progressBar.setVisibility(View.GONE);
                if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void openFormActivity() {
        Intent intent = new Intent(this, WaterObservationFormActivity.class);
        startActivity(intent);
    }

    @Override
    public void onObservationClick(WaterObservation observation) {
        // Individual item click handling
    }
}
