import { AgeRating, AudioType, PublicShowtime, ScreenFormat } from '@mb/shared';

/** Các suất cùng định dạng và âm thanh, ví dụ "2D · Phụ đề". */
export interface ShowtimeVersion {
  format: ScreenFormat;
  audioType: AudioType;
  showtimes: PublicShowtime[];
}

export interface CinemaShowtimes {
  cinemaId: number;
  cinemaName: string;
  city: string;
  versions: ShowtimeVersion[];
}

export interface MovieShowtimes {
  movieId: number;
  movieTitle: string;
  movieSlug: string;
  moviePosterUrl: string | null;
  ageRating: AgeRating;
  durationMin: number;
  versions: ShowtimeVersion[];
}

const FORMAT_ORDER: ScreenFormat[] = ['TWO_D', 'THREE_D', 'IMAX'];

/** Nhóm theo định dạng + âm thanh, giữ thứ tự giờ chiếu. */
export function groupVersions(showtimes: PublicShowtime[]): ShowtimeVersion[] {
  const versions = new Map<string, ShowtimeVersion>();
  for (const s of [...showtimes].sort((a, b) => a.startAt.localeCompare(b.startAt))) {
    const key = `${s.format}|${s.audioType}`;
    if (!versions.has(key)) {
      versions.set(key, { format: s.format, audioType: s.audioType, showtimes: [] });
    }
    versions.get(key)!.showtimes.push(s);
  }
  return [...versions.values()].sort(
    (a, b) => FORMAT_ORDER.indexOf(a.format) - FORMAT_ORDER.indexOf(b.format) || a.audioType.localeCompare(b.audioType),
  );
}

/** Trang chi tiết phim: rạp → phiên bản → giờ chiếu. */
export function groupByCinema(showtimes: PublicShowtime[]): CinemaShowtimes[] {
  const byCinema = new Map<number, PublicShowtime[]>();
  showtimes.forEach((s) => byCinema.set(s.cinemaId, [...(byCinema.get(s.cinemaId) ?? []), s]));
  return [...byCinema.values()]
    .map((items) => ({
      cinemaId: items[0].cinemaId,
      cinemaName: items[0].cinemaName,
      city: items[0].city,
      versions: groupVersions(items),
    }))
    .sort((a, b) => a.city.localeCompare(b.city) || a.cinemaName.localeCompare(b.cinemaName));
}

/** Trang chi tiết rạp: phim → phiên bản → giờ chiếu. */
export function groupByMovie(showtimes: PublicShowtime[]): MovieShowtimes[] {
  const byMovie = new Map<number, PublicShowtime[]>();
  showtimes.forEach((s) => byMovie.set(s.movieId, [...(byMovie.get(s.movieId) ?? []), s]));
  return [...byMovie.values()]
    .map((items) => ({
      movieId: items[0].movieId,
      movieTitle: items[0].movieTitle,
      movieSlug: items[0].movieSlug,
      moviePosterUrl: items[0].moviePosterUrl,
      ageRating: items[0].ageRating,
      durationMin: items[0].durationMin,
      versions: groupVersions(items),
    }))
    .sort((a, b) => a.movieTitle.localeCompare(b.movieTitle, 'vi'));
}
