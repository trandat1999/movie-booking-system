package com.moviebooking.modules.cinema.entity;

import com.moviebooking.common.entity.BaseEntity;
import com.moviebooking.modules.cinema.enums.RoomStatus;
import com.moviebooking.modules.cinema.enums.RoomType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Phòng chiếu. Sơ đồ là lưới {@code gridRows × gridCols}; ghế nằm ở bảng {@code seats}. */
@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    @Column(name = "grid_rows", nullable = false)
    private int gridRows;

    @Column(name = "grid_cols", nullable = false)
    private int gridCols;

    /** Bật khi phòng có suất chiếu đầu tiên — từ đó mã ghế phải giữ nguyên. */
    @Column(name = "layout_locked", nullable = false)
    private boolean layoutLocked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.ACTIVE;

    public boolean isActive() {
        return status == RoomStatus.ACTIVE;
    }
}
