package com.jibonshield.bd.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.utils.PreferenceUtils;

/**
 * Screen 3: Flood Warning Details.
 * Displays official advisory text, affected regions, validity timeline, and safety directives.
 * Read-only official emergency broadcast presentation.
 */
public class FloodWarningDetailActivity extends BaseActivity {

    private MaterialCardView cardMain;
    private TextView tvSeverity;
    private TextView tvStatus;
    private TextView tvTitle;
    private TextView tvRegion;
    private TextView tvBody;
    private TextView tvIssued;
    private TextView tvExpires;
    private TextView tvSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_warning_detail);

        initViews();
        setupToolbar();
        bindData();
    }

    private void initViews() {
        cardMain = findViewById(R.id.card_flood_detail_main);
        tvSeverity = findViewById(R.id.tv_detail_severity);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvRegion = findViewById(R.id.tv_detail_region);
        tvBody = findViewById(R.id.tv_detail_body);
        tvIssued = findViewById(R.id.tv_detail_meta_issued);
        tvExpires = findViewById(R.id.tv_detail_meta_expires);
        tvSource = findViewById(R.id.tv_detail_meta_source);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_flood_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void bindData() {
        FloodWarning warning = (FloodWarning) getIntent().getSerializableExtra("extra_flood_warning");
        if (warning == null) {
            finish();
            return;
        }

        boolean isBangla = PreferenceUtils.LANG_BN.equals(PreferenceUtils.getLanguage(this));
        String title = isBangla ? warning.getTitleBn() : warning.getTitleEn();
        String body = isBangla ? warning.getDetailsBn() : warning.getDetailsEn();

        tvTitle.setText(title);
        tvRegion.setText(getString(R.string.label_warning_region) + " " + warning.getRegion());
        tvBody.setText(body);
        tvIssued.setText(getString(R.string.label_warning_issued) + " " + warning.getIssuedAt());
        tvExpires.setText(getString(R.string.label_warning_expires) + " " + warning.getExpiresAt());

        String sourceText = getString(R.string.label_warning_source) + " " + warning.getSource();
        if (warning.getSourceReference() != null && !warning.getSourceReference().isEmpty()) {
            sourceText += " • " + warning.getSourceReference();
        }
        tvSource.setText(sourceText);

        // Severity styling
        String level = warning.getWarningLevel() != null ? warning.getWarningLevel().toUpperCase() : "NORMAL";
        switch (level) {
            case "CRITICAL":
                tvSeverity.setText(R.string.severity_critical_label);
                tvSeverity.setBackgroundResource(R.drawable.bg_severity_critical);
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_critical));
                cardMain.setStrokeColor(ContextCompat.getColor(this, R.color.severity_critical));
                break;
            case "WARNING":
            case "HIGH":
                tvSeverity.setText(R.string.severity_warning_label);
                tvSeverity.setBackgroundResource(R.drawable.bg_severity_warning);
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_warning));
                cardMain.setStrokeColor(ContextCompat.getColor(this, R.color.severity_warning));
                break;
            case "ADVISORY":
            case "MODERATE":
                tvSeverity.setText(R.string.severity_advisory_label);
                tvSeverity.setBackgroundResource(R.drawable.bg_severity_warning);
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
                cardMain.setStrokeColor(ContextCompat.getColor(this, R.color.card_stroke));
                break;
            default:
                tvSeverity.setText(R.string.severity_normal_label);
                tvSeverity.setBackgroundResource(R.drawable.bg_severity_safe);
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_safe));
                cardMain.setStrokeColor(ContextCompat.getColor(this, R.color.card_stroke));
                break;
        }

        // Expiry Status Banner & Verification
        TextView tvDisclaimer = findViewById(R.id.tv_detail_disclaimer);
        if (tvDisclaimer != null) {
            tvDisclaimer.setText(getString(R.string.flood_disclaimer) + "\n\n" + getString(R.string.warning_safety_caveat_body));
        }

        if (warning.isExpired()) {
            tvStatus.setText(R.string.warning_status_expired);
            tvStatus.setBackgroundResource(R.drawable.bg_severity_critical);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.severity_critical));

            TextView tvAdviceBody = findViewById(R.id.tv_detail_advice_body);
            if (tvAdviceBody != null) {
                tvAdviceBody.setText(getString(R.string.warning_expired_banner) + "\n\n" + getString(R.string.flood_advice_body));
            }
        } else if (warning.isSample()) {
            tvStatus.setText(R.string.tag_sample_alert);
            tvStatus.setBackgroundResource(R.drawable.bg_severity_sample);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.severity_sample));
        } else {
            tvStatus.setText(R.string.tag_verified_alert);
            tvStatus.setBackgroundResource(R.drawable.bg_severity_safe);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green));
        }
    }
}
