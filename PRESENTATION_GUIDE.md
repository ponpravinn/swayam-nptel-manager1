# SWAYAM NPTEL Assignment Manager — 5-Minute Demo & Presentation Script

This guide provides a structured presentation outline, live demo walkthrough steps, and talking points tailored for your college project evaluation.

---

## 1. Project Overview & Slide Outline (1 Minute)

### Slide 1: Title & Problem Statement
* **Project Name**: SWAYAM NPTEL Assignment Manager
* **Domain**: Educational Technology & Academic Workflow Automation
* **Problem**: College students taking NPTEL MOOC courses often miss weekly assignment submission deadlines, fail to track exam registration windows, and lack a centralized overview of their progress across multiple courses.

### Slide 2: Proposed Solution
* A centralized platform with role-based access control (Student & Administrator).
* Automated deadline tracking with an intelligent, non-intrusive reminder engine.
* Full examination tracker (registration windows, fee status, hall ticket instructions, Google Maps venue navigation).
* Interactive Academic Calendar with color-coded course events.
* Administrative management with bulk CSV data ingestion.

### Slide 3: Architecture & Tech Stack
* **Backend**: Java 17, Spring Boot 3.2, Spring MVC, Spring Data JPA, Spring Security, Spring Scheduler.
* **Frontend**: Thymeleaf, Bootstrap 5.3, Chart.js, FullCalendar.js.
* **Database**: H2 File Database (Embedded) & MySQL (Production Profile).

---

## 2. Live Demonstration Steps (3 Minutes)

### Step 1: Student Authentication & Dashboard Overview (45 Seconds)
1. Navigate to `http://localhost:8080/auth/login`.
2. Login with student credentials (`student@demo.com` / `Student@123`).
3. **Showcase**:
   - Dynamic Summary Metric Cards (Total Courses, Completed, Incomplete, Overdue, Upcoming Exams).
   - Assignment Completion Progress Bar.
   - Dual Visualizations: Doughnut status chart + Course-wise progress bar chart.
   - Priority Deadline Sections: "Due Today" (urgent action badge) and "Due in Next 7 Days".

### Step 2: Assignment Lifecycle & CSV Export (45 Seconds)
1. Click **Assignments** in the sidebar.
2. Filter assignments by course or search by title.
3. Mark an assignment as **Completed** with sample marks (e.g., `92.5`).
4. Click **Export CSV** to demonstrate instant client-side data report generation (`my_nptel_assignments.csv`).

### Step 3: Examination Tracker & Calendar (45 Seconds)
1. Click **Examinations** in the sidebar.
2. Open an exam detail view: highlight registration open/close countdown, fee status, and the Google Maps navigation button for the exam center.
3. Click **Calendar** in the sidebar: demonstrate monthly, weekly, and list views of all deadlines and exam dates color-coded by event type.

### Step 4: Administrator Portal & Bulk CSV Ingestion (45 Seconds)
1. Logout and log in as Administrator (`admin@nptelmanager.com` / `Admin@123`).
2. Open **Admin Panel** → click **CSV Import**.
3. Download a sample CSV template and demonstrate how an institution admin can bulk-upload semester courses, assignments, and exam schedules with one click.
4. Open **User Management** to demonstrate instant user account enable/disable moderation.

---

## 3. High-Scoring Talking Points for Evaluators

1. **Robust Deduplication Logic**: Explain that scheduled notification jobs generate a deterministic key (`ASSIGN-{userId}-{assignmentId}-D{days}`) stored in the database, ensuring zero duplicate alerts even if the server restarts.
2. **Database Portability**: Highlight the repository abstraction using Spring Data JPA, allowing seamless switching between embedded H2 for development and MySQL for production without rewriting queries.
3. **Data Isolation**: Emphasize that all student data queries are scoped to the authenticated security principal (`UserDetails`), preventing unauthorized horizontal privilege escalation.
