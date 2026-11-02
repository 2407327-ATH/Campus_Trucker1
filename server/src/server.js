require("dotenv").config();
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const rateLimit = require("express-rate-limit");
const { randomUUID } = require("crypto");
const db = require("./db");
const rules = require("./attendance");

const PORT = Number(process.env.PORT || 3000);
const JWT_SECRET = process.env.JWT_SECRET || "campustrack-dev-secret-change-me";
const ROLES = ["STUDENT", "LECTURER", "ADMIN"];

function clean(value) {
  return typeof value === "string" ? value.trim() : "";
}

function emailOf(value) {
  return clean(value).toLowerCase();
}

function roleOf(value) {
  const role = clean(value).toUpperCase();
  return role === "ADMINISTRATOR" ? "ADMIN" : role;
}

function publicUser(row) {
  return {
    id: row.id,
    name: row.name,
    email: row.email,
    role: row.role,
    studentNumber: row.student_number,
  };
}

function iso(millis) {
  return new Date(Number(millis)).toISOString();
}

function parseTime(value) {
  if (typeof value === "number") return value;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function sign(user) {
  return jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: "2h" });
}

function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!token) return res.status(401).json({ error: "Login required." });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch (error) {
    return res.status(401).json({ error: "Session expired. Log in again." });
  }
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!roles.includes(req.user.role)) return res.status(403).json({ error: "You do not have permission to do that." });
    next();
  };
}

async function loadCourse(idOrCode) {
  const pool = db.getPool();
  const byId = Number(idOrCode);
  const [rows] = Number.isInteger(byId) && String(idOrCode).match(/^\d+$/)
    ? await pool.query("SELECT * FROM courses WHERE id = ?", [byId])
    : await pool.query("SELECT * FROM courses WHERE code = ?", [clean(idOrCode).toUpperCase()]);
  return rows[0] || null;
}

async function courseDto(course) {
  const [rows] = await db.getPool().query("SELECT name, email FROM users WHERE id = ?", [course.lecturer_id]);
  return {
    id: course.id,
    code: course.code,
    title: course.title,
    room: course.room,
    lecturerEmail: rows[0] ? rows[0].email : null,
    lecturerName: rows[0] ? rows[0].name : null,
  };
}

async function sessionDto(session) {
  const course = await loadCourse(session.course_id);
  return {
    id: session.id,
    courseCode: course ? course.code : null,
    qrToken: session.qr_token,
    startedAt: iso(session.started_at),
    expiresAt: iso(session.expires_at),
    status: session.status,
  };
}

function canManage(user, course) {
  return user.role === "ADMIN" || (user.role === "LECTURER" && course.lecturer_id === user.id);
}

