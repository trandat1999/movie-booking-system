import { HttpContext, HttpContextToken } from '@angular/common/http';

/** Bật cho request tự xử lý lỗi, để interceptor không hiện thông báo lỗi chung. */
export const SKIP_ERROR_MESSAGE = new HttpContextToken<boolean>(() => false);

export function skipErrorMessage(): HttpContext {
  return new HttpContext().set(SKIP_ERROR_MESSAGE, true);
}
