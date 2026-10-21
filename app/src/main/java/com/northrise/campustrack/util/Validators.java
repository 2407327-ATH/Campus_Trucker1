package com.northrise.campustrack.util;

import com.northrise.campustrack.model.Role;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Validators {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9]{3,10}$");
    private static final Pattern STUDENT_NO = Pattern.compile("^[A-Z0-9-]{4,20}$");

    private Validators() {
    }

    public static String name(String value) {
        if (value == null || value.trim().length() < 3) {
            return "Enter a full name of at least 3 characters.";
        }
        return null;
    }

    public static String email(String value) {
        if (value == null || !EMAIL.matcher(value.trim()).matches()) {
            return "Enter a valid email address.";
        }
        return null;
    }

    public static String password(String value) {
        if (value == null || value.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) {
                letter = true;
            }
            if (Character.isDigit(c)) {
                digit = true;
            }
        }
        if (!letter || !digit) {
            return "Password must include a letter and a number.";
        }
        return null;
    }

    public static String courseCode(String value) {
        if (value == null || !CODE.matcher(value.trim().toUpperCase(Locale.US)).matches()) {
            return "Course code must be 3–10 letters or numbers.";
        }
        return null;
    }

    public static String courseTitle(String value) {
        if (value == null || value.trim().length() < 3) {
            return "Enter a course title.";
        }
        return null;
    }

    public static String role(String value) {
        String normalized = Role.normalize(value);
        if (!Role.STUDENT.equals(normalized) && !Role.LECTURER.equals(normalized) && !Role.ADMIN.equals(normalized)) {
            return "Choose Student, Lecturer, or Admin.";
        }
        return null;
    }

    public static String studentNumber(String value, boolean required) {
        if (value == null || value.trim().isEmpty()) {
            return required ? "Enter a student number." : null;
        }
        if (!STUDENT_NO.matcher(value.trim().toUpperCase(Locale.US)).matches()) {
            return "Student number must be 4–20 letters or digits.";
        }
        return null;
    }

    public static String status(String value) {
        if (!"PRESENT".equals(value) && !"LATE".equals(value) && !"ABSENT".equals(value)) {
            return "Choose present, late, or absent.";
        }
        return null;
    }
}