async function snapshotFor(user) {
  const pool = db.getPool();
  let courseSql = "SELECT * FROM courses";
  let courseParams = [];
  if (user.role === "LECTURER") {
    courseSql += " WHERE lecturer_id = ?";
    courseParams = [user.id];
  } else if (user.role === "STUDENT") {
    courseSql += " WHERE id IN (SELECT course_id FROM enrollments WHERE student_id = ?)";
    courseParams = [user.id];
  }
  const [courses] = await pool.query(courseSql, courseParams);
  const courseIds = courses.map((course) => course.id);
  const users = new Map();
  const me = (await pool.query("SELECT * FROM users WHERE id = ?", [user.id]))[0][0];
  users.set(me.email, publicUser(me));
  const courseDtos = [];
  for (const course of courses) {
    courseDtos.push(await courseDto(course));
    const lecturer = (await pool.query("SELECT * FROM users WHERE id = ?", [course.lecturer_id]))[0][0];
    if (lecturer) users.set(lecturer.email, publicUser(lecturer));
  }
  let enrollments = [];
  if (courseIds.length) {
    const [rows] = await pool.query(
      `SELECT u.email AS studentEmail, c.code AS courseCode, u.id AS studentId, u.name, u.role, u.student_number
       FROM enrollments e
       JOIN users u ON u.id = e.student_id
       JOIN courses c ON c.id = e.course_id
       WHERE e.course_id IN (${courseIds.map(() => "?").join(",")})
       ${user.role === "STUDENT" ? "AND e.student_id = ?" : ""}`,
      user.role === "STUDENT" ? [...courseIds, user.id] : courseIds
    );
    enrollments = rows.map((row) => ({ studentEmail: row.studentEmail, courseCode: row.courseCode }));
    if (user.role !== "STUDENT") {
      for (const row of rows) {
        users.set(row.studentEmail, {
          id: row.studentId,
          name: row.name,
          email: row.studentEmail,
          role: row.role,
          studentNumber: row.student_number,
        });
      }
    }
  }
  let sessions = [];
  let records = [];
  if (courseIds.length) {
    const [sessionRows] = await pool.query(
      `SELECT * FROM attendance_sessions WHERE course_id IN (${courseIds.map(() => "?").join(",")})`,
      courseIds
    );
    sessions = [];
    for (const session of sessionRows) sessions.push(await sessionDto(session));
    const recordParams = user.role === "STUDENT" ? [...courseIds, user.id] : courseIds;
    const [recordRows] = await pool.query(
      `SELECT ar.status, ar.marked_at, s.qr_token, u.email AS studentEmail
       FROM attendance_records ar
       JOIN attendance_sessions s ON s.id = ar.session_id
       JOIN users u ON u.id = ar.student_id
       WHERE s.course_id IN (${courseIds.map(() => "?").join(",")})
       ${user.role === "STUDENT" ? "AND ar.student_id = ?" : ""}`,
      recordParams
    );
    records = recordRows.map((row) => ({
      qrToken: row.qr_token,
      studentEmail: row.studentEmail,
      status: row.status,
      markedAt: iso(row.marked_at),
    }));
  }
  const [notes] = await pool.query(
    `SELECT n.type, n.message, n.subject_email, n.created_at, n.read_at, c.code AS courseCode
     FROM notifications n LEFT JOIN courses c ON c.id = n.course_id
     WHERE n.user_id = ? ORDER BY n.created_at DESC`,
    [user.id]
  );
  return {
    users: [...users.values()],
    courses: courseDtos,
    enrollments,
    sessions,
    records,
    notifications: notes.map((note) => ({
      type: note.type,
      message: note.message,
      courseCode: note.courseCode,
      subjectEmail: note.subject_email,
      createdAt: iso(note.created_at),
      readAt: note.read_at ? iso(note.read_at) : null,
    })),
  };
}

