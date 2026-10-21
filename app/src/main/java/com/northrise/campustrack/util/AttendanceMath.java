package com.northrise.campustrack.util;

import java.util.Locale;

public final class AttendanceMath {
    public static final double MINIMUM = 75.0;
    public static final long SESSION_LENGTH_MS = 15L * 60L * 1000L;
    public static final long LATE_AFTER_MS = 10L * 60L * 1000L;

    private AttendanceMath() {
    }

    public static int attended(int present, int late) {
        return present + late;
    }

    public static int total(int present, int late, int absent) {
        return present + late + absent;
    }

    public static Double percentage(int present, int late, int absent) {
        int count = total(present, late, absent);
        if (count <= 0) {
            return null;
        }
        return attended(present, late) * 100.0 / count;
    }

    public static boolean isLow(int present, int late, int absent) {
        Double value = percentage(present, late, absent);
        return value != null && value < MINIMUM;
    }

    public static String format(Double percentage) {
        if (percentage == null) {
            return "No records";
        }
        return String.format(Locale.US, "%.1f%%", percentage);
    }

    public static String number(int present, int late, int absent) {
        Double value = percentage(present, late, absent);
        if (value == null) {
            return null;
        }
        return String.format(Locale.US, "%.1f", value);
    }

    public static String studentMessage(String code, int present, int late, int absent) {
        return "Your attendance in " + code + " is " + number(present, late, absent)
                + "%. The minimum required to sit the exam is 75%.";
    }

    public static String lecturerMessage(String studentName, String code, int present, int late, int absent) {
        return studentName + " is below the attendance minimum in " + code + " ("
                + number(present, late, absent) + "%).";
    }

    public static String statusForCheckIn(long startedAt, long now) {
        if (now - startedAt > LATE_AFTER_MS) {
            return "LATE";
        }
        return "PRESENT";
    }

    public static String breakdown(int present, int late, int absent) {
        return "Present " + present + " · Late " + late + " · Absent " + absent;
    }
}
