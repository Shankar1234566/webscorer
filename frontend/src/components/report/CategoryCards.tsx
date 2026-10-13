import { motion } from 'framer-motion';
import type { CategoryScore } from '../../types/api';
import ProgressBar from '../ui/ProgressBar';
import Tooltip from '../ui/Tooltip';

const HINTS: Record<string, string> = {
  SEO: 'Weight 35% of the overall score. Title, description, headings, canonical, robots and alt text.',
  Accessibility: 'Weight 25%. Image alt text, form labels, accessible names, language declaration, iframe titles.',
  Technical: 'Weight 25%. HTTPS, retrievable document, structure, viewport, charset, link targets, mixed content.',
  Content: 'Weight 15%. Text volume, section headings, navigation landmarks and descriptive link text.',
};

export default function CategoryCards({ scores }: { scores: CategoryScore[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {scores.map((score, index) => (
        <motion.div
          key={score.category}
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.06, duration: 0.35 }}
          className="card p-5"
        >
          <div className="mb-3 flex items-start justify-between gap-2">
            <div>
              <p className="text-sm font-semibold text-white">{score.label}</p>
              <p className="text-xs text-slate-500">{score.hint}</p>
            </div>
            <Tooltip label={HINTS[score.category] ?? score.hint} />
          </div>
          <ProgressBar
            label="Score"
            value={score.score}
            defined={score.checksDefined}
            applicable={score.checksApplicable}
          />
        </motion.div>
      ))}
    </div>
  );
}
