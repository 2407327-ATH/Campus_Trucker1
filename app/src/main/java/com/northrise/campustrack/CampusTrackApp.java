package com.northrise.campustrack;

import android.app.Application;

import androidx.room.Room;

import com.northrise.campustrack.data.AppDatabase;
import com.northrise.campustrack.data.SessionManager;
import com.northrise.campustrack.data.repo.CampusRepository;

public class CampusTrackApp extends Application {
    private static CampusTrackApp instance;
    private CampusRepository repository;
    private SessionManager sessionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, "campustrack.db")
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabase.FOREIGN_KEYS)
                .build();
        sessionManager = new SessionManager(this);
        repository = new CampusRepository(database, sessionManager);
    }

    public static CampusTrackApp get() {
        return instance;
    }

    public CampusRepository repository() {
        return repository;
    }

    public SessionManager session() {
        return sessionManager;
    }
}
