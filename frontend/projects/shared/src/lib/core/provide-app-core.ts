import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import vi from '@angular/common/locales/vi';
import { EnvironmentProviders, inject, LOCALE_ID, makeEnvironmentProviders, provideAppInitializer } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { IconDefinition } from '@ant-design/icons-angular';
import { provideNzI18n, vi_VN } from 'ng-zorro-antd/i18n';
import { provideNzIcons } from 'ng-zorro-antd/icon';

import { AuthStore } from './auth/auth.store';
import { apiErrorInterceptor } from './interceptors/api-error.interceptor';
import { authInterceptor } from './interceptors/auth.interceptor';

registerLocaleData(vi);

/**
 * Provider dùng chung cho cả web và admin: HTTP + interceptor, khôi phục phiên đăng nhập khi khởi động,
 * animation, locale tiếng Việt, icon NG-ZORRO.
 */
export function provideAppCore(icons: IconDefinition[] = []): EnvironmentProviders {
  return makeEnvironmentProviders([
    // Thứ tự: apiErrorInterceptor bọc ngoài để chỉ thấy lỗi cuối cùng sau khi authInterceptor đã thử refresh
    provideHttpClient(withInterceptors([apiErrorInterceptor, authInterceptor])),
    provideAppInitializer(() => inject(AuthStore).restoreSession()),
    provideAnimationsAsync(),
    provideNzI18n(vi_VN),
    { provide: LOCALE_ID, useValue: 'vi' },
    provideNzIcons(icons),
  ]);
}
