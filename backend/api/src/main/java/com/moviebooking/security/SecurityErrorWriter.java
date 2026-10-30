package com.moviebooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.common.response.ApiError;
import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Ghi lỗi 401/403 phát sinh ở filter chain theo cùng định dạng {@link ApiResponse}. */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.fail(new ApiError(code.name(), code.getDefaultMessage(), null)));
    }
}
