import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, firstValueFrom, map, Observable, of, shareReplay, tap } from 'rxjs';

import { AuthApi } from '../../api/auth.api';
import { AuthResponse, LoginRequest, RegisterRequest, UserProfile } from '../../models/user.model';

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
}

/**
 * Trạng thái đăng nhập dùng chung cho web và admin.
 *
 * Access token chỉ giữ trong bộ nhớ (không lưu localStorage để tránh bị đánh cắp qua XSS). Khi tải lại trang,
 * {@link restoreSession} dùng refresh token trong cookie httpOnly để lấy lại phiên.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly authApi = inject(AuthApi);

  private readonly state = signal<AuthState>({ user: null, accessToken: null });
  private refreshInFlight$: Observable<AuthResponse> | null = null;

  readonly user = computed(() => this.state().user);
  readonly accessToken = computed(() => this.state().accessToken);
  readonly isAuthenticated = computed(() => this.state().accessToken !== null);
  readonly permissions = computed(() => new Set(this.state().user?.permissions ?? []));

  hasPermission(permission: string): boolean {
    return this.permissions().has(permission);
  }

  login(request: LoginRequest): Observable<UserProfile> {
    return this.authApi.login(request).pipe(
      tap((response) => this.setSession(response)),
      map((response) => response.user),
    );
  }

  register(request: RegisterRequest): Observable<UserProfile> {
    return this.authApi.register(request).pipe(
      tap((response) => this.setSession(response)),
      map((response) => response.user),
    );
  }

  /** Lấy access token mới. Các lời gọi đồng thời dùng chung một request. */
  refresh(): Observable<AuthResponse> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.authApi.refresh().pipe(
        tap({
          next: (response) => this.setSession(response),
          error: () => this.clearSession(),
        }),
        finalize(() => (this.refreshInFlight$ = null)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.refreshInFlight$;
  }

  /** Gọi khi khởi động ứng dụng; không bao giờ lỗi (chưa đăng nhập thì giữ trạng thái khách). */
  restoreSession(): Promise<void> {
    return firstValueFrom(
      this.refresh().pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
      ),
    );
  }

  logout(): Observable<void> {
    return this.authApi.logout().pipe(
      catchError(() => of(undefined)),
      finalize(() => this.clearSession()),
    );
  }

  /** Cập nhật thông tin người dùng sau khi sửa hồ sơ. */
  updateUser(user: UserProfile): void {
    this.state.update((state) => ({ ...state, user }));
  }

  clearSession(): void {
    this.state.set({ user: null, accessToken: null });
  }

  private setSession(response: AuthResponse): void {
    this.state.set({ user: response.user, accessToken: response.accessToken });
  }
}
