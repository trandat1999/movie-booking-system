package com.moviebooking.modules.showtime.entity;

import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Giá cơ bản cho 1 chỗ ngồi theo định dạng chiếu và loại ngày (SCHADM06). */
@Getter
@Setter
@Entity
@Table(name = "ticket_base_prices")
@IdClass(BasePrice.Key.class)
public class BasePrice {

    @Id
    @Enumerated(EnumType.STRING)
    private ScreenFormat format;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "day_type")
    private DayType dayType;

    @Column(nullable = false)
    private long price;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private ScreenFormat format;
        private DayType dayType;

        public Key(ScreenFormat format, DayType dayType) {
            this.format = format;
            this.dayType = dayType;
        }
    }
}
