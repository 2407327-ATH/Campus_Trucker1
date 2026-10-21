package com.northrise.campustrack.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String email;
    public String passwordHash;
    public String pendingPassword;
    public String role;
    public String studentNumber;
    public boolean pendingSync;
}
