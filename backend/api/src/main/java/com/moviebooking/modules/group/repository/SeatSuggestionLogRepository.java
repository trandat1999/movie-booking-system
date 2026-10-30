package com.moviebooking.modules.group.repository;

import com.moviebooking.modules.group.entity.SeatSuggestionLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatSuggestionLogRepository extends JpaRepository<SeatSuggestionLog, Long> {

    Optional<SeatSuggestionLog> findByIdAndGroupId(Long id, Long groupId);

    java.util.List<SeatSuggestionLog> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
