/**
 * Client-side URL checks. The backend re-validates everything (including SSRF
 * guards) - this only gives the user instant feedback before a request is sent.
 */
export function clientValidateUrl(raw: string): string | null {
  const value = raw.trim();
  if (!value) {
    return 'Enter a website URL to scan.';
  }
  if (value.length > 2048) {
    return 'That URL is too long (over 2048 characters).';
  }

  const hasScheme = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(value);
  const candidate = hasScheme ? value : `https://${value}`;

  let parsed: URL;
  try {
    parsed = new URL(candidate);
  } catch {
    return 'That is not a valid URL, for example https://example.com';
  }

  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return 'Only http:// and https:// URLs can be scanned.';
  }
  if (parsed.username || parsed.password) {
    return 'URLs containing credentials (user:password@) are not allowed.';
  }
  if (!parsed.hostname.includes('.') && parsed.hostname !== 'localhost') {
    return 'That is not a valid URL, for example https://example.com';
  }
  return null;
}

/** Normalises whatever the user typed into a absolute URL string. */
export function normalizeClientUrl(raw: string): string {
  const value = raw.trim();
  const hasScheme = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(value);
  return hasScheme ? value : `https://${value}`;
}
