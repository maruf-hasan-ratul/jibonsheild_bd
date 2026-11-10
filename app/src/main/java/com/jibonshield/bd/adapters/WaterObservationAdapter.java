package com.jibonshield.bd.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jibonshield.bd.R;
import com.jibonshield.bd.databinding.ItemWaterObservationBinding;
import com.jibonshield.bd.models.WaterObservation;

import java.util.List;

/**
 * Adapter for rendering saved drinking water observation logs.
 * Demonstrates View Binding, evidence photo thumbnails, and sync state indicators.
 */
public class WaterObservationAdapter extends RecyclerView.Adapter<WaterObservationAdapter.ObservationViewHolder> {

    public interface OnObservationClickListener {
        void onObservationClick(WaterObservation observation);
    }

    private final Context context;
    private final List<WaterObservation> observationList;
    private final OnObservationClickListener listener;

    public WaterObservationAdapter(Context context, List<WaterObservation> observationList, OnObservationClickListener listener) {
        this.context = context;
        this.observationList = observationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWaterObservationBinding binding = ItemWaterObservationBinding.inflate(
                LayoutInflater.from(context), parent, false
        );
        return new ObservationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        WaterObservation item = observationList.get(position);
        ItemWaterObservationBinding b = holder.binding;

        b.tvObsSourceType.setText(item.getSourceType());
        b.tvObsLocation.setText(item.getLocationName());
        b.tvObsDistrict.setText(context.getString(R.string.label_shelter_district) + " " + item.getDistrict());

        // Safety Status styling
        String safety = item.getSafetyStatus();
        b.tvObsSafetyStatus.setText(safety);
        if (safety.toLowerCase().contains("safe") || safety.toLowerCase().contains("নিরাপদ")) {
            b.tvObsSafetyStatus.setBackgroundResource(R.drawable.bg_severity_safe);
            b.tvObsSafetyStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green));
        } else if (safety.toLowerCase().contains("boiling") || safety.toLowerCase().contains("ফোটানো")) {
            b.tvObsSafetyStatus.setBackgroundResource(R.drawable.bg_severity_warning);
            b.tvObsSafetyStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
        } else {
            b.tvObsSafetyStatus.setBackgroundResource(R.drawable.bg_severity_critical);
            b.tvObsSafetyStatus.setTextColor(ContextCompat.getColor(context, R.color.emergency_red));
        }

        // Sync status badge
        if (WaterObservation.SYNC_SYNCED.equalsIgnoreCase(item.getSyncStatus())) {
            b.tvObsSyncBadge.setText(R.string.photo_uploaded);
            b.tvObsSyncBadge.setTextColor(ContextCompat.getColor(context, R.color.success_green));
        } else {
            b.tvObsSyncBadge.setText(R.string.photo_pending_upload);
            b.tvObsSyncBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
        }

        // Photo thumbnail
        if (item.hasImage()) {
            b.ivObsThumbnail.setVisibility(View.VISIBLE);
            try {
                if (item.getImageUri() != null) {
                    b.ivObsThumbnail.setImageURI(Uri.parse(item.getImageUri()));
                }
            } catch (Exception e) {
                b.ivObsThumbnail.setVisibility(View.GONE);
            }
        } else {
            b.ivObsThumbnail.setVisibility(View.GONE);
        }

        String phDisplay = item.isPhMeasured() ? String.format(java.util.Locale.US, "%.1f", item.getPhLevel()) : "N/A";
        String measurements = "pH: " + phDisplay
                + " • Turbidity: " + item.getTurbidityStatus()
                + " • Appearance: " + item.getAppearance()
                + " • Odor: " + item.getOdor();
        b.tvObsMeasurements.setText(measurements);

        if (item.getRemarks() != null && !item.getRemarks().trim().isEmpty()) {
            b.tvObsRemarks.setVisibility(View.VISIBLE);
            b.tvObsRemarks.setText(item.getRemarks());
        } else {
            b.tvObsRemarks.setVisibility(View.GONE);
        }

        b.tvObsFooter.setText(item.getObservationTime() + " • " + item.getReportedBy());

        b.cardObsRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onObservationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return observationList != null ? observationList.size() : 0;
    }

    public static class ObservationViewHolder extends RecyclerView.ViewHolder {
        public final ItemWaterObservationBinding binding;

        public ObservationViewHolder(@NonNull ItemWaterObservationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
