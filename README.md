# SWAYAM NPTEL Assignment Manager

A complete full-stack web application built with **Java 17 + Spring Boot 3** to help college students manage their SWAYAM NPTEL courses, track assignment deadlines, receive automated reminders, and monitor examination registration details.

---

## 🚀 Quick Start (Zero Configuration)

The application uses **H2 embedded database by default** — no MySQL installation required to run immediately.

### One-Click Windows Launchers (Easiest)
- **Start Application**: Double-click [`run.bat`](run.bat) (or execute `.\run.bat` in PowerShell/CMD)
- **Run All Tests**: Double-click [`run-tests.bat`](run-tests.bat)

### Prerequisites
- **Java 17** (already installed — confirmed at `C:\Program Files\Java\jdk-17`)
- **Maven** (bundled in local scratch directory or via `mvn.cmd`)

### Step 1 — Open in Browser
Once started, access the application at:
```
http://localhost:8080
```

---

## 📚 Academic Project Documentation & Guides

- **[Project Report & Viva Q&A Guide](PROJECT_DOCUMENTATION.md)**: System architecture diagrams, ER diagrams, deduplication algorithms, and Viva exam answers.
- **[5-Minute Demo & Presentation Guide](PRESENTATION_GUIDE.md)**: Slide outline, step-by-step live demo walkthrough, and examiner talking points.
- **[MySQL Schema DDL](schema_mysql.sql)** & **[Sample Data DML](sample_data.sql)**: Standalone SQL scripts for MySQL Workbench.

---

## 🔑 Demo Accounts

| Role    | Email                       | Password    |
|---------|-----------------------------|-------------|
| Student | student@demo.com            | Student@123 |
| Admin   | admin@nptelmanager.com      | Admin@123   |

These accounts are **created automatically** on first startup.

---

## 📁 Project Structure

```
nptel-manager/
├── src/
│   ├── main/
│   │   ├── java/com/nptelmanager/
│   │   │   ├── NptelManagerApplication.java   ← Main entry point
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java         ← Spring Security
│   │   │   │   ├── DataInitializer.java        ← Demo data seeder
│   │   │   │   └── MailConfig.java             ← Email config
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java         ← Login/Register
│   │   │   │   ├── DashboardController.java    ← Dashboard
│   │   │   │   ├── CourseController.java       ← Course management
│   │   │   │   ├── AssignmentController.java   ← Assignment management
│   │   │   │   ├── ExaminationController.java  ← Exam management
│   │   │   │   ├── NotificationController.java ← Notifications
│   │   │   │   ├── CalendarController.java     ← Calendar
│   │   │   │   ├── SettingsController.java     ← User settings
│   │   │   │   └── AdminController.java        ← Admin panel
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── Course.java
│   │   │   │   ├── StudentCourse.java
│   │   │   │   ├── Assignment.java
│   │   │   │   ├── StudentAssignment.java
│   │   │   │   ├── Examination.java
│   │   │   │   ├── ExaminationRegistration.java
│   │   │   │   └── Notification.java
│   │   │   ├── repository/            ← Spring Data JPA repositories
│   │   │   ├── service/               ← Business logic layer
│   │   │   ├── scheduler/
│   │   │   │   └── ReminderSchedulerService.java ← Hourly reminders
│   │   │   ├── security/
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.properties          ← Main config (H2 default)
│   │       ├── application-mysql.properties    ← MySQL profile
│   │       ├── templates/                      ← Thymeleaf HTML templates
│   │       └── static/                         ← CSS, JS
│   └── test/
│       └── java/com/nptelmanager/
│           └── NptelManagerApplicationTests.java ← 18 tests
├── pom.xml
└── mvnw.cmd
```

---

## 🗄️ Database Options

### Default: H2 Embedded (works out of the box)
The app creates a file-based H2 database at `./data/npteldb` automatically.

Access H2 console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/npteldb`
- Username: `sa`, Password: *(empty)*

### MySQL (optional)
1. Install MySQL and create a database (the app will create tables automatically)
2. Edit `application-mysql.properties`:
   ```properties
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```
3. Run with MySQL profile:
   ```bash
   mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

---

## 📧 Email Notifications (Optional)

Email notifications are **disabled by default**. In-app notifications work without configuration.

To enable email:
1. Edit `src/main/resources/application.properties`:
   ```properties
   spring.mail.enabled=true
   spring.mail.username=your-gmail@gmail.com
   spring.mail.password=your-16-char-app-password
   ```
2. In Gmail: **Google Account → Security → 2-Step Verification → App Passwords**
3. Generate an App Password for "Mail"

> ⚠️ Never commit real credentials. Use environment variables or `.env` files in production.

