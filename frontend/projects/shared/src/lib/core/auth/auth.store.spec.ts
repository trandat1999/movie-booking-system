import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthResponse } from '../../models/user.model';
import { AuthStore } from './auth.store';

export function authResponse(token = 'access-1', permissions: string[] = []): AuthResponse {
  return {
    accessToken: token,
    tokenType: 'Bearer',
    expiresAt: '2026-10-30T10:00:00Z',
    user: {
      id: 1,
      email: 'a@b.vn',
      phone: null,
      fullName: 'Nguyễn Văn A',
      dateOfBirth: null,
      avatarUrl: null,
      status: 'ACTIVE',
      roles: ['CUSTOMER'],
      permissions,
      createdAt: '2026-10-01T00:00:00Z',
    },
  };
}

describe('AuthStore', () => {
  let store: AuthStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    store = TestBed.inject(AuthStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores session after login and exposes permissions', () => {
    store.login({ identifier: 'a@b.vn', password: 'x' }).subscribe();
    http.expectOne('/api/v1/auth/login').flush({ success: true, data: authResponse('t', ['user:read']) });

    expect(store.isAuthenticated()).toBeTrue();
    expect(store.accessToken()).toBe('t');
    expect(store.hasPermission('user:read')).toBeTrue();
    expect(store.hasPermission('user:write')).toBeFalse();
  });

  it('shares one refresh request between concurrent callers', () => {
    const tokens: string[] = [];
    store.refresh().subscribe((r) => tokens.push(r.accessToken));
    store.refresh().subscribe((r) => tokens.push(r.accessToken));

    http.expectOne('/api/v1/auth/refresh').flush({ success: true, data: authResponse('fresh') });

    expect(tokens).toEqual(['fresh', 'fresh']);
    expect(store.accessToken()).toBe('fresh');
  });

  it('restoreSession resolves as guest when refresh fails', async () => {
    const restored = store.restoreSession();
    http
      .expectOne('/api/v1/auth/refresh')
      .flush({ success: false, error: { code: 'INVALID_REFRESH_TOKEN', message: 'x' } }, { status: 401, statusText: '' });

    await restored;
    expect(store.isAuthenticated()).toBeFalse();
  });

  it('logout clears session even if the request fails', () => {
    store.login({ identifier: 'a@b.vn', password: 'x' }).subscribe();
    http.expectOne('/api/v1/auth/login').flush({ success: true, data: authResponse() });

    store.logout().subscribe();
    http.expectOne('/api/v1/auth/logout').flush(null, { status: 500, statusText: '' });

    expect(store.isAuthenticated()).toBeFalse();
    expect(store.user()).toBeNull();
  });
});
