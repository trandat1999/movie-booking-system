package com.moviebooking.modules.showtime.repository;

import com.moviebooking.modules.showtime.entity.BasePrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasePriceRepository extends JpaRepository<BasePrice, BasePrice.Key> {}
