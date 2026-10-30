package com.moviebooking.modules.cinema.entity;

import com.moviebooking.common.entity.BaseEntity;
import com.moviebooking.modules.cinema.enums.CinemaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "cinemas")
public class Cinema extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String phone;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    /** Tiện ích của rạp (CIN09), ví dụ "Bãi đỗ xe", "Quầy bắp nước". */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> amenities = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CinemaStatus status = CinemaStatus.ACTIVE;
}
