import styles from "./ResultMessage.module.css";

export type ResultVariant = "success" | "soldout" | "duplicate" | "error";

export interface ResultMessageProps {
  variant: ResultVariant;
  text: string;
}

/**
 * atom — 발급 결과 메시지.
 * success 만 검정 밴드(폴라리티 플립), 나머지는 canvas-soft 표면.
 */
export function ResultMessage({ variant, text }: ResultMessageProps) {
  const tone = variant === "success" ? styles.success : styles.neutral;
  return (
    <p className={`${styles.message} ${tone}`} role="status">
      {text}
    </p>
  );
}
