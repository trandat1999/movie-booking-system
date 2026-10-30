import { Routes } from '@angular/router';

import { HomeStore } from './home.store';

export const HOME_ROUTES: Routes = [
  {
    path: '',
    providers: [HomeStore],
    loadComponent: () => import('./pages/home.page').then((m) => m.HomePage),
    title: 'MovieBooking — Đặt vé xem phim',
  },
];
