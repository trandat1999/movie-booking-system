import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { authResponse } from '../auth/auth.store.spec';
import { AuthStore } from '../auth/auth.store';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let store: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    store = TestBed.inject(AuthStore);

    store.login({ identifier: 'a@b.vn', password: 'x' }).subscribe();
    backend.expectOne('/api/v1/auth/login').flush({ success: true, data: authResponse('old') });
  });

  afterEach(() => backend.verify());

  it('adds bearer token to API requests only', () => {
    http.get('/api/v1/me').subscribe();
    http.get('https://cdn.example.com/poster.jpg').subscribe();

    expect(backend.expectOne('/api/v1/me').request.headers.get('Authorization')).toBe('Bearer old');
    expect(backend.expectOne('https://cdn.example.com/poster.jpg').request.headers.has('Authorization')).toBeFalse();
  });

  it('refreshes once on 401 and retries with the new token', () => {
    let body: unknown;
    http.get('/api/v1/me').subscribe((b) => (body = b));

    backend.expectOne('/api/v1/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/refresh').flush({ success: true, data: authResponse('new') });
    const retry = backend.expectOne('/api/v1/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new');
    retry.flush({ success: true, data: { id: 1 } });

    expect(body).toEqual({ success: true, data: { id: 1 } });
  });

  it('clears session and redirects to login when refresh fails', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    let failed = false;
    http.get('/api/v1/me').subscribe({ error: () => (failed = true) });

    backend.expectOne('/api/v1/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBeTrue();
    expect(store.isAuthenticated()).toBeFalse();
    expect(navigate).toHaveBeenCalledWith(['/login'], jasmine.anything());
  });
});
