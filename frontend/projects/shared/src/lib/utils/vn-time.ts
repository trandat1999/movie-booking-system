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

export interface DayTab {
  /** yyyy-MM-dd theo giờ Việt Nam */
  date: string;
  /** "Hôm nay", "Ngày mai", "Thứ 2"… "Chủ nhật" */
  label: string;
  /** dd/MM */
  dayMonth: string;
}

const WEEKDAYS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

/** {@link count} ngày liên tiếp bắt đầu từ hôm nay (giờ Việt Nam) cho thanh chọn ngày chiếu. */
export function nextDays(count: number, now = new Date()): DayTab[] {
  const [y, m, d] = todayInVietnam(now).split('-').map(Number);
  return Array.from({ length: count }, (_, i) => {
    // Dùng UTC để cộng ngày không bị ảnh hưởng múi giờ trình duyệt
    const day = new Date(Date.UTC(y, m - 1, d + i));
    const date = day.toISOString().slice(0, 10);
    const dayMonth = `${date.slice(8, 10)}/${date.slice(5, 7)}`;
    const label = i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : WEEKDAYS[day.getUTCDay()];
    return { date, label, dayMonth };
  });
}
