package com.moviebooking.modules.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Vé của đơn nhóm được giao cho ai (GRP17). Vé của khách đi kèm thuộc thành viên đưa khách ({@link #userId}),
 * {@link #guestLabel} ghi tên khách.
 */
@Getter
@Setter
@Entity
@Table(name = "group_seat_assignments")
public class GroupSeatAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "guest_label")
    private String guestLabel;
}
