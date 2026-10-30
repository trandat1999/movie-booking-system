import { inject, Injectable, signal } from '@angular/core';
import { AuthStore, ChangePasswordRequest, MeApi, UpdateProfileRequest, UserProfile } from '@mb/shared';
import { finalize, Observable, switchMap, tap } from 'rxjs';

/** Trạng thái trang "Tài khoản của tôi" (USR05, USR06). */
@Injectable()
export class AccountStore {
  private readonly meApi = inject(MeApi);
  private readonly auth = inject(AuthStore);

  readonly profile = signal<UserProfile | null>(null);
  readonly loading = signal(false);
  readonly savingProfile = signal(false);
  readonly changingPassword = signal(false);

  load(): void {
    this.loading.set(true);
    this.meApi
      .getProfile()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((profile) => this.profile.set(profile));
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfile> {
    this.savingProfile.set(true);
    return this.meApi.updateProfile(request).pipe(
      tap((profile) => {
        this.profile.set(profile);
        this.auth.updateUser(profile);
      }),
      finalize(() => this.savingProfile.set(false)),
    );
  }

  /** Đổi mật khẩu thành công thì mọi phiên bị thu hồi ở backend → đăng xuất phía client. */
  changePassword(request: ChangePasswordRequest): Observable<void> {
    this.changingPassword.set(true);
    return this.meApi.changePassword(request).pipe(
      switchMap(() => this.auth.logout()),
      finalize(() => this.changingPassword.set(false)),
    );
  }
}
