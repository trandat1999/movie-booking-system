package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /** Thành viên đang ở trong nhóm, kèm người dùng, theo thứ tự tham gia. */
    @Query("select m from GroupMember m join fetch m.user where m.groupId = :groupId and m.leftAt is null "
            + "order by m.joinedAt, m.id")
    List<GroupMember> findActive(@Param("groupId") Long groupId);

    @Query("select m from GroupMember m join fetch m.user where m.groupId = :groupId and m.user.id = :userId")
    Optional<GroupMember> findByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("select count(m) > 0 from GroupMember m where m.groupId = :groupId and m.user.id = :userId "
            + "and m.leftAt is null")
    boolean isActiveMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("select m.groupId, count(m) from GroupMember m where m.groupId in :groupIds and m.leftAt is null "
            + "group by m.groupId")
    List<Object[]> countActiveByGroup(@Param("groupIds") List<Long> groupIds);
}
