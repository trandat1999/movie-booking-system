/** Chỉ cho phép quay về đường dẫn nội bộ sau khi đăng nhập (chống open redirect). */
export function safeReturnUrl(url: string | null | undefined, fallback = '/'): string {
  return url && url.startsWith('/') && !url.startsWith('//') ? url : fallback;
}
