package com.jibonshield.bd.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.ChecklistAdapter;
import com.jibonshield.bd.databinding.ActivityPreparednessChecklistBinding;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 11: Preparedness Checklist.
 * Allows users to track household grab bags, critical documents, and emergency kits offline.
 * Demonstrates View Binding, SQLite toggle updates, and completion percentage calculation.
 */
public class PreparednessChecklistActivity extends BaseActivity implements ChecklistAdapter.OnChecklistStatusChangeListener {

    private ActivityPreparednessChecklistBinding binding;
    private ChecklistAdapter adapter;
    private EmergencyRepository repository;
    private final List<ChecklistItem> itemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreparednessChecklistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EmergencyRepository(this);

        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadItems();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarChecklist;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        binding.btnResetChecklistItems.setOnClickListener(v -> confirmReset());
    }

    private void setupRecyclerView() {
        binding.recyclerChecklistItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChecklistAdapter(this, itemList, this);
        binding.recyclerChecklistItems.setAdapter(adapter);
    }

    private void loadItems() {
        binding.progressChecklistLoading.setVisibility(View.VISIBLE);
        repository.getChecklistItems(new EmergencyRepository.DataCallback<List<ChecklistItem>>() {
            @Override
            public void onDataLoaded(List<ChecklistItem> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressChecklistLoading.setVisibility(View.GONE);
                itemList.clear();
                if (data != null) {
                    itemList.addAll(data);
                }
                adapter.notifyDataSetChanged();
                updateProgress();
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressChecklistLoading.setVisibility(View.GONE);
            }
        });
    }

    private void updateProgress() {
        if (binding == null) return;
        int total = itemList.size();
        int completed = 0;
        for (ChecklistItem item : itemList) {
            if (item.isCompleted()) {
                completed++;
            }
        }
        int percent = total > 0 ? (completed * 100) / total : 0;
        binding.progressChecklistBar.setProgress(percent);
        binding.tvChecklistProgressLabel.setText(String.format(getString(R.string.checklist_progress_format), completed, total, percent));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
