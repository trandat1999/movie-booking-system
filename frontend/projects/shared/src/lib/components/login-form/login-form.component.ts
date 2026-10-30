import { Component, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';

import { LoginRequest } from '../../models/user.model';
import { touchAll } from '../../utils/form-errors';

/** Form đăng nhập dùng chung cho web và admin (presentational: chỉ nhận input và phát output). */
@Component({
  selector: 'lib-login-form',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule, NzIconModule],
  template: `
    @if (error()) {
      <nz-alert nzType="error" [nzMessage]="error()!" nzShowIcon class="alert" />
    }
    <form nz-form nzLayout="vertical" [formGroup]="form" (ngSubmit)="onSubmit()">
      <nz-form-item>
        <nz-form-label nzFor="identifier" nzRequired>Email hoặc số điện thoại</nz-form-label>
        <nz-form-control nzErrorTip="Vui lòng nhập email hoặc số điện thoại">
          <nz-input-group nzPrefixIcon="user">
            <input nz-input id="identifier" formControlName="identifier" autocomplete="username" />
          </nz-input-group>
        </nz-form-control>
      </nz-form-item>
      <nz-form-item>
        <nz-form-label nzFor="password" nzRequired>Mật khẩu</nz-form-label>
        <nz-form-control nzErrorTip="Vui lòng nhập mật khẩu">
          <nz-input-group nzPrefixIcon="lock">
            <input
              nz-input
              id="password"
              type="password"
              formControlName="password"
              autocomplete="current-password"
            />
          </nz-input-group>
        </nz-form-control>
      </nz-form-item>
      <button nz-button nzType="primary" nzBlock nzSize="large" type="submit" [nzLoading]="loading()">
        Đăng nhập
      </button>
    </form>
  `,
  styles: `
    .alert {
      margin-bottom: 16px;
    }
  `,
})
export class LoginFormComponent {
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly submitted = output<LoginRequest>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  protected onSubmit(): void {
    if (this.form.invalid) {
      touchAll(this.form);
      return;
    }
    const { identifier, password } = this.form.getRawValue();
    this.submitted.emit({ identifier: identifier.trim(), password });
  }
}
