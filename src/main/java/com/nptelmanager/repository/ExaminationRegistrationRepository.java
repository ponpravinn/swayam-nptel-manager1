package com.nptelmanager.repository;

import com.nptelmanager.entity.ExaminationRegistration;
import com.nptelmanager.entity.User;
import com.nptelmanager.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExaminationRegistrationRepository extends JpaRepository<ExaminationRegistration, Long> {

    Optional<ExaminationRegistration> findByStudentAndExamination(User student, Examination examination);
    Optional<ExaminationRegistration> findByStudentIdAndExaminationId(Long studentId, Long examinationId);
    List<ExaminationRegistration> findByStudentId(Long studentId);

    @Query("SELECT er FROM ExaminationRegistration er JOIN FETCH er.examination e JOIN FETCH e.course " +
           "WHERE er.student.id = :studentId ORDER BY e.examDate ASC")
    List<ExaminationRegistration> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT er FROM ExaminationRegistration er WHERE er.student.id = :studentId " +
           "AND er.registrationStatus <> 'REGISTERED' " +
           "AND er.examination.registrationCloseDate >= CURRENT_DATE")
    List<ExaminationRegistration> findUpcomingUnregisteredByStudentId(@Param("studentId") Long studentId);
}
