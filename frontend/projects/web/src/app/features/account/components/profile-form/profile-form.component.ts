import { Component, effect, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  applyServerErrors,
  fromIsoDate,
  toIsoDate,
  touchAll,
  UpdateProfileRequest,
  UserProfile,
  VN_MOBILE_PATTERN,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';

@Component({
  selector: 'app-profile-form',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzDatePickerModule, NzAlertModule],
  template: `
    @if (error()) {
      <nz-alert nzType="error" [nzMessage]="error()!" nzShowIcon class="alert" />
    }
    <form nz-form nzLayout="vertical" [formGroup]="form" (ngSubmit)="submit()">
      <nz-form-item>
        <nz-form-label nzFor="fullName" nzRequired>Họ và tên</nz-form-label>
        <nz-form-control [nzErrorTip]="serverOr" >
          <input nz-input id="fullName" formControlName="fullName" />
        </nz-form-control>
      </nz-form-item>
      <nz-form-item>
        <nz-form-label nzFor="phone">Số điện thoại</nz-form-label>
        <nz-form-control [nzErrorTip]="serverOr">
          <input nz-input id="phone" formControlName="phone" placeholder="0912345678" />
        </nz-form-control>
      </nz-form-item>
      <nz-form-item>
        <nz-form-label nzFor="dateOfBirth">Ngày sinh</nz-form-label>
        <nz-form-control>
          <nz-date-picker
            id="dateOfBirth"
            formControlName="dateOfBirth"
            nzFormat="dd/MM/yyyy"
            [nzDisabledDate]="disableFutureDates"
            class="full-width"
          />
        </nz-form-control>
      </nz-form-item>
      <nz-form-item>
        <nz-form-label nzFor="avatarUrl">Ảnh đại diện (URL)</nz-form-label>
        <nz-form-control [nzErrorTip]="serverOr">
          <input nz-input id="avatarUrl" formControlName="avatarUrl" placeholder="https://..." />
        </nz-form-control>
      </nz-form-item>
      <button nz-button nzType="primary" type="submit" [nzLoading]="saving()">Lưu thay đổi</button>
    </form>

    <ng-template #serverOr let-control>
      {{ control.errors?.server ?? 'Giá trị không hợp lệ' }}
    </ng-template>
  `,
  styles: `
    .alert {
      margin-bottom: 16px;
    }

    .full-width {
      width: 100%;
    }
  `,
})
export class ProfileFormComponent {
  readonly profile = input.required<UserProfile>();
  readonly saving = input(false);
  readonly saved = output<UpdateProfileRequest>();

  protected readonly error = signal<string | null>(null);
  private readonly today = new Date();

  protected readonly form = inject(NonNullableFormBuilder).group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    phone: ['', [Validators.pattern(VN_MOBILE_PATTERN)]],
    dateOfBirth: [null as Date | null],
    avatarUrl: ['', [Validators.maxLength(500)]],
  });

  constructor() {
    effect(() => {
      const profile = this.profile();
      this.form.reset({
        fullName: profile.fullName,
        phone: profile.phone ?? '',
        dateOfBirth: fromIsoDate(profile.dateOfBirth),
        avatarUrl: profile.avatarUrl ?? '',
      });
    });
  }

  protected disableFutureDates = (date: Date): boolean => date > this.today;

  /** Gọi từ trang cha khi backend trả lỗi. */
  showServerError(error: unknown): void {
    this.error.set(applyServerErrors(this.form, error));
  }

  protected submit(): void {
    if (this.form.invalid) {
      touchAll(this.form);
      return;
    }
    this.error.set(null);
    const value = this.form.getRawValue();
    this.saved.emit({
      fullName: value.fullName.trim(),
      phone: value.phone.trim() || null,
      dateOfBirth: toIsoDate(value.dateOfBirth),
      avatarUrl: value.avatarUrl.trim() || null,
    });
  }
}
