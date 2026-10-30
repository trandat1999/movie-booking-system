package com.moviebooking.seatengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.moviebooking.seatengine.SeatAssigner.Assignment;
import com.moviebooking.seatengine.SeatAssigner.Person;
import com.moviebooking.seatengine.SeatAssigner.Preference;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeatAssignerTest {

    // Id ghế trong Layouts = hàng · 100 + cột
    private static Person person(String key) {
        return new Person(key, Preference.NONE, Set.of());
    }

    private static Person nextTo(String key, String... others) {
        return new Person(key, Preference.NONE, Set.of(others));
    }

    private static Person prefers(String key, Preference preference) {
        return new Person(key, preference, Set.of());
    }

    private static boolean adjacent(Assignment a, String p, String q) {
        long s = a.seatByPerson().get(p);
        long t = a.seatByPerson().get(q);
        return s / 100 == t / 100 && Math.abs(s - t) == 1;
    }

    @Test
    void guestEndsUpNextToHost() {
        SeatLayout layout = Layouts.parse("..SSSS..");
        List<Long> seats = List.of(2L, 3L, 4L, 5L);
        // Thứ tự khởi tạo đặt khách của An ở cuối, cách An 2 ghế
        Assignment a = SeatAssigner.assign(
                layout, seats, List.of(person("lead"), person("an"), person("binh"), nextTo("an#1", "an")));

        assertThat(adjacent(a, "an", "an#1")).isTrue();
        assertThat(a.satisfied()).isEqualTo(1);
        assertThat(a.requested()).isEqualTo(1);
        assertThat(a.seatByPerson().values()).containsExactlyInAnyOrderElementsOf(seats);
    }

    @Test
    void mutualNextToPairCountsOnce() {
        SeatLayout layout = Layouts.parse("SSSS");
        Assignment a = SeatAssigner.assign(
                layout, List.of(0L, 1L, 2L, 3L), List.of(nextTo("an", "dung"), person("binh"), person("chi"), nextTo("dung", "an")));

        assertThat(adjacent(a, "an", "dung")).isTrue();
        assertThat(a.requested()).isEqualTo(1);
        assertThat(a.satisfied()).isEqualTo(1);
    }

    @Test
    void satisfiesSeveralWishesAcrossTwoRows() {
        // Cụm 2 hàng × 3 ghế; Chi muốn ngồi cạnh An, Em cạnh Bình, Dũng thích đầu dãy
        SeatLayout layout = Layouts.parse("SSSSS", "SSSSS");
        List<Long> seats = List.of(1L, 2L, 3L, 101L, 102L, 103L);
        Assignment a = SeatAssigner.assign(layout, seats, List.of(
                person("an"),
                person("binh"),
                nextTo("chi", "an"),
                prefers("dung", Preference.AISLE),
                nextTo("em", "binh"),
                person("giang")));

        assertThat(adjacent(a, "chi", "an")).isTrue();
        assertThat(adjacent(a, "em", "binh")).isTrue();
        // Không ghế nào của cụm ở mép phòng → sở thích đầu dãy không đáp ứng được
        assertThat(a.requested()).isEqualTo(3);
        assertThat(a.satisfied()).isEqualTo(2);
    }

    @Test
    void aislePreferenceGetsSeatNextToAisle() {
        SeatLayout layout = Layouts.parse("SSS.SSS");
        // Nhóm mua C(2) | lối đi | D(4), E(5)
        Assignment a = SeatAssigner.assign(
                layout, List.of(2L, 4L, 5L), List.of(person("an"), person("binh"), prefers("chi", Preference.AISLE)));

        assertThat(a.seatByPerson().get("chi")).isIn(2L, 4L);
        assertThat(a.satisfied()).isEqualTo(1);
    }

    @Test
    void centerPreferenceGetsSeatClosestToScreenAxis() {
        SeatLayout layout = Layouts.parse("SSSSSSSS");
        Assignment a = SeatAssigner.assign(
                layout, List.of(0L, 1L, 2L, 3L), List.of(prefers("an", Preference.CENTER), person("binh"), person("chi"), person("dung")));

        // Trục giữa phòng ở cột 4 → hai ghế gần trục nhất là cột 3, 2
        assertThat(a.seatByPerson().get("an")).isIn(2L, 3L);
        assertThat(a.satisfied()).isEqualTo(1);
    }

    @Test
    void conflictingWishesKeepBestScore() {
        // An muốn ngồi cạnh cả Bình và Chi, Dũng cũng muốn cạnh An — chỉ được tối đa 2 người cạnh An
        SeatLayout layout = Layouts.parse("SSSS");
        Assignment a = SeatAssigner.assign(layout, List.of(0L, 1L, 2L, 3L), List.of(
                nextTo("an", "binh", "chi"), person("binh"), person("chi"), nextTo("dung", "an")));

        assertThat(a.requested()).isEqualTo(3);
        assertThat(a.satisfied()).isEqualTo(2);
    }

    @Test
    void handlesMoreSeatsThanPeopleAndMorePeopleThanSeats() {
        SeatLayout layout = Layouts.parse("SSSS");
        Assignment fewer = SeatAssigner.assign(layout, List.of(0L, 1L, 2L, 3L), List.of(person("an"), nextTo("binh", "an")));
        assertThat(fewer.seatByPerson()).hasSize(2);
        assertThat(adjacent(fewer, "an", "binh")).isTrue();

        Assignment more = SeatAssigner.assign(layout, List.of(0L, 1L), List.of(person("an"), person("binh"), person("chi")));
        assertThat(more.seatByPerson()).containsOnlyKeys("an", "binh");
    }

    @Test
    void isDeterministic() {
        SeatLayout layout = RandomRooms.room(new java.util.Random(7), 15, 20, 0.0);
        List<Long> seats = layout.seats().stream().limit(10).map(SeatSpec::id).toList();
        List<Person> people = List.of(
                nextTo("a", "j"), person("b"), prefers("c", Preference.AISLE), nextTo("d", "b"), person("e"),
                prefers("f", Preference.CENTER), person("g"), nextTo("h", "c"), person("i"), person("j"));

        assertThat(SeatAssigner.assign(layout, seats, people)).isEqualTo(SeatAssigner.assign(layout, seats, people));
    }
}
