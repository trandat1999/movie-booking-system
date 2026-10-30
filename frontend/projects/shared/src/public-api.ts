/*
 * Public API của thư viện @mb/shared — dùng chung cho web và admin (PLAN.md mục 2A.3).
 */

// core
export * from './lib/core/auth/auth.guards';
export * from './lib/core/auth/auth.store';
export * from './lib/core/auth/permissions';
export * from './lib/core/config/api-config';
export * from './lib/core/http/api-client';
export * from './lib/core/http/http-context';
export * from './lib/core/interceptors/api-error.interceptor';
export * from './lib/core/interceptors/auth.interceptor';
export * from './lib/core/provide-app-core';
export * from './lib/core/realtime/realtime.service';

// api
export * from './lib/api/admin-catalog.api';
export * from './lib/api/admin-cinema.api';
export * from './lib/api/admin-showtime.api';
export * from './lib/api/admin-user.api';
export * from './lib/api/catalog.api';
export * from './lib/api/auth.api';
export * from './lib/api/me.api';
export * from './lib/api/booking.api';
export * from './lib/api/notification.api';
export * from './lib/api/group.api';

// models
export * from './lib/models/api-response.model';
export * from './lib/models/booking.model';
export * from './lib/models/group.model';
export * from './lib/models/catalog.model';
export * from './lib/models/labels';
export * from './lib/models/public.model';
export * from './lib/models/user.model';

// components
export * from './lib/components/login-form/login-form.component';
export * from './lib/components/movie-poster/movie-poster.component';
export * from './lib/components/seat-grid/seat-grid.component';
export * from './lib/components/user-status-tag/user-status-tag.component';

// pipes
export * from './lib/pipes/vnd.pipe';

// utils
export * from './lib/utils/form-errors';
export * from './lib/utils/navigation';
export * from './lib/utils/validation';
export * from './lib/utils/vn-time';
export * from './lib/utils/youtube';
