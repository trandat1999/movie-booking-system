package com.moviebooking.modules.cinema.entity;

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

/**
 * Ghế trong phòng. {@code (gridRow, gridCol)} là ô trái nhất mà ghế chiếm; ghế đôi chiếm thêm ô bên phải
 * ({@code colSpan = 2}). Nhãn hiển thị {@code rowLabel + number}, ví dụ "E7".
 */
@Getter
@Setter
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(nullable = false)
    private int number;

    @Column(name = "grid_row", nullable = false)
    private int gridRow;

    @Column(name = "grid_col", nullable = false)
    private int gridCol;

    @Column(name = "col_span", nullable = false)
    private int colSpan;

    @Column(name = "seat_type_code", nullable = false)
    private String seatTypeCode;

    /** false = ghế hỏng/không bán (CIN06). */
    @Column(nullable = false)
    private boolean active = true;

    public String label() {
        return rowLabel + number;
    }
}
