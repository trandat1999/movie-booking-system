import { OrderStatus } from './booking.model';
import { AgeRating, AudioType, ScreenFormat } from './catalog.model';

/** Đặt vé theo nhóm (M6) — khớp DTO của module group. */

export type GroupStatus = 'OPEN' | 'LOCKED' | 'HOLDING' | 'BOOKED' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED';
export type GroupRole = 'LEADER' | 'MEMBER';
export type Rsvp = 'PENDING' | 'YES' | 'NO' | 'MAYBE';

export interface GroupShowtimeInfo {
  id: number;
  startAt: string;
  endAt: string;
  format: ScreenFormat;
  audioType: AudioType;
  cinemaId: number;
  cinemaName: string;
  roomName: string;
}

export interface GroupMemberView {
  userId: number;
  fullName: string;
  role: GroupRole;
  rsvp: Rsvp;
  guestCount: number;
  maxBudget: number | null;
  joinedAt: string;
}

export interface GroupOptionView {
  id: number;
  showtime: GroupShowtimeInfo;
  proposedBy: number;
  votes: number;
  votedByMe: boolean;
  voters: string[];
}

export interface GroupAssignmentView {
  ticketCode: string;
  seatLabel: string;
  userId: number;
  holderName: string | null;
  guestLabel: string | null;
}

export interface GroupShareView {
  userId: number;
  fullName: string;
  people: number;
  amount: number;
}

export interface GroupProgress {
  members: number;
  responded: number;
  yes: number;
  no: number;
  maybe: number;
  pending: number;
  ticketCount: number;
}

export interface GroupDetail {
  id: number;
  code: string;
  name: string;
  status: GroupStatus;
  movie: {
    id: number;
    title: string;
    slug: string;
    posterUrl: string | null;
    ageRating: AgeRating;
    durationMin: number;
  };
  cinema: { id: number; name: string } | null;
  dateFrom: string | null;
  dateTo: string | null;
  rsvpDeadline: string;
  leaderId: number;
  myRole: GroupRole | null;
  members: GroupMemberView[];
  options: GroupOptionView[];
  locked: { showtime: GroupShowtimeInfo; ticketCount: number } | null;
  hold: { holdId: number; expiresAt: string; seatIds: number[] } | null;
  order: { code: string; status: OrderStatus; total: number } | null;
  assignments: GroupAssignmentView[];
  shares: GroupShareView[];
  progress: GroupProgress;
  ageWarnings: string[];
  serverTime: string;
}

export type ClusterStatus = 'SAME_ROW' | 'SPLIT' | 'NONE' | 'UNAVAILABLE';

export interface GroupSummary {
  ticketCount: number;
  yes: number;
  maybe: number;
  no: number;
  pending: number;
  guests: number;
  minBudget: number | null;
  options: {
    optionId: number;
    showtime: GroupShowtimeInfo;
    votes: number;
    availablePlaces: number;
    cluster: ClusterStatus;
    bestScore: number | null;
  }[];
  topOptionId: number | null;
  ageWarnings: string[];
}

export interface GroupListItem {
  id: number;
  name: string;
  status: GroupStatus;
  movieTitle: string;
  posterUrl: string | null;
  myRole: GroupRole;
  memberCount: number;
  rsvpDeadline: string;
  createdAt: string;
}

export interface InvitePreview {
  id: number;
  name: string;
  movieTitle: string;
  posterUrl: string | null;
  leaderName: string;
  memberCount: number;
  status: GroupStatus;
  alreadyMember: boolean;
}

export interface SeatSuggestionView {
  /** 1 = tốt nhất; 0 = lựa chọn thủ công được chấm điểm */
  rank: number;
  seatIds: number[];
  labels: string[];
  rowCount: number;
  capacity: number;
  totalPrice: number;
  score: number;
  explanation: string;
  breakdown: {
    center: number;
    rows: number;
    misalignment: number;
    orphans: number;
    overBudget: number;
    waste: number;
    orphanSeats: number;
    wastedPlaces: number;
  };
}

export interface SeatSuggestions {
  logId: number | null;
  ticketCount: number;
  options: SeatSuggestionView[];
  /** NOT_ENOUGH_SEATS / NO_CLUSTER */
  reason: string | null;
}

export interface CreateGroupRequest {
  name: string;
  movieId: number;
  cinemaId: number | null;
  dateFrom: string | null;
  dateTo: string | null;
  rsvpDeadline: string;
}

export interface RsvpRequest {
  rsvp: Rsvp;
  guestCount: number;
  maxBudget: number | null;
}

export interface SuggestionOptions {
  allowSplit: boolean;
  maxPricePerPerson: number | null;
  seatTypes: string[];
}

/** Thông điệp realtime trên /topic/groups/{id} */
export interface GroupChangedMessage {
  type: string;
  groupId: number;
  seatIds?: number[];
}
