import { BookingShowtimeInfo, Order, OrderStatus } from './booking.model';
import { GroupDetail, GroupStatus } from './group.model';

/** Quản trị & thống kê (M8) — khớp DTO module stats, booking (admin), group (admin), promotion, audit. */

export interface StatsFilter {
  from: string | null;
  to: string | null;
  cinemaId: number | null;
  movieId: number | null;
}

export interface Breakdown {
  id: number;
  name: string;
  orders: number;
  tickets: number;
  revenue: number;
}

export interface OverviewStats {
  from: string;
  to: string;
  revenue: number;
  orders: number;
  ticketsSold: number;
  avgOrderValue: number;
  discountTotal: number;
  refundTotal: number;
  refundCount: number;
  paymentAttempts: number;
  paymentFailed: number;
  paymentFailureRate: number;
  expiredOrders: number;
  seatsSold: number;
  seatsTotal: number;
  occupancyRate: number;
  daily: { date: string; orders: number; tickets: number; revenue: number }[];
  topMovies: Breakdown[];
  cinemas: Breakdown[];
}

export interface GroupStats {
  from: string;
  to: string;
  totalGroups: number;
  byStatus: Record<string, number>;
  booked: number;
  conversionRate: number;
  avgTickets: number;
  avgMembers: number;
  avgHoursToBook: number | null;
  suggestions: {
    count: number;
    contiguousRate: number;
    noCluster: number;
    avgDurationMs: number;
    holds: number;
    topChoiceRate: number;
    manualRate: number;
  };
  sizeBuckets: { label: string; count: number }[];
  failures: { label: string; count: number }[];
}

export interface UserBrief {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
}

export interface AdminOrderQuery {
  q: string | null;
  email: string | null;
  status: OrderStatus | null;
  from: string | null;
  to: string | null;
  page: number;
  size: number;
}

export interface AdminOrderRow {
  code: string;
  status: OrderStatus;
  showtime: BookingShowtimeInfo;
  seatCount: number;
  total: number;
  discount: number;
  promotionCode: string | null;
  groupId: number | null;
  customer: UserBrief | null;
  createdAt: string;
  paidAt: string | null;
}

export interface AdminOrderDetail {
  order: Order;
  customer: UserBrief | null;
  events: {
    type: string;
    fromStatus: OrderStatus | null;
    toStatus: OrderStatus | null;
    actorId: number | null;
    data: Record<string, unknown> | null;
    createdAt: string;
  }[];
  payments: { provider: string; providerTxnId: string; amount: number; status: string; createdAt: string }[];
}

export interface AdminGroupRow {
  id: number;
  code: string;
  name: string;
  status: GroupStatus;
  movieTitle: string;
  leader: UserBrief | null;
  memberCount: number;
  lockedTicketCount: number | null;
  rsvpDeadline: string;
  createdAt: string;
}

export interface AdminGroupDetail {
  group: GroupDetail;
  events: { type: string; actorId: number | null; actorName: string | null; data: Record<string, unknown> | null; createdAt: string }[];
  suggestions: {
    id: number;
    showtimeId: number;
    partySize: number;
    contiguous: boolean;
    reason: string | null;
    chosenRank: number | null;
    durationMs: number;
    optionCount: number;
    createdAt: string;
  }[];
}

export type PromotionType = 'PERCENT' | 'AMOUNT';
export type PromotionStatus = 'ACTIVE' | 'INACTIVE';

export interface Promotion {
  id: number;
  code: string;
  name: string;
  description: string | null;
  type: PromotionType;
  value: number;
  maxDiscount: number | null;
  minOrder: number;
  minTickets: number;
  startsAt: string;
  endsAt: string;
  usageLimit: number | null;
  perUserLimit: number | null;
  usedCount: number;
  status: PromotionStatus;
  createdAt: string;
}

export type PromotionRequest = Omit<Promotion, 'id' | 'usedCount' | 'createdAt'>;

export interface AuditLog {
  id: number;
  actorId: number | null;
  actorEmail: string | null;
  action: string;
  resource: string;
  resourceId: string | null;
  data: Record<string, unknown> | null;
  ipAddress: string | null;
  createdAt: string;
}
