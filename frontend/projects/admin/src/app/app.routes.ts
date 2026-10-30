import { Routes } from '@angular/router';
import { Permissions, permissionGuard } from '@mb/shared';

import { PricingStore } from './features/pricing/pricing.store';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login.page').then((m) => m.LoginPage),
    title: 'Đăng nhập — MovieBooking Admin',
  },
  {
    path: '',
    component: AdminLayoutComponent,
    // Không có quyền vào trang quản trị → về trang đăng nhập
    canActivate: [permissionGuard(Permissions.ADMIN_ACCESS, '/login')],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'users',
        canActivate: [permissionGuard(Permissions.USER_READ, '/dashboard')],
        loadChildren: () => import('./features/users/users.routes').then((m) => m.USERS_ROUTES),
      },
      {
        path: 'movies',
        canActivate: [permissionGuard(Permissions.MOVIE_WRITE, '/dashboard')],
        loadChildren: () => import('./features/movies/movies.routes').then((m) => m.MOVIES_ROUTES),
      },
      {
        path: 'cinemas',
        canActivate: [permissionGuard(Permissions.CINEMA_WRITE, '/dashboard')],
        loadChildren: () => import('./features/cinemas/cinemas.routes').then((m) => m.CINEMAS_ROUTES),
      },
      {
        path: 'showtimes',
        canActivate: [permissionGuard(Permissions.SHOWTIME_WRITE, '/dashboard')],
        loadChildren: () => import('./features/showtimes/showtimes.routes').then((m) => m.SHOWTIMES_ROUTES),
      },
      {
        path: 'pricing',
        canActivate: [permissionGuard(Permissions.SHOWTIME_WRITE, '/dashboard')],
        loadComponent: () => import('./features/pricing/pricing.page').then((m) => m.PricingPage),
        providers: [PricingStore],
        title: 'Giá vé — MovieBooking Admin',
      },
      {
        path: 'settings',
        canActivate: [permissionGuard(Permissions.SETTING_WRITE, '/dashboard')],
        loadComponent: () => import('./features/settings/settings.page').then((m) => m.SettingsPage),
        title: 'Cấu hình hệ thống — MovieBooking Admin',
      },
      {
        path: 'checkin',
        canActivate: [permissionGuard(Permissions.TICKET_CHECKIN, '/dashboard')],
        loadComponent: () => import('./features/checkin/checkin.page').then((m) => m.CheckinPage),
        title: 'Soát vé — MovieBooking Admin',
      },
      {
        path: 'refunds',
        canActivate: [permissionGuard(Permissions.ORDER_REFUND, '/dashboard')],
        loadComponent: () => import('./features/refunds/refunds.page').then((m) => m.RefundsPage),
        title: 'Hoàn tiền — MovieBooking Admin',
      },
      {
        path: 'orders',
        canActivate: [permissionGuard(Permissions.ORDER_READ, '/dashboard')],
        loadComponent: () => import('./features/orders/orders.page').then((m) => m.OrdersPage),
        title: 'Đơn đặt vé — MovieBooking Admin',
      },
      {
        path: 'groups',
        canActivate: [permissionGuard(Permissions.GROUP_READ, '/dashboard')],
        loadComponent: () => import('./features/groups/groups.page').then((m) => m.GroupsPage),
        title: 'Nhóm đặt vé — MovieBooking Admin',
      },
      {
        path: 'promotions',
        canActivate: [permissionGuard(Permissions.PROMOTION_WRITE, '/dashboard')],
        loadComponent: () => import('./features/promotions/promotions.page').then((m) => m.PromotionsPage),
        title: 'Mã giảm giá — MovieBooking Admin',
      },
      {
        path: 'audit',
        canActivate: [permissionGuard(Permissions.AUDIT_READ, '/dashboard')],
        loadComponent: () => import('./features/audit/audit.page').then((m) => m.AuditPage),
        title: 'Nhật ký thao tác — MovieBooking Admin',
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
