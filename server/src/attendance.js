const MINIMUM = 75;
const SESSION_MS = 15 * 60 * 1000;
const LATE_AFTER_MS = 10 * 60 * 1000;

function percentage(present, late, absent) {
  const total = present + late + absent;
  if (total <= 0) return null;
  return ((present + late) * 100) / total;
}

function isLow(present, late, absent) {
  const value = percentage(present, late, absent);
  return value !== null && value < MINIMUM;
}

function number(present, late, absent) {
  const value = percentage(present, late, absent);
  return value === null ? null : value.toFixed(1);
}

function studentMessage(code, present, late, absent) {
  return `Your attendance in ${code} is ${number(present, late, absent)}%. The minimum required to sit the exam is 75%.`;
}

function lecturerMessage(studentName, code, present, late, absent) {
  return `${studentName} is below the attendance minimum in ${code} (${number(present, late, absent)}%).`;
}

function statusForCheckIn(startedAtMs, nowMs) {
  return nowMs - startedAtMs > LATE_AFTER_MS ? "LATE" : "PRESENT";
}

module.exports = {
  MINIMUM,
  SESSION_MS,
  LATE_AFTER_MS,
  percentage,
  isLow,
  studentMessage,
  lecturerMessage,
  statusForCheckIn,
};
