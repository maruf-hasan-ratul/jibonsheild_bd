package com.jibonshield.bd.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.ShelterAdapter;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 4: Shelter Directory.
 * Provides search query and district filtering interface for designated emergency shelters.
 */
public class ShelterDirectoryActivity extends BaseActivity implements ShelterAdapter.OnShelterClickListener {

    private EditText etSearch;
    private Spinner spinnerDistrict;
    private Spinner spinnerUpazila;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private MaterialButton btnEmptyAction;

    private ShelterAdapter adapter;
    private EmergencyRepository repository;
    private final List<Shelter> shelterList = new ArrayList<>();
    private final String[] districts = new String[]{
            "All Districts", "Cox's Bazar", "Sunamganj", "Sylhet", "Kurigram", "Patuakhali", "Chittagong"
    };
    private final List<String> upazilaList = new ArrayList<>();
    private ArrayAdapter<String> upazilaAdapter;

    private String currentQuery = "";
    private String currentDistrict = "All Districts";
    private String currentUpazila = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelter_directory);

        initViews();
        setupToolbar();
        setupSpinners();
        setupSearch();
        setupRecyclerView();
        loadUpazilas(currentDistrict);
        loadShelters();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search_shelter);
        spinnerDistrict = findViewById(R.id.spinner_shelter_district);
        spinnerUpazila = findViewById(R.id.spinner_shelter_upazila);
        recyclerView = findViewById(R.id.recycler_shelters);
        progressBar = findViewById(R.id.progress_shelters);
        emptyStateLayout = findViewById(R.id.empty_state_shelter);

        if (emptyStateLayout != null) {
            tvEmptyTitle = emptyStateLayout.findViewById(R.id.tv_empty_title);
            tvEmptyDesc = emptyStateLayout.findViewById(R.id.tv_empty_message);
            btnEmptyAction = emptyStateLayout.findViewById(R.id.btn_empty_action);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.shelter_empty_title);
            if (tvEmptyDesc != null) tvEmptyDesc.setText(R.string.shelter_empty_desc);
            if (btnEmptyAction != null) {
                btnEmptyAction.setVisibility(View.VISIBLE);
                btnEmptyAction.setText(R.string.action_clear_filters);
                btnEmptyAction.setOnClickListener(v -> {
                    etSearch.setText("");
                    spinnerDistrict.setSelection(0);
                    if (spinnerUpazila != null && upazilaList.size() > 0) {
                        spinnerUpazila.setSelection(0);
                    }
                });
            }
        }

        repository = new EmergencyRepository(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_shelter_directory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        // 1. District Spinner
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts);
        spinnerDistrict.setAdapter(districtAdapter);
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDistrict = districts[position];
                loadUpazilas(currentDistrict);
                loadShelters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 2. Upazila Spinner
        upazilaList.clear();
        upazilaList.add("All");
        upazilaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, upazilaList);
        spinnerUpazila.setAdapter(upazilaAdapter);
        spinnerUpazila.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                upazilaList.clear();
                upazilaList.add("All");
                if (data != null) {
                    upazilaList.addAll(data);
                }
                upazilaAdapter.notifyDataSetChanged();
                currentUpazila = "All";
                spinnerUpazila.setSelection(0);
            }

            @Override
            public void onError(Exception error) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShelterAdapter(this, shelterList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadShelters() {
        progressBar.setVisibility(View.VISIBLE);
        repository.searchShelters(currentQuery, currentDistrict, currentUpazila, new EmergencyRepository.DataCallback<List<Shelter>>() {
            @Override
            public void onDataLoaded(List<Shelter> data) {
                progressBar.setVisibility(View.GONE);
                shelterList.clear();
                if (data != null) {
                    shelterList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                if (shelterList.isEmpty()) {
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
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("Shelter Phone", phone);
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
        progressBar.setVisibility(View.VISIBLE);
        android.widget.Toast.makeText(this, R.string.sync_in_progress, android.widget.Toast.LENGTH_SHORT).show();

        com.jibonshield.bd.sync.SyncManager.getInstance(this).syncShelters(null, new com.jibonshield.bd.sync.SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String entityName) {}

            @Override
            public void onSyncComplete(com.jibonshield.bd.sync.SyncResult result) {
                loadShelters();
                if (result.isOffline()) {
                    android.widget.Toast.makeText(ShelterDirectoryActivity.this, R.string.sync_offline_toast, android.widget.Toast.LENGTH_LONG).show();
                } else if (result.isSuccess()) {
                    android.widget.Toast.makeText(ShelterDirectoryActivity.this, getString(R.string.sync_complete_toast, result.getMessage()), android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(ShelterDirectoryActivity.this, getString(R.string.sync_error_toast, result.getMessage()), android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
