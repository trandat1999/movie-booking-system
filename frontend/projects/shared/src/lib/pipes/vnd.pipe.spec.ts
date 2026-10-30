import { VndPipe } from './vnd.pipe';

describe('VndPipe', () => {
  const pipe = new VndPipe();

  it('formats integer VND amounts', () => {
    expect(pipe.transform(95000).replace(/\s/g, ' ')).toBe('95.000 ₫');
  });

  it('returns empty string for missing values', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
