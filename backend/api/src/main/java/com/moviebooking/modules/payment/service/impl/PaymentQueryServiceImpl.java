package com.moviebooking.modules.payment.service.impl;

import com.moviebooking.modules.payment.dto.response.PaymentView;
import com.moviebooking.modules.payment.repository.PaymentRepository;
import com.moviebooking.modules.payment.service.PaymentQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentView> listByOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(p -> new PaymentView(p.getProvider(), p.getProviderTxnId(), p.getAmount(), p.getStatus(), p.getCreatedAt()))
                .toList();
    }
}
