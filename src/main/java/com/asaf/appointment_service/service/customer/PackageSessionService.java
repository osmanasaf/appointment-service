package com.asaf.appointment_service.service.customer;

import com.asaf.appointment_service.entity.customer.PackageSession;
import com.asaf.appointment_service.entity.customer.PackageSessionStatus;
import com.asaf.appointment_service.entity.customer.CustomerPackage;
import com.asaf.appointment_service.repository.customer.CustomerPackageRepository;
import com.asaf.appointment_service.repository.customer.PackageSessionRepository;
import java.util.List;
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
        for (int i = 1; i <= customerPackage.getTotalSessions(); i++) {
            PackageSession session = PackageSession.builder()
                .customerPackageId(customerPackage.getId())
                .sessionNumber(i)
                .status(PackageSessionStatus.PENDING)
                .build();
            sessionRepository.save(session);
        }
    }

    @Transactional(readOnly = true)
    public List<PackageSession> listByPackage(Long customerPackageId) {
        return sessionRepository.findByCustomerPackageIdOrderBySessionNumberAsc(customerPackageId);
    }

    @Transactional
    public PackageSession assignAppointment(Long sessionId, Long appointmentId, java.time.LocalDateTime scheduledAt) {
        PackageSession session = findByIdOrThrow(sessionId);
        if (session.getStatus() != PackageSessionStatus.PENDING) {
            throw new IllegalStateException("Sadece bekleyen seansa randevu atanabilir");
        }
        session.assignAppointment(appointmentId, scheduledAt);
        return sessionRepository.save(session);
    }

    @Transactional
    public PackageSession unassignAppointment(Long sessionId) {
        PackageSession session = findByIdOrThrow(sessionId);
        if (session.getStatus() != PackageSessionStatus.SCHEDULED) {
            throw new IllegalStateException("Sadece randevu atanmış seanstan randevu kaldırılabilir");
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
            .orElseThrow(() -> new IllegalStateException("Paket bulunamadı"));
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
    }

    public PackageSession findByIdOrThrow(Long id) {
        return sessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Paket seansı bulunamadı: " + id));
    }

    public java.util.Optional<PackageSession> findByAppointmentId(Long appointmentId) {
        return sessionRepository.findByAppointmentId(appointmentId);
    }
}
