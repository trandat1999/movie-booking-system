import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  applyServerErrors,
  AuthStore,
  matchFields,
  PASSWORD_HINT,
  PASSWORD_PATTERN,
  toIsoDate,
  touchAll,
  VN_MOBILE_PATTERN,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NzCardModule,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAlertModule,
    NzDatePickerModule,
  ],
  templateUrl: './register.page.html',
  styleUrl: '../auth-page.less',
})
export class RegisterPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly message = inject(NzMessageService);

  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly today = new Date();

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      phone: ['', [Validators.pattern(VN_MOBILE_PATTERN)]],
      dateOfBirth: [null as Date | null],
      password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchFields('password', 'confirmPassword') },
  );

  protected disableFutureDates = (date: Date): boolean => date > this.today;

  protected submit(): void {
    if (this.form.invalid) {
      touchAll(this.form);
      return;
    }
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.auth
      .register({
        fullName: value.fullName.trim(),
        email: value.email.trim(),
        phone: value.phone.trim() || null,
        password: value.password,
        dateOfBirth: toIsoDate(value.dateOfBirth),
      })
      .subscribe({
        next: (user) => {
          this.message.success(`Chào mừng ${user.fullName}!`);
          void this.router.navigateByUrl('/');
        },
        error: (err: unknown) => {
          this.error.set(applyServerErrors(this.form, err));
          this.loading.set(false);
        },
      });
  }
}
