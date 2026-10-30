/** Mã quyền — khớp với bảng permissions (Flyway V2__auth.sql). */
export const Permissions = {
  ADMIN_ACCESS: 'admin:access',
  USER_READ: 'user:read',
  USER_WRITE: 'user:write',
  ROLE_READ: 'role:read',
  AUDIT_READ: 'audit:read',
  SETTING_WRITE: 'setting:write',
  MOVIE_WRITE: 'movie:write',
  CINEMA_WRITE: 'cinema:write',
  SHOWTIME_WRITE: 'showtime:write',
  ORDER_READ: 'order:read',
  ORDER_REFUND: 'order:refund',
  TICKET_CHECKIN: 'ticket:checkin',
  GROUP_READ: 'group:read',
  PROMOTION_WRITE: 'promotion:write',
  STATS_READ: 'stats:read',
} as const;

export type PermissionCode = (typeof Permissions)[keyof typeof Permissions];
