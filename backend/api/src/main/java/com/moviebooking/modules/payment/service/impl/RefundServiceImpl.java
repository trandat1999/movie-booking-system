package com.moviebooking.modules.payment.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.infrastructure.payment.PaymentProvider;
import com.moviebooking.modules.payment.dto.response.RefundView;
import com.moviebooking.modules.payment.entity.Payment;
import com.moviebooking.modules.payment.entity.Refund;
import com.moviebooking.modules.payment.enums.PaymentStatus;
import com.moviebooking.modules.payment.enums.RefundStatus;
import com.moviebooking.modules.payment.repository.PaymentRepository;
import com.moviebooking.modules.payment.repository.RefundRepository;
import com.moviebooking.modules.payment.service.RefundService;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RefundServiceImpl implements RefundService {

    private final Map<String, PaymentProvider> providers;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final Clock clock;

    public RefundServiceImpl(
            List<PaymentProvider> providers,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            Clock clock) {
        this.providers = providers.stream().collect(Collectors.toMap(PaymentProvider::code, Function.identity()));
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RefundView request(Long orderId, long amount, String reason, Long actorId) {
        Optional<Refund> existing = refundRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return view(existing.get());
        }
        Payment payment = paymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .max(Comparator.comparing(Payment::getCreatedAt))
                .orElse(null);
        Refund refund = new Refund();
        refund.setOrderId(orderId);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setRequestedBy(actorId);
        refund.setCreatedAt(clock.instant());
        refund.setStatus(RefundStatus.PENDING);
        if (payment != null) {
            refund.setPaymentId(payment.getId());
            refund.setProvider(payment.getProvider());
        }
        PaymentProvider provider = payment == null ? null : providers.get(payment.getProvider());
        if (amount == 0 || payment == null) {
            // Không có gì để hoàn (tỷ lệ hoàn 0 % hoặc đơn chưa từng thu tiền)
            complete(refund, null, "Không phát sinh tiền hoàn");
        } else if (provider != null && provider.supportsAutoRefund()) {
            try {
                refund.setProviderRef(provider.refund(payment.getProviderTxnId(), amount, reason));
                complete(refund, null, null);
            } catch (RuntimeException ex) {
                // Giữ PENDING để admin hoàn thủ công
                log.warn("Automatic refund for order {} failed: {}", orderId, ex.getMessage());
                refund.setNote("Hoàn tự động thất bại: " + ex.getMessage());
            }
        }
        refundRepository.save(refund);
        return view(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefundView> findByOrder(Long orderId) {
        return refundRepository.findByOrderId(orderId).map(RefundServiceImpl::view);
    }

    @Override
    @Transactional
    public RefundView markCompleted(Long refundId, Long actorId, String note) {
        Refund refund = refundRepository
                .findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCode.REFUND_NOT_PENDING);
        }
        complete(refund, actorId, note);
        return view(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundView> list(RefundStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return PageResponse.from((status == null
                        ? refundRepository.findAllByOrderByCreatedAtDesc(pageable)
                        : refundRepository.findByStatusOrderByCreatedAtAsc(status, pageable))
                .map(RefundServiceImpl::view));
    }

    private void complete(Refund refund, Long actorId, String note) {
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setProcessedAt(clock.instant());
        refund.setProcessedBy(actorId);
        if (note != null) {
            refund.setNote(note);
        }
    }

    private static RefundView view(Refund r) {
        return new RefundView(
                r.getId(), r.getOrderId(), r.getAmount(), r.getReason(), r.getStatus(), r.getProvider(), r.getNote(),
                r.getCreatedAt(), r.getProcessedAt());
    }
}
