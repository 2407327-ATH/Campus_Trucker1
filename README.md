# CampusTrack

Student Attendance & Academic Tracker for Northrise University, Group 1.

Open the project folder in Android Studio. It is a Java app (min SDK 26) with Room on the phone and an optional Node.js + MySQL API.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Student | student@nu.ac.zm | Student@123 |
| Student | student2@nu.ac.zm | Student@123 |
| Lecturer | lecturer@nu.ac.zm | Lecturer@123 |
| Admin | admin@nu.ac.zm | Admin@123 |

Chipo Phiri (`student@nu.ac.zm`) is below 75% in DBS220. James Bwalya is not. Dr. Banda teaches MAD301 and DBS220.

The app works with no server. Attendance is stored in Room on the device and syncs when the API is reachable.

## Run the Android app

1. Install Android Studio and Android SDK 34.
2. Open this folder (`MA_Team_Project`).
3. Let Gradle sync. If it asks for the SDK path, point it at your Android SDK.
4. Select an emulator (API 26 or newer) or a phone.
5. Run the `app` configuration.

The first screen is the splash screen, then login. Use a demo account above.

### Try the main flows

- Student: home shows the overall percentage, course list, and a warning when a course is under 75%. History can be filtered. Alerts explains the low course.
- Lecturer: open a course, start a session, show the QR code, and mark Present / Late / Absent. Reports lists students under 75%.
- Admin: users, all courses, and the same reports.
- Same phone: the camera cannot see a QR code displayed on itself. The lecturer QR dialog shows the text code. Log in as the student and use **Enter attendance code**.
- Offline: turn the network off. Login, marking, and check-in (for a session already on the phone) still work.

## Optional API and MySQL

The API is in `server/`. It implements the Week 2 endpoints under `/api`.

1. Copy `server/.env.example` to `server/.env`.
2. Set `DB_PASSWORD` to the MySQL root password. MySQL 8 must be running on localhost.
3. From `server/`, run `npm install` and `npm start`.
4. The server creates the `campustrack` database, tables, and the same demo accounts.
5. The emulator reaches the computer at `http://10.0.2.2:3000/api/` (set in `ApiClient.java`). A physical phone must use the computer's LAN address instead of `10.0.2.2`.
6. Log in while the API is running, then use **Sync** on the toolbar or profile screen.

Passwords are bcrypt hashes in MySQL and salted SHA-256 hashes on the phone. A wrong password from a live server is rejected and is not treated as offline.

## Tests already run

- `gradlew :app:testDebugUnitTest` — 7 tests passed (percentage, 75% cutoff, alert text, QR payload, password hash, validation).
- `gradlew :app:assembleDebug` — debug APK built.
- `npm test` in `server/` — 2 tests passed for the same alert text and late rule.
- Pixel 5 emulator: the APK installed, splash opened, and the login screen showed CampusTrack, email, password, and Log in.

The instrumented login test did not finish. The emulator already had another build of this package at version code 3, then System UI stopped responding. If Android Studio reports a version downgrade, uninstall the old CampusTrack app first. The MySQL API was not started here because the local root password is not in the project. Set it in `server/.env` before `npm start`.

## What was left out

- No public hosting, Docker image, or signed release APK. Build a debug APK with `gradlew :app:assembleDebug`.
- Low attendance is an in-app alert, not a push notification.
- The attendance minimum is 75%. The documents did not give a number.
- Admin is included because the team design has three roles. The brief's required roles are student and lecturer.
