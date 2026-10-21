package com.northrise.campustrack.model;

public final class Role {
    public static final String STUDENT = "STUDENT";
    public static final String LECTURER = "LECTURER";
    public static final String ADMIN = "ADMIN";

    private Role() {
    }

    public static String label(String role) {
        if (LECTURER.equals(role)) {
            return "Lecturer";
        }
        if (ADMIN.equals(role)) {
            return "Administrator";
        }
        return "Student";
    }

    public static String normalize(String role) {
        if (role == null) {
            return "";
        }
        String value = role.trim().toUpperCase();
        if ("ADMINISTRATOR".equals(value)) {
            return ADMIN;
        }
        return value;
    }
}
