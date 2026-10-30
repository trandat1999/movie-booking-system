package com.moviebooking.modules.auth.repository;

import com.moviebooking.modules.auth.entity.RefreshToken;
import com.moviebooking.modules.auth.enums.RevokeReason;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Khóa dòng (SELECT ... FOR UPDATE) để hai request refresh cùng token được xử lý tuần tự. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    boolean existsByFamilyIdAndRevokedAtIsNull(UUID familyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :at, t.revokeReason = :reason "
            + "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(
            @Param("familyId") UUID familyId, @Param("at") Instant at, @Param("reason") RevokeReason reason);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :at, t.revokeReason = :reason "
            + "where t.userId = :userId and t.revokedAt is null")
    int revokeAllByUserId(
            @Param("userId") Long userId, @Param("at") Instant at, @Param("reason") RevokeReason reason);
}
