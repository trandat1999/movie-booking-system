package com.moviebooking.modules.movie.entity;

import com.moviebooking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Đạo diễn hoặc diễn viên — vai trò nằm ở {@link MovieCredit}. */
@Getter
@Setter
@Entity
@Table(name = "people")
public class Person extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;
}
