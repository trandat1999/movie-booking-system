package com.moviebooking.modules.showtime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Ngày lễ — suất chiếu trong ngày này dùng giá HOLIDAY. */
@Getter
@Setter
@Entity
@Table(name = "holidays")
public class Holiday {

    @Id
    @Column(name = "holiday_date")
    private LocalDate date;

    @Column(nullable = false)
    private String name;
}
