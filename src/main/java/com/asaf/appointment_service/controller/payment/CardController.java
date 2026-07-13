package com.asaf.appointment_service.controller.payment;

import com.asaf.appointment_service.common.dto.ApiResponse;
import com.asaf.appointment_service.common.service.CurrentUserService;
import com.asaf.appointment_service.dto.payment.StoredCardResponse;
import com.asaf.appointment_service.mapper.payment.PaymentMapper;
import com.asaf.appointment_service.service.payment.PaymentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public CardController(PaymentService paymentService,
                          CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoredCardResponse>>> getStoredCards() {
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        List<StoredCardResponse> cards = paymentService.getStoredCards(businessId)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Long cardId) {
        currentUserService.requireBusinessOwnerOrAdmin();
        Long businessId = currentUserService.getCurrentBusinessId();
        paymentService.deleteStoredCard(businessId, cardId);
        return ResponseEntity.ok(ApiResponse.success(null, "Kart silindi"));
    }
}
