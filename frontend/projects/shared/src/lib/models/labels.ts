import {
  ActiveStatus,
  AgeRating,
  AudioType,
  DayType,
  MovieStatus,
  RoomType,
  ScreenFormat,
  ShowtimePhase,
} from './catalog.model';
import { OrderStatus, TicketStatus } from './booking.model';
import { GroupStatus, Rsvp } from './group.model';

/** Nhãn tiếng Việt cho các enum hiển thị trên giao diện. */

export const MOVIE_STATUS_LABELS: Record<MovieStatus, string> = {
  COMING_SOON: 'Sắp chiếu',
  NOW_SHOWING: 'Đang chiếu',
  ENDED: 'Ngừng chiếu',
};

export const MOVIE_STATUS_COLORS: Record<MovieStatus, string> = {
  COMING_SOON: 'blue',
  NOW_SHOWING: 'green',
  ENDED: 'default',
};

export const AGE_RATING_LABELS: Record<AgeRating, string> = {
  P: 'P — Mọi lứa tuổi',
  K: 'K — Dưới 13 tuổi xem cùng cha mẹ',
  T13: 'T13 — Từ 13 tuổi',
  T16: 'T16 — Từ 16 tuổi',
  T18: 'T18 — Từ 18 tuổi',
};

export const AGE_RATING_COLORS: Record<AgeRating, string> = {
  P: 'green',
  K: 'cyan',
  T13: 'gold',
  T16: 'orange',
  T18: 'red',
};

export const ACTIVE_STATUS_LABELS: Record<ActiveStatus, string> = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Ngừng hoạt động',
};

export const ROOM_TYPE_LABELS: Record<RoomType, string> = {
  STANDARD: 'Tiêu chuẩn',
  PREMIUM: 'Cao cấp',
  IMAX: 'IMAX',
};

export const FORMAT_LABELS: Record<ScreenFormat, string> = {
  TWO_D: '2D',
  THREE_D: '3D',
  IMAX: 'IMAX',
};

export const AUDIO_LABELS: Record<AudioType, string> = {
  SUBTITLED: 'Phụ đề',
  DUBBED: 'Lồng tiếng',
};

export const DAY_TYPE_LABELS: Record<DayType, string> = {
  WEEKDAY: 'Ngày thường',
  WEEKEND: 'Cuối tuần',
  HOLIDAY: 'Ngày lễ',
};

export const PHASE_LABELS: Record<ShowtimePhase, string> = {
  UPCOMING: 'Sắp chiếu',
  ONGOING: 'Đang chiếu',
  ENDED: 'Đã kết thúc',
  CANCELLED: 'Đã hủy',
};

export const PHASE_COLORS: Record<ShowtimePhase, string> = {
  UPCOMING: 'blue',
  ONGOING: 'green',
  ENDED: 'default',
  CANCELLED: 'red',
};

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  EXPIRED: 'Hết hạn',
  CANCELLED: 'Đã hủy',
  REFUND_PENDING: 'Chờ hoàn tiền',
  REFUNDED: 'Đã hoàn tiền',
  COMPLETED: 'Hoàn tất',
};

export const ORDER_STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'gold',
  PAID: 'green',
  EXPIRED: 'default',
  CANCELLED: 'default',
  REFUND_PENDING: 'orange',
  REFUNDED: 'purple',
  COMPLETED: 'blue',
};

export const TICKET_STATUS_LABELS: Record<TicketStatus, string> = {
  VALID: 'Còn hiệu lực',
  USED: 'Đã sử dụng',
  CANCELLED: 'Đã hủy',
};

export const GROUP_STATUS_LABELS: Record<GroupStatus, string> = {
  OPEN: 'Đang lên kế hoạch',
  LOCKED: 'Đã chốt suất',
  HOLDING: 'Đang giữ ghế',
  BOOKED: 'Đã đặt vé',
  COMPLETED: 'Hoàn tất',
  EXPIRED: 'Hết hạn',
  CANCELLED: 'Đã hủy',
};

export const GROUP_STATUS_COLORS: Record<GroupStatus, string> = {
  OPEN: 'blue',
  LOCKED: 'gold',
  HOLDING: 'orange',
  BOOKED: 'green',
  COMPLETED: 'cyan',
  EXPIRED: 'default',
  CANCELLED: 'default',
};

export const RSVP_LABELS: Record<Rsvp, string> = {
  PENDING: 'Chưa phản hồi',
  YES: 'Tham gia',
  NO: 'Không tham gia',
  MAYBE: 'Chưa chắc',
};

export const RSVP_COLORS: Record<Rsvp, string> = {
  PENDING: 'default',
  YES: 'green',
  NO: 'red',
  MAYBE: 'gold',
};

/** Danh sách lựa chọn cho nz-select từ một bảng nhãn. */
export function toOptions<K extends string>(labels: Record<K, string>): { value: K; label: string }[] {
  return (Object.keys(labels) as K[]).map((value) => ({ value, label: labels[value] }));
}
