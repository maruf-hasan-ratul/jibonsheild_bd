package com.jibonshield.bd.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.WaterObservation;

import java.util.List;

/**
 * Adapter for rendering saved drinking water observation logs.
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_water_observation, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        WaterObservation item = observationList.get(position);

        holder.tvSourceType.setText(item.getSourceType());
        holder.tvLocation.setText(item.getLocationName());
        holder.tvDistrict.setText(context.getString(R.string.label_shelter_district) + " " + item.getDistrict());

        String safety = item.getSafetyStatus();
        holder.tvSafetyStatus.setText(safety);
        if (safety.toLowerCase().contains("safe") || safety.toLowerCase().contains("নিরাপদ")) {
            holder.tvSafetyStatus.setBackgroundResource(R.drawable.bg_severity_safe);
            holder.tvSafetyStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green));
        } else if (safety.toLowerCase().contains("boiling") || safety.toLowerCase().contains("ফোটানো")) {
            holder.tvSafetyStatus.setBackgroundResource(R.drawable.bg_severity_warning);
            holder.tvSafetyStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
        } else {
            holder.tvSafetyStatus.setBackgroundResource(R.drawable.bg_severity_critical);
            holder.tvSafetyStatus.setTextColor(ContextCompat.getColor(context, R.color.emergency_red));
        }

        String phDisplay = item.isPhMeasured() ? String.format(java.util.Locale.US, "%.1f", item.getPhLevel()) : "N/A";
        String measurements = "pH: " + phDisplay
                + " • Turbidity: " + item.getTurbidityStatus()
                + " • Appearance: " + item.getAppearance()
                + " • Odor: " + item.getOdor();
        holder.tvMeasurements.setText(measurements);

        if (item.getRemarks() != null && !item.getRemarks().trim().isEmpty()) {
            holder.tvRemarks.setVisibility(View.VISIBLE);
            holder.tvRemarks.setText(item.getRemarks());
        } else {
            holder.tvRemarks.setVisibility(View.GONE);
        }

        holder.tvFooter.setText(item.getObservationTime() + " • " + item.getReportedBy());

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onObservationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return observationList != null ? observationList.size() : 0;
    }

    static class ObservationViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView tvSourceType;
        final TextView tvSafetyStatus;
        final TextView tvLocation;
        final TextView tvDistrict;
        final TextView tvMeasurements;
        final TextView tvRemarks;
        final TextView tvFooter;

        ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_obs_root);
            tvSourceType = itemView.findViewById(R.id.tv_obs_source_type);
            tvSafetyStatus = itemView.findViewById(R.id.tv_obs_safety_status);
            tvLocation = itemView.findViewById(R.id.tv_obs_location);
            tvDistrict = itemView.findViewById(R.id.tv_obs_district);
            tvMeasurements = itemView.findViewById(R.id.tv_obs_measurements);
            tvRemarks = itemView.findViewById(R.id.tv_obs_remarks);
            tvFooter = itemView.findViewById(R.id.tv_obs_footer);
        }
    }
}
