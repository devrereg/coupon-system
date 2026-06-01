import styles from "./Badge.module.css";

export type BadgeTone = "normal" | "low" | "sold-out";

export interface BadgeProps {
  tone: BadgeTone;
  text: string;
}

const toneClass: Record<BadgeTone, string> = {
  normal: styles.normal,
  low: styles.low,
  "sold-out": styles.soldOut,
};

/**
 * atom — 잔여 재고 상태 칩. 색이 아니라 톤(흑백/그레이 폴라리티)으로 의미 전달.
 */
export function Badge({ tone, text }: BadgeProps) {
  return <span className={`${styles.badge} ${toneClass[tone]}`}>{text}</span>;
}
