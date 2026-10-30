package com.moviebooking.modules.setting.repository;

import com.moviebooking.modules.setting.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {}
