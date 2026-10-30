package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.GroupVote;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupVoteRepository extends JpaRepository<GroupVote, GroupVote.Key> {

    @Query("select v from GroupVote v where v.id.optionId in :optionIds")
    List<GroupVote> findByOptions(@Param("optionIds") Collection<Long> optionIds);

    /** Phiếu của người rời nhóm không còn tính. */
    @Modifying
    @Query("delete from GroupVote v where v.id.userId = :userId and v.id.optionId in "
            + "(select o.id from GroupShowtimeOption o where o.groupId = :groupId)")
    int deleteByMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
