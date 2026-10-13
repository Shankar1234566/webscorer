export function SkeletonLine({ className = '' }: { className?: string }) {
  return <div className={`skeleton h-4 ${className}`} />;
}

export function SkeletonCard({ lines = 3 }: { lines?: number }) {
  return (
    <div className="card space-y-3 p-5">
      <SkeletonLine className="w-1/3" />
      {Array.from({ length: lines }).map((_, index) => (
        <SkeletonLine key={index} className="w-full" />
      ))}
    </div>
  );
}

/** Loading placeholder shown while the report request is in flight. */
export default function ReportSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true" aria-label="Loading report">
      <div className="card p-6">
        <SkeletonLine className="w-1/4" />
        <div className="mt-4 flex items-center gap-6">
          <div className="skeleton h-28 w-28 rounded-full" />
          <div className="flex-1 space-y-3">
            <SkeletonLine className="w-2/3" />
            <SkeletonLine className="w-full" />
            <SkeletonLine className="w-5/6" />
          </div>
        </div>
      </div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <SkeletonCard key={index} lines={2} />
        ))}
      </div>
      <SkeletonCard lines={4} />
    </div>
  );
}
