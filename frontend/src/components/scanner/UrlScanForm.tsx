import { Loader2, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import { api, ApiRequestError } from '../../services/api';
import { clientValidateUrl, normalizeClientUrl } from '../../utils/url';
import Button from '../ui/Button';

interface Props {
  /** Element id so Webby's "focus the input" action can find it. */
  inputId?: string;
  autoFocus?: boolean;
}

/** Shared URL entry form used by the hero and the scanner page. */
export default function UrlScanForm({ inputId = 'url-input', autoFocus = false }: Props) {
  const [value, setValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { setActiveScan, pushToast } = useApp();

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const validationError = clientValidateUrl(value);
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const scan = await api.createScan(normalizeClientUrl(value));
      setActiveScan(scan);
      pushToast('Scan started - Webby will show each stage.', 'success');
      navigate(`/report/${scan.id}`);
    } catch (e) {
      const message = e instanceof ApiRequestError ? e.message : 'Could not start the scan.';
      setError(message);
      pushToast(message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={submit} noValidate className="w-full">
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            id={inputId}
            type="text"
            inputMode="url"
            autoComplete="url"
            spellCheck={false}
            autoFocus={autoFocus}
            value={value}
            onChange={(event) => {
              setValue(event.target.value);
              if (error) setError(null);
            }}
            placeholder="https://your-website.com"
            aria-label="Website URL to scan"
            aria-invalid={error !== null}
            aria-describedby={error ? `${inputId}-error` : undefined}
            className="input-field h-12 pl-11 text-[15px]"
          />
        </div>
        <Button type="submit" size="lg" disabled={submitting} className="h-12 sm:w-auto">
          {submitting ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" /> Starting…
            </>
          ) : (
            <>Analyze Website</>
          )}
        </Button>
      </div>
      {error && (
        <p id={`${inputId}-error`} role="alert" className="mt-2 text-sm text-rose-300">
          {error}
        </p>
      )}
      <p className="mt-2.5 text-xs text-slate-500">
        Only public HTTP(S) pages are fetched. Internal, loopback and private addresses are rejected
        by the backend.
      </p>
    </form>
  );
}
