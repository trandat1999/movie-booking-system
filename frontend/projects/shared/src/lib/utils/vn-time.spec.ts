import { isoToVnParts, nextDays, todayInVietnam, vnDateTimeToIso } from './vn-time';

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

describe('nextDays', () => {
  it('starts today in Vietnam and labels weekdays', () => {
    // 30/10/2026 23:00 giờ VN là thứ 6
    const days = nextDays(4, new Date('2026-10-30T16:00:00Z'));

    expect(days.map((d) => d.date)).toEqual(['2026-10-30', '2026-10-31', '2026-11-01', '2026-11-02']);
    expect(days.map((d) => d.label)).toEqual(['Hôm nay', 'Ngày mai', 'Chủ nhật', 'Thứ 2']);
    expect(days[2].dayMonth).toBe('01/11');
  });
});
