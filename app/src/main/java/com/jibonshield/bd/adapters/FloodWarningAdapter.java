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
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.List;

/**
 * Adapter for displaying flood warnings with severity labels and data verification badges.
 */
public class FloodWarningAdapter extends RecyclerView.Adapter<FloodWarningAdapter.WarningViewHolder> {

    public interface OnWarningClickListener {
        void onWarningClick(FloodWarning warning);
    }

    private final Context context;
    private final List<FloodWarning> warningList;
    private final OnWarningClickListener clickListener;
    private final boolean isBangla;

    public FloodWarningAdapter(Context context, List<FloodWarning> warningList, OnWarningClickListener listener) {
        this.context = context;
        this.warningList = warningList;
        this.clickListener = listener;
        this.isBangla = PreferenceUtils.LANG_BN.equals(PreferenceUtils.getLanguage(context));
    }

    @NonNull
    @Override
    public WarningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flood_warning, parent, false);
        return new WarningViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WarningViewHolder holder, int position) {
        FloodWarning item = warningList.get(position);

        String title = isBangla ? item.getTitleBn() : item.getTitleEn();
        String details = isBangla ? item.getDetailsBn() : item.getDetailsEn();

        holder.tvTitle.setText(title);
        holder.tvRegion.setText(context.getString(R.string.label_warning_region) + " " + item.getRegion());
        holder.tvSnippet.setText(details);
        holder.tvIssued.setText(item.getIssuedAt() + " • " + item.getSource());

        // Severity styling
        switch (item.getWarningLevel().toUpperCase()) {
            case "CRITICAL":
                holder.tvSeverity.setText(R.string.severity_critical_label);
                holder.tvSeverity.setBackgroundResource(R.drawable.bg_severity_critical);
                holder.tvSeverity.setTextColor(ContextCompat.getColor(context, R.color.severity_critical));
                holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.severity_critical));
                break;
            case "WARNING":
                holder.tvSeverity.setText(R.string.severity_warning_label);
                holder.tvSeverity.setBackgroundResource(R.drawable.bg_severity_warning);
                holder.tvSeverity.setTextColor(ContextCompat.getColor(context, R.color.severity_warning));
                holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.severity_warning));
                break;
            case "ADVISORY":
                holder.tvSeverity.setText(R.string.severity_advisory_label);
                holder.tvSeverity.setBackgroundResource(R.drawable.bg_severity_warning);
                holder.tvSeverity.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
                break;
            default:
                holder.tvSeverity.setText(R.string.severity_normal_label);
                holder.tvSeverity.setBackgroundResource(R.drawable.bg_severity_safe);
                holder.tvSeverity.setTextColor(ContextCompat.getColor(context, R.color.severity_safe));
                holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
                break;
        }

        // Data status: Verified vs Sample
        if (item.isSample()) {
            holder.tvStatus.setText(R.string.tag_sample_alert);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_severity_sample);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.severity_sample));
        } else {
            holder.tvStatus.setText(R.string.tag_verified_alert);
            holder.tvStatus.setBackgroundResource(R.drawable.bg_severity_safe);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success_green));
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onWarningClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return warningList != null ? warningList.size() : 0;
    }

    static class WarningViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView tvSeverity;
        final TextView tvStatus;
        final TextView tvTitle;
        final TextView tvRegion;
        final TextView tvSnippet;
        final TextView tvIssued;

        WarningViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_flood_root);
            tvSeverity = itemView.findViewById(R.id.tv_warning_severity_badge);
            tvStatus = itemView.findViewById(R.id.tv_warning_status_badge);
            tvTitle = itemView.findViewById(R.id.tv_warning_title);
            tvRegion = itemView.findViewById(R.id.tv_warning_region);
            tvSnippet = itemView.findViewById(R.id.tv_warning_details_snippet);
            tvIssued = itemView.findViewById(R.id.tv_warning_issued_at);
        }
    }
}
