package com.moviebooking.modules.promotion.repository;

import com.moviebooking.modules.promotion.entity.Promotion;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /** Khóa dòng mã khi áp vào đơn để đếm lượt dùng chính xác khi nhiều đơn áp cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where upper(p.code) = upper(:code)")
    Optional<Promotion> findForUpdateByCode(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("select p from Promotion p where (:q = '' or upper(p.code) like upper(concat('%', :q, '%')) "
            + "or upper(p.name) like upper(concat('%', :q, '%'))) order by p.createdAt desc")
    Page<Promotion> search(@Param("q") String q, Pageable pageable);
}
