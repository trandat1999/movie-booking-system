import { Routes } from '@angular/router';

import { CinemasStore } from './cinemas.store';

export const CINEMAS_ROUTES: Routes = [
  {
    path: '',
    providers: [CinemasStore],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/cinemas.page').then((m) => m.CinemasPage),
        title: 'Rạp chiếu — MovieBooking',
      },
      {
        path: ':id',
        loadComponent: () => import('./pages/cinema-detail.page').then((m) => m.CinemaDetailPage),
        title: 'Lịch chiếu tại rạp — MovieBooking',
      },
    ],
  },
];
