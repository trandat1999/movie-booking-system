package com.moviebooking.modules.group.entity;

import com.moviebooking.modules.group.enums.GroupRole;
import com.moviebooking.modules.group.enums.Rsvp;
import com.moviebooking.modules.group.enums.SeatPreference;
import com.moviebooking.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Thành viên nhóm; rời nhóm = đặt {@link #leftAt} (giữ lịch sử, tham gia lại thì xóa). */
@Getter
@Setter
@Entity
@Table(name = "group_members")
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupRole role = GroupRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rsvp rsvp = Rsvp.PENDING;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Column(name = "max_budget")
    private Long maxBudget;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    /** Sở thích chỗ ngồi (GRP22). */
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_preference", nullable = false)
    private SeatPreference seatPreference = SeatPreference.NONE;

    @Column(name = "next_to_user_id")
    private Long nextToUserId;

    public Long userId() {
        return user.getId();
    }

    public boolean isActive() {
        return leftAt == null;
    }

    /** Số vé người này cần: bản thân + khách đi kèm, chỉ khi phản hồi "Có" (BR18). */
    public int tickets() {
        return rsvp == Rsvp.YES ? 1 + guestCount : 0;
    }
}
