package com.nptelmanager.service;

import com.nptelmanager.entity.Course;
import com.nptelmanager.entity.StudentAssignment;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Implements the official SWAYAM NPTEL grading and certification calculation rules:
 * - 12-week course: Average of best 8 out of 12 assignments (25% weightage).
 * - 8-week course:  Average of best 6 out of 8 assignments (25% weightage).
 * - 4-week course:  Average of best 3 out of 4 assignments (25% weightage).
 * - Proctored End-Term Exam: 75% weightage.
 * - Passing criteria:
 *   1. Assignment score >= 10 / 25 (40%)
 *   2. Proctored Exam score >= 30 / 75 (40%)
 *   3. Final score (Assignment + Exam) >= 40 / 100
 */
@Service
public class NptelScoringService {

    @Data
    @Builder
    public static class NptelScoreSummary {
        private String courseName;
        private String courseCode;
        private int totalWeeks;
        private int bestN;
        private int submittedCount;
        private double assignmentScoreOutOf25;
        private double assignmentPercentage;
        private boolean assignmentCriteriaMet; // >= 10 / 25
        private double minExamScoreToPass;    // Minimum out of 75 needed for overall >= 40
        private double minExamScoreForElite;   // Minimum out of 75 for overall >= 60
        private double minExamScoreForSilver;  // Minimum out of 75 for overall >= 75
        private double minExamScoreForGold;    // Minimum out of 75 for overall >= 90
        private String certificateEligibilityVerdict;
        private String medalTier;
    }

    public NptelScoreSummary calculateScore(Course course, List<StudentAssignment> studentAssignments, Double hypotheticalExamScore) {
        int durationWeeks = parseWeeks(course != null ? course.getDuration() : "12 Weeks");
        int bestN = getBestN(durationWeeks);

        // Filter assignments with scores
        List<Double> scores = studentAssignments.stream()
                .map(StudentAssignment::getMarksObtained)
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .toList();

        int submittedCount = scores.size();

        // Calculate average of best N (or submitted so far)
        double totalSum = 0;
        int countToUse = Math.min(bestN, scores.size());
        for (int i = 0; i < countToUse; i++) {
            totalSum += scores.get(i);
        }

        // Assignment score scaled to 25 marks
        double assignmentAvg100 = bestN > 0 ? (totalSum / bestN) : 0.0;
        double assignmentScore25 = Math.round((assignmentAvg100 * 0.25) * 100.0) / 100.0;
        boolean assignmentCriteriaMet = assignmentScore25 >= 10.0;

        // Exam score calculations (out of 75)
        // Passing rule: Exam >= 30 AND (Assignment + Exam) >= 40
        double minExamToPass = Math.max(30.0, Math.round((40.0 - assignmentScore25) * 10.0) / 10.0);
        double minExamForElite = Math.min(75.0, Math.max(30.0, Math.round((60.0 - assignmentScore25) * 10.0) / 10.0));
        double minExamForSilver = Math.min(75.0, Math.max(30.0, Math.round((75.0 - assignmentScore25) * 10.0) / 10.0));
        double minExamForGold = Math.min(75.0, Math.max(30.0, Math.round((90.0 - assignmentScore25) * 10.0) / 10.0));

        // Evaluate hypothetical exam score if provided
        String medal = "Pending Exam";
        String verdict;
        if (hypotheticalExamScore != null) {
            double totalFinal = assignmentScore25 + hypotheticalExamScore;
            if (hypotheticalExamScore < 30.0) {
                verdict = "Failed (Exam score below 30/75 threshold)";
                medal = "No Certificate";
            } else if (!assignmentCriteriaMet) {
                verdict = "Failed (Assignment score below 10/25 threshold)";
                medal = "No Certificate";
            } else if (totalFinal >= 90.0) {
                verdict = "Certified with Elite + Gold Medal";
                medal = "Elite + Gold (Top Tier)";
            } else if (totalFinal >= 75.0) {
                verdict = "Certified with Elite + Silver Medal";
                medal = "Elite + Silver";
            } else if (totalFinal >= 60.0) {
                verdict = "Certified with Elite";
                medal = "Elite";
            } else if (totalFinal >= 40.0) {
                verdict = "Successfully Completed";
                medal = "Pass Certificate";
            } else {
                verdict = "Failed (Total score below 40%)";
                medal = "No Certificate";
            }
        } else {
            if (assignmentCriteriaMet) {
                verdict = "On Track! Assignment criteria satisfied (>= 10/25). Score >= " + minExamToPass + "/75 on final exam to get certified.";
            } else {
                verdict = "In Progress. Need " + (bestN - countToUse) + " more assignments or higher marks to reach the 10/25 assignment threshold.";
            }
        }

        return NptelScoreSummary.builder()
                .courseName(course != null ? course.getCourseName() : "NPTEL Course")
                .courseCode(course != null ? course.getCourseCode() : "noc")
                .totalWeeks(durationWeeks)
                .bestN(bestN)
                .submittedCount(submittedCount)
                .assignmentScoreOutOf25(assignmentScore25)
                .assignmentPercentage(Math.round(assignmentAvg100 * 10.0) / 10.0)
                .assignmentCriteriaMet(assignmentCriteriaMet)
                .minExamScoreToPass(minExamToPass)
                .minExamScoreForElite(minExamForElite)
                .minExamScoreForSilver(minExamForSilver)
                .minExamScoreForGold(minExamForGold)
                .certificateEligibilityVerdict(verdict)
                .medalTier(medal)
                .build();
    }

    public int parseWeeks(String duration) {
        if (duration == null) return 12;
        if (duration.contains("4")) return 4;
        if (duration.contains("8")) return 8;
        return 12;
    }

    public int getBestN(int totalWeeks) {
        if (totalWeeks <= 4) return 3;
        if (totalWeeks <= 8) return 6;
        return 8;
    }
}
