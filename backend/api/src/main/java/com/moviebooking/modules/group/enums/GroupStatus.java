package com.moviebooking.modules.group.enums;

/** Trạng thái nhóm (PLAN.md 6.3). */
public enum GroupStatus {
    OPEN,
    LOCKED,
    HOLDING,
    BOOKED,
    COMPLETED,
    EXPIRED,
    CANCELLED;

    /** Nhóm còn hoạt động — tính vào giới hạn số nhóm của người dùng (BR15). */
    public boolean isActive() {
        return this == OPEN || this == LOCKED || this == HOLDING;
    }
}
