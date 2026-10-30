import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthStore, Permissions } from '@mb/shared';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';

interface MenuItem {
  label: string;
  icon: string;
  link: string;
  permission: string;
}

/** Menu theo PLAN.md mục 4.2; mỗi mục chỉ hiện khi có quyền tương ứng. */
const MENU: MenuItem[] = [
  { label: 'Dashboard', icon: 'dashboard', link: '/dashboard', permission: Permissions.ADMIN_ACCESS },
  { label: 'Phim', icon: 'video-camera', link: '/movies', permission: Permissions.MOVIE_WRITE },
  { label: 'Đánh giá phim', icon: 'star', link: '/reviews', permission: Permissions.MOVIE_WRITE },
  { label: 'Rạp & phòng chiếu', icon: 'bank', link: '/cinemas', permission: Permissions.CINEMA_WRITE },
  { label: 'Lịch chiếu', icon: 'calendar', link: '/showtimes', permission: Permissions.SHOWTIME_WRITE },
  { label: 'Giá vé & ngày lễ', icon: 'dollar', link: '/pricing', permission: Permissions.SHOWTIME_WRITE },
  { label: 'Đơn đặt vé', icon: 'shopping-cart', link: '/orders', permission: Permissions.ORDER_READ },
  { label: 'Nhóm đặt vé', icon: 'team', link: '/groups', permission: Permissions.GROUP_READ },
  { label: 'Khuyến mãi', icon: 'gift', link: '/promotions', permission: Permissions.PROMOTION_WRITE },
  { label: 'Soát vé', icon: 'scan', link: '/checkin', permission: Permissions.TICKET_CHECKIN },
  { label: 'Hoàn tiền', icon: 'rollback', link: '/refunds', permission: Permissions.ORDER_REFUND },
  { label: 'Người dùng', icon: 'user', link: '/users', permission: Permissions.USER_READ },
  { label: 'Nhật ký thao tác', icon: 'file-search', link: '/audit', permission: Permissions.AUDIT_READ },
  { label: 'Cấu hình hệ thống', icon: 'setting', link: '/settings', permission: Permissions.SETTING_WRITE },
];

@Component({
  selector: 'adm-admin-layout',
  imports: [RouterOutlet, RouterLink, NzLayoutModule, NzMenuModule, NzIconModule, NzDropDownModule],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.less',
})
export class AdminLayoutComponent {
  protected readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly collapsed = signal(false);
  protected readonly menu = computed(() => MENU.filter((item) => this.auth.permissions().has(item.permission)));

  protected toggle(): void {
    this.collapsed.update((value) => !value);
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigateByUrl('/login'));
  }
}
