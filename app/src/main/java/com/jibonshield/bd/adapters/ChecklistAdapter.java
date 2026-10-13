package com.jibonshield.bd.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jibonshield.bd.R;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.List;

/**
 * Adapter for rendering individual preparedness checklist items with accessible CheckBoxes.
 */
public class ChecklistAdapter extends RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder> {

    public interface OnChecklistStatusChangeListener {
        void onChecklistToggled(ChecklistItem item, boolean isChecked);
    }

    private final Context context;
    private final List<ChecklistItem> items;
    private final OnChecklistStatusChangeListener listener;
    private final boolean isBangla;

    public ChecklistAdapter(Context context, List<ChecklistItem> items, OnChecklistStatusChangeListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.isBangla = PreferenceUtils.LANG_BN.equals(PreferenceUtils.getLanguage(context));
    }

    @NonNull
    @Override
    public ChecklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checklist_item, parent, false);
        return new ChecklistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChecklistViewHolder holder, int position) {
        ChecklistItem item = items.get(position);

        String title = isBangla ? item.getItemBn() : item.getItemEn();
        holder.tvTitle.setText(title);

        holder.cbItem.setOnCheckedChangeListener(null);
        holder.cbItem.setChecked(item.isCompleted());

        updateStrikeThrough(holder.tvTitle, item.isCompleted());

        if (item.isEssential()) {
            holder.tvEssential.setVisibility(View.VISIBLE);
        } else {
            holder.tvEssential.setVisibility(View.GONE);
        }

        holder.cbItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setCompleted(isChecked);
            updateStrikeThrough(holder.tvTitle, isChecked);
            if (listener != null) {
                listener.onChecklistToggled(item, isChecked);
            }
        });

        holder.rootView.setOnClickListener(v -> {
            boolean nextState = !holder.cbItem.isChecked();
            holder.cbItem.setChecked(nextState);
        });
    }

    private void updateStrikeThrough(TextView tv, boolean isChecked) {
        if (isChecked) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ChecklistViewHolder extends RecyclerView.ViewHolder {
        final View rootView;
        final CheckBox cbItem;
        final TextView tvTitle;
        final TextView tvEssential;

        ChecklistViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.layout_checklist_item_root);
            cbItem = itemView.findViewById(R.id.cb_checklist_item);
            tvTitle = itemView.findViewById(R.id.tv_checklist_title);
            tvEssential = itemView.findViewById(R.id.tv_checklist_essential);
        }
    }
}
