package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.common.constants.PackageErrorMessages;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.entity.customer.PackageSession;
import com.asaf.appointment_service.entity.customer.PackageSessionStatus;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.customer.PackageSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackageSessionService {

    private final PackageSessionRepository sessionRepository;
    private final CustomerPackageRepository packageRepository;

    public PackageSessionService(PackageSessionRepository sessionRepository,
                                 CustomerPackageRepository packageRepository) {
        this.sessionRepository = sessionRepository;
        this.packageRepository = packageRepository;
    }

    @Transactional
    public void createSessionsForPackage(CustomerPackage customerPackage) {
        List<PackageSession> sessions = IntStream.rangeClosed(1, customerPackage.getTotalSessions())
            .mapToObj(i -> PackageSession.builder()
                .customerPackageId(customerPackage.getId())
                .sessionNumber(i)
                .status(PackageSessionStatus.PENDING)
                .build())
            .toList();
        sessionRepository.saveAll(sessions);
    }

    @Transactional(readOnly = true)
    public List<PackageSession> listByPackage(Long customerPackageId) {
        return sessionRepository.findByCustomerPackageIdOrderBySessionNumberAsc(customerPackageId);
    }

    @Transactional(readOnly = true)
    public Optional<PackageSession> findFirstPendingSession(Long customerPackageId) {
        return sessionRepository.findFirstByCustomerPackageIdAndStatusOrderBySessionNumberAsc(
            customerPackageId, PackageSessionStatus.PENDING);
    }

    @Transactional
    public PackageSession assignAppointment(Long sessionId, Long appointmentId, LocalDateTime scheduledAt) {
        PackageSession session = sessionRepository.findByIdWithLock(sessionId)
            .orElseThrow(() -> new IllegalArgumentException(PackageErrorMessages.SESSION_NOT_FOUND + sessionId));
        if (session.getStatus() != PackageSessionStatus.PENDING) {
            throw new IllegalStateException(PackageErrorMessages.SESSION_NOT_PENDING);
        }
        session.assignAppointment(appointmentId, scheduledAt);
        return sessionRepository.save(session);
    }

    @Transactional
    public PackageSession unassignAppointment(Long sessionId) {
        PackageSession session = findByIdOrThrow(sessionId);
        if (session.getStatus() != PackageSessionStatus.SCHEDULED) {
            throw new IllegalStateException(PackageErrorMessages.SESSION_NOT_SCHEDULED);
        }
        session.unassignAppointment();
        return sessionRepository.save(session);
    }

    @Transactional
    public void markCompleted(Long sessionId) {
        PackageSession session = findByIdOrThrow(sessionId);
        session.markCompleted();
        sessionRepository.save(session);
        CustomerPackage customerPackage = packageRepository.findById(session.getCustomerPackageId())
            .orElseThrow(() -> new IllegalStateException(PackageErrorMessages.PACKAGE_RECORD_NOT_FOUND));
        if (customerPackage.hasRemainingSessions()) {
            customerPackage.deductSession();
            packageRepository.save(customerPackage);
        }
    }

    @Transactional
    public void markNoShow(Long sessionId) {
        PackageSession session = findByIdOrThrow(sessionId);
        session.markNoShow();
        sessionRepository.save(session);
        CustomerPackage customerPackage = packageRepository.findById(session.getCustomerPackageId())
            .orElseThrow(() -> new IllegalStateException(PackageErrorMessages.PACKAGE_RECORD_NOT_FOUND));
        if (customerPackage.hasRemainingSessions()) {
            customerPackage.deductSession();
            packageRepository.save(customerPackage);
        }
    }

    public PackageSession findByIdOrThrow(Long id) {
        return sessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(PackageErrorMessages.SESSION_NOT_FOUND + id));
    }
}
