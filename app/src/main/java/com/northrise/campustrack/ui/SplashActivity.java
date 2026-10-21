package com.northrise.campustrack.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.northrise.campustrack.CampusTrackApp;
import com.northrise.campustrack.R;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.util.AppExecutors;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        AppExecutors.io(() -> {
            CampusTrackApp.get().repository().seedIfEmpty();
            AppExecutors.main(() -> {
                if (isFinishing()) {
                    return;
                }
                SessionManager session = CampusTrackApp.get().session();
                Class<?> next = session.isLoggedIn() ? MainActivity.class : LoginActivity.class;
                startActivity(new Intent(this, next));
                finish();
            });
        });
    }
}