async function main() {
  await db.init();
  const app = express();
  app.use(helmet());
  app.use(cors());
  app.use(express.json({ limit: "256kb" }));
  const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 30, standardHeaders: true, legacyHeaders: false });

  app.get("/api/health", (req, res) => res.json({ ok: true }));

  app.post("/api/auth/register", authLimiter, async (req, res) => {
    const name = clean(req.body.name);
    const email = emailOf(req.body.email);
    const password = req.body.password || "";
    const role = roleOf(req.body.role);
    const studentNumber = clean(req.body.studentNumber).toUpperCase() || null;
    if (name.length < 3) return res.status(400).json({ error: "Enter a full name of at least 3 characters." });
    if (!email.includes("@")) return res.status(400).json({ error: "Enter a valid email address." });
    if (password.length < 6) return res.status(400).json({ error: "Password must be at least 6 characters." });
    if (!ROLES.includes(role)) return res.status(400).json({ error: "Choose Student, Lecturer, or Admin." });
    if (role === "STUDENT" && (!studentNumber || studentNumber.length < 4)) {
      return res.status(400).json({ error: "Enter a student number." });
    }
    const [existing] = await db.getPool().query("SELECT id FROM users WHERE email = ?", [email]);
    if (existing.length) return res.status(409).json({ error: "An account with that email already exists." });
    const hash = await bcrypt.hash(password, 10);
    const [result] = await db.getPool().query(
      "INSERT INTO users (name, email, password_hash, role, student_number) VALUES (?, ?, ?, ?, ?)",
      [name, email, hash, role, role === "STUDENT" ? studentNumber : null]
    );
    const user = { id: result.insertId, name, email, role, student_number: role === "STUDENT" ? studentNumber : null };
    res.status(201).json({ token: sign(user), user: publicUser(user) });
  });

  app.post("/api/auth/login", authLimiter, async (req, res) => {
    const email = emailOf(req.body.email);
    const password = req.body.password || "";
    const [rows] = await db.getPool().query("SELECT * FROM users WHERE email = ?", [email]);
    if (!rows.length || !(await bcrypt.compare(password, rows[0].password_hash))) {
      return res.status(401).json({ error: "Email or password is incorrect." });
    }
    res.json({ token: sign(rows[0]), user: publicUser(rows[0]) });
  });

  app.get("/api/auth/me", requireAuth, async (req, res) => {
    const [rows] = await db.getPool().query("SELECT * FROM users WHERE id = ?", [req.user.id]);
    if (!rows.length) return res.status(401).json({ error: "Account not found." });
    res.json(publicUser(rows[0]));
  });

  app.get("/api/courses", requireAuth, async (req, res) => {
    const snap = await snapshotFor(req.user);
    res.json(snap.courses);
  });

  app.post("/api/courses", requireAuth, requireRole("LECTURER", "ADMIN"), async (req, res) => {
    const code = clean(req.body.code).toUpperCase();
    const title = clean(req.body.title);
    const room = clean(req.body.room);
    if (!/^[A-Z0-9]{3,10}$/.test(code)) return res.status(400).json({ error: "Course code must be 3–10 letters or numbers." });
    if (title.length < 3) return res.status(400).json({ error: "Enter a course title." });
    let lecturerId = req.user.id;
    if (req.user.role === "ADMIN") {
      const lecturerEmail = emailOf(req.body.lecturerEmail);
      const [lecturers] = await db.getPool().query("SELECT * FROM users WHERE email = ? AND role = 'LECTURER'", [lecturerEmail]);
      if (!lecturers.length) return res.status(400).json({ error: "No lecturer account uses that email." });
      lecturerId = lecturers[0].id;
    }
    try {
      const [result] = await db.getPool().query(
        "INSERT INTO courses (code, title, room, lecturer_id) VALUES (?, ?, ?, ?)",
        [code, title, room, lecturerId]
      );
      const course = await loadCourse(result.insertId);
      res.status(201).json(await courseDto(course));
    } catch (error) {
      if (error.code === "ER_DUP_ENTRY") return res.status(409).json({ error: "That course code already exists." });
      throw error;
    }
  });

  app.post("/api/courses/:id/enroll", requireAuth, requireRole("LECTURER", "ADMIN"), async (req, res) => {
    const course = await loadCourse(req.params.id);
    if (!course) return res.status(404).json({ error: "Course not found." });
    if (!canManage(req.user, course)) return res.status(403).json({ error: "You do not have permission to change this course." });
    const studentEmail = emailOf(req.body.email || req.body.studentEmail);
    const [students] = await db.getPool().query("SELECT * FROM users WHERE email = ? AND role = 'STUDENT'", [studentEmail]);
    if (!students.length) return res.status(404).json({ error: "No student account uses that email." });
    try {
      await db.getPool().query("INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)", [students[0].id, course.id]);
    } catch (error) {
      if (error.code === "ER_DUP_ENTRY") return res.status(409).json({ error: "That student is already enrolled." });
      throw error;
    }
    await db.refreshAlerts(course.id);
    res.json({ ok: true });
  });

  app.get("/api/courses/:id/students", requireAuth, requireRole("LECTURER", "ADMIN"), async (req, res) => {
    const course = await loadCourse(req.params.id);
    if (!course) return res.status(404).json({ error: "Course not found." });
    if (!canManage(req.user, course)) return res.status(403).json({ error: "You do not have permission to view this roster." });
    const [rows] = await db.getPool().query(
      `SELECT u.* FROM users u JOIN enrollments e ON e.student_id = u.id WHERE e.course_id = ? ORDER BY u.name`,
      [course.id]
    );
    res.json(rows.map(publicUser));
  });

  app.post("/api/sessions", requireAuth, requireRole("LECTURER"), async (req, res) => {
    await db.closeExpired();
    const course = await loadCourse(req.body.courseId || req.body.courseCode);
    if (!course) return res.status(404).json({ error: "Course not found." });
    if (course.lecturer_id !== req.user.id) return res.status(403).json({ error: "Only the lecturer who owns this course can start a session." });
    const [open] = await db.getPool().query(
      "SELECT * FROM attendance_sessions WHERE course_id = ? AND status = 'OPEN' AND expires_at > ? ORDER BY started_at DESC LIMIT 1",
      [course.id, Date.now()]
    );
    if (open.length) return res.json(await sessionDto(open[0]));
    const now = Date.now();
    const token = randomUUID().replace(/-/g, "");
    const [result] = await db.getPool().query(
      "INSERT INTO attendance_sessions (course_id, qr_token, started_at, expires_at, status) VALUES (?, ?, ?, ?, 'OPEN')",
      [course.id, token, now, now + rules.SESSION_MS]
    );
    const [created] = await db.getPool().query("SELECT * FROM attendance_sessions WHERE id = ?", [result.insertId]);
    res.status(201).json(await sessionDto(created[0]));
  });

  app.post("/api/attendance/checkin", requireAuth, async (req, res) => {
    await db.closeExpired();
    const pool = db.getPool();
    let session = null;
    if (req.body.qrToken) {
      const [rows] = await pool.query("SELECT * FROM attendance_sessions WHERE qr_token = ?", [clean(req.body.qrToken)]);
      session = rows[0];
    } else if (req.body.sessionId) {
      const [rows] = await pool.query("SELECT * FROM attendance_sessions WHERE id = ?", [req.body.sessionId]);
      session = rows[0];
    }
    if (!session) return res.status(404).json({ error: "Attendance session not found." });
    const course = await loadCourse(session.course_id);
    let student;
    let status;
    if (req.user.role === "STUDENT") {
      if (session.status !== "OPEN" || Number(session.expires_at) <= Date.now()) {
        return res.status(400).json({ error: "This session has expired. Ask the lecturer to start a new one." });
      }
      const [enrolled] = await pool.query(
        "SELECT id FROM enrollments WHERE student_id = ? AND course_id = ?",
        [req.user.id, course.id]
      );
      if (!enrolled.length) return res.status(403).json({ error: "You are not enrolled in this course." });
      const [rows] = await pool.query("SELECT * FROM users WHERE id = ?", [req.user.id]);
      student = rows[0];
      status = rules.statusForCheckIn(Number(session.started_at), Date.now());
    } else {
      if (!canManage(req.user, course)) return res.status(403).json({ error: "You do not have permission to mark this course." });
      const [rows] = await pool.query("SELECT * FROM users WHERE email = ? AND role = 'STUDENT'", [emailOf(req.body.studentEmail)]);
      if (!rows.length) return res.status(404).json({ error: "No student account uses that email." });
      student = rows[0];
      const [enrolled] = await pool.query(
        "SELECT id FROM enrollments WHERE student_id = ? AND course_id = ?",
        [student.id, course.id]
      );
      if (!enrolled.length) return res.status(400).json({ error: "That student is not enrolled in this course." });
      status = clean(req.body.status).toUpperCase();
      if (!["PRESENT", "LATE", "ABSENT"].includes(status)) return res.status(400).json({ error: "Choose present, late, or absent." });
    }
    const markedAt = Date.now();
    await db.mark(session.id, student.id, status, markedAt);
    await db.refreshAlerts(course.id);
    res.json({
      status,
      markedAt: iso(markedAt),
      session: await sessionDto(session),
      course: await courseDto(course),
    });
  });

  app.get("/api/attendance/course/:id/summary", requireAuth, requireRole("LECTURER", "ADMIN"), async (req, res) => {
    const course = await loadCourse(req.params.id);
    if (!course) return res.status(404).json({ error: "Course not found." });
    if (!canManage(req.user, course)) return res.status(403).json({ error: "You do not have permission to view this course." });
    const [rows] = await db.getPool().query(
      `SELECT u.name, u.email,
          SUM(ar.status = 'PRESENT') AS present,
          SUM(ar.status = 'LATE') AS late,
          SUM(ar.status = 'ABSENT') AS absent
       FROM enrollments e JOIN users u ON u.id = e.student_id
       LEFT JOIN attendance_sessions s ON s.course_id = e.course_id
       LEFT JOIN attendance_records ar ON ar.session_id = s.id AND ar.student_id = u.id
       WHERE e.course_id = ?
       GROUP BY u.id, u.name, u.email ORDER BY u.name`,
      [course.id]
    );
    const students = rows.map((row) => {
      const present = Number(row.present || 0);
      const late = Number(row.late || 0);
      const absent = Number(row.absent || 0);
      const percentage = rules.percentage(present, late, absent);
      return { name: row.name, email: row.email, present, late, absent, percentage, low: rules.isLow(present, late, absent) };
    });
    const totals = students.reduce((sum, student) => ({
      present: sum.present + student.present,
      late: sum.late + student.late,
      absent: sum.absent + student.absent,
    }), { present: 0, late: 0, absent: 0 });
    res.json({ course: await courseDto(course), ...totals, percentage: rules.percentage(totals.present, totals.late, totals.absent), students });
  });

  app.get("/api/attendance/student/:id/history", requireAuth, async (req, res) => {
    const studentId = Number(req.params.id);
    if (req.user.role === "STUDENT" && req.user.id !== studentId) {
      return res.status(403).json({ error: "You can only view your own attendance." });
    }
    if (req.user.role === "LECTURER") {
      const [allowed] = await db.getPool().query(
        `SELECT e.id FROM enrollments e JOIN courses c ON c.id = e.course_id
         WHERE e.student_id = ? AND c.lecturer_id = ? LIMIT 1`,
        [studentId, req.user.id]
      );
      if (!allowed.length) return res.status(403).json({ error: "You can only view students in your courses." });
    }
    const [rows] = await db.getPool().query(
      `SELECT c.code AS courseCode, c.title AS courseTitle, ar.status, ar.marked_at AS markedAt, s.started_at AS startedAt
       FROM attendance_records ar
       JOIN attendance_sessions s ON s.id = ar.session_id
       JOIN courses c ON c.id = s.course_id
       WHERE ar.student_id = ? ORDER BY ar.marked_at DESC`,
      [studentId]
    );
    res.json(rows.map((row) => ({ ...row, markedAt: iso(row.markedAt), startedAt: iso(row.startedAt) })));
  });

  app.get("/api/notifications", requireAuth, async (req, res) => {
    const snap = await snapshotFor(req.user);
    res.json(snap.notifications);
  });

  app.post("/api/sync/batch", requireAuth, async (req, res) => {
    const pool = db.getPool();
    const errors = [];
    const saved = { users: [], courses: [], enrollments: [], sessions: [], records: [] };
    const body = req.body || {};
    for (const item of body.users || []) {
      const email = emailOf(item.email);
      try {
        if (req.user.role !== "ADMIN" && email !== req.user.email) {
          throw new Error("You can only sync your own account.");
        }
        const [existing] = await pool.query("SELECT id FROM users WHERE email = ?", [email]);
        if (existing.length) {
          saved.users.push(email);
          continue;
        }
        const role = roleOf(item.role);
        if (!ROLES.includes(role) || !item.password) throw new Error(`Could not create ${email}.`);
        const hash = await bcrypt.hash(item.password, 10);
        await pool.query(
          "INSERT INTO users (name, email, password_hash, role, student_number) VALUES (?, ?, ?, ?, ?)",
          [clean(item.name), email, hash, role, item.studentNumber || null]
        );
        saved.users.push(email);
      } catch (error) {
        errors.push(error.message);
      }
    }
    for (const item of body.courses || []) {
      try {
        const code = clean(item.code).toUpperCase();
        const [existing] = await pool.query("SELECT id FROM courses WHERE code = ?", [code]);
        if (existing.length) {
          saved.courses.push(code);
          continue;
        }
        const lecturerEmail = emailOf(item.lecturerEmail);
        if (req.user.role === "LECTURER" && lecturerEmail !== req.user.email) throw new Error("You can only sync your own courses.");
        if (req.user.role === "STUDENT") throw new Error("Students cannot sync courses.");
        const [lecturers] = await pool.query("SELECT id FROM users WHERE email = ? AND role = 'LECTURER'", [lecturerEmail]);
        if (!lecturers.length) throw new Error(`No lecturer found for ${code}.`);
        await pool.query(
          "INSERT INTO courses (code, title, room, lecturer_id) VALUES (?, ?, ?, ?)",
          [code, clean(item.title), clean(item.room), lecturers[0].id]
        );
        saved.courses.push(code);
      } catch (error) {
        errors.push(error.message);
      }
    }
    for (const item of body.enrollments || []) {
      const key = `${emailOf(item.studentEmail)}|${clean(item.courseCode).toUpperCase()}`;
      try {
        const course = await loadCourse(item.courseCode);
        const [students] = await pool.query("SELECT id FROM users WHERE email = ? AND role = 'STUDENT'", [emailOf(item.studentEmail)]);
        if (!course || !students.length) throw new Error(`Could not enrol ${key}.`);
        if (!canManage(req.user, course)) throw new Error(`You cannot enrol students in ${course.code}.`);
        await pool.query("INSERT IGNORE INTO enrollments (student_id, course_id) VALUES (?, ?)", [students[0].id, course.id]);
        saved.enrollments.push(key);
      } catch (error) {
        errors.push(error.message);
      }
    }
    for (const item of body.sessions || []) {
      try {
        const course = await loadCourse(item.courseCode);
        if (!course) throw new Error("Course missing for a session.");
        if (course.lecturer_id !== req.user.id && req.user.role !== "ADMIN") throw new Error("You cannot sync this session.");
        const [existing] = await pool.query("SELECT id FROM attendance_sessions WHERE qr_token = ?", [item.qrToken]);
        if (!existing.length) {
          await pool.query(
            "INSERT INTO attendance_sessions (course_id, qr_token, started_at, expires_at, status) VALUES (?, ?, ?, ?, ?)",
            [course.id, item.qrToken, parseTime(item.startedAt), parseTime(item.expiresAt), item.status === "OPEN" ? "OPEN" : "CLOSED"]
          );
        }
        saved.sessions.push(item.qrToken);
      } catch (error) {
        errors.push(error.message);
      }
    }
    const touched = new Set();
    for (const item of body.records || []) {
      const key = `${item.qrToken}|${emailOf(item.studentEmail)}`;
      try {
        const [sessions] = await pool.query("SELECT * FROM attendance_sessions WHERE qr_token = ?", [item.qrToken]);
        const [students] = await pool.query("SELECT * FROM users WHERE email = ? AND role = 'STUDENT'", [emailOf(item.studentEmail)]);
        if (!sessions.length || !students.length) throw new Error(`Could not save attendance for ${key}.`);
        const course = await loadCourse(sessions[0].course_id);
        const ownCheckIn = req.user.role === "STUDENT" && students[0].id === req.user.id;
        if (!ownCheckIn && !canManage(req.user, course)) throw new Error("You cannot sync this attendance record.");
        const status = clean(item.status).toUpperCase();
        if (!["PRESENT", "LATE", "ABSENT"].includes(status)) throw new Error("Invalid attendance status.");
        await db.mark(sessions[0].id, students[0].id, status, parseTime(item.markedAt) || Date.now());
        touched.add(course.id);
        saved.records.push(key);
      } catch (error) {
        errors.push(error.message);
      }
    }
    for (const courseId of touched) await db.refreshAlerts(courseId);
    const [current] = await db.getPool().query("SELECT * FROM users WHERE id = ?", [req.user.id]);
    res.json({ errors, saved, snapshot: await snapshotFor(current[0]) });
  });

  app.use((error, req, res, next) => {
    console.error(error);
    res.status(500).json({ error: "The server could not complete that request." });
  });

  app.listen(PORT, () => {
    console.log(`CampusTrack API listening on http://localhost:${PORT}`);
  });
}

main().catch((error) => {
  console.error("Could not start CampusTrack API.");
  console.error(error.message);
  console.error("Check server/.env. Copy server/.env.example and set DB_PASSWORD to the MySQL root password.");
  process.exit(1);
});
