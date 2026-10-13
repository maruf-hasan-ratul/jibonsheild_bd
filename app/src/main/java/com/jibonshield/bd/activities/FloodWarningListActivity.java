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
 * Displays official flood advisories and simulated offline warning drills with region filtering.
 */
public class FloodWarningListActivity extends AppCompatActivity implements FloodWarningAdapter.OnWarningClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAction;
    private Spinner spinnerRegion;

    private FloodWarningAdapter adapter;
    private EmergencyRepository repository;
    private final List<FloodWarning> warningList = new ArrayList<>();
    private final String[] regions = new String[]{"All", "Sylhet", "Sunamganj", "Kurigram", "Sirajganj", "Bogura"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_warning_list);

        initViews();
        setupToolbar();
        setupSpinner();
        setupRecyclerView();
        loadWarnings("All");
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_flood_warnings);
        progressBar = findViewById(R.id.progress_flood_warnings);
        emptyStateLayout = findViewById(R.id.empty_state_flood);
        spinnerRegion = findViewById(R.id.spinner_flood_region);

        if (emptyStateLayout != null) {
            tvEmptyTitle = emptyStateLayout.findViewById(R.id.tv_empty_title);
            tvEmptyDesc = emptyStateLayout.findViewById(R.id.tv_empty_message);
            btnEmptyAction = emptyStateLayout.findViewById(R.id.btn_empty_action);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.flood_empty_title);
            if (tvEmptyDesc != null) tvEmptyDesc.setText(R.string.flood_empty_desc);
            if (btnEmptyAction != null) {
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setText(R.string.action_clear_filters);
                btnEmptyAction.setOnClickListener(v -> spinnerRegion.setSelection(0));
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

    private void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, regions);
        spinnerRegion.setAdapter(spinnerAdapter);
        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadWarnings(regions[position]);
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

    private void loadWarnings(String region) {
        progressBar.setVisibility(View.VISIBLE);
        repository.getFloodWarnings(region, new EmergencyRepository.DataCallback<List<FloodWarning>>() {
            @Override
            public void onDataLoaded(List<FloodWarning> data) {
                progressBar.setVisibility(View.GONE);
                warningList.clear();
                if (data != null) {
                    warningList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                if (warningList.isEmpty()) {
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

    @Override
    public void onWarningClick(FloodWarning warning) {
        Intent intent = new Intent(this, FloodWarningDetailActivity.class);
        intent.putExtra("extra_flood_warning", warning);
        startActivity(intent);
    }
}
