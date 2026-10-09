package com.jibonshield.bd.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.DashboardCardItem;

import java.util.List;

/**
 * Adapter for rendering emergency safety modules in the Dashboard RecyclerView.
 */
public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ModuleViewHolder> {

    public interface OnModuleClickListener {
        void onModuleClick(DashboardCardItem item);
    }

    private final Context context;
    private final List<DashboardCardItem> moduleList;
    private final OnModuleClickListener clickListener;

    public DashboardAdapter(Context context, List<DashboardCardItem> moduleList, OnModuleClickListener clickListener) {
        this.context = context;
        this.moduleList = moduleList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dashboard_card, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        DashboardCardItem item = moduleList.get(position);

        holder.ivIcon.setImageResource(item.getIconResId());
        holder.tvTitle.setText(item.getTitleResId());
        holder.tvDesc.setText(item.getDescriptionResId());
        holder.tvBadge.setText(item.getBadgeResId());

        if (item.isImplemented()) {
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
        } else {
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onModuleClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return moduleList != null ? moduleList.size() : 0;
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvDesc;
        final TextView tvBadge;

        ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_module_root);
            ivIcon = itemView.findViewById(R.id.iv_card_icon);
            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvDesc = itemView.findViewById(R.id.tv_card_desc);
            tvBadge = itemView.findViewById(R.id.tv_card_badge);
        }
    }
}
