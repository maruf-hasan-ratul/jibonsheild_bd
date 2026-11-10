package com.jibonshield.bd.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jibonshield.bd.R;
import com.jibonshield.bd.adapters.EmergencyContactAdapter;
import com.jibonshield.bd.databinding.ActivityEmergencyContactsBinding;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.repositories.EmergencyRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 6: Emergency Contacts.
 * Lists national emergency response hotlines stored in the offline database with direct dialing.
 * Demonstrates View Binding, implicit Intent ACTION_DIAL launching, and clipboard fallbacks.
 */
public class EmergencyContactsActivity extends BaseActivity implements EmergencyContactAdapter.OnContactActionListener {

    private ActivityEmergencyContactsBinding binding;
    private EmergencyContactAdapter adapter;
    private EmergencyRepository repository;
    private final List<EmergencyContact> contactList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmergencyContactsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new EmergencyRepository(this);

        setupToolbar();
        setupRecyclerView();
        loadContacts();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarEmergencyContacts;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        binding.recyclerEmergencyContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyContactAdapter(this, contactList, this);
        binding.recyclerEmergencyContacts.setAdapter(adapter);
    }

    private void loadContacts() {
        binding.progressEmergencyContacts.setVisibility(View.VISIBLE);
        repository.getEmergencyContacts(new EmergencyRepository.DataCallback<List<EmergencyContact>>() {
            @Override
            public void onDataLoaded(List<EmergencyContact> data) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressEmergencyContacts.setVisibility(View.GONE);
                contactList.clear();
                if (data != null) {
                    contactList.addAll(data);
                }
                adapter.notifyDataSetChanged();

                if (contactList.isEmpty()) {
                    binding.emptyStateContacts.getRoot().setVisibility(View.VISIBLE);
                    binding.recyclerEmergencyContacts.setVisibility(View.GONE);
                } else {
                    binding.emptyStateContacts.getRoot().setVisibility(View.GONE);
                    binding.recyclerEmergencyContacts.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed() || binding == null) return;
                binding.progressEmergencyContacts.setVisibility(View.GONE);
                binding.emptyStateContacts.getRoot().setVisibility(View.VISIBLE);
                binding.recyclerEmergencyContacts.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onCallClick(EmergencyContact contact) {
        dialPhoneNumber(contact.getPhoneNumber());
    }

    @Override
    public void onContactItemClick(EmergencyContact contact) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Emergency Phone", contact.getPhoneNumber());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.toast_phone_copied, contact.getPhoneNumber()), Toast.LENGTH_SHORT).show();
        }
    }

    private void dialPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty() || phone.trim().length() < 3) {
            Toast.makeText(this, "Invalid emergency phone number", Toast.LENGTH_SHORT).show();
            return;
        }

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
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Emergency Phone", phone);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, getString(R.string.toast_phone_copied, phone), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
            Toast.makeText(this, getString(R.string.toast_dialer_opened, phone), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_sync_refresh) {
            triggerSync();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerSync() {
        binding.progressEmergencyContacts.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show();

        com.jibonshield.bd.sync.SyncManager.getInstance(this).syncEmergencyContacts(null, new com.jibonshield.bd.sync.SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted(String entityName) {}

            @Override
            public void onSyncComplete(com.jibonshield.bd.sync.SyncResult result) {
                loadContacts();
                if (result.isOffline()) {
                    Toast.makeText(EmergencyContactsActivity.this, R.string.sync_offline_toast, Toast.LENGTH_LONG).show();
                } else if (result.isSuccess()) {
                    Toast.makeText(EmergencyContactsActivity.this, getString(R.string.sync_complete_toast, result.getMessage()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EmergencyContactsActivity.this, getString(R.string.sync_error_toast, result.getMessage()), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
