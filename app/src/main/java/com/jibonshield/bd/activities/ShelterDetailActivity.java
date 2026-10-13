package com.jibonshield.bd.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.utils.PreferenceUtils;

/**
 * Screen 5: Shelter Details.
 * Displays offline coordinates, capacity, verified facilities, and direct focal point dialing.
 */
public class ShelterDetailActivity extends AppCompatActivity {

    private TextView tvDistrict;
    private TextView tvUpazila;
    private TextView tvName;
    private TextView tvCapacity;
    private TextView tvOccupancy;
    private TextView tvCoords;
    private TextView tvPhone;
    private TextView tvVerified;
    private MaterialButton btnCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelter_detail);

        initViews();
        setupToolbar();
        bindData();
    }

    private void initViews() {
        tvDistrict = findViewById(R.id.tv_shelter_detail_district);
        tvUpazila = findViewById(R.id.tv_shelter_detail_upazila);
        tvName = findViewById(R.id.tv_shelter_detail_name);
        tvCapacity = findViewById(R.id.tv_shelter_detail_capacity);
        tvOccupancy = findViewById(R.id.tv_shelter_detail_occupancy);
        tvCoords = findViewById(R.id.tv_shelter_detail_coords);
        tvPhone = findViewById(R.id.tv_shelter_detail_phone);
        tvVerified = findViewById(R.id.tv_shelter_detail_verified);
        btnCall = findViewById(R.id.btn_shelter_detail_call);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_shelter_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void bindData() {
        Shelter shelter = (Shelter) getIntent().getSerializableExtra("extra_shelter");
        if (shelter == null) {
            finish();
            return;
        }

        boolean isBangla = PreferenceUtils.LANG_BN.equals(PreferenceUtils.getLanguage(this));
        String name = isBangla ? shelter.getNameBn() : shelter.getNameEn();

        tvDistrict.setText(shelter.getDistrict());
        tvUpazila.setText(shelter.getUpazila());
        tvName.setText(name);
        tvCapacity.setText(String.format(getString(R.string.shelter_capacity_format), shelter.getCapacity()));
        tvOccupancy.setText(shelter.getCurrentOccupancy() > 0
                ? String.format(getString(R.string.shelter_occupancy_format), shelter.getCurrentOccupancy(), shelter.getCapacity())
                : getString(R.string.badge_offline_ready));

        tvCoords.setText(getString(R.string.label_shelter_coordinates) + " " +
                String.format("%.4f° N, %.4f° E", shelter.getLatitude(), shelter.getLongitude()));

        tvPhone.setText(getString(R.string.label_shelter_contact) + " " + shelter.getContactPhone());
        tvVerified.setText(getString(R.string.label_shelter_verified) + " " + shelter.getVerifiedAt() + " • DDM / CPP");

        btnCall.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + shelter.getContactPhone()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.toast_dialer_opened, shelter.getContactPhone()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
