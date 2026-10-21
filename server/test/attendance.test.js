const test = require("node:test");
const assert = require("node:assert/strict");
const rules = require("../src/attendance");

test("75 percent is not low and one of three matches the Android alert", () => {
  assert.equal(rules.isLow(3, 0, 1), false);
  assert.equal(rules.isLow(1, 0, 2), true);
  assert.equal(
    rules.studentMessage("DBS220", 1, 0, 2),
    "Your attendance in DBS220 is 33.3%. The minimum required to sit the exam is 75%."
  );
  assert.equal(
    rules.lecturerMessage("Chipo Phiri", "DBS220", 1, 0, 2),
    "Chipo Phiri is below the attendance minimum in DBS220 (33.3%)."
  );
});

test("check-in is late only after ten minutes", () => {
  const start = 1_000_000;
  assert.equal(rules.statusForCheckIn(start, start + rules.LATE_AFTER_MS), "PRESENT");
  assert.equal(rules.statusForCheckIn(start, start + rules.LATE_AFTER_MS + 1), "LATE");
});
