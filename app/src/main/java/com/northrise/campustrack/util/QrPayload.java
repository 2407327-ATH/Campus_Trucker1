package com.northrise.campustrack.util;

public final class QrPayload {
    public static final String PREFIX = "CAMPUSTRACK";

    private QrPayload() {
    }

    public static String encode(long sessionId, String token) {
        return PREFIX + "|" + sessionId + "|" + token;
    }

    public static String tokenFrom(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.trim().split("\\|");
        if (parts.length != 3 || !PREFIX.equals(parts[0]) || parts[2].isEmpty()) {
            return null;
        }
        return parts[2];
    }
}
