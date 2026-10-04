package com.vk.service;

import com.vk.entities.AuditLog;
import com.vk.entities.Order;
import com.vk.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentValidatorService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void validatePayment(Order order) {
        // Assume payment processing happens here
        boolean paymentSuccessful = false;

        // If payment is unsuccessful, we log the payment failed
        if (!paymentSuccessful) {
            AuditLog paymentFailureLog = new AuditLog();
            paymentFailureLog.setOrderId(Long.valueOf(order.getId()));
            paymentFailureLog.setAction("Payment failed for order");
            paymentFailureLog.setTimestamp(LocalDateTime.now());

            if (order.getTotalPrice().compareTo(BigDecimal.valueOf(2000)) > 0) {
                throw new RuntimeException("Error in payment validator");
            }

            // Save the payment failure log
            auditLogRepository.save(paymentFailureLog);
        }
    }
}
