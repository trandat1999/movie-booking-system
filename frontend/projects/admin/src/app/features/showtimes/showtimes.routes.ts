import { Routes } from '@angular/router';

import { ShowtimesStore } from './showtimes.store';

export const SHOWTIMES_ROUTES: Routes = [
  {
    path: '',
    providers: [ShowtimesStore],
    loadComponent: () => import('./pages/showtimes.page').then((m) => m.ShowtimesPage),
    title: 'Lịch chiếu — MovieBooking Admin',
  },
];
