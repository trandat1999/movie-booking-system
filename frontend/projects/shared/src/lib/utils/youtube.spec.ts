import { youtubeEmbedUrl } from './youtube';

describe('youtubeEmbedUrl', () => {
  it('accepts common YouTube link shapes', () => {
    const embed = 'https://www.youtube.com/embed/dQw4w9WgXcQ';
    expect(youtubeEmbedUrl('https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10')).toBe(embed);
    expect(youtubeEmbedUrl('https://youtu.be/dQw4w9WgXcQ')).toBe(embed);
    expect(youtubeEmbedUrl('https://m.youtube.com/shorts/dQw4w9WgXcQ')).toBe(embed);
  });

  it('rejects other hosts and malformed ids', () => {
    expect(youtubeEmbedUrl('https://evil.example.com/watch?v=dQw4w9WgXcQ')).toBeNull();
    expect(youtubeEmbedUrl('https://www.youtube.com/watch?v=abc"onload=')).toBeNull();
    expect(youtubeEmbedUrl('javascript:alert(1)')).toBeNull();
    expect(youtubeEmbedUrl(null)).toBeNull();
  });
});
