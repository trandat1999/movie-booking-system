package com.moviebooking.modules.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Một ghế trong đơn, giá chụp lại lúc đặt (BR09). */
@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @Column(name = "seat_type_code", nullable = false)
    private String seatTypeCode;

    @Column(nullable = false)
    private long price;
}
