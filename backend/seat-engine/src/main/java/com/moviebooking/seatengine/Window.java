package com.moviebooking.seatengine;

/** Dãy ghế liền kề {@code seats[from..to]} trong một hàng — một phần của phương án. Giá trị tính sẵn khi tạo. */
final class Window {

    final SeatRow row;
    final int from;
    final int to;
    final int capacity;
    final long price;
    /** Phần giá vượt ngân sách của các ghế trong cửa sổ (VND). */
    final long overBudget;
    final int orphans;
    /** Σ capacity · tâm ngang của ghế — để tính tâm cụm có trọng số. */
    final double weightedX;
    final int colStart;
    final int colEnd;

    Window(SeatRow row, int from, int to, SuggestionRequest request) {
        this.row = row;
        this.from = from;
        this.to = to;
        int cap = 0;
        long sum = 0;
        long over = 0;
        double x = 0;
        for (int i = from; i <= to; i++) {
            SeatSpec seat = row.seats[i];
            cap += seat.capacity();
            sum += seat.price();
            x += seat.capacity() * seat.centerX();
            if (request.maxPricePerPerson() != null) {
                over += Math.max(0, seat.price() - request.maxPricePerPerson() * seat.capacity());
            }
        }
        this.capacity = cap;
        this.price = sum;
        this.overBudget = over;
        this.weightedX = x;
        this.orphans = row.orphansCreatedBy(from, to);
        this.colStart = row.seats[from].gridCol();
        this.colEnd = row.seats[to].colEnd();
    }

    boolean overlapsColumns(Window other) {
        return Math.min(colEnd, other.colEnd) > Math.max(colStart, other.colStart);
    }

    /**
     * Tỷ lệ chồng cột với cửa sổ khác, trong [0, 1]: phần giao / bề rộng cửa sổ hẹp hơn. Cửa sổ hẹp nằm gọn trong
     * cửa sổ rộng (ví dụ 3 + 2 ghế) được coi là thẳng cột hoàn toàn.
     */
    double columnOverlapRatio(Window other) {
        int intersection = Math.max(0, Math.min(colEnd, other.colEnd) - Math.max(colStart, other.colStart));
        int narrower = Math.min(colEnd - colStart, other.colEnd - other.colStart);
        return (double) intersection / narrower;
    }

    int seatCount() {
        return to - from + 1;
    }
}
