package com.moviebooking.modules.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.infrastructure.payment.PaymentProvider;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.booking.service.PaymentTarget;
import com.moviebooking.modules.payment.dto.response.PaymentCallbackResponse;
import com.moviebooking.modules.payment.dto.response.PaymentSessionResponse;
import com.moviebooking.modules.payment.enums.PaymentStatus;
import com.moviebooking.modules.payment.repository.PaymentRepository;
import com.moviebooking.modules.payment.service.PaymentService;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final Map<String, PaymentProvider> providers;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentServiceImpl(
            List<PaymentProvider> providers,
            OrderService orderService,
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.providers = providers.stream().collect(Collectors.toMap(PaymentProvider::code, Function.identity()));
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSessionResponse initiate(Long userId, String orderCode, String providerCode) {
        PaymentProvider provider = provider(providerCode);
        PaymentTarget order = orderService.getPayable(userId, orderCode);
        PaymentProvider.PaymentSession session =
                provider.createPayment(order.code(), order.total(), "Thanh toan don " + order.code());
        return new PaymentSessionResponse(provider.code(), session.redirectUrl());
    }

    @Override
    @Transactional
    public PaymentCallbackResponse handleCallback(String providerCode, Map<String, String> params) {
        PaymentProvider provider = provider(providerCode);
        PaymentProvider.PaymentCallback callback = provider.verifyCallback(params);
        // Khóa đơn trước: hai callback của cùng đơn xử lý tuần tự
        PaymentTarget order = orderService.lockForPayment(callback.orderCode());

        PaymentStatus status;
        if (!callback.success()) {
            status = PaymentStatus.FAILED;
        } else if (callback.amount() != order.total()) {
            status = PaymentStatus.AMOUNT_MISMATCH;
        } else {
            status = PaymentStatus.SUCCEEDED;
        }
        int inserted = paymentRepository.insertIfAbsent(
                order.orderId(),
                provider.code(),
                callback.transactionId(),
                callback.amount(),
                status.name(),
                toJson(callback.raw()),
                clock.instant());
        if (inserted == 0) {
            log.info("Duplicate payment callback {} {} ignored", provider.code(), callback.transactionId());
            return new PaymentCallbackResponse(order.code(), true, order.status());
        }

        Map<String, Object> data = Map.of(
                "provider", provider.code(),
                "transactionId", callback.transactionId(),
                "amount", callback.amount());
        switch (status) {
            case SUCCEEDED -> orderService.applySuccessfulPayment(
                    order.code(), provider.code(), callback.transactionId());
            case FAILED -> orderService.recordPaymentIssue(order.code(), "PAYMENT_FAILED", data);
            case AMOUNT_MISMATCH -> {
                log.warn("Payment amount mismatch for order {}: expected {}, got {}",
                        order.code(), order.total(), callback.amount());
                orderService.recordPaymentIssue(order.code(), "PAYMENT_AMOUNT_MISMATCH", data);
            }
        }
        return new PaymentCallbackResponse(
                order.code(), false, orderService.lockForPayment(order.code()).status());
    }

    private PaymentProvider provider(String code) {
        PaymentProvider provider = code == null ? null : providers.get(code.toUpperCase(Locale.ROOT));
        if (provider == null) {
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_NOT_SUPPORTED);
        }
        return provider;
    }

    private String toJson(Map<String, String> raw) {
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
