import { Routes } from '@angular/router';

import { MoviesStore } from './movies.store';

export const MOVIES_ROUTES: Routes = [
  {
    path: '',
    providers: [MoviesStore],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/movies.page').then((m) => m.MoviesPage),
        title: 'Phim — MovieBooking',
      },
      {
        path: ':slug',
        loadComponent: () => import('./pages/movie-detail.page').then((m) => m.MovieDetailPage),
        title: 'Chi tiết phim — MovieBooking',
      },
    ],
  },
];
