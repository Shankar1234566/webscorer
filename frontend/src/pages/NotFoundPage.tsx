import { Compass } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="card mx-auto mt-10 max-w-md p-8 text-center">
      <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-electric-500/15 text-electric-400">
        <Compass className="h-6 w-6" />
      </span>
      <h1 className="mt-4 text-xl font-semibold text-white">Page not found</h1>
      <p className="mt-2 text-sm text-slate-400">
        That route does not exist in WebScorer AI.
      </p>
      <Link
        to="/"
        className="mt-5 inline-flex rounded-xl bg-gradient-to-r from-electric-500 to-violetish-600 px-4 py-2.5 text-sm font-medium text-white"
      >
        Back home
      </Link>
    </div>
  );
}
