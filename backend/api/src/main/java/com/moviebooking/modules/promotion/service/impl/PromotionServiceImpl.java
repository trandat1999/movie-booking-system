package com.moviebooking.modules.promotion.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.promotion.dto.request.PromotionRequest;
import com.moviebooking.modules.promotion.dto.response.PromotionResponse;
import com.moviebooking.modules.promotion.entity.Promotion;
import com.moviebooking.modules.promotion.entity.PromotionUsage;
import com.moviebooking.modules.promotion.enums.PromotionStatus;
import com.moviebooking.modules.promotion.enums.PromotionType;
import com.moviebooking.modules.promotion.repository.PromotionRepository;
import com.moviebooking.modules.promotion.repository.PromotionUsageRepository;
import com.moviebooking.modules.promotion.service.PromotionService;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository usageRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> search(String q, int page, int size) {
        return PageResponse.from(promotionRepository
                .search(q == null ? "" : q.trim(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(PromotionServiceImpl::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        if (promotionRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException(ErrorCode.PROMOTION_CODE_EXISTS);
        }
        Promotion promotion = new Promotion();
        apply(promotion, request);
        return toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = find(id);
        if (!promotion.getCode().equalsIgnoreCase(request.code())
                && promotionRepository.existsByCodeIgnoreCase(request.code())) {
            throw new BusinessException(ErrorCode.PROMOTION_CODE_EXISTS);
        }
        apply(promotion, request);
        if (promotion.getUsageLimit() != null && promotion.getUsageLimit() < promotion.getUsedCount()) {
            throw new BusinessException(ErrorCode.INVALID_PROMOTION, "Giới hạn lượt dùng nhỏ hơn số lượt đã dùng");
        }
        return toResponse(promotion);
    }

    @Override
    @Transactional
    public Applied reserve(Long userId, Long orderId, String code, long subtotal, int tickets) {
        Promotion promotion = promotionRepository
                .findForUpdateByCode(code.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_INVALID));
        Instant now = clock.instant();
        if (promotion.getStatus() != PromotionStatus.ACTIVE
                || now.isBefore(promotion.getStartsAt())
                || !now.isBefore(promotion.getEndsAt())) {
            throw new BusinessException(ErrorCode.PROMOTION_INVALID);
        }
        if (subtotal < promotion.getMinOrder()) {
            throw new BusinessException(
                    ErrorCode.PROMOTION_NOT_APPLICABLE, "Mã chỉ áp dụng cho đơn từ " + vnd(promotion.getMinOrder()));
        }
        if (tickets < promotion.getMinTickets()) {
            throw new BusinessException(
                    ErrorCode.PROMOTION_NOT_APPLICABLE,
                    "Mã chỉ áp dụng cho đơn từ " + promotion.getMinTickets() + " vé trở lên");
        }

        // Đơn đã áp mã trước đó → trả lượt cũ rồi mới giữ lượt mới
        release(orderId);
        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new BusinessException(ErrorCode.PROMOTION_USED_UP);
        }
        if (promotion.getPerUserLimit() != null
                && usageRepository.countByPromotionIdAndUserId(promotion.getId(), userId) >= promotion.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.PROMOTION_USED_UP, "Bạn đã dùng hết lượt của mã này");
        }
        long discount = promotion.discountFor(subtotal);
        PromotionUsage usage = new PromotionUsage();
        usage.setPromotionId(promotion.getId());
        usage.setOrderId(orderId);
        usage.setUserId(userId);
        usage.setDiscount(discount);
        usage.setCreatedAt(now);
        usageRepository.save(usage);
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        return new Applied(promotion.getCode(), promotion.getName(), discount);
    }

    @Override
    @Transactional
    public void release(Long orderId) {
        usageRepository.findByOrderId(orderId).ifPresent(usage -> {
            promotionRepository.findById(usage.getPromotionId())
                    .ifPresent(p -> p.setUsedCount(Math.max(0, p.getUsedCount() - 1)));
            usageRepository.delete(usage);
            usageRepository.flush();
        });
    }

    private void apply(Promotion p, PromotionRequest r) {
        if (!r.startsAt().isBefore(r.endsAt())
                || (r.type() == PromotionType.PERCENT && r.value() > 100)) {
            throw new BusinessException(ErrorCode.INVALID_PROMOTION);
        }
        p.setCode(r.code().trim().toUpperCase(Locale.ROOT));
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setType(r.type());
        p.setValue(r.value());
        p.setMaxDiscount(r.type() == PromotionType.PERCENT ? r.maxDiscount() : null);
        p.setMinOrder(r.minOrder());
        p.setMinTickets(r.minTickets());
        p.setStartsAt(r.startsAt());
        p.setEndsAt(r.endsAt());
        p.setUsageLimit(r.usageLimit());
        p.setPerUserLimit(r.perUserLimit());
        p.setStatus(r.status());
    }

    private Promotion find(Long id) {
        return promotionRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    private static String vnd(long amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }

    private static PromotionResponse toResponse(Promotion p) {
        return new PromotionResponse(
                p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getType(), p.getValue(), p.getMaxDiscount(),
                p.getMinOrder(), p.getMinTickets(), p.getStartsAt(), p.getEndsAt(), p.getUsageLimit(),
                p.getPerUserLimit(), p.getUsedCount(), p.getStatus(), p.getCreatedAt());
    }
}
