import { Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  applyServerErrors,
  ChangePasswordRequest,
  matchFields,
  PASSWORD_HINT,
  PASSWORD_PATTERN,
  touchAll,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';

@Component({
  selector: 'app-change-password-form',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule],
  template: `
    <nz-alert
      nzType="info"
      nzMessage="Sau khi đổi mật khẩu, bạn sẽ được đăng xuất khỏi mọi thiết bị."
      nzShowIcon
      class="alert"
    />
    @if (error()) {
      <nz-alert nzType="error" [nzMessage]="error()!" nzShowIcon class="alert" />
    }
    <form nz-form nzLayout="vertical" [formGroup]="form" (ngSubmit)="submit()">
      <nz-form-item>
        <nz-form-label nzFor="currentPassword" nzRequired>Mật khẩu hiện tại</nz-form-label>
        <nz-form-control nzErrorTip="Vui lòng nhập mật khẩu hiện tại">
          <input
            nz-input
            id="currentPassword"
            type="password"
            formControlName="currentPassword"
            autocomplete="current-password"
          />
        </nz-form-control>
      </nz-form-item>
      <nz-form-item>
        <nz-form-label nzFor="newPassword" nzRequired>Mật khẩu mới</nz-form-label>
        <nz-form-control [nzErrorTip]="passwordHint" [nzExtra]="passwordHint">
          <input nz-input id="newPassword" type="password" formControlName="newPassword" autocomplete="new-password" />
        </nz-form-control>
      </nz-form-item>
      <nz-form-item>
        <nz-form-label nzFor="confirmPassword" nzRequired>Nhập lại mật khẩu mới</nz-form-label>
        <nz-form-control nzErrorTip="Mật khẩu nhập lại không khớp">
          <input
            nz-input
            id="confirmPassword"
            type="password"
            formControlName="confirmPassword"
            autocomplete="new-password"
          />
        </nz-form-control>
      </nz-form-item>
      <button nz-button nzType="primary" type="submit" [nzLoading]="saving()">Đổi mật khẩu</button>
    </form>
  `,
  styles: `
    .alert {
      margin-bottom: 16px;
    }
  `,
})
export class ChangePasswordFormComponent {
  readonly saving = input(false);
  readonly submitted = output<ChangePasswordRequest>();

  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchFields('newPassword', 'confirmPassword') },
  );

  showServerError(error: unknown): void {
    this.error.set(applyServerErrors(this.form, error));
  }

  protected submit(): void {
    if (this.form.invalid) {
      touchAll(this.form);
      return;
    }
    this.error.set(null);
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.submitted.emit({ currentPassword, newPassword });
  }
}
