package com.moviebooking.modules.group.service;

import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.event.HoldClosedEvent;
import com.moviebooking.modules.booking.event.OrderStatusChangedEvent;
import com.moviebooking.modules.group.dto.request.AssignmentRequest;
import com.moviebooking.modules.group.dto.request.CreateGroupRequest;
import com.moviebooking.modules.group.dto.request.GroupHoldRequest;
import com.moviebooking.modules.group.dto.request.LockGroupRequest;
import com.moviebooking.modules.group.dto.request.RsvpRequest;
import com.moviebooking.modules.group.dto.request.SeatSuggestionRequest;
import com.moviebooking.modules.group.dto.request.UpdateGroupRequest;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse;
import com.moviebooking.modules.group.dto.response.GroupListItem;
import com.moviebooking.modules.group.dto.response.GroupSummaryResponse;
import com.moviebooking.modules.group.dto.response.InvitePreviewResponse;
import com.moviebooking.modules.group.dto.response.SeatSuggestionsResponse;
import java.util.List;

/**
 * Đặt vé theo nhóm, mô hình A (GRP01–GRP20). Mọi thao tác kiểm tra người gọi là thành viên đang hoạt động; nhóm
 * của người khác coi như không tồn tại (chống IDOR). Thao tác của trưởng nhóm kiểm tra BR08.
 */
public interface GroupService {

    // --- Nhóm & thành viên (GRP01–GRP04) ---

    GroupDetailResponse create(Long userId, CreateGroupRequest request);

    GroupDetailResponse get(Long userId, Long groupId);

    GroupDetailResponse update(Long userId, Long groupId, UpdateGroupRequest request);

    InvitePreviewResponse previewInvite(Long userId, String code);

    GroupDetailResponse join(Long userId, String code);

    void leave(Long userId, Long groupId);

    GroupDetailResponse removeMember(Long userId, Long groupId, Long memberUserId);

    GroupDetailResponse transferLeader(Long userId, Long groupId, Long newLeaderId);

    GroupDetailResponse regenerateCode(Long userId, Long groupId);

    List<GroupListItem> listMine(Long userId);

    // --- Phản hồi, bình chọn, tổng hợp, chốt (GRP05–GRP09) ---

    GroupDetailResponse rsvp(Long userId, Long groupId, RsvpRequest request);

    GroupDetailResponse proposeOption(Long userId, Long groupId, Long showtimeId);

    GroupDetailResponse removeOption(Long userId, Long groupId, Long optionId);

    GroupDetailResponse vote(Long userId, Long groupId, Long optionId, boolean voted);

    GroupSummaryResponse summary(Long userId, Long groupId);

    GroupDetailResponse lock(Long userId, Long groupId, LockGroupRequest request);

    GroupDetailResponse unlock(Long userId, Long groupId);

    GroupDetailResponse cancel(Long userId, Long groupId);

    // --- Chọn & giữ ghế, thanh toán, phân phối vé (GRP10–GRP17) ---

    SeatSuggestionsResponse suggestSeats(Long userId, Long groupId, SeatSuggestionRequest request);

    /** Chấm điểm lựa chọn thủ công (GRP13). */
    SeatSuggestionsResponse.SuggestionView evaluateSeats(Long userId, Long groupId, List<Long> seatIds);

    /** Trưởng nhóm xem trước phương án → thành viên thấy realtime (GRP15). */
    void previewSeats(Long userId, Long groupId, List<Long> seatIds);

    /**
     * Giữ ghế nguyên khối cho cả nhóm (GRP14). Thất bại → 409 SEAT_UNAVAILABLE, {@code details} gồm ghế bị mất và
     * phương án đề xuất lại.
     */
    GroupDetailResponse hold(Long userId, Long groupId, GroupHoldRequest request);

    GroupDetailResponse releaseHold(Long userId, Long groupId);

    /** Tạo đơn từ lượt giữ của nhóm; trưởng nhóm thanh toán qua /orders/{code}/pay (GRP16). */
    OrderResponse createOrder(Long userId, Long groupId);

    GroupDetailResponse updateAssignments(Long userId, Long groupId, AssignmentRequest request);

    // --- Dùng cho STOMP, job, sự kiện của module đặt vé ---

    boolean isActiveMember(Long groupId, Long userId);

    void onHoldClosed(HoldClosedEvent event);

    void onOrderStatusChanged(OrderStatusChangedEvent event);

    /** Rạp hủy suất đã chốt: nhóm chưa đặt quay lại OPEN, nhóm đã đặt → CANCELLED (SCHADM03). */
    void onShowtimeCancelled(Long showtimeId);

    /** BOOKED → COMPLETED cho nhóm có suất đã chiếu xong. @return số nhóm */
    int completeFinishedGroups();

    /** @return số nhóm đã chuyển EXPIRED */
    int expireOverdueGroups();

    /** @return số nhóm đã gửi nhắc */
    int remindPendingMembers();
}
