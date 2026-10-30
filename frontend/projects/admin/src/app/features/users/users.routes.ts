import { Routes } from '@angular/router';

import { UsersStore } from './users.store';

export const USERS_ROUTES: Routes = [
  {
    path: '',
    providers: [UsersStore],
    loadComponent: () => import('./pages/users.page').then((m) => m.UsersPage),
    title: 'Người dùng — MovieBooking Admin',
  },
];
