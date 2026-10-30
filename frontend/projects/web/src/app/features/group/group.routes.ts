import { Routes } from '@angular/router';

import { GroupSeatsStore } from './group-seats.store';
import { GroupStore } from './group.store';
import { MyGroupsStore } from './my-groups.store';

/** Toàn bộ trang nhóm cần đăng nhập (D2: thành viên bắt buộc có tài khoản) — authGuard đặt ở app.routes. */
export const GROUP_ROUTES: Routes = [
  {
    path: '',
    providers: [MyGroupsStore],
    loadComponent: () => import('./pages/groups.page').then((m) => m.GroupsPage),
    title: 'Đặt vé nhóm — MovieBooking',
  },
  {
    path: 'new',
    providers: [MyGroupsStore],
    loadComponent: () => import('./pages/group-create.page').then((m) => m.GroupCreatePage),
    title: 'Tạo nhóm — MovieBooking',
  },
  {
    path: 'join/:code',
    providers: [MyGroupsStore],
    loadComponent: () => import('./pages/group-join.page').then((m) => m.GroupJoinPage),
    title: 'Tham gia nhóm — MovieBooking',
  },
  {
    path: ':id',
    providers: [GroupStore],
    loadComponent: () => import('./pages/group-detail.page').then((m) => m.GroupDetailPage),
    title: 'Nhóm — MovieBooking',
  },
  {
    path: ':id/seats',
    providers: [GroupSeatsStore],
    loadComponent: () => import('./pages/group-seats.page').then((m) => m.GroupSeatsPage),
    title: 'Chọn ghế cho nhóm — MovieBooking',
  },
];
