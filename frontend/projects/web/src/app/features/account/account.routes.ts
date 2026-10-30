import { Routes } from '@angular/router';

import { AccountStore } from './account.store';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: '',
    providers: [AccountStore],
    loadComponent: () => import('./pages/account.page').then((m) => m.AccountPage),
    title: 'Tài khoản của tôi — MovieBooking',
  },
];
