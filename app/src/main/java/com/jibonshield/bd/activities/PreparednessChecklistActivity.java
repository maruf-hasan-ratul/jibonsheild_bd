package com.jibonshield.bd.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.ChecklistAdapter;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 11: Preparedness Checklist.
 * Allows users to track household grab bags, critical documents, and emergency kits offline.
 */
public class PreparednessChecklistActivity extends AppCompatActivity implements ChecklistAdapter.OnChecklistStatusChangeListener {

    private TextView tvProgressLabel;
    private ProgressBar progressBarTracker;
    private MaterialButton btnReset;
    private ProgressBar progressLoading;
    private RecyclerView recyclerView;

    private ChecklistAdapter adapter;
    private EmergencyRepository repository;
    private final List<ChecklistItem> itemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preparedness_checklist);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadItems();
    }

    private void initViews() {
        tvProgressLabel = findViewById(R.id.tv_checklist_progress_label);
        progressBarTracker = findViewById(R.id.progress_checklist_bar);
        btnReset = findViewById(R.id.btn_reset_checklist_items);
        progressLoading = findViewById(R.id.progress_checklist_loading);
        recyclerView = findViewById(R.id.recycler_checklist_items);

        repository = new EmergencyRepository(this);

        btnReset.setOnClickListener(v -> confirmReset());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_checklist);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChecklistAdapter(this, itemList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadItems() {
        progressLoading.setVisibility(View.VISIBLE);
        repository.getChecklistItems(new EmergencyRepository.DataCallback<List<ChecklistItem>>() {
            @Override
            public void onDataLoaded(List<ChecklistItem> data) {
                progressLoading.setVisibility(View.GONE);
                itemList.clear();
                if (data != null) {
                    itemList.addAll(data);
                }
                adapter.notifyDataSetChanged();
                updateProgress();
            }

            @Override
            public void onError(Exception error) {
                progressLoading.setVisibility(View.GONE);
            }
        });
    }

    private void updateProgress() {
        int total = itemList.size();
        int completed = 0;
        for (ChecklistItem item : itemList) {
            if (item.isCompleted()) {
                completed++;
            }
        }
        int percent = total > 0 ? (completed * 100) / total : 0;
        progressBarTracker.setProgress(percent);
        tvProgressLabel.setText(String.format(getString(R.string.checklist_progress_format), completed, total, percent));
    }

    @Override
    public void onChecklistToggled(ChecklistItem item, boolean isChecked) {
        repository.updateChecklistItem(item.getId(), isChecked, null);
        updateProgress();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_reset_checklist)
                .setMessage(R.string.toast_checklist_reset)
                .setPositiveButton(R.string.action_reset, (dialog, which) -> {
                    repository.resetAllChecklistItems(new EmergencyRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            for (ChecklistItem item : itemList) {
                                item.setCompleted(false);
                            }
                            adapter.notifyDataSetChanged();
                            updateProgress();
                            Toast.makeText(PreparednessChecklistActivity.this, R.string.toast_checklist_reset, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception error) {}
                    });
                })
                .setNegativeButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
