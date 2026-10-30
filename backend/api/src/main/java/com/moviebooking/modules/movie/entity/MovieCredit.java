package com.moviebooking.modules.movie.entity;

import com.moviebooking.modules.movie.enums.PersonRole;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một người tham gia phim với vai trò đạo diễn/diễn viên, theo thứ tự hiển thị. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "movie_people")
public class MovieCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersonRole role;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public MovieCredit(Movie movie, Person person, PersonRole role, int sortOrder) {
        this.movie = movie;
        this.person = person;
        this.role = role;
        this.sortOrder = sortOrder;
    }
}
