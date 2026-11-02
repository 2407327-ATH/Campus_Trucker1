const mysql = require("mysql2/promise");
const bcrypt = require("bcryptjs");
const rules = require("./attendance");

const SCHEMA = `
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('STUDENT','LECTURER','ADMIN') NOT NULL,
  student_number VARCHAR(40) NULL
);
CREATE TABLE IF NOT EXISTS courses (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL UNIQUE,
  title VARCHAR(160) NOT NULL,
  room VARCHAR(80) NOT NULL DEFAULT '',
  lecturer_id INT NOT NULL,
  FOREIGN KEY (lecturer_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS enrollments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  course_id INT NOT NULL,
  UNIQUE KEY uniq_enrollment (student_id, course_id),
  FOREIGN KEY (student_id) REFERENCES users(id),
  FOREIGN KEY (course_id) REFERENCES courses(id)
);
CREATE TABLE IF NOT EXISTS attendance_sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  course_id INT NOT NULL,
  qr_token VARCHAR(80) NOT NULL UNIQUE,
  started_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  status ENUM('OPEN','CLOSED') NOT NULL,
  FOREIGN KEY (course_id) REFERENCES courses(id)
);
CREATE TABLE IF NOT EXISTS attendance_records (
  id INT AUTO_INCREMENT PRIMARY KEY,
  session_id INT NOT NULL,
  student_id INT NOT NULL,
  status ENUM('PRESENT','LATE','ABSENT') NOT NULL,
  marked_at BIGINT NOT NULL,
  UNIQUE KEY uniq_mark (session_id, student_id),
  FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),
  FOREIGN KEY (student_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS notifications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  course_id INT NULL,
  subject_email VARCHAR(190) NULL,
  type VARCHAR(40) NOT NULL,
  message VARCHAR(500) NOT NULL,
  read_at BIGINT NULL,
  created_at BIGINT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
`;

const BASE = 1791439200000;
const DAY = 86400000;
let pool;

function config() {
  return {
    host: process.env.DB_HOST || "localhost",
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || "root",
    password: process.env.DB_PASSWORD || "",
    database: process.env.DB_NAME || "campustrack",
  };
}

async function init() {
  const db = config();
  const bootstrap = await mysql.createConnection({
    host: db.host,
    port: db.port,
    user: db.user,
    password: db.password,
    multipleStatements: true,
  });
  await bootstrap.query(`CREATE DATABASE IF NOT EXISTS \`${db.database}\``);
  await bootstrap.end();
  pool = mysql.createPool({ ...db, waitForConnections: true, connectionLimit: 10 });
  await pool.query(SCHEMA);
  await seed();
  return pool;
}

function getPool() {
  return pool;
}

async function seed() {
  const [rows] = await pool.query("SELECT COUNT(*) AS count FROM users");
  if (rows[0].count > 0) return;
  const admin = await insertUser("Admin User", "admin@nu.ac.zm", "Admin@123", "ADMIN", null);
  const lecturer = await insertUser("Dr. Banda", "lecturer@nu.ac.zm", "Lecturer@123", "LECTURER", null);
  const chipo = await insertUser("Chipo Phiri", "student@nu.ac.zm", "Student@123", "STUDENT", "SN2201001");
  const james = await insertUser("James Bwalya", "student2@nu.ac.zm", "Student@123", "STUDENT", "SN2201002");
  const mad = await insertCourse("MAD301", "Mobile Application Development", "Lab 2", lecturer);
  const dbs = await insertCourse("DBS220", "Database Systems", "LH1", lecturer);
  await enroll(chipo, mad);
  await enroll(james, mad);
  await enroll(chipo, dbs);
  await enroll(james, dbs);
  await closedSession(mad, "mad-20260917", BASE - 21 * DAY, chipo, james, "ABSENT", "PRESENT");
  await closedSession(mad, "mad-20260924", BASE - 14 * DAY, chipo, james, "PRESENT", "LATE");
  await closedSession(mad, "mad-20261001", BASE - 7 * DAY, chipo, james, "PRESENT", "PRESENT");
  await closedSession(mad, "mad-20261008", BASE, chipo, james, "PRESENT", "PRESENT");
  await closedSession(dbs, "dbs-20260922", BASE - 16 * DAY, chipo, james, "ABSENT", "PRESENT");
  await closedSession(dbs, "dbs-20260929", BASE - 9 * DAY, chipo, james, "ABSENT", "LATE");
  await closedSession(dbs, "dbs-20261006", BASE - 2 * DAY, chipo, james, "PRESENT", "PRESENT");
  await refreshAlerts(mad);
  await refreshAlerts(dbs);
}

