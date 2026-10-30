/** Khớp với com.moviebooking.common.response.* ở backend. */

export interface FieldViolation {
  field: string;
  message: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: unknown;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
}

/** `page` bắt đầu từ 0 (giống Spring Pageable). */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
