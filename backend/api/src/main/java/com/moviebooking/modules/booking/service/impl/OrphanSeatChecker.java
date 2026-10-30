package com.moviebooking.modules.booking.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Quy tắc ghế lẻ (BR14): lựa chọn không được tạo ra đúng một ghế trống bị kẹt giữa ghế vừa chọn và ghế không bán
 * được / mép dãy ghế. Ghế đã lẻ từ trước (không do lựa chọn này gây ra) thì bỏ qua.
 *
 * <p>Ghế liền kề khi cùng hàng và ô bắt đầu của ghế sau = ô bắt đầu ghế trước + số ô ghế trước chiếm; ô trống ở
 * giữa là lối đi nên tách thành dãy khác. Ghế đôi tính là một ghế. Lớp thuần để unit test.
 */
public final class OrphanSeatChecker {

    private OrphanSeatChecker() {}

    /**
     * @param available ghế đang trống trước khi chọn
     */
    public record RowSeat(Long id, String label, int gridRow, int gridCol, int colSpan, boolean available) {}

    /** @return nhãn các ghế sẽ thành ghế lẻ nếu chọn {@code selected} */
    public static List<String> findOrphans(Collection<RowSeat> seats, Set<Long> selected) {
        Map<Integer, List<RowSeat>> rows = new TreeMap<>();
        seats.forEach(seat -> rows.computeIfAbsent(seat.gridRow(), r -> new ArrayList<>()).add(seat));

        List<String> orphans = new ArrayList<>();
        for (List<RowSeat> row : rows.values()) {
            if (row.stream().noneMatch(seat -> selected.contains(seat.id()))) {
                continue;
            }
            row.sort(Comparator.comparingInt(RowSeat::gridCol));
            for (List<RowSeat> block : contiguousBlocks(row)) {
                orphans.addAll(orphansInBlock(block, selected));
            }
        }
        return orphans;
    }

    private static List<List<RowSeat>> contiguousBlocks(List<RowSeat> row) {
        List<List<RowSeat>> blocks = new ArrayList<>();
        List<RowSeat> current = new ArrayList<>();
        for (RowSeat seat : row) {
            if (!current.isEmpty()) {
                RowSeat previous = current.get(current.size() - 1);
                if (seat.gridCol() != previous.gridCol() + previous.colSpan()) {
                    blocks.add(current);
                    current = new ArrayList<>();
                }
            }
            current.add(seat);
        }
        if (!current.isEmpty()) {
            blocks.add(current);
        }
        return blocks;
    }

    private static List<String> orphansInBlock(List<RowSeat> block, Set<Long> selected) {
        List<String> orphans = new ArrayList<>();
        for (int i = 0; i < block.size(); i++) {
            RowSeat seat = block.get(i);
            if (!isFreeAfter(seat, selected)) {
                continue;
            }
            boolean leftTaken = i == 0 || !isFreeAfter(block.get(i - 1), selected);
            boolean rightTaken = i == block.size() - 1 || !isFreeAfter(block.get(i + 1), selected);
            if (!leftTaken || !rightTaken) {
                continue;
            }
            // Ghế trống bị kẹt — chỉ tính nếu do lựa chọn gây ra (có ghế vừa chọn ngay bên cạnh)
            boolean besideSelection = (i > 0 && selected.contains(block.get(i - 1).id()))
                    || (i < block.size() - 1 && selected.contains(block.get(i + 1).id()));
            if (besideSelection) {
                orphans.add(seat.label());
            }
        }
        return orphans;
    }

    private static boolean isFreeAfter(RowSeat seat, Set<Long> selected) {
        return seat.available() && !selected.contains(seat.id());
    }
}
