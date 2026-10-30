import { Routes } from '@angular/router';

import { ShowtimeStore } from './showtime.store';

export const SHOWTIME_ROUTES: Routes = [
  {
    path: ':id',
    providers: [ShowtimeStore],
    loadComponent: () => import('./pages/showtime.page').then((m) => m.ShowtimePage),
    title: 'Chọn ghế — MovieBooking',
  },
];
