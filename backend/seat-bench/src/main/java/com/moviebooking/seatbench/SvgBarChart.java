package com.moviebooking.seatbench;

import java.util.List;
import java.util.Locale;

/** Biểu đồ cột nhóm dạng SVG đơn giản cho báo cáo (không cần thư viện ngoài). */
final class SvgBarChart {

    private static final String[] COLORS = {"#2a6fdb", "#e8833a", "#7a8b99", "#3aa76d"};
    private static final int WIDTH = 720;
    private static final int HEIGHT = 380;
    private static final int LEFT = 60;
    private static final int RIGHT = 20;
    private static final int TOP = 50;
    private static final int BOTTOM = 70;

    private SvgBarChart() {}

    /**
     * @param values {@code values[series][category]}
     * @param yMax giá trị lớn nhất của trục tung
     * @param valueFormat ví dụ {@code "%.0f%%"}
     */
    static String render(
            String title,
            List<String> categories,
            List<String> series,
            double[][] values,
            double yMax,
            String valueFormat) {
        int plotW = WIDTH - LEFT - RIGHT;
        int plotH = HEIGHT - TOP - BOTTOM;
        double groupW = (double) plotW / categories.size();
        double barW = groupW * 0.7 / series.size();
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(
                Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\""
                        + " font-family=\"Arial, sans-serif\" font-size=\"12\">%n",
                WIDTH, HEIGHT, WIDTH, HEIGHT));
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
        svg.append(String.format(
                Locale.ROOT, "<text x=\"%d\" y=\"26\" font-size=\"16\" font-weight=\"bold\">%s</text>%n", LEFT, escape(title)));
        for (int t = 0; t <= 4; t++) {
            double v = yMax * t / 4;
            double y = TOP + plotH - plotH * t / 4.0;
            svg.append(String.format(
                    Locale.ROOT,
                    "<line x1=\"%d\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\" stroke=\"#e5e5e5\"/>"
                            + "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\" fill=\"#666\">%s</text>%n",
                    LEFT, WIDTH - RIGHT, y, y, LEFT - 6, y + 4, String.format(Locale.ROOT, valueFormat, v)));
        }
        for (int c = 0; c < categories.size(); c++) {
            double groupX = LEFT + groupW * c + groupW * 0.15;
            for (int s = 0; s < series.size(); s++) {
                double v = values[s][c];
                double h = plotH * Math.min(1, v / yMax);
                double x = groupX + barW * s;
                double y = TOP + plotH - h;
                svg.append(String.format(
                        Locale.ROOT,
                        "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\"/>"
                                + "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-size=\"10\" fill=\"#333\">%s</text>%n",
                        x, y, barW - 2, h, COLORS[s % COLORS.length], x + (barW - 2) / 2, y - 3,
                        String.format(Locale.ROOT, valueFormat, v)));
            }
            svg.append(String.format(
                    Locale.ROOT,
                    "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">%s</text>%n",
                    LEFT + groupW * c + groupW / 2, TOP + plotH + 18, escape(categories.get(c))));
        }
        for (int s = 0; s < series.size(); s++) {
            int x = LEFT + s * 200;
            int y = HEIGHT - 22;
            svg.append(String.format(
                    Locale.ROOT,
                    "<rect x=\"%d\" y=\"%d\" width=\"12\" height=\"12\" fill=\"%s\"/>"
                            + "<text x=\"%d\" y=\"%d\">%s</text>%n",
                    x, y - 10, COLORS[s % COLORS.length], x + 18, y, escape(series.get(s))));
        }
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
