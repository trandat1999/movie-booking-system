import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthStore } from './auth.store';

/** Yêu cầu đăng nhập; chưa đăng nhập → /login?returnUrl=... */
export const authGuard: CanActivateFn = (_route, state) => {
  const store = inject(AuthStore);
  const router = inject(Router);
  return store.isAuthenticated() || router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Chỉ dành cho khách (trang đăng nhập, đăng ký); đã đăng nhập → về trang chủ. */
export const guestGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);
  return !store.isAuthenticated() || router.createUrlTree(['/']);
};

/**
 * Yêu cầu đăng nhập và có quyền {@link permission}. Thiếu quyền → {@link fallbackUrl}.
 * Đây chỉ là trải nghiệm giao diện — backend luôn kiểm tra lại quyền (BR11).
 */
export function permissionGuard(permission: string, fallbackUrl = '/'): CanActivateFn {
  return (_route, state) => {
    const store = inject(AuthStore);
    const router = inject(Router);
    if (!store.isAuthenticated()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    return store.hasPermission(permission) || router.createUrlTree([fallbackUrl]);
  };
}
