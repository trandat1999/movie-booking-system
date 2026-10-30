import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStore, LoginFormComponent, LoginRequest, Permissions, resolveErrorMessage, safeReturnUrl } from '@mb/shared';
import { NzCardModule } from 'ng-zorro-antd/card';
import { switchMap, throwError } from 'rxjs';

const NO_ADMIN_ACCESS = 'Tài khoản không có quyền truy cập trang quản trị';

@Component({
  selector: 'adm-login-page',
  imports: [NzCardModule, LoginFormComponent],
  template: `
    <div class="page">
      <nz-card nzTitle="MovieBooking Admin">
        <lib-login-form [loading]="loading()" [error]="error()" (submitted)="login($event)" />
      </nz-card>
    </div>
  `,
  styles: `
    .page {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 16px;
      background: #f0f2f5;
    }

    nz-card {
      width: 100%;
      max-width: 400px;
    }
  `,
})
export class LoginPage implements OnInit {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    if (this.auth.hasPermission(Permissions.ADMIN_ACCESS)) {
      void this.router.navigateByUrl('/dashboard');
    } else if (this.auth.isAuthenticated()) {
      // Đang đăng nhập bằng tài khoản khách hàng (phiên dùng chung cookie với web người dùng)
      this.error.set(NO_ADMIN_ACCESS);
    }
  }

  protected login(request: LoginRequest): void {
    this.loading.set(true);
    this.error.set(null);
    this.auth
      .login(request)
      .pipe(
        switchMap((user) =>
          user.permissions.includes(Permissions.ADMIN_ACCESS)
            ? [user]
            : this.auth.logout().pipe(switchMap(() => throwError(() => new Error(NO_ADMIN_ACCESS)))),
        ),
      )
      .subscribe({
        next: () => void this.router.navigateByUrl(safeReturnUrl(this.returnUrl(), '/dashboard')),
        error: (err: unknown) => {
          this.error.set(err instanceof HttpErrorResponse ? resolveErrorMessage(err) : NO_ADMIN_ACCESS);
          this.loading.set(false);
        },
      });
  }
}
