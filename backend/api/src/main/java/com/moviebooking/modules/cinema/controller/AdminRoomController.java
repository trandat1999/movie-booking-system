package com.moviebooking.modules.cinema.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.cinema.dto.request.RoomLayoutRequest;
import com.moviebooking.modules.cinema.dto.request.RoomRequest;
import com.moviebooking.modules.cinema.dto.request.SeatStatusRequest;
import com.moviebooking.modules.cinema.dto.response.RoomLayoutResponse;
import com.moviebooking.modules.cinema.dto.response.RoomResponse;
import com.moviebooking.modules.cinema.dto.response.SeatResponse;
import com.moviebooking.modules.cinema.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Phòng chiếu & sơ đồ ghế")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('cinema:write', 'showtime:write')")
public class AdminRoomController {

    private final RoomService roomService;

    @Operation(summary = "Danh sách phòng của rạp (CIN03)")
    @GetMapping("/cinemas/{cinemaId}/rooms")
    public ApiResponse<List<RoomResponse>> list(@PathVariable Long cinemaId) {
        return ApiResponse.ok(roomService.listByCinema(cinemaId));
    }

    @Operation(summary = "Thêm phòng (CIN03)")
    @PostMapping("/cinemas/{cinemaId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('cinema:write')")
    @Audited(action = "ROOM_CREATE", resource = "cinema", resourceId = "#cinemaId", data = "#request")
    public ApiResponse<RoomResponse> create(@PathVariable Long cinemaId, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.ok(roomService.create(cinemaId, request));
    }

    @Operation(summary = "Phòng kèm sơ đồ ghế (CIN08)")
    @GetMapping("/rooms/{roomId}")
    public ApiResponse<RoomLayoutResponse> get(@PathVariable Long roomId) {
        return ApiResponse.ok(roomService.getLayout(roomId));
    }

    @Operation(summary = "Sửa tên, loại, trạng thái phòng (CIN03)")
    @PutMapping("/rooms/{roomId}")
    @PreAuthorize("hasAuthority('cinema:write')")
    @Audited(action = "ROOM_UPDATE", resource = "room", resourceId = "#roomId", data = "#request")
    public ApiResponse<RoomResponse> update(@PathVariable Long roomId, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.ok(roomService.update(roomId, request));
    }

    @Operation(summary = "Lưu sơ đồ ghế (CIN04, CIN07) — chỉ khi phòng chưa có suất chiếu")
    @PutMapping("/rooms/{roomId}/layout")
    @PreAuthorize("hasAuthority('cinema:write')")
    @Audited(
            action = "ROOM_LAYOUT_UPDATE",
            resource = "room",
            resourceId = "#roomId",
            data = "{'gridRows': #request.gridRows(), 'gridCols': #request.gridCols(), 'seats': #request.seats().size()}")
    public ApiResponse<RoomLayoutResponse> saveLayout(
            @PathVariable Long roomId, @Valid @RequestBody RoomLayoutRequest request) {
        return ApiResponse.ok(roomService.saveLayout(roomId, request));
    }

    @Operation(summary = "Bật/tắt bán ghế (CIN06)")
    @PatchMapping("/rooms/{roomId}/seats/{seatId}")
    @PreAuthorize("hasAuthority('cinema:write')")
    @Audited(action = "SEAT_STATUS_UPDATE", resource = "seat", resourceId = "#seatId", data = "#request")
    public ApiResponse<SeatResponse> setSeatStatus(
            @PathVariable Long roomId, @PathVariable Long seatId, @Valid @RequestBody SeatStatusRequest request) {
        return ApiResponse.ok(roomService.setSeatActive(roomId, seatId, request.active()));
    }
}
