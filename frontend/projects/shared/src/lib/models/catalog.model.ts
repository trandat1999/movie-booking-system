/** Khớp với DTO các module movie, cinema, showtime, setting ở backend. */

// ---------- Phim ----------
export type MovieStatus = 'COMING_SOON' | 'NOW_SHOWING' | 'ENDED';
export type AgeRating = 'P' | 'K' | 'T13' | 'T16' | 'T18';

export interface Genre {
  id: number;
  name: string;
  slug: string;
}

export interface Person {
  id: number;
  fullName: string;
  avatarUrl: string | null;
}

export interface MovieSummary {
  id: number;
  title: string;
  originalTitle: string | null;
  slug: string;
  durationMin: number;
  ageRating: AgeRating;
  language: string;
  releaseDate: string;
  posterUrl: string | null;
  status: MovieStatus;
  genres: Genre[];
}

export interface MovieDetail extends MovieSummary {
  description: string | null;
  bannerUrl: string | null;
  trailerUrl: string | null;
  directors: Person[];
  actors: Person[];
  createdAt: string;
  updatedAt: string;
}

export interface MovieRequest {
  title: string;
  originalTitle: string | null;
  description: string | null;
  durationMin: number;
  ageRating: AgeRating;
  language: string;
  releaseDate: string;
  posterUrl: string | null;
  bannerUrl: string | null;
  trailerUrl: string | null;
  status: MovieStatus;
  genreIds: number[];
  directorIds: number[];
  actorIds: number[];
}

export interface MovieQuery {
  q?: string | null;
  status?: MovieStatus | null;
  genreId?: number | null;
  page: number;
  size: number;
}

// ---------- Rạp & phòng ----------
export type ActiveStatus = 'ACTIVE' | 'INACTIVE';
export type RoomType = 'STANDARD' | 'PREMIUM' | 'IMAX';

export interface Cinema {
  id: number;
  name: string;
  address: string;
  city: string;
  phone: string | null;
  description: string | null;
  imageUrl: string | null;
  amenities: string[];
  status: ActiveStatus;
  roomCount: number;
}

export type CinemaRequest = Omit<Cinema, 'id' | 'roomCount'>;

export interface Room {
  id: number;
  cinemaId: number;
  cinemaName: string;
  name: string;
  roomType: RoomType;
  status: ActiveStatus;
  gridRows: number;
  gridCols: number;
  layoutLocked: boolean;
  seatCount: number;
  capacity: number;
}

export interface RoomRequest {
  name: string;
  roomType: RoomType;
  status: ActiveStatus;
}

export interface SeatType {
  code: string;
  name: string;
  capacity: number;
  colSpan: number;
  surcharge: number;
  color: string;
  sortOrder: number;
}

export interface Seat {
  id: number;
  rowLabel: string;
  number: number;
  gridRow: number;
  gridCol: number;
  colSpan: number;
  seatTypeCode: string;
  active: boolean;
}

export interface RoomLayout {
  room: Room;
  seats: Seat[];
  seatTypes: SeatType[];
}

export interface SeatCell {
  gridRow: number;
  gridCol: number;
  seatTypeCode: string;
}

export interface RoomLayoutRequest {
  gridRows: number;
  gridCols: number;
  seats: SeatCell[];
}

// ---------- Lịch chiếu & giá ----------
export type ScreenFormat = 'TWO_D' | 'THREE_D' | 'IMAX';
export type AudioType = 'SUBTITLED' | 'DUBBED';
export type DayType = 'WEEKDAY' | 'WEEKEND' | 'HOLIDAY';
export type ShowtimeStatus = 'SCHEDULED' | 'CANCELLED';
export type ShowtimePhase = 'UPCOMING' | 'ONGOING' | 'ENDED' | 'CANCELLED';

export interface SeatStats {
  total: number;
  available: number;
  held: number;
  sold: number;
  blocked: number;
}

export interface ShowtimePrice {
  seatTypeCode: string;
  seatTypeName: string;
  price: number;
}

export interface Showtime {
  id: number;
  movieId: number;
  movieTitle: string;
  moviePosterUrl: string | null;
  ageRating: AgeRating;
  durationMin: number;
  cinemaId: number;
  cinemaName: string;
  roomId: number;
  roomName: string;
  roomType: RoomType;
  startAt: string;
  endAt: string;
  blockedUntil: string;
  format: ScreenFormat;
  audioType: AudioType;
  dayType: DayType;
  status: ShowtimeStatus;
  phase: ShowtimePhase;
  seats: SeatStats;
  prices: ShowtimePrice[];
}

export interface ShowtimeRequest {
  movieId: number;
  roomId: number;
  startAt: string;
  format: ScreenFormat;
  audioType: AudioType;
  prices: Record<string, number> | null;
}

export type ShowtimeUpdateRequest = Omit<ShowtimeRequest, 'movieId' | 'roomId'>;

export interface ShowtimeQuery {
  cinemaId?: number | null;
  roomId?: number | null;
  movieId?: number | null;
  /** yyyy-MM-dd theo giờ Việt Nam */
  date?: string | null;
  status?: ShowtimeStatus | null;
  page: number;
  size: number;
}

export interface ConflictingShowtime {
  id: number;
  movieTitle: string;
  startAt: string;
  endAt: string;
  blockedUntil: string;
}

export interface BasePrice {
  format: ScreenFormat;
  dayType: DayType;
  price: number;
  updatedAt: string;
}

export interface Holiday {
  date: string;
  name: string;
}

export interface PriceQuote {
  dayType: DayType;
  endAt: string | null;
  prices: ShowtimePrice[];
}

// ---------- Cấu hình ----------
export interface Setting {
  key: string;
  group: string;
  description: string;
  value: string;
  type: 'INTEGER' | 'OPTION';
  min: number | null;
  max: number | null;
  options: string[] | null;
  updatedAt: string;
}
