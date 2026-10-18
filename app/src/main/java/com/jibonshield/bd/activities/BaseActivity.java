package com.jibonshield.bd.activities;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.jibonshield.bd.utils.LocaleHelper;

/**
 * Base activity class for JibonShield BD.
 * Injects selected user locale (Bangla/English) into the activity base context.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
