package com.jibonshield.bd.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for safe local media file persistence and ContentResolver stream copying.
 * Guarantees offline availability of evidence photos even if original system URI permission lapses.
 */
public final class FileUtils {

    private static final String TAG = "FileUtils";
    private static final String OBSERVATIONS_DIR = "water_observations";

    private FileUtils() {}

    /**
     * Copies a ContentResolver stream to a managed private app file.
     *
     * @param context Application context
     * @param contentUri Source content URI from Photo Picker / Document Provider
     * @return Local File URI string (e.g. file:///data/user/0/com.jibonshield.bd/files/...) or null on error
     */
    public static String saveImageToInternalStorage(Context context, Uri contentUri) {
        if (contentUri == null) return null;

        InputStream in = null;
        OutputStream out = null;
        try {
            File dir = new File(context.getFilesDir(), OBSERVATIONS_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Could not create directory: " + dir.getAbsolutePath());
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            String fileName = "obs_" + timestamp + ".jpg";
            File destinationFile = new File(dir, fileName);

            in = context.getContentResolver().openInputStream(contentUri);
            if (in == null) {
                Log.e(TAG, "Cannot open stream for content URI: " + contentUri);
                return null;
            }

            out = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();

            return Uri.fromFile(destinationFile).toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy media file from URI: " + contentUri, e);
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {}
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {}
        }
    }
}

