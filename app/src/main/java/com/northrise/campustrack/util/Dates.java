package com.northrise.campustrack.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

public final class Dates {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private Dates() {
    }

    public static synchronized String format(long millis) {
        return FORMAT.format(new Date(millis));
    }

    public static String iso(long millis) {
        return Instant.ofEpochMilli(millis).toString();
    }

    public static long parseIso(String value) {
        return Instant.parse(value).toEpochMilli();
    }
}
