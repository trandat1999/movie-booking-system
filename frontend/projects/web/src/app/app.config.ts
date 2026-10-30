import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import {
  BellOutline,
  DownOutline,
  HeartFill,
  HeartOutline,
  HistoryOutline,
  IdcardOutline,
  LockOutline,
  LogoutOutline,
  MailOutline,
  MenuOutline,
  PhoneOutline,
  QrcodeOutline,
  StarFill,
  TeamOutline,
  UserOutline,
} from '@ant-design/icons-angular/icons';
import { provideAppCore } from '@mb/shared';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideAppCore([
      BellOutline,
      DownOutline,
      HeartFill,
      HeartOutline,
      HistoryOutline,
      IdcardOutline,
      LockOutline,
      LogoutOutline,
      MailOutline,
      MenuOutline,
      PhoneOutline,
      QrcodeOutline,
      StarFill,
      TeamOutline,
      UserOutline,
    ]),
  ],
};
