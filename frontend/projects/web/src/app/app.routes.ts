import { Routes } from '@angular/router';
import { authGuard, guestGuard } from '@mb/shared';

import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', loadChildren: () => import('./features/home/home.routes').then((m) => m.HOME_ROUTES) },
      { path: 'movies', loadChildren: () => import('./features/movies/movies.routes').then((m) => m.MOVIES_ROUTES) },
      { path: 'cinemas', loadChildren: () => import('./features/cinemas/cinemas.routes').then((m) => m.CINEMAS_ROUTES) },
      {
        path: 'showtimes',
        loadChildren: () => import('./features/showtime/showtime.routes').then((m) => m.SHOWTIME_ROUTES),
      },
      {
        path: '',
        canActivate: [guestGuard],
        loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
      },
      {
        path: 'account',
        canActivate: [authGuard],
        loadChildren: () => import('./features/account/account.routes').then((m) => m.ACCOUNT_ROUTES),
      },
      // M4+: booking, group, notifications
    ],
  },
  { path: '**', redirectTo: '' },
];
