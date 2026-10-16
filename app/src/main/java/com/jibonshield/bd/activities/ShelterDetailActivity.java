package com.jibonshield.bd.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.utils.PreferenceUtils;

/**
 * Screen 5: Shelter Details.
 * Displays offline coordinates, capacity, verified facilities, accessibility notes,
 * provenance authority, and direct focal point dialing.
 */
public class ShelterDetailActivity extends AppCompatActivity {

    private TextView tvDistrict;
    private TextView tvUpazila;
    private TextView tvStatusTag;
    private TextView tvName;
    private TextView tvAddress;
    private TextView tvCapacity;
    private TextView tvOccupancy;
    private TextView tvCoords;
    private TextView tvPhone;
    private TextView tvDirections;
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
        tvStatusTag = findViewById(R.id.tv_shelter_detail_status_tag);
        tvName = findViewById(R.id.tv_shelter_detail_name);
        tvAddress = findViewById(R.id.tv_shelter_detail_address);
        tvCapacity = findViewById(R.id.tv_shelter_detail_capacity);
        tvOccupancy = findViewById(R.id.tv_shelter_detail_occupancy);
        tvCoords = findViewById(R.id.tv_shelter_detail_coords);
        tvPhone = findViewById(R.id.tv_shelter_detail_phone);
        tvDirections = findViewById(R.id.tv_shelter_detail_directions);
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

        // Address
        String address = shelter.getAddress();
        if (address != null && !address.trim().isEmpty()) {
            tvAddress.setText(getString(R.string.label_shelter_address) + " " + address);
            tvAddress.setVisibility(View.VISIBLE);
        } else {
            tvAddress.setText(getString(R.string.label_shelter_address) + " " + shelter.getUpazila() + ", " + shelter.getDistrict());
        }

        // Capacity - Never invent fabricated numbers
        if (shelter.getCapacity() > 0) {
            tvCapacity.setText(String.format(getString(R.string.shelter_capacity_format), shelter.getCapacity()));
        } else {
            tvCapacity.setText(R.string.shelter_capacity_unspecified);
        }

        // Occupancy - Clearly state not tracked in real-time
        tvOccupancy.setText(R.string.shelter_occupancy_unverified);

        // Coordinates - Graceful fallback if missing or 0.0
        if (shelter.getLatitude() > 0.1 && shelter.getLongitude() > 0.1) {
            tvCoords.setText(getString(R.string.label_shelter_coordinates) + " " +
                    String.format(java.util.Locale.US, "%.4f° N, %.4f° E", shelter.getLatitude(), shelter.getLongitude()));
        } else {
            tvCoords.setText(R.string.shelter_coords_missing);
        }

        // Directions & Access Notes
        String directions = shelter.getAccessibilityNotes();
        if (directions != null && !directions.trim().isEmpty()) {
            tvDirections.setText(directions);
        } else {
            tvDirections.setText(shelter.getAddress() != null ? shelter.getAddress() : (shelter.getUpazila() + ", " + shelter.getDistrict()));
        }

        // Phone & Safe Dialer
        final String phone = shelter.getContactPhone();
        boolean hasValidPhone = phone != null && !phone.trim().isEmpty() && !phone.equalsIgnoreCase("N/A") && phone.trim().length() >= 3;

        if (hasValidPhone) {
            tvPhone.setText(getString(R.string.label_shelter_contact) + " " + phone);
            btnCall.setEnabled(true);
            btnCall.setAlpha(1.0f);
            btnCall.setOnClickListener(v -> dialPhoneNumber(phone));
        } else {
            tvPhone.setText(R.string.shelter_phone_missing);
            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);
        }

        // Verification & Provenance
        String verifiedText = getString(R.string.label_shelter_verified) + " " + shelter.getVerifiedAt()
                + " • Authority: " + shelter.getSource();
        tvVerified.setText(verifiedText);
    }

    private void dialPhoneNumber(String phone) {
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
}
