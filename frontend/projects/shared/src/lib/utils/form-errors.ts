import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';

import { resolveErrorMessage } from '../core/interceptors/api-error.interceptor';
import { ApiResponse, FieldViolation } from '../models/api-response.model';

/**
 * Gắn lỗi validate từ backend (VALIDATION_ERROR) vào control cùng tên dưới dạng `{ server: message }`.
 * Trả về thông điệp chung để hiển thị trên form.
 */
export function applyServerErrors(form: FormGroup, error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Đã có lỗi xảy ra, vui lòng thử lại sau';
  }
  const body = error.error as ApiResponse<unknown> | null;
  if (body?.error?.code === 'VALIDATION_ERROR' && Array.isArray(body.error.details)) {
    for (const violation of body.error.details as FieldViolation[]) {
      const control = form.get(violation.field);
      if (control) {
        control.setErrors({ ...(control.errors ?? {}), server: violation.message });
        control.markAsDirty();
      }
    }
  }
  return resolveErrorMessage(error);
}

/** Đánh dấu toàn bộ control để hiện lỗi validate trước khi gửi form. */
export function touchAll(form: FormGroup): void {
  Object.values(form.controls).forEach((control: AbstractControl) => {
    control.markAsDirty();
    control.updateValueAndValidity({ onlySelf: true });
  });
  // Chạy lại validator cấp form (ví dụ matchFields) sau khi validator của từng control đã xóa lỗi cũ
  form.updateValueAndValidity();
}

/** Chuyển Date của date-picker thành yyyy-MM-dd theo giờ địa phương (không lệch ngày do UTC). */
export function toIsoDate(date: Date | null | undefined): string | null {
  if (!date) {
    return null;
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** yyyy-MM-dd → Date địa phương. */
export function fromIsoDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}
