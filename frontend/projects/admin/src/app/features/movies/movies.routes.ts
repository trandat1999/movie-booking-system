import { Routes } from '@angular/router';

import { CatalogStore } from './catalog.store';

export const MOVIES_ROUTES: Routes = [
  {
    path: '',
    providers: [CatalogStore],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/movies.page').then((m) => m.MoviesPage),
        title: 'Phim — MovieBooking Admin',
      },
      {
        path: 'genres',
        loadComponent: () => import('./pages/genres.page').then((m) => m.GenresPage),
        title: 'Thể loại — MovieBooking Admin',
      },
      {
        path: 'people',
        loadComponent: () => import('./pages/people.page').then((m) => m.PeoplePage),
        title: 'Đạo diễn & diễn viên — MovieBooking Admin',
      },
      {
        path: 'new',
        loadComponent: () => import('./pages/movie-form.page').then((m) => m.MovieFormPage),
        title: 'Thêm phim — MovieBooking Admin',
      },
      {
        path: ':id',
        loadComponent: () => import('./pages/movie-form.page').then((m) => m.MovieFormPage),
        title: 'Sửa phim — MovieBooking Admin',
      },
    ],
  },
];
