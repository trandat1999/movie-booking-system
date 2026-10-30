import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AppNotification, AuthStore, VN_OFFSET } from '@mb/shared';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzNotificationService } from 'ng-zorro-antd/notification';

import { NotificationStore } from './notification.store';

interface NavItem {
  label: string;
  link: string;
}

@Component({
  selector: 'app-main-layout',
  imports: [
    DatePipe,
    RouterOutlet,
    RouterLink,
    NzLayoutModule,
    NzMenuModule,
    NzButtonModule,
    NzIconModule,
    NzDropDownModule,
    NzBadgeModule,
  ],
  providers: [NotificationStore],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.less',
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthStore);
  protected readonly notifications = inject(NotificationStore);
  private readonly router = inject(Router);
  private readonly toast = inject(NzNotificationService);

  protected readonly year = new Date().getFullYear();
  protected readonly vn = VN_OFFSET;

  protected readonly navItems: NavItem[] = [
    { label: 'Trang chủ', link: '/' },
    { label: 'Phim', link: '/movies' },
    { label: 'Rạp chiếu', link: '/cinemas' },
    { label: 'Đặt vé nhóm', link: '/groups' },
  ];

  constructor() {
    this.notifications.arrived$
      .pipe(takeUntilDestroyed())
      .subscribe((n) => this.toast.info(n.title, n.body ?? '', { nzPlacement: 'bottomRight' }));
  }

  protected open(notification: AppNotification): void {
    this.notifications.markRead(notification);
    if (notification.link) {
      void this.router.navigateByUrl(notification.link);
    }
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/'));
  }
}
