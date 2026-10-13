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
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.utils.PreferenceUtils;

import java.util.List;

/**
 * Adapter for rendering verified emergency response hotlines.
 */
public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    public interface OnContactActionListener {
        void onCallClick(EmergencyContact contact);
        void onContactItemClick(EmergencyContact contact);
    }

    private final Context context;
    private final List<EmergencyContact> contactList;
    private final OnContactActionListener listener;
    private final boolean isBangla;

    public EmergencyContactAdapter(Context context, List<EmergencyContact> contactList, OnContactActionListener listener) {
        this.context = context;
        this.contactList = contactList;
        this.listener = listener;
        this.isBangla = PreferenceUtils.LANG_BN.equals(PreferenceUtils.getLanguage(context));
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact item = contactList.get(position);

        String name = isBangla ? item.getServiceNameBn() : item.getServiceNameEn();
        holder.tvName.setText(name);
        holder.tvPhone.setText(item.getPhoneNumber());
        holder.tvCategory.setText(item.getCategory());
        holder.tvHours.setText(String.format(context.getString(R.string.label_contact_hours), item.getAvailableHours()));

        if (item.isTollFree()) {
            holder.tvTollFree.setText(R.string.label_contact_toll_free);
            holder.tvTollFree.setVisibility(View.VISIBLE);
        } else {
            holder.tvTollFree.setText(R.string.label_contact_standard);
            holder.tvTollFree.setVisibility(View.VISIBLE);
        }

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(item);
            }
        });

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView tvCategory;
        final TextView tvTollFree;
        final TextView tvHours;
        final TextView tvName;
        final TextView tvPhone;
        final MaterialButton btnCall;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_contact_root);
            tvCategory = itemView.findViewById(R.id.tv_contact_category);
            tvTollFree = itemView.findViewById(R.id.tv_contact_tollfree);
            tvHours = itemView.findViewById(R.id.tv_contact_hours);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            btnCall = itemView.findViewById(R.id.btn_contact_call);
        }
    }
}
