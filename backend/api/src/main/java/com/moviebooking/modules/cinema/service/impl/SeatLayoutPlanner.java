package com.moviebooking.modules.cinema.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Kiểm tra sơ đồ ghế do quản trị viên vẽ và đánh nhãn ghế (CIN04, CIN07).
 *
 * <ul>
 *   <li>Ghế phải nằm trong lưới; ghế đôi chiếm 2 ô liền nhau trên cùng hàng; không ô nào bị 2 ghế chiếm.
 *   <li>Hàng lưới không có ghế là lối đi ngang → không được đặt chữ cái.
 *   <li>Hàng có ghế được đặt nhãn A, B, …, Z, AA, AB… từ trên (gần màn hình) xuống; số ghế đánh từ 1, trái sang
 *       phải, bỏ qua ô trống (lối đi dọc).
 * </ul>
 *
 * Lớp thuần, không phụ thuộc Spring để unit test trực tiếp.
 */
public final class SeatLayoutPlanner {

    private SeatLayoutPlanner() {}

    public record Cell(int gridRow, int gridCol, String seatTypeCode) {}

    public record PlannedSeat(String rowLabel, int number, int gridRow, int gridCol, int colSpan, String seatTypeCode) {}

    /**
     * @param colSpanByType số ô mỗi loại ghế chiếm; loại không có trong map là không hợp lệ
     * @return ghế đã đánh nhãn, theo thứ tự hàng rồi cột
     * @throws InvalidLayoutException kèm danh sách mọi lỗi tìm thấy
     */
    public static List<PlannedSeat> plan(
            int gridRows, int gridCols, List<Cell> cells, Map<String, Integer> colSpanByType) {
        List<String> errors = new ArrayList<>();
        if (cells.isEmpty()) {
            errors.add("Sơ đồ phải có ít nhất một ghế");
        }
        Map<Long, Cell> occupied = new HashMap<>();
        TreeMap<Integer, List<Cell>> byRow = new TreeMap<>();

        for (Cell cell : cells) {
            Integer span = colSpanByType.get(cell.seatTypeCode());
            if (span == null) {
                errors.add("Loại ghế không hợp lệ: " + cell.seatTypeCode());
                continue;
            }
            if (cell.gridRow() < 0 || cell.gridRow() >= gridRows || cell.gridCol() < 0
                    || cell.gridCol() + span > gridCols) {
                errors.add("Ghế tại hàng %d, cột %d nằm ngoài lưới".formatted(cell.gridRow() + 1, cell.gridCol() + 1));
                continue;
            }
            boolean overlap = false;
            for (int c = cell.gridCol(); c < cell.gridCol() + span; c++) {
                Cell previous = occupied.putIfAbsent(key(cell.gridRow(), c), cell);
                if (previous != null) {
                    overlap = true;
                }
            }
            if (overlap) {
                errors.add("Ô hàng %d, cột %d bị nhiều ghế chiếm".formatted(cell.gridRow() + 1, cell.gridCol() + 1));
                continue;
            }
            byRow.computeIfAbsent(cell.gridRow(), r -> new ArrayList<>()).add(cell);
        }
        if (!errors.isEmpty()) {
            throw new InvalidLayoutException(errors);
        }

        List<PlannedSeat> seats = new ArrayList<>(cells.size());
        int rowIndex = 0;
        for (Map.Entry<Integer, List<Cell>> row : byRow.entrySet()) {
            String label = rowLabel(rowIndex++);
            List<Cell> rowCells = row.getValue();
            rowCells.sort(Comparator.comparingInt(Cell::gridCol));
            for (int i = 0; i < rowCells.size(); i++) {
                Cell cell = rowCells.get(i);
                seats.add(new PlannedSeat(
                        label, i + 1, cell.gridRow(), cell.gridCol(), colSpanByType.get(cell.seatTypeCode()),
                        cell.seatTypeCode()));
            }
        }
        return seats;
    }

    /** 0 → A, 25 → Z, 26 → AA, 27 → AB … */
    static String rowLabel(int index) {
        StringBuilder label = new StringBuilder();
        int n = index;
        do {
            label.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return label.toString();
    }

    private static long key(int row, int col) {
        return ((long) row << 32) | col;
    }

    public static final class InvalidLayoutException extends RuntimeException {

        private final transient List<String> errors;

        InvalidLayoutException(List<String> errors) {
            super(String.join("; ", errors));
            this.errors = List.copyOf(errors);
        }

        public List<String> errors() {
            return errors;
        }
    }
}
