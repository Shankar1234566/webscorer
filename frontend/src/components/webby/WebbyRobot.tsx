/**
 * Webby - the WebScorer AI copilot mascot.
 *
 * Original SVG artwork: a small rounded white-and-blue robot with a dark
 * visor, big cyan eyes, a chest light and tiny arms. Everything is drawn
 * locally, so the app never depends on an external image URL.
 *
 * The blinking and chest-glow motion comes from CSS classes defined in
 * index.css and is disabled automatically under prefers-reduced-motion.
 */

export type WebbyMood = 'happy' | 'thinking' | 'worried' | 'excited';

interface Props {
  mood?: WebbyMood;
  size?: number;
  className?: string;
  /** Skip CSS animations (used for static inline renders such as chat avatars). */
  static?: boolean;
}

function mouthPath(mood: WebbyMood): string {
  switch (mood) {
    case 'excited':
      return 'M 56 68 Q 70 84 84 68';
    case 'thinking':
      return 'M 60 73 Q 70 71 80 74';
    case 'worried':
      return 'M 58 77 Q 70 68 82 77';
    case 'happy':
    default:
      return 'M 58 70 Q 70 80 82 70';
  }
}

export default function WebbyRobot({
  mood = 'happy',
  size = 96,
  className = '',
  isStatic = false,
}: Props & { isStatic?: boolean }) {
  const eyelidClass = isStatic ? '' : 'webby-eyelid';
  const chestClass = isStatic ? '' : 'webby-chest-glow';

  return (
    <svg
      width={size}
      height={(size * 150) / 140}
      viewBox="0 0 140 150"
      role="img"
      aria-label={`Webby the robot, looking ${mood}`}
      className={className}
    >
      <defs>
        <linearGradient id="webby-body" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#ffffff" />
          <stop offset="100%" stopColor="#cfe0ff" />
        </linearGradient>
        <linearGradient id="webby-head" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#ffffff" />
          <stop offset="100%" stopColor="#dbe7ff" />
        </linearGradient>
        <radialGradient id="webby-halo" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#22d3ee" stopOpacity="0.45" />
          <stop offset="100%" stopColor="#22d3ee" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="webby-visor" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#101a35" />
          <stop offset="100%" stopColor="#070d1f" />
        </linearGradient>
      </defs>

      {/* soft cyan halo */}
      <circle cx="70" cy="76" r="62" fill="url(#webby-halo)" />

      {/* antenna */}
      <line x1="70" y1="30" x2="70" y2="16" stroke="#94a3d8" strokeWidth="4" strokeLinecap="round" />
      <circle cx="70" cy="12" r="6.5" fill="#22d3ee" />
      <circle cx="70" cy="12" r="11" fill="#22d3ee" opacity="0.18" />

      {/* feet */}
      <rect x="47" y="130" width="20" height="12" rx="6" fill="#a9bde8" />
      <rect x="73" y="130" width="20" height="12" rx="6" fill="#a9bde8" />

      {/* arms */}
      <rect x="28" y="98" width="13" height="30" rx="6.5" fill="#dbe7ff" transform="rotate(-12 34.5 113)" />
      <rect x="99" y="98" width="13" height="30" rx="6.5" fill="#dbe7ff" transform="rotate(12 105.5 113)" />

      {/* body */}
      <rect x="43" y="92" width="54" height="44" rx="20" fill="url(#webby-body)" stroke="#9fb6ea" strokeWidth="1.5" />
      <circle cx="70" cy="112" r="9.5" fill="#0b1226" />
      <circle cx="70" cy="112" r="7" fill="#22d3ee" className={chestClass} />
      <circle cx="67.5" cy="109.5" r="2.4" fill="#ffffff" opacity="0.85" />

      {/* head */}
      <rect x="26" y="26" width="88" height="68" rx="26" fill="url(#webby-head)" stroke="#9fb6ea" strokeWidth="1.5" />
      {/* ears */}
      <rect x="18" y="47" width="10" height="26" rx="5" fill="#b8c9f2" />
      <rect x="112" y="47" width="10" height="26" rx="5" fill="#b8c9f2" />

      {/* visor */}
      <rect x="36" y="38" width="68" height="44" rx="18" fill="url(#webby-visor)" />

      {/* eyes */}
      <g>
        <ellipse cx="57" cy="58" rx="9.5" ry="11.5" fill="#22d3ee" />
        <ellipse cx="83" cy="58" rx="9.5" ry="11.5" fill="#22d3ee" />
        <circle cx="60" cy="54" r="3.2" fill="#ffffff" opacity="0.9" />
        <circle cx="86" cy="54" r="3.2" fill="#ffffff" opacity="0.9" />
        {/* eyelids - animated by CSS, laid over the eyes */}
        <rect x="46" y="45" width="22" height="26" rx="11" fill="#0b1226" className={eyelidClass} />
        <rect x="72" y="45" width="22" height="26" rx="11" fill="#0b1226" className={eyelidClass ? `${eyelidClass} webby-eyelid--delayed` : ''} />
      </g>

      {/* mouth */}
      <path
        d={mouthPath(mood)}
        fill="none"
        stroke="#22d3ee"
        strokeWidth="4"
        strokeLinecap="round"
      />

      {/* cheek blush */}
      <circle cx="45" cy="74" r="4" fill="#8b5cf6" opacity="0.35" />
      <circle cx="95" cy="74" r="4" fill="#8b5cf6" opacity="0.35" />
    </svg>
  );
}
