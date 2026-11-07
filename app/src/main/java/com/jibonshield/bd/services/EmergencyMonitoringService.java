package com.jibonshield.bd.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jibonshield.bd.R;
import com.jibonshield.bd.activities.MainActivity;

/**
 * Foreground Service for active Emergency Monitoring Mode.
 * Maintains a persistent user-visible notification during active disaster vigilance,
 * provides one-tap cancellation from the notification bar, and manages session lifecycle safely.
 */
public class EmergencyMonitoringService extends Service {

    private static final String TAG = "EmergencyMonitoring";

    public static final String ACTION_START_MONITORING = "com.jibonshield.bd.action.START_MONITORING";
    public static final String ACTION_STOP_MONITORING = "com.jibonshield.bd.action.STOP_MONITORING";
    public static final String CHANNEL_ID = "channel_emergency_monitoring";
    public static final int NOTIFICATION_ID = 2001;

    private static volatile boolean isRunning = false;

    public static boolean isMonitoringActive() {
        return isRunning;
    }

    public static void startMonitoring(Context context) {
        Intent intent = new Intent(context, EmergencyMonitoringService.class);
        intent.setAction(ACTION_START_MONITORING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopMonitoring(Context context) {
        Intent intent = new Intent(context, EmergencyMonitoringService.class);
        intent.setAction(ACTION_STOP_MONITORING);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_MONITORING.equals(intent.getAction())) {
            stopMonitoringSession();
            return START_NOT_STICKY;
        }

        startMonitoringSession();
        return START_STICKY;
    }

    private void startMonitoringSession() {
        isRunning = true;
        Log.i(TAG, "Emergency Monitoring Session Activated");

        Notification notification = buildMonitoringNotification(
                getString(R.string.monitoring_active_title),
                getString(R.string.monitoring_active_desc)
        );

        startForeground(NOTIFICATION_ID, notification);
    }

    private void stopMonitoringSession() {
        Log.i(TAG, "Emergency Monitoring Session Stopped");
        isRunning = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private Notification buildMonitoringNotification(String title, String message) {
        // Open MainActivity when tapping notification
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        // Stop action button
        Intent stopIntent = new Intent(this, EmergencyMonitoringService.class);
        stopIntent.setAction(ACTION_STOP_MONITORING);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_stop_monitoring), stopPendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_emergency_monitoring_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.channel_emergency_monitoring_desc));
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

