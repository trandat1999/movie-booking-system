package com.moviebooking.devdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.moviebooking.modules.cinema.dto.request.RoomLayoutRequest;
import com.moviebooking.modules.cinema.enums.RoomType;
import com.moviebooking.modules.cinema.service.impl.SeatLayoutPlanner;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Sơ đồ demo phải hợp lệ theo đúng quy tắc của trình thiết kế sơ đồ. */
class DevCatalogSeederLayoutTest {

    @Test
    void generatedLayoutsPassPlanner() {
        var plan = new DevCatalogSeeder.RoomPlan("Phòng 1", RoomType.STANDARD, 10, 14, Set.of(3, 11), 4, 7, true);
        RoomLayoutRequest layout = DevCatalogSeeder.layout(plan);

        List<SeatLayoutPlanner.PlannedSeat> seats = SeatLayoutPlanner.plan(
                layout.gridRows(),
                layout.gridCols(),
                layout.seats().stream()
                        .map(c -> new SeatLayoutPlanner.Cell(c.gridRow(), c.gridCol(), c.seatTypeCode()))
                        .toList(),
                Map.of("STANDARD", 1, "VIP", 1, "COUPLE", 2, "ACCESSIBLE", 1));

        assertThat(layout.gridCols()).isEqualTo(16); // 14 ghế + 2 lối đi
        assertThat(seats).filteredOn(s -> s.rowLabel().equals("A")).hasSize(14);
        // hàng cuối: các khối 3 | 8 | 3 ghế → 1 đôi + 1 đơn | 4 đôi | 1 đôi + 1 đơn
        assertThat(seats).filteredOn(s -> s.rowLabel().equals("J")).extracting(SeatLayoutPlanner.PlannedSeat::seatTypeCode)
                .containsExactly("COUPLE", "STANDARD", "COUPLE", "COUPLE", "COUPLE", "COUPLE", "COUPLE", "STANDARD");
        assertThat(seats).filteredOn(s -> s.seatTypeCode().equals("ACCESSIBLE")).hasSize(2);
    }
}
