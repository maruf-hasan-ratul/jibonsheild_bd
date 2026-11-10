package com.jibonshield.bd.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.ShelterAdapter;
import com.jibonshield.bd.databinding.ActivityShelterDirectoryBinding;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.sync.SyncManager;
import com.jibonshield.bd.sync.SyncResult;
import com.jibonshield.bd.utils.DistrictData;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 4: Shelter Directory.
 * Provides search query and district/upazila filtering interface for designated emergency shelters.
 * Demonstrates View Binding, implicit Intent ACTION_DIAL launching, and dynamic searching.
 */
public class ShelterDirectoryActivity extends BaseActivity implements ShelterAdapter.OnShelterClickListener {

    private ActivityShelterDirectoryBinding binding;
    private ShelterAdapter adapter;
    private EmergencyRepository repository;
    private final List<Shelter> shelterList = new ArrayList<>();
    private final String[] districts = DistrictData.getDistrictsWithAllDistricts();
    private final List<String> upazilaList = new ArrayList<>();
    private ArrayAdapter<String> upazilaAdapter;

    private String currentQuery = "";
    private String currentDistrict = "All Districts";
    private String currentUpazila = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShelterDirectoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EmergencyRepository(this);

        setupToolbar();
        setupSpinners();
        setupSearch();
        setupRecyclerView();
        setupEmptyStateActions();
        loadUpazilas(currentDistrict);
        loadShelters();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarShelterDirectory;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupEmptyStateActions() {
        View emptyLayout = binding.emptyStateShelter.getRoot();
        View btnAction = emptyLayout.findViewById(R.id.btn_empty_action);
        if (btnAction != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> {
                binding.etSearchShelter.setText("");
                binding.spinnerShelterDistrict.setSelection(0);
                if (upazilaList.size() > 0) {
                    binding.spinnerShelterUpazila.setSelection(0);
                }
            });
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts);
        binding.spinnerShelterDistrict.setAdapter(districtAdapter);

        String hometownDistrict = com.jibonshield.bd.utils.PreferenceUtils.getHometownDistrict(this);
        if (com.jibonshield.bd.utils.PreferenceUtils.isHometownConfigured(this) && !hometownDistrict.isEmpty()) {
            for (int i = 0; i < districts.length; i++) {
                if (districts[i].equalsIgnoreCase(hometownDistrict)) {
                    currentDistrict = districts[i];
                    binding.spinnerShelterDistrict.setSelection(i);
                    break;
                }
            }
        }

        binding.spinnerShelterDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDistrict = districts[position];
                loadUpazilas(currentDistrict);
                loadShelters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        upazilaList.clear();
        upazilaList.add("All");
        upazilaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, upazilaList);
        binding.spinnerShelterUpazila.setAdapter(upazilaAdapter);
        binding.spinnerShelterUpazila.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < upazilaList.size()) {
                    currentUpazila = upazilaList.get(position);
                    loadShelters();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUpazilas(String district) {
        repository.getUpazilasForDistrict(district, new EmergencyRepository.DataCallback<List<String>>() {
            @Override
            public void onDataLoaded(List<String> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                upazilaList.clear();
                upazilaList.add("All");
                if (data != null) {
                    upazilaList.addAll(data);
                }
                upazilaAdapter.notifyDataSetChanged();

                String hometownUpazila = com.jibonshield.bd.utils.PreferenceUtils.getHometownUpazila(ShelterDirectoryActivity.this);
                int selectedPos = 0;
                if (com.jibonshield.bd.utils.PreferenceUtils.isHometownConfigured(ShelterDirectoryActivity.this) && !hometownUpazila.isEmpty()) {
                    for (int i = 0; i < upazilaList.size(); i++) {
                        if (upazilaList.get(i).equalsIgnoreCase(hometownUpazila)) {
                            selectedPos = i;
                            currentUpazila = upazilaList.get(i);
                            break;
                        }
                    }
                }
                if (selectedPos == 0) {
                    currentUpazila = "All";
                }
                binding.spinnerShelterUpazila.setSelection(selectedPos);
            }

            @Override
            public void onError(Exception error) {}
        });
    }

    private void setupSearch() {
        binding.etSearchShelter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString() : "";
                loadShelters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        binding.recyclerShelters.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShelterAdapter(this, shelterList, this);
        binding.recyclerShelters.setAdapter(adapter);
    }

    private void loadShelters() {
        binding.progressShelters.setVisibility(View.VISIBLE);
        repository.searchShelters(currentQuery, currentDistrict, currentUpazila, new EmergencyRepository.DataCallback<List<Shelter>>() {
            @Override
            public void onDataLoaded(List<Shelter> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressShelters.setVisibility(View.GONE);
                shelterList.clear();
                if (data != null) {
                    shelterList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                if (shelterList.isEmpty()) {
                    binding.emptyStateShelter.getRoot().setVisibility(View.VISIBLE);
                    binding.recyclerShelters.setVisibility(View.GONE);
                } else {
                    binding.emptyStateShelter.getRoot().setVisibility(View.GONE);
                    binding.recyclerShelters.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressShelters.setVisibility(View.GONE);
                binding.emptyStateShelter.getRoot().setVisibility(View.VISIBLE);
                binding.recyclerShelters.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onShelterClick(Shelter shelter) {
        Intent intent = new Intent(this, ShelterDetailActivity.class);
        intent.putExtra("extra_shelter", shelter);
        startActivity(intent);
    }

    @Override
    public void onShelterCallClick(Shelter shelter) {
        dialPhoneNumber(shelter.getContactPhone());
    }

    private void dialPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty() || phone.trim().length() < 3) {
            Toast.makeText(this, "No valid contact telephone available for this shelter", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone.trim()));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                copyPhoneToClipboard(phone);
            }
        } catch (Exception e) {
            copyPhoneToClipboard(phone);
        }
    }

    private void copyPhoneToClipboard(String phone) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Shelter Phone", phone);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, getString(R.string.toast_phone_copied, phone), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
            Toast.makeText(this, getString(R.string.toast_dialer_opened, phone), Toast.LENGTH_SHORT).show();
        }
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
        binding.progressShelters.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show();

        SyncManager.getInstance(this).syncShelters(null, new SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String entityName) {}

            @Override
            public void onSyncComplete(SyncResult result) {
                loadShelters();
                if (result.isOffline()) {
                    Toast.makeText(ShelterDirectoryActivity.this, R.string.sync_offline_toast, Toast.LENGTH_LONG).show();
                } else if (result.isSuccess()) {
                    Toast.makeText(ShelterDirectoryActivity.this, getString(R.string.sync_complete_toast, result.getMessage()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ShelterDirectoryActivity.this, getString(R.string.sync_error_toast, result.getMessage()), Toast.LENGTH_LONG).show();
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
