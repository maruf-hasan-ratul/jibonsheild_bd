package com.jibonshield.bd.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.jibonshield.bd.R;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.List;

/**
 * Adapter for rendering designated shelters with capacity, district/upazila, and direct call trigger.
 */
public class ShelterAdapter extends RecyclerView.Adapter<ShelterAdapter.ShelterViewHolder> {

    public interface OnShelterClickListener {
        void onShelterClick(Shelter shelter);
        void onShelterCallClick(Shelter shelter);
    }

    private final Context context;
    private final List<Shelter> shelterList;
    private final OnShelterClickListener clickListener;
    private final boolean isBangla;

    public ShelterAdapter(Context context, List<Shelter> shelterList, OnShelterClickListener listener) {
        this.context = context;
        this.shelterList = shelterList;
        this.clickListener = listener;
        this.isBangla = PreferenceUtils.LANG_BN.equals(PreferenceUtils.getLanguage(context));
    }

    @NonNull
    @Override
    public ShelterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shelter, parent, false);
        return new ShelterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShelterViewHolder holder, int position) {
        Shelter item = shelterList.get(position);

        String name;
        if (isBangla) {
            name = (item.getNameBn() != null && !item.getNameBn().trim().isEmpty() && !item.getNameBn().equalsIgnoreCase("Shelter"))
                    ? item.getNameBn()
                    : item.getNameEn();
        } else {
            name = (item.getNameEn() != null && !item.getNameEn().trim().isEmpty() && !item.getNameEn().equalsIgnoreCase("Shelter"))
                    ? item.getNameEn()
                    : item.getNameBn();
        }
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Shelter")) {
            String dist = item.getDistrict() != null ? item.getDistrict() : "";
            String up = item.getUpazila() != null ? item.getUpazila() : "";
            name = (dist.isEmpty() && up.isEmpty()) ? "Designated Disaster Shelter" : (up + " " + dist + " Shelter").trim();
        }
        holder.tvName.setText(name);
        holder.tvDistrict.setText(item.getDistrict());
        holder.tvUpazila.setText(item.getUpazila());
        holder.tvCapacity.setText(String.format(context.getString(R.string.shelter_capacity_format), item.getCapacity()));
        holder.tvCoords.setText(String.format("%.4f° N, %.4f° E", item.getLatitude(), item.getLongitude()));

        holder.cardRoot.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onShelterClick(item);
            }
        });

        holder.btnCall.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onShelterCallClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shelterList != null ? shelterList.size() : 0;
    }

    static class ShelterViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView tvDistrict;
        final TextView tvUpazila;
        final TextView tvCapacity;
        final TextView tvName;
        final TextView tvCoords;
        final MaterialButton btnCall;

        ShelterViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_shelter_root);
            tvDistrict = itemView.findViewById(R.id.tv_shelter_district_tag);
            tvUpazila = itemView.findViewById(R.id.tv_shelter_upazila_tag);
            tvCapacity = itemView.findViewById(R.id.tv_shelter_capacity_badge);
            tvName = itemView.findViewById(R.id.tv_shelter_name);
            tvCoords = itemView.findViewById(R.id.tv_shelter_coords);
            btnCall = itemView.findViewById(R.id.btn_shelter_call);
        }
    }
}
