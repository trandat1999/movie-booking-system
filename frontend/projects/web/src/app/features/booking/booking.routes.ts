import { Routes } from '@angular/router';
import { authGuard } from '@mb/shared';

import { OrderStore } from './order.store';

export const BOOKING_ROUTES: Routes = [
  {
    path: 'checkout/:code',
    canActivate: [authGuard],
    providers: [OrderStore],
    loadComponent: () => import('./pages/checkout.page').then((m) => m.CheckoutPage),
    title: 'Thanh toán — MovieBooking',
  },
  {
    path: 'orders/:code',
    canActivate: [authGuard],
    providers: [OrderStore],
    loadComponent: () => import('./pages/order.page').then((m) => m.OrderPage),
    title: 'Đơn hàng — MovieBooking',
  },
  {
    // Cổng giả lập không cần đăng nhập — cổng thật cũng không biết phiên của người dùng
    path: 'payment/mock',
    providers: [OrderStore],
    loadComponent: () => import('./pages/mock-gateway.page').then((m) => m.MockGatewayPage),
    title: 'Cổng thanh toán giả lập',
  },
];
