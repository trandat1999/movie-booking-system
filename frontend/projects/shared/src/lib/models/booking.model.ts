import { AgeRating, AudioType, ScreenFormat } from './catalog.model';
import { SeatStatus } from './public.model';

/** Đặt vé cá nhân (M4) — khớp DTO của module booking/payment/notification. */

export interface SeatLine {
  seatId: number;
  label: string;
  seatTypeCode: string;
  price: number;
}

export interface HoldResponse {
  id: number;
  showtimeId: number;
  expiresAt: string;
  seats: SeatLine[];
  total: number;
  /** Cảnh báo không chặn, ví dụ ghế lẻ khi quy tắc = warn */
  warnings: string[];
  serverTime: string;
}

export interface BookingShowtimeInfo {
  id: number;
  movieId: number;
  movieTitle: string;
  posterUrl: string | null;
  ageRating: AgeRating;
  startAt: string;
  endAt: string;
  format: ScreenFormat;
  audioType: AudioType;
  cinemaId: number;
  cinemaName: string;
  cinemaAddress: string;
  roomName: string;
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'EXPIRED'
  | 'CANCELLED'
  | 'REFUND_PENDING'
  | 'REFUNDED'
  | 'COMPLETED';

export type TicketStatus = 'VALID' | 'USED' | 'CANCELLED';

export interface TicketInfo {
  code: string;
  seatLabel: string;
  status: TicketStatus;
  /** Ảnh QR (data URI) — chỉ có khi vé còn hiệu lực */
  qrImage: string | null;
}

export interface Order {
  code: string;
  status: OrderStatus;
  showtime: BookingShowtimeInfo;
  items: SeatLine[];
  subtotal: number;
  discount: number;
  total: number;
  expiresAt: string;
  paidAt: string | null;
  cancelledAt: string | null;
  createdAt: string;
  tickets: TicketInfo[];
  /** Khác null nếu là đơn của nhóm (chỉ hủy từ trang nhóm) */
  groupId: number | null;
  /** Hạn tự hủy vé; null = không tự hủy được */
  cancellableUntil: string | null;
  /** QR của cả đơn (soát vé cả nhóm một lần) */
  qrImage: string | null;
  refund: { amount: number; status: RefundStatus; processedAt: string | null } | null;
  serverTime: string;
}

export type RefundStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

export interface PaymentProviderInfo {
  code: string;
  name: string;
}

/** Yêu cầu hoàn tiền (trang quản trị) */
export interface AdminRefund {
  id: number;
  orderCode: string;
  userId: number;
  orderTotal: number;
  amount: number;
  reason: string;
  status: RefundStatus;
  provider: string | null;
  note: string | null;
  createdAt: string;
  processedAt: string | null;
}

export interface CheckinResult {
  orderCode: string;
  showtime: BookingShowtimeInfo;
  checkedIn: number;
  tickets: { code: string; seatLabel: string; holderName: string | null; status: TicketStatus; checkedInAt: string | null }[];
}

export interface OrderSummary {
  code: string;
  status: OrderStatus;
  showtime: BookingShowtimeInfo;
  seatCount: number;
  total: number;
  createdAt: string;
}

export interface MyTicket {
  code: string;
  orderCode: string;
  seatLabel: string;
  status: TicketStatus;
  showtime: BookingShowtimeInfo;
  qrImage: string | null;
}

export interface PaymentSession {
  provider: string;
  redirectUrl: string;
}

export interface PaymentCallbackResult {
  orderCode: string;
  duplicate: boolean;
  orderStatus: OrderStatus;
}

export interface AppNotification {
  id: number;
  type: string;
  title: string;
  body: string | null;
  link: string | null;
  readAt: string | null;
  createdAt: string;
}

/** Thông điệp realtime trên /topic/showtimes/{id}/seats */
export interface SeatsChangedMessage {
  type: 'SEATS_CHANGED';
  seats: { id: number; status: SeatStatus }[];
}

/** Thông điệp realtime trên /user/queue/orders */
export interface OrderStatusMessage {
  orderCode: string;
  status: OrderStatus;
}
