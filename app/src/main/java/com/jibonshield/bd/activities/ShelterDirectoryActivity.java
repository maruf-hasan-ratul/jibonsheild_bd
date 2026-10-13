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
public class ShelterDirectoryActivity extends AppCompatActivity implements ShelterAdapter.OnShelterClickListener {

    private EditText etSearch;
    private Spinner spinnerDistrict;
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

    private String currentQuery = "";
    private String currentDistrict = "All Districts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelter_directory);

        initViews();
        setupToolbar();
        setupSpinner();
        setupSearch();
        setupRecyclerView();
        loadShelters();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search_shelter);
        spinnerDistrict = findViewById(R.id.spinner_shelter_district);
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

    private void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, districts);
        spinnerDistrict.setAdapter(spinnerAdapter);
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentDistrict = districts[position];
                loadShelters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
        repository.getShelters(currentQuery, currentDistrict, new EmergencyRepository.DataCallback<List<Shelter>>() {
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
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_dialer_opened, phone), Toast.LENGTH_SHORT).show();
        }
    }
}
