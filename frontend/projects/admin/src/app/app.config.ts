import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import {
  BankOutline,
  CalendarOutline,
  DashboardOutline,
  DollarOutline,
  DownOutline,
  FileSearchOutline,
  GiftOutline,
  LockOutline,
  LogoutOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  RollbackOutline,
  ScanOutline,
  SearchOutline,
  SettingOutline,
  StarFill,
  StarOutline,
  ShoppingCartOutline,
  TeamOutline,
  UserOutline,
  VideoCameraOutline,
} from '@ant-design/icons-angular/icons';
import { provideAppCore } from '@mb/shared';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideAppCore([
      BankOutline,
      CalendarOutline,
      DashboardOutline,
      DollarOutline,
      DownOutline,
      FileSearchOutline,
      GiftOutline,
      LockOutline,
      LogoutOutline,
      MenuFoldOutline,
      MenuUnfoldOutline,
      RollbackOutline,
      ScanOutline,
      SearchOutline,
      SettingOutline,
      StarFill,
      StarOutline,
      ShoppingCartOutline,
      TeamOutline,
      UserOutline,
      VideoCameraOutline,
    ]),
  ],
};
