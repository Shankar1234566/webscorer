import { motion } from 'framer-motion';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  className?: string;
  /** Sections fade up slightly as they scroll into view. */
  animate?: boolean;
}

export default function Section({ children, className = '', animate = true }: Props) {
  if (!animate) {
    return <section className={className}>{children}</section>;
  }
  return (
    <motion.section
      initial={{ opacity: 0, y: 18 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-60px' }}
      transition={{ duration: 0.45, ease: 'easeOut' }}
      className={className}
    >
      {children}
    </motion.section>
  );
}
