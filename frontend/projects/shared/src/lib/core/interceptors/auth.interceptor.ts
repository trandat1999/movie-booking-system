import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthStore } from '../auth/auth.store';
import { API_BASE_URL } from '../config/api-config';

/**
 * Gắn access token vào request API. Gặp 401 khi đang đăng nhập → refresh token một lần rồi gửi lại request;
 * refresh thất bại → xóa phiên và chuyển về trang đăng nhập.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(AuthStore);
  const router = inject(Router);
  const baseUrl = inject(API_BASE_URL);

  if (!req.url.startsWith(baseUrl) || req.url.startsWith(`${baseUrl}/auth/`)) {
    return next(req);
  }

  const token = store.accessToken();
  return next(token ? withToken(req, token) : req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
        return throwError(() => error);
      }
      return store.refresh().pipe(
        switchMap((response) => next(withToken(req, response.accessToken))),
        catchError(() => {
          void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
          return throwError(() => error);
        }),
      );
    }),
  );
};

function withToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
