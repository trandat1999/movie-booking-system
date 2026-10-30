import { InjectionToken } from '@angular/core';

/** Đường dẫn gốc của REST API. Khi dev, `/api` được proxy tới backend (proxy.conf.json). */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api/v1',
});
