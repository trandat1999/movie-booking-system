package com.moviebooking.modules.booking.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.booking.dto.request.CheckinRequest;
import com.moviebooking.modules.booking.dto.response.CheckinResponse;
import com.moviebooking.modules.booking.service.CheckinService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — soát vé")
@RestController
@RequestMapping("/api/v1/admin/checkin")
@PreAuthorize("hasAuthority('ticket:checkin')")
@RequiredArgsConstructor
public class AdminCheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "Soát vé: quét QR vé hoặc QR đơn nhóm (CHK01) — vé đã soát bị từ chối")
    @PostMapping
    @Audited(action = "TICKET_CHECKIN", resource = "ticket")
    public ApiResponse<CheckinResponse> checkin(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody CheckinRequest request) {
        return ApiResponse.ok(checkinService.checkin(user.id(), request.payload()));
    }
}
