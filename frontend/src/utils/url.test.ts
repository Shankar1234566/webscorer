import { describe, expect, it } from 'vitest';
import { clientValidateUrl, normalizeClientUrl } from './url';

describe('clientValidateUrl', () => {
  it('accepts a normal https URL', () => {
    expect(clientValidateUrl('https://example.com')).toBeNull();
  });

  it('accepts a bare domain by assuming https', () => {
    expect(clientValidateUrl('example.com/page')).toBeNull();
    expect(normalizeClientUrl('example.com/page')).toBe('https://example.com/page');
  });

  it('rejects an empty value with a helpful message', () => {
    expect(clientValidateUrl('   ')).toBe('Enter a website URL to scan.');
  });

  it('rejects non-http schemes', () => {
    expect(clientValidateUrl('ftp://example.com')).toContain('Only http:// and https://');
    expect(clientValidateUrl('javascript:alert(1)')).toBe('That is not a valid URL, for example https://example.com');
  });

  it('rejects credentials inside the URL', () => {
    expect(clientValidateUrl('https://user:pass@example.com')).toContain('credentials');
  });

  it('rejects nonsense', () => {
    expect(clientValidateUrl('not a url')).toBe('That is not a valid URL, for example https://example.com');
  });

  it('rejects over-long values', () => {
    expect(clientValidateUrl(`https://example.com/${'a'.repeat(3000)}`)).toContain('too long');
  });
});
