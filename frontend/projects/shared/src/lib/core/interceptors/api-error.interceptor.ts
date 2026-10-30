import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { NzMessageService } from 'ng-zorro-antd/message';
import { catchError, throwError } from 'rxjs';

import { ApiResponse } from '../../models/api-response.model';
import { SKIP_ERROR_MESSAGE } from '../http/http-context';

const NETWORK_ERROR_MESSAGE = 'Không kết nối được máy chủ, vui lòng thử lại';
const FALLBACK_ERROR_MESSAGE = 'Đã có lỗi xảy ra, vui lòng thử lại sau';

/** Hiện thông báo lỗi chung bằng nz-message rồi ném lỗi tiếp cho nơi gọi. */
export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const message = inject(NzMessageService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !req.context.get(SKIP_ERROR_MESSAGE)) {
        message.error(resolveErrorMessage(error));
      }
      return throwError(() => error);
    }),
  );
};

export function resolveErrorMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return NETWORK_ERROR_MESSAGE;
  }
  const body = error.error as ApiResponse<unknown> | null;
  return body?.error?.message ?? FALLBACK_ERROR_MESSAGE;
}
