package com.moviebooking.modules.movie.entity;

import com.moviebooking.common.entity.BaseEntity;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.enums.PersonRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "movies")
public class Movie extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_rating", nullable = false)
    private AgeRating ageRating;

    @Column(nullable = false)
    private String language;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieStatus status;

    @ManyToMany
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    /**
     * Set (không phải List) vì được nạp cùng {@link #genres} trong một truy vấn: List sẽ bị lặp phần tử theo số thể
     * loại (tích Descartes của fetch join).
     */
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("role ASC, sortOrder ASC")
    private Set<MovieCredit> credits = new LinkedHashSet<>();

    /**
     * Thay toàn bộ danh sách người tham gia theo thứ tự truyền vào. Giữ lại bản ghi cũ trùng (người, vai trò) thay
     * vì xóa rồi thêm lại — Hibernate chạy INSERT trước DELETE nên thêm lại sẽ vi phạm ràng buộc UNIQUE.
     */
    public void replaceCredits(List<Person> directors, List<Person> actors) {
        List<MovieCredit> next = new ArrayList<>();
        addCredits(next, directors, PersonRole.DIRECTOR);
        addCredits(next, actors, PersonRole.ACTOR);
        credits.retainAll(next);
        next.stream().filter(credit -> !credits.contains(credit)).forEach(credits::add);
    }

    private void addCredits(List<MovieCredit> target, List<Person> people, PersonRole role) {
        for (int i = 0; i < people.size(); i++) {
            Person person = people.get(i);
            int order = i;
            MovieCredit credit = credits.stream()
                    .filter(c -> c.getRole() == role && c.getPerson().getId().equals(person.getId()))
                    .findFirst()
                    .orElseGet(() -> new MovieCredit(this, person, role, order));
            credit.setSortOrder(order);
            target.add(credit);
        }
    }

    public List<Person> peopleWithRole(PersonRole role) {
        return credits.stream()
                .filter(credit -> credit.getRole() == role)
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(MovieCredit::getPerson)
                .toList();
    }
}
