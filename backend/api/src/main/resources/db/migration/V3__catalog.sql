-- M2: danh mục phim (MOVADM)

CREATE TABLE genres (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    slug       VARCHAR(60)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_genres_name UNIQUE (name),
    CONSTRAINT uq_genres_slug UNIQUE (slug)
);

-- Đạo diễn, diễn viên
CREATE TABLE people (
    id         BIGSERIAL    PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_people_full_name ON people (lower(full_name));

CREATE TABLE movies (
    id             BIGSERIAL     PRIMARY KEY,
    title          VARCHAR(200)  NOT NULL,
    original_title VARCHAR(200),
    slug           VARCHAR(220)  NOT NULL,
    description    TEXT,
    duration_min   INT           NOT NULL,
    age_rating     VARCHAR(5)    NOT NULL,
    language       VARCHAR(50)   NOT NULL,
    release_date   DATE          NOT NULL,
    poster_url     VARCHAR(500),
    banner_url     VARCHAR(500),
    trailer_url    VARCHAR(500),
    status         VARCHAR(20)   NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_movies_slug UNIQUE (slug),
    CONSTRAINT ck_movies_duration CHECK (duration_min BETWEEN 1 AND 600),
    CONSTRAINT ck_movies_age_rating CHECK (age_rating IN ('P', 'K', 'T13', 'T16', 'T18')),
    CONSTRAINT ck_movies_status CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'ENDED'))
);
CREATE INDEX ix_movies_status_release ON movies (status, release_date DESC);

CREATE TABLE movie_genres (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres (id),
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE movie_people (
    id         BIGSERIAL   PRIMARY KEY,
    movie_id   BIGINT      NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    person_id  BIGINT      NOT NULL REFERENCES people (id),
    role       VARCHAR(20) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_movie_people UNIQUE (movie_id, person_id, role),
    CONSTRAINT ck_movie_people_role CHECK (role IN ('DIRECTOR', 'ACTOR'))
);
CREATE INDEX ix_movie_people_person ON movie_people (person_id);
