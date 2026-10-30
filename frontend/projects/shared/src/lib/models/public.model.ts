import { AgeRating, AudioType, MovieStatus, RoomType, ScreenFormat, ShowtimePrice } from './catalog.model';

/** Khớp với DTO công khai của module showtime (ShowtimeController). */

export interface PublicMovieQuery {
  q?: string | null;
  status?: MovieStatus | null;
  genreId?: number | null;
  ageRating?: AgeRating | null;
  language?: string | null;
  page: number;
  size: number;
}

export interface PublicShowtime {
  id: number;
  movieId: number;
  movieTitle: string;
  movieSlug: string;
  moviePosterUrl: string | null;
  ageRating: AgeRating;
  durationMin: number;
  cinemaId: number;
  cinemaName: string;
  city: string;
  roomId: number;
  roomName: string;
  roomType: RoomType;
  startAt: string;
  endAt: string;
  format: ScreenFormat;
  audioType: AudioType;
  availableSeats: number;
  minPrice: number;
}

export interface PublicShowtimeDetail {
  showtime: PublicShowtime;
  cinemaAddress: string;
  prices: ShowtimePrice[];
}

export type SeatStatus = 'AVAILABLE' | 'HELD' | 'SOLD' | 'BLOCKED';

export interface SeatMapSeat {
  id: number;
  rowLabel: string;
  number: number;
  gridRow: number;
  gridCol: number;
  colSpan: number;
  seatTypeCode: string;
  status: SeatStatus;
}

export interface SeatTypePrice {
  code: string;
  name: string;
  capacity: number;
  color: string;
  price: number;
}

export interface SeatMap {
  showtimeId: number;
  gridRows: number;
  gridCols: number;
  seats: SeatMapSeat[];
  seatTypes: SeatTypePrice[];
}
