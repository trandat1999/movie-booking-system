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
        title: 'Rạp — MovieBooking Admin',
      },
      {
        path: ':cinemaId',
        loadComponent: () => import('./pages/cinema-detail.page').then((m) => m.CinemaDetailPage),
        title: 'Phòng chiếu — MovieBooking Admin',
      },
      {
        path: ':cinemaId/rooms/:roomId',
        loadComponent: () => import('./pages/room-layout.page').then((m) => m.RoomLayoutPage),
        title: 'Sơ đồ ghế — MovieBooking Admin',
      },
    ],
  },
];
