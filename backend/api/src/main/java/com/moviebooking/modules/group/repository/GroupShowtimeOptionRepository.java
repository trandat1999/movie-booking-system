package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.GroupShowtimeOption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupShowtimeOptionRepository extends JpaRepository<GroupShowtimeOption, Long> {

    @Query("select o from GroupShowtimeOption o join fetch o.showtime s join fetch s.room r join fetch r.cinema "
            + "where o.groupId = :groupId order by s.startAt, o.id")
    List<GroupShowtimeOption> findByGroup(@Param("groupId") Long groupId);

    Optional<GroupShowtimeOption> findByIdAndGroupId(Long id, Long groupId);

    boolean existsByGroupIdAndShowtimeId(Long groupId, Long showtimeId);
}
