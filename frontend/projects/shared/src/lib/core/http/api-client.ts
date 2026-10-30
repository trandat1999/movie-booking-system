import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../models/api-response.model';
import { API_BASE_URL } from '../config/api-config';

type QueryParams = Record<string, string | number | boolean | readonly (string | number | boolean)[] | null | undefined>;

export interface RequestOptions {
  params?: QueryParams;
  context?: HttpContext;
}

/**
 * Lớp gọi HTTP duy nhất của frontend: thêm tiền tố API và bóc `data` ra khỏi {@link ApiResponse}.
 * Các API service theo module (MovieApi, GroupApi, ...) dùng lớp này; component không gọi HttpClient trực tiếp.
 */
@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  get<T>(path: string, options: RequestOptions = {}): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(this.url(path), { params: toParams(options.params), context: options.context })
      .pipe(map(unwrap));
  }

  post<T>(path: string, body?: unknown, options: RequestOptions = {}): Observable<T> {
    return this.http
      .post<ApiResponse<T>>(this.url(path), body ?? null, { params: toParams(options.params), context: options.context })
      .pipe(map(unwrap));
  }

  put<T>(path: string, body?: unknown, options: RequestOptions = {}): Observable<T> {
    return this.http
      .put<ApiResponse<T>>(this.url(path), body ?? null, { params: toParams(options.params), context: options.context })
      .pipe(map(unwrap));
  }

  patch<T>(path: string, body?: unknown, options: RequestOptions = {}): Observable<T> {
    return this.http
      .patch<ApiResponse<T>>(this.url(path), body ?? null, { params: toParams(options.params), context: options.context })
      .pipe(map(unwrap));
  }

  delete<T>(path: string, options: RequestOptions = {}): Observable<T> {
    return this.http
      .delete<ApiResponse<T>>(this.url(path), { params: toParams(options.params), context: options.context })
      .pipe(map(unwrap));
  }

  private url(path: string): string {
    return `${this.baseUrl}${path.startsWith('/') ? path : `/${path}`}`;
  }
}

function unwrap<T>(response: ApiResponse<T>): T {
  return response.data as T;
}

function toParams(params?: QueryParams): HttpParams | undefined {
  if (!params) {
    return undefined;
  }
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined) {
      continue;
    }
    const values = Array.isArray(value) ? value : [value];
    for (const v of values) {
      httpParams = httpParams.append(key, String(v));
    }
  }
  return httpParams;
}
