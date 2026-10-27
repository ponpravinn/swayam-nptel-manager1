package com.nptelmanager.service;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service for importing CSV data for courses, assignments, and examinations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CsvImportService {

    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentAssignmentRepository studentAssignmentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final ExaminationRepository examinationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ───────────────────────────────────────────────────────────────
    // COURSE CSV
    // ───────────────────────────────────────────────────────────────
    /**
     * Import courses from CSV.
     * Expected columns: courseName, courseCode, instructor, duration, startDate, endDate, semester, description, courseUrl
     */
    public CsvImportResult importCourses(MultipartFile file) {
        CsvImportResult result = new CsvImportResult("Courses");
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] header = reader.readNext(); // skip header
            String[] row;
            int lineNum = 1;
            while ((row = reader.readNext()) != null) {
                lineNum++;
                try {
                    if (row.length < 2) continue;
                    Course course = new Course();
                    course.setCourseName(safe(row, 0));
                    course.setCourseCode(safe(row, 1));
                    course.setInstructor(safe(row, 2));
                    course.setDuration(safe(row, 3));
                    course.setStartDate(parseDate(safe(row, 4)));
                    course.setEndDate(parseDate(safe(row, 5)));
                    course.setSemester(safe(row, 6));
                    course.setDescription(safe(row, 7));
                    course.setCourseUrl(safe(row, 8));
                    course.setAdminCreated(true);

                    if (course.getCourseName() == null || course.getCourseName().isBlank()) {
                        result.addError(lineNum, "Course name is required");
                        continue;
                    }

                    // Check for duplicate course code
                    if (course.getCourseCode() != null && !course.getCourseCode().isBlank()
                            && courseRepository.existsByCourseCode(course.getCourseCode())) {
                        result.addWarning(lineNum, "Course code already exists, skipping: " + course.getCourseCode());
                        continue;
                    }

                    courseRepository.save(course);
                    result.incrementSuccess();
                } catch (Exception e) {
                    result.addError(lineNum, e.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError(0, "Failed to read CSV: " + e.getMessage());
        }
        return result;
    }

    // ───────────────────────────────────────────────────────────────
    // ASSIGNMENT CSV
    // ───────────────────────────────────────────────────────────────
    /**
     * Import assignments from CSV.
     * Expected columns: courseCode, title, assignmentNumber, description, releaseDate, dueDate, submissionUrl
     */
    public CsvImportResult importAssignments(MultipartFile file) {
        CsvImportResult result = new CsvImportResult("Assignments");
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            reader.readNext(); // skip header
            String[] row;
            int lineNum = 1;
            while ((row = reader.readNext()) != null) {
                lineNum++;
                try {
                    String courseCode = safe(row, 0);
                    if (courseCode == null || courseCode.isBlank()) {
                        result.addError(lineNum, "courseCode is required");
                        continue;
                    }
                    Optional<Course> courseOpt = courseRepository.findByCourseCode(courseCode);
                    if (courseOpt.isEmpty()) {
                        result.addError(lineNum, "Course not found for code: " + courseCode);
                        continue;
                    }

                    Assignment a = new Assignment();
                    a.setCourse(courseOpt.get());
                    a.setTitle(safe(row, 1));
                    a.setAssignmentNumber(parseInt(safe(row, 2)));
                    a.setDescription(safe(row, 3));
                    a.setReleaseDate(parseDate(safe(row, 4)));
                    a.setDueDate(parseDateTime(safe(row, 5)));
                    a.setSubmissionUrl(safe(row, 6));
                    a.setAdminCreated(true);

                    if (a.getTitle() == null || a.getTitle().isBlank()) {
                        result.addError(lineNum, "Assignment title is required");
                        continue;
                    }

                    Assignment saved = assignmentRepository.save(a);

                    // Create StudentAssignment for all enrolled students
                    List<StudentCourse> enrolled = studentCourseRepository.findAll().stream()
                        .filter(sc -> sc.getCourse().getId().equals(courseOpt.get().getId()))
                        .toList();
                    for (StudentCourse sc : enrolled) {
                        if (studentAssignmentRepository
                                .findByStudentIdAndAssignmentId(sc.getStudent().getId(), saved.getId())
                                .isEmpty()) {
                            studentAssignmentRepository.save(StudentAssignment.builder()
                                .student(sc.getStudent())
                                .assignment(saved)
                                .status(StudentAssignment.AssignmentStatus.NOT_STARTED)
                                .build());
                        }
                    }
                    result.incrementSuccess();
                } catch (Exception e) {
                    result.addError(lineNum, e.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError(0, "Failed to read CSV: " + e.getMessage());
        }
        return result;
    }

    // ───────────────────────────────────────────────────────────────
    // EXAMINATION CSV
    // ───────────────────────────────────────────────────────────────
    /**
     * Import examinations from CSV.
     * Expected columns: courseCode, examSession, registrationOpenDate, registrationCloseDate,
     *                   registrationFee, examDate, examMode, examCenterName, city, importantInstructions
     */
    public CsvImportResult importExaminations(MultipartFile file) {
        CsvImportResult result = new CsvImportResult("Examinations");
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            reader.readNext(); // skip header
            String[] row;
            int lineNum = 1;
            while ((row = reader.readNext()) != null) {
                lineNum++;
                try {
                    String courseCode = safe(row, 0);
                    Optional<Course> courseOpt = courseRepository.findByCourseCode(courseCode);
                    if (courseOpt.isEmpty()) {
                        result.addError(lineNum, "Course not found: " + courseCode);
                        continue;
                    }

                    Examination exam = new Examination();
                    exam.setCourse(courseOpt.get());
                    exam.setExamSession(safe(row, 1));
                    exam.setRegistrationOpenDate(parseDate(safe(row, 2)));
                    exam.setRegistrationCloseDate(parseDate(safe(row, 3)));
                    String fee = safe(row, 4);
                    if (fee != null && !fee.isBlank()) {
                        try { exam.setRegistrationFee(new BigDecimal(fee.replaceAll("[^\\d.]", ""))); }
                        catch (NumberFormatException ignored) {}
                    }
                    exam.setExamDate(parseDate(safe(row, 5)));
                    exam.setExamMode(safe(row, 6));
                    exam.setExamCenterName(safe(row, 7));
                    exam.setCity(safe(row, 8));
                    exam.setImportantInstructions(safe(row, 9));
                    exam.setPublished(true);

                    examinationRepository.save(exam);
                    result.incrementSuccess();
                } catch (Exception e) {
                    result.addError(lineNum, e.getMessage());
                }
            }
        } catch (Exception e) {
            result.addError(0, "Failed to read CSV: " + e.getMessage());
        }
        return result;
    }

    // ───────────────────────────────────────────────────────────────
    // CSV TEMPLATE GENERATORS
    // ───────────────────────────────────────────────────────────────
    public String getCoursesCsvTemplate() {
        return "courseName,courseCode,instructor,duration,startDate,endDate,semester,description,courseUrl\n" +
               "Introduction to Programming,CS001,Prof. Smith,12 weeks,2024-01-15,2024-04-15,Semester 1," +
               "Learn programming basics,https://nptel.ac.in/courses/106\n";
    }

    public String getAssignmentsCsvTemplate() {
        return "courseCode,title,assignmentNumber,description,releaseDate,dueDate,submissionUrl\n" +
               "CS001,Week 1 Quiz,1,Complete all questions,2024-01-20 10:00,2024-01-27 23:59," +
               "https://onlinecourses.nptel.ac.in\n";
    }

    public String getExaminationsCsvTemplate() {
        return "courseCode,examSession,registrationOpenDate,registrationCloseDate,registrationFee," +
               "examDate,examMode,examCenterName,city,importantInstructions\n" +
               "CS001,Jan-Apr 2024,2024-03-01,2024-03-15,1000,2024-04-27,Online,NPTEL Center,Chennai," +
               "Bring Hall ticket and valid ID\n";
    }

    // ───────────────────────────────────────────────────────────────
    // HELPERS
    // ───────────────────────────────────────────────────────────────
    private String safe(String[] row, int idx) {
        if (row == null || idx >= row.length) return null;
        String v = row[idx].trim();
        return v.isEmpty() ? null : v;
    }

    private LocalDate parseDate(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s, DATE_FMT); } catch (DateTimeParseException e) { return null; }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null) return null;
        try { return LocalDateTime.parse(s, DATE_TIME_FMT); } catch (DateTimeParseException e1) {
            try { return LocalDate.parse(s, DATE_FMT).atStartOfDay(); } catch (DateTimeParseException e2) { return null; }
        }
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    // ───────────────────────────────────────────────────────────────
    // RESULT DTO
    // ───────────────────────────────────────────────────────────────
    public static class CsvImportResult {
        private final String type;
        private int successCount = 0;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public CsvImportResult(String type) { this.type = type; }
        public void incrementSuccess() { successCount++; }
        public void addError(int line, String msg) { errors.add("Line " + line + ": " + msg); }
        public void addWarning(int line, String msg) { warnings.add("Line " + line + ": " + msg); }

        public String getType() { return type; }
        public int getSuccessCount() { return successCount; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public boolean hasErrors() { return !errors.isEmpty(); }
        public String getSummary() {
            return successCount + " " + type + " imported successfully" +
                   (errors.isEmpty() ? "" : ", " + errors.size() + " error(s)") +
                   (warnings.isEmpty() ? "" : ", " + warnings.size() + " warning(s)");
        }
    }
}