---

## 🧪 Running Tests

```bash
mvnw.cmd test
```

**18 tests** covering:
- User registration and authentication
- Password hashing (BCrypt)
- Duplicate email prevention
- Course creation and enrollment
- Duplicate enrollment prevention
- Assignment creation and completion
- Overdue assignment detection
- Completed assignment not marked overdue
- Duplicate notification prevention
- Mark notification as read
- Examination registration status
- Mark exam as registered
- Fee payment status
- Student data access restriction
- Profile update
- Password change validation

---

## 🌐 Application Pages

| URL | Description |
|-----|-------------|
| `/auth/login` | Login page |
| `/auth/register` | Student registration |
| `/dashboard` | Main dashboard with stats |
| `/courses` | View and manage courses |
| `/courses/add` | Add a new course |
| `/assignments` | View all assignments |
| `/assignments/add` | Add a new assignment |
| `/examinations` | View examinations |
| `/examinations/{id}` | Exam detail and registration |
| `/calendar` | Calendar with all events |
| `/notifications` | All notifications |
| `/settings` | Profile and password settings |
| `/admin` | Admin panel (admin only) |
| `/h2-console` | H2 database console |

---

## 📋 Module-by-Module Test Workflow

### 1. Authentication
1. Visit `http://localhost:8080` → redirects to login
2. Login with `student@demo.com / Student@123`
3. Logout and register a new account at `/auth/register`

### 2. Course Management
1. Go to **My Courses** → see 2 demo courses (Java + DSA)
2. Click **Enroll** on available admin courses
3. Click **Add Course** to add a custom course

### 3. Assignment Management
1. Go to **Assignments** → see pre-loaded assignments
2. Filter by course, status, or search by title
3. Click ✓ (green) to mark an assignment complete
4. Click ▶ (yellow) to mark as "In Progress"
5. Click **Add Assignment** to add manually

### 4. Notifications
1. Go to **Notifications** → see reminder notifications
2. Click "Mark Read" on individual notifications
3. Click "Mark All Read" to clear all

### 5. Examinations
1. Go to **Examinations** → see the Java Programming exam
2. Click **Details** to view exam center, fee, timings
3. Click ✓ button to mark as Registered
4. Click 💳 button to mark fee as Paid
5. Click **Track** to add any exam to your tracker

### 6. Calendar
1. Go to **Calendar** → see all events color-coded
2. Assignment deadlines shown in grey/yellow/green/red
3. Registration dates in cyan, exam dates in red

### 7. Admin Panel
1. Logout and login as `admin@nptelmanager.com / Admin@123`
2. Go to **Admin Panel**
3. Add a new course — it appears for all students
4. Add an assignment — it's automatically assigned to enrolled students
5. Add examination details

---

## ⚙️ Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | HTTP port |
| `spring.mail.enabled` | false | Enable email notifications |
| `spring.h2.console.enabled` | true | Enable H2 web console |
| `spring.jpa.hibernate.ddl-auto` | update | Auto-create DB tables |
| `app.notification.reminder-days` | 7,3,1,0 | Reminder intervals (days before deadline) |

---

## 🛠️ Running in IntelliJ IDEA

1. File → Open → Select the `nptel-manager` folder
2. IntelliJ auto-detects Maven project
3. Wait for indexing to complete
4. Open `NptelManagerApplication.java`
5. Click the green ▶ Run button
6. Visit `http://localhost:8080`

## 🛠️ Running in VS Code

1. Install extensions: **Extension Pack for Java**, **Spring Boot Extension Pack**
2. File → Open Folder → Select `nptel-manager`
3. VS Code detects Spring Boot project automatically
4. Press `F5` or click **Run** → **Run Without Debugging**
5. Visit `http://localhost:8080`

---

## 📌 Important Notes

- **Sample data** is labeled `[SAMPLE DATA]` — do not use it for actual exam registration
- All exam details (dates, fees, centers) must be verified at **https://onlinecourses.nptel.ac.in**
- The application does **not** connect to NPTEL APIs — all data is manually entered
- Reminders run **every hour** automatically after app startup
- H2 database persists to `./data/npteldb.mv.db` — delete this file to reset all data

---

## 📞 Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + BCrypt |
| Database | H2 (default) / MySQL (optional) |
| ORM | Spring Data JPA + Hibernate |
| Templates | Thymeleaf |
| CSS | Bootstrap 5.3 |
| Charts | Chart.js |
| Calendar | FullCalendar.js |
| Icons | Bootstrap Icons |
| Scheduler | Spring @Scheduled |
| Build | Maven 3.9 |
| Tests | JUnit 5 + Spring Test |
