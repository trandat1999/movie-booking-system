/** Khớp với DTO của module auth và user ở backend. */

export type UserStatus = 'ACTIVE' | 'LOCKED';

export interface UserProfile {
  id: number;
  email: string;
  phone: string | null;
  fullName: string;
  /** yyyy-MM-dd */
  dateOfBirth: string | null;
  avatarUrl: string | null;
  status: UserStatus;
  roles: string[];
  permissions: string[];
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: UserProfile;
}

export interface LoginRequest {
  /** Email hoặc số điện thoại */
  identifier: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone: string | null;
  password: string;
  dateOfBirth: string | null;
}

export interface UpdateProfileRequest {
  fullName: string;
  phone: string | null;
  dateOfBirth: string | null;
  avatarUrl: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface RoleSummary {
  code: string;
  name: string;
}

export interface AdminUser {
  id: number;
  email: string;
  phone: string | null;
  fullName: string;
  dateOfBirth: string | null;
  status: UserStatus;
  roles: RoleSummary[];
  lastLoginAt: string | null;
  createdAt: string;
}

export interface Permission {
  code: string;
  name: string;
  groupName: string;
}

export interface Role {
  id: number;
  code: string;
  name: string;
  description: string | null;
  permissions: Permission[];
}

export interface AdminUserQuery {
  q?: string | null;
  status?: UserStatus | null;
  role?: string | null;
  /** Bắt đầu từ 0 */
  page: number;
  size: number;
}
