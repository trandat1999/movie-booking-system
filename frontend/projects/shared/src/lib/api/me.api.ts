import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { ChangePasswordRequest, UpdateProfileRequest, UserProfile } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class MeApi {
  private readonly api = inject(ApiClient);

  getProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/me');
  }

  /** Lỗi validate được hiển thị trên form nên không hiện thông báo chung. */
  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    return this.api.patch<UserProfile>('/me', request, { context: skipErrorMessage() });
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.api.post<void>('/me/password', request, { context: skipErrorMessage() });
  }
}
