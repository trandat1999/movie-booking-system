package com.moviebooking.modules.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Phiếu bầu của một thành viên cho một suất đề xuất — mỗi người tối đa một phiếu / suất. */
@Getter
@Setter
@Entity
@Table(name = "group_votes")
public class GroupVote {

    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Embeddable
    public record Key(@Column(name = "option_id") Long optionId, @Column(name = "user_id") Long userId)
            implements Serializable {}
}
