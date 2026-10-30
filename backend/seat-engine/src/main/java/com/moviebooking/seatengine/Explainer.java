package com.moviebooking.seatengine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Giải thích ngắn cho một phương án, ví dụ "Cùng hàng E (E5–E8), gần trung tâm, không tạo ghế lẻ". */
final class Explainer {

    private Explainer() {}

    static String explain(List<Window> windows, ScoreBreakdown b) {
        List<String> parts = new ArrayList<>();
        long distinctRows = windows.stream().map(w -> w.row).distinct().count();
        if (windows.size() == 1) {
            Window w = windows.get(0);
            parts.add("Cùng hàng " + w.row.label + " (" + range(w) + ")");
        } else if (distinctRows < windows.size()) {
            // Chỉ gặp khi chấm điểm lựa chọn tự do: ghế rời rạc trong cùng một hàng
            parts.add("Tách " + windows.size() + " cụm trên " + distinctRows + " hàng");
        } else {
            String rows = windows.get(0).row.label + "–" + windows.get(windows.size() - 1).row.label;
            String split = windows.stream()
                    .map(w -> String.valueOf(w.capacity))
                    .collect(Collectors.joining(" + "));
            parts.add("Chia " + windows.size() + " hàng " + rows + " (" + split + " chỗ)");
            parts.add(b.misalignment() <= 0.34 ? "thẳng cột" : "lệch cột");
        }
        if (b.center() <= 0.2) {
            parts.add("vị trí trung tâm");
        } else if (b.center() <= 0.4) {
            parts.add("gần trung tâm");
        } else {
            parts.add("xa trung tâm");
        }
        parts.add(b.orphanSeats() == 0 ? "không tạo ghế lẻ" : "để lại " + b.orphanSeats() + " ghế lẻ");
        if (b.wastedPlaces() > 0) {
            parts.add("dư " + b.wastedPlaces() + " chỗ do ghế đôi");
        }
        if (b.overBudget() > 0) {
            parts.add("vượt ngân sách");
        }
        return String.join(", ", parts);
    }

    private static String range(Window w) {
        String first = w.row.seats[w.from].label();
        return w.from == w.to ? first : first + "–" + w.row.seats[w.to].label();
    }
}
