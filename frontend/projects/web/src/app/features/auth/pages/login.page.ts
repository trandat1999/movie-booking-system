import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthStore, LoginFormComponent, LoginRequest, resolveErrorMessage, safeReturnUrl } from '@mb/shared';
import { NzCardModule } from 'ng-zorro-antd/card';

@Component({
  selector: 'app-login-page',
  imports: [RouterLink, NzCardModule, LoginFormComponent],
  template: `
    <div class="auth-page">
      <nz-card nzTitle="Đăng nhập">
        <lib-login-form [loading]="loading()" [error]="error()" (submitted)="login($event)" />
        <p class="switch">Chưa có tài khoản? <a routerLink="/register">Đăng ký ngay</a></p>
      </nz-card>
    </div>
  `,
  styleUrl: '../auth-page.less',
})
export class LoginPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  /** Từ query param ?returnUrl=... (withComponentInputBinding) */
  readonly returnUrl = input<string>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected login(request: LoginRequest): void {
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(request).subscribe({
      next: () => void this.router.navigateByUrl(safeReturnUrl(this.returnUrl())),
      error: (err: HttpErrorResponse) => {
        this.error.set(resolveErrorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
