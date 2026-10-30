package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.GroupSeatAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupSeatAssignmentRepository extends JpaRepository<GroupSeatAssignment, Long> {

    List<GroupSeatAssignment> findByGroupId(Long groupId);
}
