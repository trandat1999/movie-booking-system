import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/user.model';

/**
 * Refresh token nằm trong cookie httpOnly do backend đặt; trình duyệt tự gửi kèm vì cùng origin (proxy khi dev).
 * Các API này tự xử lý lỗi nên không hiện thông báo lỗi chung.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly api = inject(ApiClient);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', request, { context: skipErrorMessage() });
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/register', request, { context: skipErrorMessage() });
  }

  refresh(): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/refresh', null, { context: skipErrorMessage() });
  }

  logout(): Observable<void> {
    return this.api.post<void>('/auth/logout', null, { context: skipErrorMessage() });
  }
}
