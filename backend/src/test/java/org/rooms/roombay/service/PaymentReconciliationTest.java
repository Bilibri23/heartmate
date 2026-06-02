package org.rooms.roombay.service;

import org.junit.jupiter.api.Test;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.repository.PaymentRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentReconciliationTest {

    @Test
    void duplicateRiskFlagsReusedTransactionAndProof() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentService service = new PaymentService(paymentRepository, null, null, null, null, null);
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .momoTransactionId("TX-123")
                .paymentProofUrl("https://res.cloudinary.com/demo/proof.jpg")
                .build();

        when(paymentRepository.countSubmittedOrVerifiedByTransactionIdExcluding("TX-123", payment.getId())).thenReturn(1L);
        when(paymentRepository.countSubmittedOrVerifiedByProofUrlExcluding("https://res.cloudinary.com/demo/proof.jpg", payment.getId())).thenReturn(1L);

        @SuppressWarnings("unchecked")
        List<String> reasons = (List<String>) ReflectionTestUtils.invokeMethod(service, "duplicateRiskReasons", payment);

        assertThat(reasons).containsExactly("MOMO_TRANSACTION_ID_REUSED", "PAYMENT_PROOF_URL_REUSED");
    }
}
