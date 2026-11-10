package com.jibonshield.bd.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.jibonshield.bd.R;

/**
 * Screen 9: Heat Safety Activity.
 * Complete offline educational guide on extreme heatwave preparation,
 * hydration, outdoor worker guidance, heat illness differentiation, and emergency hotlines.
 */
public class HeatSafetyActivity extends BaseActivity {

    private MaterialButton btnCall16263;
    private MaterialButton btnCall999;
    private MaterialButton btnCall1090;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heat_safety);

        setupToolbar();
        initViews();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_heat_safety);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        btnCall16263 = findViewById(R.id.btn_heat_call_16263);
        btnCall999 = findViewById(R.id.btn_heat_call_999);
        btnCall1090 = findViewById(R.id.btn_heat_call_1090);

        btnCall16263.setOnClickListener(v -> dialHotline("16263"));
        btnCall999.setOnClickListener(v -> dialHotline("999"));
        btnCall1090.setOnClickListener(v -> dialHotline("1090"));
    }

    private void dialHotline(String phone) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.fromParts("tel", phone.trim(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open phone dialer for " + phone, Toast.LENGTH_SHORT).show();
        }
    }
}
