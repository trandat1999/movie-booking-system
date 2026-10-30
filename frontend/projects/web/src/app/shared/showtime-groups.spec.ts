import { PublicShowtime } from '@mb/shared';

import { groupByCinema, groupByMovie, groupVersions } from './showtime-groups';

function showtime(overrides: Partial<PublicShowtime>): PublicShowtime {
  return {
    id: 1,
    movieId: 10,
    movieTitle: 'Phim A',
    movieSlug: 'phim-a',
    moviePosterUrl: null,
    ageRating: 'T13',
    durationMin: 100,
    cinemaId: 1,
    cinemaName: 'Rạp 1',
    city: 'Hà Nội',
    roomId: 1,
    roomName: 'Phòng 1',
    roomType: 'STANDARD',
    startAt: '2026-11-01T02:00:00Z',
    endAt: '2026-11-01T03:40:00Z',
    format: 'TWO_D',
    audioType: 'SUBTITLED',
    availableSeats: 10,
    minPrice: 75000,
    ...overrides,
  };
}

describe('showtime-groups', () => {
  it('groups by format and audio, 2D before 3D, times ascending', () => {
    const versions = groupVersions([
      showtime({ id: 1, format: 'THREE_D', startAt: '2026-11-01T05:00:00Z' }),
      showtime({ id: 2, startAt: '2026-11-01T08:00:00Z' }),
      showtime({ id: 3, startAt: '2026-11-01T02:00:00Z' }),
      showtime({ id: 4, audioType: 'DUBBED' }),
    ]);

    expect(versions.map((v) => `${v.format}/${v.audioType}`)).toEqual([
      'TWO_D/DUBBED',
      'TWO_D/SUBTITLED',
      'THREE_D/SUBTITLED',
    ]);
    expect(versions[1].showtimes.map((s) => s.id)).toEqual([3, 2]);
  });

  it('groups by cinema sorted by city then name', () => {
    const groups = groupByCinema([
      showtime({ id: 1, cinemaId: 2, cinemaName: 'B', city: 'TP. Hồ Chí Minh' }),
      showtime({ id: 2, cinemaId: 1, cinemaName: 'A', city: 'Hà Nội' }),
      showtime({ id: 3, cinemaId: 2, cinemaName: 'B', city: 'TP. Hồ Chí Minh' }),
    ]);

    expect(groups.map((g) => g.cinemaId)).toEqual([1, 2]);
    expect(groups[1].versions[0].showtimes).toHaveSize(2);
  });

  it('groups by movie', () => {
    const groups = groupByMovie([
      showtime({ id: 1, movieId: 20, movieTitle: 'Bầu Trời' }),
      showtime({ id: 2, movieId: 10, movieTitle: 'An Nhiên' }),
    ]);

    expect(groups.map((g) => g.movieTitle)).toEqual(['An Nhiên', 'Bầu Trời']);
  });
});
