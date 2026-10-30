package com.moviebooking.modules.cinema;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomLayoutIntegrationTest extends AbstractAdminIntegrationTest {

    @Test
    void savesLayoutWithAislesAndCoupleSeatsAndComputesLabels() throws Exception {
        long roomId = createRoom(createCinema());

        putJson("/api/v1/admin/rooms/{id}/layout", smallLayout(), roomId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.room.seatCount").value(6))
                .andExpect(jsonPath("$.data.room.capacity").value(8)) // 2 ghế đôi tính 4 chỗ
                .andExpect(jsonPath("$.data.room.gridCols").value(5))
                .andExpect(jsonPath("$.data.seats[*].rowLabel", contains("A", "A", "A", "A", "B", "B")))
                .andExpect(jsonPath("$.data.seats[*].number", contains(1, 2, 3, 4, 1, 2)))
                .andExpect(jsonPath("$.data.seats[4].colSpan").value(2));

        // Vẽ lại (phòng chưa có suất chiếu) được phép
        putJson("/api/v1/admin/rooms/{id}/layout",
                        Map.of("gridRows", 1, "gridCols", 1, "seats",
                                List.of(Map.of("gridRow", 0, "gridCol", 0, "seatTypeCode", "STANDARD"))),
                        roomId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats", hasSize(1)));
    }

    @Test
    void rejectsOverlappingAndOutOfGridSeats() throws Exception {
        long roomId = createRoom(createCinema());

        putJson("/api/v1/admin/rooms/{id}/layout",
                        Map.of("gridRows", 1, "gridCols", 3, "seats", List.of(
                                Map.of("gridRow", 0, "gridCol", 0, "seatTypeCode", "COUPLE"),
                                Map.of("gridRow", 0, "gridCol", 1, "seatTypeCode", "STANDARD"),
                                Map.of("gridRow", 0, "gridCol", 2, "seatTypeCode", "COUPLE"))),
                        roomId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SEAT_LAYOUT"))
                .andExpect(jsonPath("$.error.details", hasSize(2)));
    }

    @Test
    void roomListShowsSeatCounts() throws Exception {
        long cinemaId = createCinema();
        long roomId = createRoom(cinemaId);
        putJson("/api/v1/admin/rooms/{id}/layout", smallLayout(), roomId).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/cinemas/{id}/rooms", cinemaId).header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seatCount").value(6))
                .andExpect(jsonPath("$.data[0].layoutLocked").value(false));
        mockMvc.perform(get("/api/v1/admin/cinemas/{id}", cinemaId).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.roomCount").value(1))
                .andExpect(jsonPath("$.data.amenities[0]").value("Bãi đỗ xe"));
    }

    @Test
    void duplicateRoomNameInSameCinemaIsRejected() throws Exception {
        long cinemaId = createCinema();
        Map<String, Object> body = Map.of("name", "Phòng 1", "roomType", "STANDARD", "status", "ACTIVE");
        postJson("/api/v1/admin/cinemas/{id}/rooms", body, cinemaId).andExpect(status().isCreated());
        postJson("/api/v1/admin/cinemas/{id}/rooms", body, cinemaId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROOM_ALREADY_EXISTS"));
    }
}
