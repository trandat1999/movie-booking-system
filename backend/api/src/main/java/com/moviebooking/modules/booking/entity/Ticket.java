package com.moviebooking.modules.booking.entity;

import com.moviebooking.modules.booking.enums.TicketStatus;
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

/** Vé điện tử của một ghế (BR07). Một ghế/suất chỉ có một vé chưa hủy (unique index trong CSDL). */
@Getter
@Setter
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "holder_user_id")
    private Long holderUserId;

    @Column(name = "holder_name")
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.VALID;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
