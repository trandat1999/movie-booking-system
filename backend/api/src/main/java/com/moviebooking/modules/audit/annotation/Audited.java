package com.moviebooking.modules.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ghi nhật ký thao tác (BR12) sau khi method thực thi thành công. Dùng trên method của controller.
 *
 * <pre>
 * &#64;Audited(action = "USER_STATUS_UPDATE", resource = "user", resourceId = "#id", data = "#request")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Mã hành động, viết hoa, ví dụ {@code USER_LOCK}. */
    String action();

    /** Loại tài nguyên, ví dụ {@code user}, {@code order}. */
    String resource();

    /** Biểu thức SpEL trên tham số method để lấy mã tài nguyên, ví dụ {@code #id}. */
    String resourceId() default "";

    /** Biểu thức SpEL trên tham số method cho dữ liệu chi tiết cần lưu, ví dụ {@code #request}. */
    String data() default "";
}
