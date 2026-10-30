/**
 * Giờ chiếu luôn nhập và hiển thị theo giờ Việt Nam (UTC+7), không phụ thuộc múi giờ của trình duyệt.
 */
export const VN_OFFSET = '+0700';

const VN_ZONE = 'Asia/Ho_Chi_Minh';

/** yyyy-MM-dd của hôm nay theo giờ Việt Nam. */
export function todayInVietnam(now = new Date()): string {
  return now.toLocaleDateString('en-CA', { timeZone: VN_ZONE });
}

/** Ghép ngày (yyyy-MM-dd) và giờ (HH:mm) theo giờ Việt Nam thành ISO-8601 có offset. */
export function vnDateTimeToIso(date: string, time: string): string {
  return new Date(`${date}T${time}:00+07:00`).toISOString();
}

/** ISO instant → { date: yyyy-MM-dd, time: HH:mm } theo giờ Việt Nam. */
export function isoToVnParts(iso: string): { date: string; time: string } {
  const instant = new Date(iso);
  return {
    date: instant.toLocaleDateString('en-CA', { timeZone: VN_ZONE }),
    time: instant.toLocaleTimeString('en-GB', { timeZone: VN_ZONE, hour: '2-digit', minute: '2-digit' }),
  };
}
