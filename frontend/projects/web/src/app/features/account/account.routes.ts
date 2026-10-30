import { Routes } from '@angular/router';

import { AccountStore } from './account.store';
import { MyBookingStore } from './my-booking.store';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: '',
    providers: [AccountStore],
    loadComponent: () => import('./pages/account.page').then((m) => m.AccountPage),
    title: 'Tài khoản của tôi — MovieBooking',
  },
  {
    path: 'tickets',
    providers: [MyBookingStore],
    loadComponent: () => import('./pages/my-tickets.page').then((m) => m.MyTicketsPage),
    title: 'Vé của tôi — MovieBooking',
  },
  {
    path: 'orders',
    providers: [MyBookingStore],
    loadComponent: () => import('./pages/my-orders.page').then((m) => m.MyOrdersPage),
    title: 'Lịch sử đơn hàng — MovieBooking',
  },
];
