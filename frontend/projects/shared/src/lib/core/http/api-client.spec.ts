import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiClient } from './api-client';

describe('ApiClient', () => {
  let client: ApiClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    client = TestBed.inject(ApiClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('prefixes the API base URL and unwraps data', () => {
    let result: { id: number }[] | undefined;
    client.get<{ id: number }[]>('/movies', { params: { genre: [1, 2], q: 'abc', empty: null } }).subscribe((r) => (result = r));

    const req = http.expectOne((r) => r.url === '/api/v1/movies');
    expect(req.request.params.getAll('genre')).toEqual(['1', '2']);
    expect(req.request.params.get('q')).toBe('abc');
    expect(req.request.params.has('empty')).toBeFalse();
    req.flush({ success: true, data: [{ id: 1 }] });

    expect(result).toEqual([{ id: 1 }]);
  });
});
