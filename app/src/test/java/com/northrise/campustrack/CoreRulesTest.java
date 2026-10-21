package com.northrise.campustrack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.northrise.campustrack.util.AttendanceMath;
import com.northrise.campustrack.util.PasswordHasher;
import com.northrise.campustrack.util.QrPayload;
import com.northrise.campustrack.util.Validators;

import org.junit.Test;

public class CoreRulesTest {
    @Test
    public void percentageIsSafeWhenThereAreNoRecords() {
        assertNull(AttendanceMath.percentage(0, 0, 0));
        assertEquals("No records", AttendanceMath.format(null));
        assertFalse(AttendanceMath.isLow(0, 0, 0));
    }

    @Test
    public void exactlySeventyFivePercentIsNotLow() {
        assertEquals(75.0, AttendanceMath.percentage(3, 0, 1), 0.001);
        assertFalse(AttendanceMath.isLow(3, 0, 1));
    }

    @Test
    public void oneOfThreeIsLowAndMatchesTheAlertText() {
        assertTrue(AttendanceMath.isLow(1, 0, 2));
        assertEquals(
                "Your attendance in DBS220 is 33.3%. The minimum required to sit the exam is 75%.",
                AttendanceMath.studentMessage("DBS220", 1, 0, 2)
        );
        assertEquals(
                "Chipo Phiri is below the attendance minimum in DBS220 (33.3%).",
                AttendanceMath.lecturerMessage("Chipo Phiri", "DBS220", 1, 0, 2)
        );
    }

    @Test
    public void lateCheckInStartsAfterTenMinutes() {
        long start = 1_000_000L;
        assertEquals("PRESENT", AttendanceMath.statusForCheckIn(start, start + AttendanceMath.LATE_AFTER_MS));
        assertEquals("LATE", AttendanceMath.statusForCheckIn(start, start + AttendanceMath.LATE_AFTER_MS + 1));
    }

    @Test
    public void qrPayloadRoundTripRejectsOtherCodes() {
        assertEquals("token-1", QrPayload.tokenFrom(QrPayload.encode(44, "token-1")));
        assertNull(QrPayload.tokenFrom("https://example.com"));
        assertNull(QrPayload.tokenFrom(null));
    }

    @Test
    public void passwordHashVerifiesOnlyTheOriginalPassword() {
        String stored = PasswordHasher.hash("Student@123");
        assertTrue(PasswordHasher.verify("Student@123", stored));
        assertFalse(PasswordHasher.verify("Student@124", stored));
        assertFalse(PasswordHasher.verify("Student@123", "not-a-hash"));
    }

    @Test
    public void validationRejectsBlankAndAcceptsDemoValues() {
        assertNull(Validators.email("student@nu.ac.zm"));
        assertNull(Validators.password("Student@123"));
        assertNull(Validators.courseCode("mad301"));
        assertNull(Validators.studentNumber("SN2201001", true));
        assertTrue(Validators.email("student") != null);
        assertTrue(Validators.password("short") != null);
        assertTrue(Validators.password("longpassword") != null);
        assertTrue(Validators.courseCode("A") != null);
    }
}
