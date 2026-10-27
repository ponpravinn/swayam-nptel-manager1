package com.nptelmanager.service;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationRegistrationRepository examRegRepo;

    @Transactional(readOnly = true)
    public List<Examination> getAllPublished() {
        return examinationRepository.findAllPublished();
    }

    @Transactional(readOnly = true)
    public Optional<Examination> findById(Long id) {
        return examinationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ExaminationRegistration> getStudentRegistrations(User student) {
        return examRegRepo.findByStudentIdWithDetails(student.getId());
    }

    @Transactional(readOnly = true)
    public Optional<ExaminationRegistration> getStudentRegistration(User student, Long examId) {
        return examRegRepo.findByStudentIdAndExaminationId(student.getId(), examId);
    }

    /**
     * Enroll/link student to an examination (creates ExaminationRegistration record).
     */
    public ExaminationRegistration enrollStudentInExam(User student, Long examId) {
        Examination exam = examinationRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Examination not found"));

        Optional<ExaminationRegistration> existing = examRegRepo.findByStudentAndExamination(student, exam);
        if (existing.isPresent()) return existing.get();

        ExaminationRegistration reg = ExaminationRegistration.builder()
            .student(student)
            .examination(exam)
            .registrationStatus(computeAutoStatus(exam))
            .paymentStatus(ExaminationRegistration.PaymentStatus.NOT_PAID)
            .build();
        return examRegRepo.save(reg);
    }

    public ExaminationRegistration updateRegistrationStatus(User student, Long examId,
                                                             ExaminationRegistration.RegistrationStatus status) {
        ExaminationRegistration reg = examRegRepo
            .findByStudentIdAndExaminationId(student.getId(), examId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));
        reg.setRegistrationStatus(status);
        if (status == ExaminationRegistration.RegistrationStatus.REGISTERED) {
            reg.setRegisteredAt(LocalDateTime.now());
        }
        return examRegRepo.save(reg);
    }

    public ExaminationRegistration updatePaymentStatus(User student, Long examId,
                                                       ExaminationRegistration.PaymentStatus paymentStatus) {
        ExaminationRegistration reg = examRegRepo
            .findByStudentIdAndExaminationId(student.getId(), examId)
            .orElseThrow(() -> new RuntimeException("Registration not found"));
        reg.setPaymentStatus(paymentStatus);
        if (paymentStatus == ExaminationRegistration.PaymentStatus.PAID) {
            reg.setPaymentConfirmedAt(LocalDateTime.now());
        }
        return examRegRepo.save(reg);
    }

    public Examination saveExamination(Examination examination) {
        return examinationRepository.save(examination);
    }

    public void deleteExamination(Long id) {
        examinationRepository.deleteById(id);
    }

    /**
     * Auto-sync registration statuses for all unregistered student registrations.
     */
    public void syncRegistrationStatuses() {
        List<ExaminationRegistration> all = examRegRepo.findAll();
        for (ExaminationRegistration reg : all) {
            if (reg.getRegistrationStatus() != ExaminationRegistration.RegistrationStatus.REGISTERED) {
                ExaminationRegistration.RegistrationStatus computed = computeAutoStatus(reg.getExamination());
                if (computed != reg.getRegistrationStatus()) {
                    reg.setRegistrationStatus(computed);
                    examRegRepo.save(reg);
                }
            }
        }
    }

    private ExaminationRegistration.RegistrationStatus computeAutoStatus(Examination exam) {
        LocalDate today = LocalDate.now();
        if (exam.getRegistrationOpenDate() == null) {
            return ExaminationRegistration.RegistrationStatus.NOT_OPENED;
        }
        if (today.isBefore(exam.getRegistrationOpenDate())) {
            return ExaminationRegistration.RegistrationStatus.NOT_OPENED;
        }
        if (exam.getRegistrationCloseDate() != null && today.isAfter(exam.getRegistrationCloseDate())) {
            return ExaminationRegistration.RegistrationStatus.REGISTRATION_CLOSED;
        }
        return ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN;
    }
}
