import { coverageTone } from './testing.models';

describe('coverageTone', () => {
  it('maps coverage bands', () => {
    expect(coverageTone(90)).toBe('green');
    expect(coverageTone(89)).toBe('yellow');
    expect(coverageTone(70)).toBe('yellow');
    expect(coverageTone(69)).toBe('red');
  });
});
