import { motion } from 'framer-motion';
import {
  Accessibility,
  ArrowRight,
  Bot,
  FileSearch,
  Gauge,
  ListChecks,
  Sparkles,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Section from '../components/ui/Section';
import UrlScanForm from '../components/scanner/UrlScanForm';
import WebbyRobot from '../components/webby/WebbyRobot';

const FEATURES = [
  {
    icon: Gauge,
    title: 'Website Quality Score',
    text: 'A 0-100 score per category, calculated from documented rules - never invented.',
  },
  {
    icon: FileSearch,
    title: 'SEO Checks',
    text: 'Title, meta description, heading structure, canonical URL and indexability.',
  },
  {
    icon: Accessibility,
    title: 'Accessibility Basics',
    text: 'Alt text, form labels, accessible names, declared document language.',
  },
  {
    icon: ListChecks,
    title: 'Technical HTML Checks',
    text: 'HTTPS, document structure, viewport, charset, link targets and mixed content.',
  },
  {
    icon: Sparkles,
    title: 'Actionable Recommendations',
    text: 'Every finding ships with evidence, a reason it matters and a suggested fix.',
  },
  {
    icon: Bot,
    title: 'AI Developer Copilot',
    text: 'Webby explains findings and drafts code fixes, grounded in your actual report.',
  },
];

const STEPS = [
  { title: 'Enter a public website URL', text: 'The backend validates the address and refuses internal or private targets.' },
  { title: 'Scan measurable indicators', text: '25 checks run against the HTML that was really downloaded - nothing else is assumed.' },
  { title: 'Review evidence and priorities', text: 'Findings are grouped by severity with the exact evidence behind each one.' },
  { title: 'Improve with Webby’s guidance', text: 'Ask the copilot what to fix first and copy an example fix into your codebase.' },
];

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-20">
      <Section className="pt-6">
        <div className="grid items-center gap-10 lg:grid-cols-[1.15fr_0.85fr]">
          <div>
            <motion.span
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className="inline-flex items-center gap-2 rounded-full border border-cyanx-400/25 bg-cyanx-500/10 px-3 py-1 text-xs text-cyanx-300"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-cyanx-400" />
              Real HTML scanning · deterministic scoring
            </motion.span>

            <motion.h1
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05 }}
              className="mt-5 max-w-2xl text-4xl font-bold leading-[1.1] tracking-tight text-white sm:text-5xl"
            >
              Make Your Website Better,
              <span className="bg-gradient-to-r from-cyanx-300 via-electric-400 to-violetish-400 bg-clip-text text-transparent">
                {' '}
                One Scan at a Time.
              </span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.12 }}
              className="mt-4 max-w-xl text-[15px] leading-relaxed text-slate-400"
            >
              Paste a URL and WebScorer AI reads the public HTML, scores SEO, accessibility,
              technical structure and content, then hands every problem to Webby - your website’s
              smartest little copilot.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.18 }}
              className="mt-7 max-w-xl"
            >
              <UrlScanForm />
              <button
                type="button"
                onClick={() => navigate('/report/sample')}
                className="mt-4 inline-flex items-center gap-1.5 text-sm text-slate-400 underline decoration-white/20 underline-offset-4 transition hover:text-cyanx-300 hover:decoration-cyanx-400/50"
              >
                View Sample Report <ArrowRight className="h-3.5 w-3.5" />
              </button>
            </motion.div>
          </div>

          <motion.div
            initial={{ opacity: 0, scale: 0.94 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.1, duration: 0.5 }}
            className="card relative flex flex-col items-center gap-3 px-6 py-8"
          >
            <div className="absolute inset-x-8 top-0 h-px bg-gradient-to-r from-transparent via-cyanx-400/40 to-transparent" />
            <motion.div
              animate={{ y: [0, -9, 0], rotate: [-2.5, 2.5, -2.5] }}
              transition={{ duration: 4.6, repeat: Infinity, ease: 'easeInOut' }}
            >
              <WebbyRobot mood="happy" size={150} />
            </motion.div>
            <p className="text-center text-sm text-slate-300">
              “Hi! I’m Webby. Paste a URL and I’ll help you inspect it.”
            </p>
            <span className="rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-[11px] uppercase tracking-widest text-slate-500">
              Scan. Score. Improve.
            </span>
          </motion.div>
        </div>
      </Section>

      <Section>
        <div className="mb-8 text-center">
          <h2 className="text-2xl font-semibold text-white">What the scan actually measures</h2>
          <p className="mt-2 text-sm text-slate-400">
            Six report areas, one transparent scoring engine.
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.05, duration: 0.4 }}
              className="card group p-5 transition hover:border-cyanx-400/25 hover:bg-navy-800/60"
            >
              <span className="mb-4 flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-electric-500/20 to-violetish-600/20 text-cyanx-300 transition group-hover:from-electric-500/30 group-hover:to-violetish-600/30">
                <feature.icon className="h-5 w-5" />
              </span>
              <h3 className="text-[15px] font-semibold text-white">{feature.title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-slate-400">{feature.text}</p>
            </motion.div>
          ))}
        </div>
      </Section>

      <Section>
        <div className="card p-6 sm:p-8">
          <h2 className="text-xl font-semibold text-white">How it works</h2>
          <ol className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {STEPS.map((step, index) => (
              <li key={step.title} className="relative">
                <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-full border border-cyanx-400/30 bg-cyanx-500/10 text-sm font-semibold text-cyanx-300">
                  {index + 1}
                </div>
                <h3 className="text-sm font-semibold text-white">{step.title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-slate-400">{step.text}</p>
              </li>
            ))}
          </ol>
          <p className="mt-6 border-t border-white/[0.06] pt-4 text-xs leading-relaxed text-slate-500">
            WebScorer AI reports on HTML structure and metadata only. It is not a performance
            benchmark, a penetration test or a guarantee of search rankings - the report says so
            whenever a measurement was not taken.
          </p>
        </div>
      </Section>
    </div>
  );
}
