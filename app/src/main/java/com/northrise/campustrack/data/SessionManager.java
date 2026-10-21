package com.northrise.campustrack.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.northrise.campustrack.model.User;

public class SessionManager {
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = open(context.getApplicationContext());
    }

    public boolean isLoggedIn() {
        return prefs.getLong("userId", 0) > 0;
    }

    public long getUserId() {
        return prefs.getLong("userId", 0);
    }

    public String getName() {
        return prefs.getString("name", "");
    }

    public String getEmail() {
        return prefs.getString("email", "");
    }

    public String getRole() {
        return prefs.getString("role", "");
    }

    public String getStudentNumber() {
        return prefs.getString("studentNumber", "");
    }

    public String getToken() {
        return prefs.getString("token", "");
    }

    public String getLastSync() {
        return prefs.getString("lastSync", "Not synced yet. Attendance is saved on this phone.");
    }

    public void save(User user, String token) {
        prefs.edit()
                .putLong("userId", user.id)
                .putString("name", user.name)
                .putString("email", user.email)
                .putString("role", user.role)
                .putString("studentNumber", user.studentNumber == null ? "" : user.studentNumber)
                .putString("token", token == null ? "" : token)
                .apply();
    }

    public void setLastSync(String message) {
        prefs.edit().putString("lastSync", message).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    private static SharedPreferences open(Context context) {
        try {
            MasterKey key = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    "campustrack.secure",
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception ex) {
            return context.getSharedPreferences("campustrack.session", Context.MODE_PRIVATE);
        }
    }
}
