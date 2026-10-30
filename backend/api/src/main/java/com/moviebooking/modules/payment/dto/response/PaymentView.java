package com.moviebooking.modules.payment.dto.response;

import com.moviebooking.modules.payment.enums.PaymentStatus;
import java.time.Instant;

/** Giao dịch nhận từ cổng — phục vụ đối soát (ORDADM-R). */
public record PaymentView(String provider, String providerTxnId, long amount, PaymentStatus status, Instant createdAt) {}
