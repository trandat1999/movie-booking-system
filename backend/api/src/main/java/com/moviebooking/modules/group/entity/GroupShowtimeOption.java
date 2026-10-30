package com.moviebooking.modules.group.entity;

import com.moviebooking.modules.showtime.entity.Showtime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Suất chiếu được đề xuất để nhóm bình chọn (GRP06). */
@Getter
@Setter
@Entity
@Table(name = "group_showtime_options")
public class GroupShowtimeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @Column(name = "proposed_by", nullable = false)
    private Long proposedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
