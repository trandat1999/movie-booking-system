package com.moviebooking.modules.movie.dto.request;

import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Tạo/cập nhật phim (MOVADM02, MOVADM03). Đạo diễn/diễn viên theo thứ tự hiển thị. */
public record MovieRequest(
        @NotBlank(message = "Vui lòng nhập tên phim") @Size(max = 200, message = "Tên phim tối đa 200 ký tự")
                String title,
        @Size(max = 200, message = "Tên gốc tối đa 200 ký tự") String originalTitle,
        @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự") String description,
        @NotNull(message = "Vui lòng nhập thời lượng")
                @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
                @Max(value = 600, message = "Thời lượng tối đa 600 phút")
                Integer durationMin,
        @NotNull(message = "Vui lòng chọn phân loại độ tuổi") AgeRating ageRating,
        @NotBlank(message = "Vui lòng nhập ngôn ngữ") @Size(max = 50) String language,
        @NotNull(message = "Vui lòng chọn ngày khởi chiếu") LocalDate releaseDate,
        @Size(max = 500) String posterUrl,
        @Size(max = 500) String bannerUrl,
        @Size(max = 500) String trailerUrl,
        @NotNull(message = "Vui lòng chọn trạng thái") MovieStatus status,
        @NotEmpty(message = "Chọn ít nhất một thể loại") Set<@NotNull Long> genreIds,
        List<@NotNull Long> directorIds,
        List<@NotNull Long> actorIds) {}
