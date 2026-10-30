package com.moviebooking.modules.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Loại ghế (danh mục cố định trong migration). Giá một ghế = giá cơ bản × {@code capacity} + {@code surcharge}.
 */
@Getter
@Setter
@Entity
@Table(name = "seat_types")
public class SeatType {

    public static final String STANDARD = "STANDARD";
    public static final String VIP = "VIP";
    public static final String COUPLE = "COUPLE";
    public static final String ACCESSIBLE = "ACCESSIBLE";

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    /** Số người ngồi được (ghế đôi = 2). */
    @Column(nullable = false)
    private int capacity;

    /** Số ô lưới chiếm theo chiều ngang (ghế đôi = 2). */
    @Column(name = "col_span", nullable = false)
    private int colSpan;

    @Column(nullable = false)
    private long surcharge;

    @Column(nullable = false)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
