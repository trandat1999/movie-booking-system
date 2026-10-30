package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.GroupEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {

    List<GroupEvent> findTop30ByGroupIdOrderByCreatedAtDescIdDesc(Long groupId);
}
