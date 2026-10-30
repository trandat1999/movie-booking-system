package com.moviebooking.modules.group.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.infrastructure.ratelimit.RateLimitProperties;
import com.moviebooking.infrastructure.ratelimit.RateLimiter;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.group.dto.request.AssignmentRequest;
import com.moviebooking.modules.group.dto.request.CreateGroupRequest;
import com.moviebooking.modules.group.dto.request.GroupHoldRequest;
import com.moviebooking.modules.group.dto.request.JoinGroupRequest;
import com.moviebooking.modules.group.dto.request.LockGroupRequest;
import com.moviebooking.modules.group.dto.request.ProposeOptionRequest;
import com.moviebooking.modules.group.dto.request.RsvpRequest;
import com.moviebooking.modules.group.dto.request.SeatPreferenceRequest;
import com.moviebooking.modules.group.dto.request.SeatSelectionRequest;
import com.moviebooking.modules.group.dto.request.SeatSuggestionRequest;
import com.moviebooking.modules.group.dto.request.TransferLeaderRequest;
import com.moviebooking.modules.group.dto.request.UpdateGroupRequest;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse;
import com.moviebooking.modules.group.dto.response.GroupListItem;
import com.moviebooking.modules.group.dto.response.GroupSummaryResponse;
import com.moviebooking.modules.group.dto.response.InvitePreviewResponse;
import com.moviebooking.modules.group.dto.response.SeatSuggestionsResponse;
import com.moviebooking.modules.group.service.GroupService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Đặt vé nhóm")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    // --- Nhóm & thành viên ---

    @Operation(summary = "Tạo nhóm (GRP01)")
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupDetailResponse> create(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody CreateGroupRequest request) {
        rateLimiter.check("group:" + user.id(), rateLimitProperties.holdPerMinute());
        return ApiResponse.ok(groupService.create(user.id(), request));
    }

    @Operation(summary = "Nhóm của tôi (GRP20)")
    @GetMapping("/me/groups")
    public ApiResponse<List<GroupListItem>> mine(@CurrentUser AuthenticatedUser user) {
        return ApiResponse.ok(groupService.listMine(user.id()));
    }

    @Operation(summary = "Chi tiết nhóm — chỉ thành viên (GRP03, GRP19)")
    @GetMapping("/groups/{id}")
    public ApiResponse<GroupDetailResponse> get(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.get(user.id(), id));
    }

    @Operation(summary = "Sửa thông tin nhóm (trưởng nhóm, khi chưa chốt)")
    @PatchMapping("/groups/{id}")
    public ApiResponse<GroupDetailResponse> update(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody UpdateGroupRequest request) {
        return ApiResponse.ok(groupService.update(user.id(), id, request));
    }

    @Operation(summary = "Xem nhóm qua mã mời trước khi tham gia (GRP02)")
    @GetMapping("/groups/invite/{code}")
    public ApiResponse<InvitePreviewResponse> preview(@CurrentUser AuthenticatedUser user, @PathVariable String code) {
        return ApiResponse.ok(groupService.previewInvite(user.id(), code));
    }

    @Operation(summary = "Tham gia nhóm bằng mã mời (GRP02)")
    @PostMapping("/groups/join")
    public ApiResponse<GroupDetailResponse> join(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody JoinGroupRequest request) {
        return ApiResponse.ok(groupService.join(user.id(), request.code()));
    }

    @Operation(summary = "Rời nhóm (GRP03)")
    @PostMapping("/groups/{id}/leave")
    public ApiResponse<Void> leave(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        groupService.leave(user.id(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Mời thành viên ra khỏi nhóm (trưởng nhóm, GRP03)")
    @DeleteMapping("/groups/{id}/members/{userId}")
    public ApiResponse<GroupDetailResponse> removeMember(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @PathVariable Long userId) {
        return ApiResponse.ok(groupService.removeMember(user.id(), id, userId));
    }

    @Operation(summary = "Chuyển quyền trưởng nhóm (GRP04)")
    @PostMapping("/groups/{id}/transfer-leader")
    public ApiResponse<GroupDetailResponse> transferLeader(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody TransferLeaderRequest request) {
        return ApiResponse.ok(groupService.transferLeader(user.id(), id, request.userId()));
    }

    @Operation(summary = "Tạo lại mã mời — mã cũ hết hiệu lực")
    @PostMapping("/groups/{id}/invite-code/regenerate")
    public ApiResponse<GroupDetailResponse> regenerate(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.regenerateCode(user.id(), id));
    }

    // --- Phản hồi, bình chọn, chốt ---

    @Operation(summary = "Phản hồi tham gia + khách đi kèm + ngân sách (GRP05)")
    @PutMapping("/groups/{id}/rsvp")
    public ApiResponse<GroupDetailResponse> rsvp(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody RsvpRequest request) {
        return ApiResponse.ok(groupService.rsvp(user.id(), id, request));
    }

    @Operation(summary = "Sở thích chỗ ngồi: vị trí ưa thích, muốn ngồi cạnh ai (GRP22)")
    @PutMapping("/groups/{id}/seat-preference")
    public ApiResponse<GroupDetailResponse> seatPreference(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody SeatPreferenceRequest request) {
        return ApiResponse.ok(groupService.updateSeatPreference(user.id(), id, request));
    }

    @Operation(summary = "Đề xuất suất chiếu để bình chọn (GRP06)")
    @PostMapping("/groups/{id}/options")
    public ApiResponse<GroupDetailResponse> propose(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody ProposeOptionRequest request) {
        return ApiResponse.ok(groupService.proposeOption(user.id(), id, request.showtimeId()));
    }

    @Operation(summary = "Bỏ suất đề xuất (người đề xuất hoặc trưởng nhóm)")
    @DeleteMapping("/groups/{id}/options/{optionId}")
    public ApiResponse<GroupDetailResponse> removeOption(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @PathVariable Long optionId) {
        return ApiResponse.ok(groupService.removeOption(user.id(), id, optionId));
    }

    @Operation(summary = "Bình chọn suất (GRP06)")
    @PostMapping("/groups/{id}/options/{optionId}/vote")
    public ApiResponse<GroupDetailResponse> vote(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @PathVariable Long optionId) {
        return ApiResponse.ok(groupService.vote(user.id(), id, optionId, true));
    }

    @Operation(summary = "Bỏ phiếu đã bầu")
    @DeleteMapping("/groups/{id}/options/{optionId}/vote")
    public ApiResponse<GroupDetailResponse> unvote(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @PathVariable Long optionId) {
        return ApiResponse.ok(groupService.vote(user.id(), id, optionId, false));
    }

    @Operation(summary = "Tổng hợp nhu cầu + cảnh báo cụm ghế còn lại từng suất + cảnh báo độ tuổi (GRP07, GRP08)")
    @GetMapping("/groups/{id}/summary")
    public ApiResponse<GroupSummaryResponse> summary(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.summary(user.id(), id));
    }

    @Operation(summary = "Chốt suất & số vé (trưởng nhóm, GRP09)")
    @PostMapping("/groups/{id}/lock")
    public ApiResponse<GroupDetailResponse> lock(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody LockGroupRequest request) {
        return ApiResponse.ok(groupService.lock(user.id(), id, request));
    }

    @Operation(summary = "Mở chốt để đổi suất (trưởng nhóm)")
    @PostMapping("/groups/{id}/unlock")
    public ApiResponse<GroupDetailResponse> unlock(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.unlock(user.id(), id));
    }

    @Operation(summary = "Hủy nhóm khi chưa thanh toán (trưởng nhóm)")
    @PostMapping("/groups/{id}/cancel")
    public ApiResponse<GroupDetailResponse> cancel(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.cancel(user.id(), id));
    }

    // --- Ghế, giữ ghế, đơn ---

    @Operation(summary = "Đề xuất cụm ghế cho nhóm (GRP10, GRP11)")
    @PostMapping("/groups/{id}/seat-suggestions")
    public ApiResponse<SeatSuggestionsResponse> suggest(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody SeatSuggestionRequest request) {
        return ApiResponse.ok(groupService.suggestSeats(user.id(), id, request));
    }

    @Operation(summary = "Chấm điểm ghế tự chọn (GRP13)")
    @PostMapping("/groups/{id}/seat-evaluation")
    public ApiResponse<SeatSuggestionsResponse.SuggestionView> evaluate(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody SeatSelectionRequest request) {
        return ApiResponse.ok(groupService.evaluateSeats(user.id(), id, request.seatIds()));
    }

    @Operation(summary = "Cho thành viên xem trước phương án đang chọn (GRP15)")
    @PostMapping("/groups/{id}/preview")
    public ApiResponse<Void> preview(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody SeatSelectionRequest request) {
        groupService.previewSeats(user.id(), id, request.seatIds());
        return ApiResponse.ok();
    }

    @Operation(summary = "Giữ ghế nguyên khối cho cả nhóm (GRP14) — thất bại trả phương án mới")
    @PostMapping("/groups/{id}/hold")
    public ApiResponse<GroupDetailResponse> hold(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody GroupHoldRequest request) {
        rateLimiter.check("hold:" + user.id(), rateLimitProperties.holdPerMinute());
        return ApiResponse.ok(groupService.hold(user.id(), id, request));
    }

    @Operation(summary = "Bỏ giữ ghế để chọn lại")
    @DeleteMapping("/groups/{id}/hold")
    public ApiResponse<GroupDetailResponse> releaseHold(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.releaseHold(user.id(), id));
    }

    @Operation(summary = "Tạo đơn cho nhóm — trưởng nhóm thanh toán qua /orders/{code}/pay (GRP16)")
    @PostMapping("/groups/{id}/order")
    public ApiResponse<OrderResponse> order(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return ApiResponse.ok(groupService.createOrder(user.id(), id));
    }

    @Operation(summary = "Sửa phân phối vé cho thành viên (GRP17)")
    @PutMapping("/groups/{id}/assignments")
    public ApiResponse<GroupDetailResponse> assignments(
            @CurrentUser AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return ApiResponse.ok(groupService.updateAssignments(user.id(), id, request));
    }
}
