package com.northrise.campustrack.ui;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.northrise.campustrack.R;
import com.northrise.campustrack.util.AttendanceMath;

public final class Ui {
    private Ui() {
    }

    public static int percentColor(Context context, int present, int late, int absent) {
        if (AttendanceMath.total(present, late, absent) == 0) {
            return ContextCompat.getColor(context, R.color.muted);
        }
        if (AttendanceMath.isLow(present, late, absent)) {
            return ContextCompat.getColor(context, R.color.absent);
        }
        return ContextCompat.getColor(context, R.color.present);
    }

    public static int statusColor(Context context, String status) {
        if ("LATE".equals(status)) {
            return ContextCompat.getColor(context, R.color.late);
        }
        if ("ABSENT".equals(status)) {
            return ContextCompat.getColor(context, R.color.absent);
        }
        if ("PRESENT".equals(status)) {
            return ContextCompat.getColor(context, R.color.present);
        }
        return ContextCompat.getColor(context, R.color.muted);
    }

    public static int progress(int present, int late, int absent) {
        Double percentage = AttendanceMath.percentage(present, late, absent);
        if (percentage == null) {
            return 0;
        }
        return (int) Math.round(percentage);
    }

    public static int textOn(int background) {
        return background == Color.TRANSPARENT ? Color.BLACK : Color.WHITE;
    }
}
