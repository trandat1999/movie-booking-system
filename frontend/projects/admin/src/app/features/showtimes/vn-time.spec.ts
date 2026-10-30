import { isoToVnParts, todayInVietnam, vnDateTimeToIso } from './vn-time';

describe('vn-time', () => {
  it('converts Vietnam local date/time to UTC instant and back', () => {
    const iso = vnDateTimeToIso('2026-11-04', '23:30');

    expect(iso).toBe('2026-11-04T16:30:00.000Z');
    expect(isoToVnParts(iso)).toEqual({ date: '2026-11-04', time: '23:30' });
  });

  it('uses Vietnam date for today even when UTC is still the previous day', () => {
    expect(todayInVietnam(new Date('2026-10-30T18:00:00Z'))).toBe('2026-10-31');
  });
});
