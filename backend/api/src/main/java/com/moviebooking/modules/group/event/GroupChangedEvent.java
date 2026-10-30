package com.moviebooking.modules.group.event;

import java.util.Map;

/**
 * Nhóm vừa thay đổi; sau commit được đẩy tới {@code /topic/groups/{id}} (PLAN.md 8.3). Client tải lại snapshot
 * khi nhận sự kiện.
 *
 * @param type MEMBER_JOINED, MEMBER_LEFT, RSVP_UPDATED, VOTE_UPDATED, STATUS_CHANGED, SEATS_PREVIEWED, SEATS_HELD,
 *     PAID, UPDATED
 */
public record GroupChangedEvent(Long groupId, String type, Map<String, Object> data) {}