async function insertUser(name, email, password, role, studentNumber) {
  const hash = await bcrypt.hash(password, 10);
  const [result] = await pool.query(
    "INSERT INTO users (name, email, password_hash, role, student_number) VALUES (?, ?, ?, ?, ?)",
    [name, email, hash, role, studentNumber]
  );
  return result.insertId;
}

async function insertCourse(code, title, room, lecturerId) {
  const [result] = await pool.query(
    "INSERT INTO courses (code, title, room, lecturer_id) VALUES (?, ?, ?, ?)",
    [code, title, room, lecturerId]
  );
  return result.insertId;
}

async function enroll(studentId, courseId) {
  await pool.query("INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)", [studentId, courseId]);
}

async function closedSession(courseId, token, startedAt, firstId, secondId, firstStatus, secondStatus) {
  const [result] = await pool.query(
    "INSERT INTO attendance_sessions (course_id, qr_token, started_at, expires_at, status) VALUES (?, ?, ?, ?, 'CLOSED')",
    [courseId, token, startedAt, startedAt + rules.SESSION_MS]
  );
  await mark(result.insertId, firstId, firstStatus, startedAt + offset(firstStatus));
  await mark(result.insertId, secondId, secondStatus, startedAt + offset(secondStatus));
}

function offset(status) {
  if (status === "ABSENT") return 20 * 60 * 1000;
  if (status === "LATE") return 12 * 60 * 1000;
  return 5 * 60 * 1000;
}

async function mark(sessionId, studentId, status, markedAt) {
  await pool.query(
    `INSERT INTO attendance_records (session_id, student_id, status, marked_at)
     VALUES (?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE status = VALUES(status), marked_at = VALUES(marked_at)`,
    [sessionId, studentId, status, markedAt]
  );
}

async function refreshAlerts(courseId) {
  const [courses] = await pool.query(
    `SELECT c.id, c.code, u.id AS lecturer_id
     FROM courses c JOIN users u ON u.id = c.lecturer_id WHERE c.id = ?`,
    [courseId]
  );
  if (!courses.length) return;
  const course = courses[0];
  const [students] = await pool.query(
    `SELECT u.id, u.name, u.email,
        SUM(ar.status = 'PRESENT') AS present,
        SUM(ar.status = 'LATE') AS late,
        SUM(ar.status = 'ABSENT') AS absent
     FROM enrollments e
     JOIN users u ON u.id = e.student_id
     LEFT JOIN attendance_sessions s ON s.course_id = e.course_id
     LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = u.id
     WHERE e.course_id = ?
     GROUP BY u.id, u.name, u.email`,
    [courseId]
  );
  const [admins] = await pool.query("SELECT id FROM users WHERE role = 'ADMIN'");
  for (const student of students) {
    const present = Number(student.present || 0);
    const late = Number(student.late || 0);
    const absent = Number(student.absent || 0);
    const low = rules.isLow(present, late, absent);
    const studentMessage = rules.studentMessage(course.code, present, late, absent);
    const staffMessage = rules.lecturerMessage(student.name, course.code, present, late, absent);
    await upsertAlert(student.id, course.id, student.email, low, studentMessage);
    await upsertAlert(course.lecturer_id, course.id, student.email, low, staffMessage);
    for (const admin of admins) {
      await upsertAlert(admin.id, course.id, student.email, low, staffMessage);
    }
  }
}

async function upsertAlert(userId, courseId, subjectEmail, low, message) {
  const [existing] = await pool.query(
    `SELECT id, message FROM notifications
     WHERE user_id = ? AND course_id = ? AND type = 'LOW_ATTENDANCE' AND subject_email = ? AND read_at IS NULL
     LIMIT 1`,
    [userId, courseId, subjectEmail]
  );
  if (!low) {
    if (existing.length) await pool.query("DELETE FROM notifications WHERE id = ?", [existing[0].id]);
    return;
  }
  if (existing.length && existing[0].message === message) return;
  if (existing.length) await pool.query("DELETE FROM notifications WHERE id = ?", [existing[0].id]);
  await pool.query(
    `INSERT INTO notifications (user_id, course_id, subject_email, type, message, created_at)
     VALUES (?, ?, ?, 'LOW_ATTENDANCE', ?, ?)`,
    [userId, courseId, subjectEmail, message, Date.now()]
  );
}

async function closeExpired() {
  await pool.query(
    "UPDATE attendance_sessions SET status = 'CLOSED' WHERE status = 'OPEN' AND expires_at <= ?",
    [Date.now()]
  );
}

module.exports = { init, getPool, refreshAlerts, mark, closeExpired, offset };
