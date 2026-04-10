package com.asaf.appointment_service.service.employee;

import com.asaf.appointment_service.dto.employee.CreateEmployeeInviteRequest;
import com.asaf.appointment_service.dto.employee.EmployeeInviteResponse;
import com.asaf.appointment_service.entity.business.Business;
import com.asaf.appointment_service.entity.employee.Employee;
import com.asaf.appointment_service.entity.employee.EmployeeInvite;
import com.asaf.appointment_service.entity.employee.InviteStatus;
import com.asaf.appointment_service.integration.email.EmailClient;
import com.asaf.appointment_service.repository.employee.EmployeeInviteRepository;
import com.asaf.appointment_service.service.business.BusinessService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeInviteService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeInviteService.class);

    private final EmployeeInviteRepository inviteRepository;
    private final EmployeeService employeeService;
    private final BusinessService businessService;
    private final EmailClient emailClient;

    public EmployeeInviteService(EmployeeInviteRepository inviteRepository,
                                  EmployeeService employeeService,
                                  BusinessService businessService,
                                  EmailClient emailClient) {
        this.inviteRepository = inviteRepository;
        this.employeeService = employeeService;
        this.businessService = businessService;
        this.emailClient = emailClient;
    }

    @Transactional
    public EmployeeInviteResponse createInvite(Long businessId, CreateEmployeeInviteRequest request) {
        Employee employee = employeeService.findByIdOrThrow(request.employeeId());
        
        if (!employee.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Bu çalışan başka bir işletmeye ait");
        }

        Business business = businessService.findByIdOrThrow(businessId);

        // Mevcut bekleyen daveti iptal et
        inviteRepository.findByEmployeeIdAndStatus(request.employeeId(), InviteStatus.PENDING)
            .forEach(invite -> {
                invite.cancel();
                inviteRepository.save(invite);
            });

        String token = EmployeeInvite.generateToken();
        EmployeeInvite invite = EmployeeInvite.builder()
            .businessId(businessId)
            .employeeId(request.employeeId())
            .token(token)
            .email(request.email())
            .expiresAt(EmployeeInvite.defaultExpiry())
            .build();

        EmployeeInvite saved = inviteRepository.save(invite);
        
        // Email gönder
        emailClient.sendEmployeeInvite(
            request.email(),
            employee.getName(),
            business.getName(),
            token
        );
        
        log.info("Çalışan daveti oluşturuldu ve email gönderildi: inviteId={}, employeeId={}, email={}", 
            saved.getId(), request.employeeId(), request.email());

        return toResponse(saved, employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeInviteResponse> listInvites(Long businessId) {
        return inviteRepository.findByBusinessIdAndStatus(businessId, InviteStatus.PENDING)
            .stream()
            .map(invite -> {
                Employee employee = employeeService.findByIdOrThrow(invite.getEmployeeId());
                return toResponse(invite, employee);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeInvite findByTokenOrThrow(String token) {
        return inviteRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Geçersiz davet kodu"));
    }

    @Transactional
    public void markAsUsed(String token) {
        EmployeeInvite invite = findByTokenOrThrow(token);
        invite.markAsUsed();
        inviteRepository.save(invite);
    }

    @Transactional
    public void cancelInvite(Long businessId, Long inviteId) {
        EmployeeInvite invite = inviteRepository.findById(inviteId)
            .orElseThrow(() -> new IllegalArgumentException("Davet bulunamadı"));
        
        if (!invite.getBusinessId().equals(businessId)) {
            throw new IllegalArgumentException("Bu davet başka bir işletmeye ait");
        }

        invite.cancel();
        inviteRepository.save(invite);
        
        log.info("Davet iptal edildi: inviteId={}", inviteId);
    }

    private EmployeeInviteResponse toResponse(EmployeeInvite invite, Employee employee) {
        return new EmployeeInviteResponse(
            invite.getId(),
            employee.getId(),
            employee.getName(),
            invite.getEmail(),
            invite.getToken(),
            invite.getStatus(),
            invite.getExpiresAt(),
            invite.getUsedAt(),
            invite.getCreatedAt()
        );
    }
}
