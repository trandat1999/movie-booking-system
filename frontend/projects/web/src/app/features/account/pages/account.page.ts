import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ChangePasswordRequest, UpdateProfileRequest } from '@mb/shared';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTabsModule } from 'ng-zorro-antd/tabs';

import { AccountStore } from '../account.store';
import { ChangePasswordFormComponent } from '../components/change-password-form/change-password-form.component';
import { ProfileFormComponent } from '../components/profile-form/profile-form.component';

@Component({
  selector: 'app-account-page',
  imports: [
    DatePipe,
    NzCardModule,
    NzTabsModule,
    NzSkeletonModule,
    NzDescriptionsModule,
    ProfileFormComponent,
    ChangePasswordFormComponent,
  ],
  template: `
    <nz-card nzTitle="Tài khoản của tôi">
      @if (store.profile(); as profile) {
        <nz-descriptions nzSize="small" [nzColumn]="{ xxl: 2, xl: 2, lg: 2, md: 2, sm: 2, xs: 1 }" class="summary">
          <nz-descriptions-item nzTitle="Email">{{ profile.email }}</nz-descriptions-item>
          <nz-descriptions-item nzTitle="Ngày tham gia">{{ profile.createdAt | date: 'dd/MM/yyyy' }}</nz-descriptions-item>
        </nz-descriptions>

        <nz-tabset>
          <nz-tab nzTitle="Hồ sơ">
            <app-profile-form [profile]="profile" [saving]="store.savingProfile()" (saved)="saveProfile($event)" />
          </nz-tab>
          <nz-tab nzTitle="Đổi mật khẩu">
            <app-change-password-form [saving]="store.changingPassword()" (submitted)="changePassword($event)" />
          </nz-tab>
        </nz-tabset>
      } @else {
        <nz-skeleton [nzActive]="true" />
      }
    </nz-card>
  `,
  styles: `
    nz-card {
      max-width: 720px;
      margin: 0 auto;
    }

    .summary {
      margin-bottom: 8px;
    }
  `,
})
export class AccountPage implements OnInit {
  protected readonly store = inject(AccountStore);
  private readonly message = inject(NzMessageService);
  private readonly router = inject(Router);

  private readonly profileForm = viewChild(ProfileFormComponent);
  private readonly passwordForm = viewChild(ChangePasswordFormComponent);

  ngOnInit(): void {
    this.store.load();
  }

  protected saveProfile(request: UpdateProfileRequest): void {
    this.store.updateProfile(request).subscribe({
      next: () => this.message.success('Đã cập nhật hồ sơ'),
      error: (err: unknown) => this.profileForm()?.showServerError(err),
    });
  }

  protected changePassword(request: ChangePasswordRequest): void {
    this.store.changePassword(request).subscribe({
      next: () => {
        this.message.success('Đổi mật khẩu thành công, vui lòng đăng nhập lại');
        void this.router.navigateByUrl('/login');
      },
      error: (err: unknown) => this.passwordForm()?.showServerError(err),
    });
  }
}
