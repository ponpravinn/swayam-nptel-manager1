package com.nptelmanager.repository;

import com.nptelmanager.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long> {

    List<Examination> findByCourseId(Long courseId);

    @Query("SELECT e FROM Examination e WHERE e.isPublished = true ORDER BY e.examDate ASC")
    List<Examination> findAllPublished();

    @Query("SELECT e FROM Examination e WHERE e.examDate BETWEEN :start AND :end AND e.isPublished = true ORDER BY e.examDate ASC")
    List<Examination> findExamsBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e FROM Examination e WHERE e.registrationCloseDate BETWEEN :start AND :end AND e.isPublished = true ORDER BY e.registrationCloseDate ASC")
    List<Examination> findRegistrationsClosingBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
