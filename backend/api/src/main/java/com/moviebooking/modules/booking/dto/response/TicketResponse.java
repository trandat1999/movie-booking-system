package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.TicketStatus;

/** @param qrImage ảnh QR (data URI PNG) chứa mã vé đã ký — chỉ có khi vé còn hiệu lực */
public record TicketResponse(String code, String seatLabel, TicketStatus status, String qrImage) {}
