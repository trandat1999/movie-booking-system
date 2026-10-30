/**
 * Các module nghiệp vụ (auth, user, movie, cinema, showtime, booking, payment, group, checkin, promotion,
 * notification, statistics, setting, audit). Mỗi module tự chứa đủ các tầng:
 *
 * <pre>
 * modules/&lt;module&gt;/
 * ├── controller/      REST, chỉ nhận/trả DTO
 * ├── dto/request/, dto/response/
 * ├── mapper/          MapStruct entity ↔ DTO
 * ├── service/         interface
 * ├── service/impl/    nghiệp vụ + @Transactional
 * ├── repository/      Spring Data JPA, chỉ dùng trong module
 * ├── entity/
 * ├── enums/
 * └── event/           ApplicationEvent + listener (sau commit)
 * </pre>
 *
 * <p>Quy tắc phân tầng L1–L9: PLAN.md mục 2A.2, được kiểm tra bởi {@code ArchitectureTest}.
 */
package com.moviebooking.modules;
