package com.moviebooking.modules.movie;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MovieAdminIntegrationTest extends AbstractAdminIntegrationTest {

    @Test
    void createMovieWithGenresAndOrderedCredits() throws Exception {
        long genre = createGenre();
        long director = createPerson("Đạo Diễn A");
        long actor1 = createPerson("Diễn Viên B");
        long actor2 = createPerson("Diễn Viên C");
        Map<String, Object> body = movieBody("Mùa Hè Đỏ Lửa", 120, "COMING_SOON", List.of(genre));
        body.put("directorIds", List.of(director));
        body.put("actorIds", List.of(actor2, actor1, actor2));

        long id = idOf(postJson("/api/v1/admin/movies", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value(org.hamcrest.Matchers.startsWith("mua-he-do-lua")))
                .andExpect(jsonPath("$.data.directors[0].fullName").value("Đạo Diễn A"))
                .andExpect(jsonPath("$.data.actors[*].id", org.hamcrest.Matchers.contains((int) actor2, (int) actor1)))
                .andReturn());

        // Đổi thứ tự diễn viên: phải cập nhật tại chỗ, không vướng ràng buộc UNIQUE (movie, person, role)
        body.put("actorIds", List.of(actor1, actor2));
        putJson("/api/v1/admin/movies/{id}", body, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actors[*].id", org.hamcrest.Matchers.contains((int) actor1, (int) actor2)));

        mockMvc.perform(get("/api/v1/admin/movies").param("q", "mùa hè đỏ").header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem((int) id)));
    }

    /** Hồi quy: nạp thể loại + người tham gia cùng lúc không được nhân bản người tham gia theo số thể loại. */
    @Test
    void creditsAreNotDuplicatedWhenMovieHasSeveralGenres() throws Exception {
        Map<String, Object> body = movieBody(unique("Nhiều Thể Loại"), 100, "NOW_SHOWING", List.of(createGenre(), createGenre(), createGenre()));
        body.put("directorIds", List.of(createPerson("Đạo Diễn X")));
        body.put("actorIds", List.of(createPerson("Diễn Viên Y"), createPerson("Diễn Viên Z")));
        String response = postJson("/api/v1/admin/movies", body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
        String slug = com.jayway.jsonpath.JsonPath.read(response, "$.data.slug");

        mockMvc.perform(get("/api/v1/admin/movies/{id}", id).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.genres", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.data.directors", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.actors", org.hamcrest.Matchers.hasSize(2)));
        mockMvc.perform(get("/api/v1/movies/{slug}", slug))
                .andExpect(jsonPath("$.data.actors[*].fullName", org.hamcrest.Matchers.contains("Diễn Viên Y", "Diễn Viên Z")));
    }

    @Test
    void sameTitleGetsUniqueSlug() throws Exception {
        long genre = createGenre();
        String title = unique("Trùng Tên");
        postJson("/api/v1/admin/movies", movieBody(title, 90, "NOW_SHOWING", List.of(genre))).andExpect(status().isCreated());
        postJson("/api/v1/admin/movies", movieBody(title, 90, "NOW_SHOWING", List.of(genre)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value(org.hamcrest.Matchers.endsWith("-2")));
    }

    @Test
    void validatesMovieInput() throws Exception {
        Map<String, Object> body = movieBody("", 0, "NOW_SHOWING", List.of());
        postJson("/api/v1/admin/movies", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[*].field", containsInAnyOrder("title", "durationMin", "genreIds")));
        postJson("/api/v1/admin/movies", movieBody("Phim", 90, "NOW_SHOWING", List.of(999_999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GENRE_NOT_FOUND"));
    }

    @Test
    void genreInUseCannotBeDeletedButUnusedCan() throws Exception {
        long used = createGenre();
        long unused = createGenre();
        postJson("/api/v1/admin/movies", movieBody(unique("Phim"), 90, "NOW_SHOWING", List.of(used)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/admin/genres/{id}", used).header("Authorization", admin.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GENRE_IN_USE"));
        mockMvc.perform(delete("/api/v1/admin/genres/{id}", unused).header("Authorization", admin.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateGenreNameIsRejected() throws Exception {
        String name = unique("Hài");
        postJson("/api/v1/admin/genres", Map.of("name", name)).andExpect(status().isCreated());
        postJson("/api/v1/admin/genres", Map.of("name", name.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GENRE_ALREADY_EXISTS"));
    }

    @Test
    void staffCannotManageMovies() throws Exception {
        Session staff = login(createUser(uniqueEmail("staff"), Role.STAFF).getEmail(), PASSWORD);
        mockMvc.perform(get("/api/v1/admin/movies").header("Authorization", staff.bearer()))
                .andExpect(status().isForbidden());
    }
}
