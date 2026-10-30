/**
 * URL nhúng YouTube từ link xem (watch?v=, youtu.be/, /embed/, /shorts/). Trả về null nếu không phải link
 * YouTube hợp lệ — chỉ URL do hàm này tạo mới được đưa vào iframe.
 */
export function youtubeEmbedUrl(url: string | null | undefined): string | null {
  if (!url) {
    return null;
  }
  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    return null;
  }
  const host = parsed.hostname.replace(/^www\.|^m\./, '');
  let id: string | null = null;
  if (host === 'youtu.be') {
    id = parsed.pathname.slice(1);
  } else if (host === 'youtube.com') {
    id = parsed.searchParams.get('v') ?? parsed.pathname.match(/^\/(?:embed|shorts)\/([^/]+)/)?.[1] ?? null;
  }
  return id && /^[\w-]{11}$/.test(id) ? `https://www.youtube.com/embed/${id}` : null;
}
