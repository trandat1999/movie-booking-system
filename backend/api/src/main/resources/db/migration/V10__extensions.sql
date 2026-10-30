-- M10: sở thích chỗ ngồi trong nhóm (GRP22), phim yêu thích (MOV04), đánh giá phim (FBK01)

-- GRP22: vị trí ưa thích + muốn ngồi cạnh một thành viên khác; dùng khi phân phối vé tự động
ALTER TABLE group_members
    ADD COLUMN seat_preference VARCHAR(10) NOT NULL DEFAULT 'NONE',
    ADD COLUMN next_to_user_id BIGINT REFERENCES users (id),
    ADD CONSTRAINT ck_group_members_seat_preference CHECK (seat_preference IN ('NONE', 'AISLE', 'CENTER')),
    ADD CONSTRAINT ck_group_members_next_to CHECK (next_to_user_id IS NULL OR next_to_user_id <> user_id);

-- Kết quả xếp chỗ tự động: số mong muốn được đáp ứng / tổng số mong muốn
ALTER TABLE booking_groups
    ADD COLUMN preferences_satisfied INT,
    ADD COLUMN preferences_requested INT;

-- MOV04
CREATE TABLE favorite_movies (
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    movie_id   BIGINT      NOT NULL REFERENCES movies (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, movie_id)
);

CREATE INDEX idx_favorite_movies_movie ON favorite_movies (movie_id);

-- FBK01: mỗi người một đánh giá cho mỗi phim; chỉ người có vé đã soát của phim mới được viết (kiểm ở service)
CREATE TABLE movie_reviews (
    id         BIGSERIAL     PRIMARY KEY,
    movie_id   BIGINT        NOT NULL REFERENCES movies (id),
    user_id    BIGINT        NOT NULL REFERENCES users (id),
    rating     INT           NOT NULL,
    comment    VARCHAR(1000),
    -- Quản trị ẩn đánh giá vi phạm (không xóa để còn dấu vết)
    hidden     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_movie_reviews_user UNIQUE (movie_id, user_id),
    CONSTRAINT ck_movie_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_movie_reviews_movie ON movie_reviews (movie_id, created_at DESC) WHERE NOT hidden;
