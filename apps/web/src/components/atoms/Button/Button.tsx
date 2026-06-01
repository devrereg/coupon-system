import styles from "./Button.module.css";

export interface ButtonProps {
  /** 기본 라벨. loading 일 때는 "발급 중…" 으로 대체된다. */
  label: string;
  onClick: () => void;
  disabled?: boolean;
  /** 발급 POST 진행 중. 클릭 비활성 + 흰 스피너 + "발급 중…". */
  loading?: boolean;
}

/**
 * atom — 검정 999px pill, full-width 의 주요 CTA.
 * 도메인 지식 없는 범용 버튼.
 */
export function Button({ label, onClick, disabled = false, loading = false }: ButtonProps) {
  return (
    <button
      type="button"
      className={`${styles.button} ${loading ? styles.loading : ""}`}
      onClick={onClick}
      disabled={disabled || loading}
      aria-busy={loading}
    >
      {loading ? (
        <>
          <span className={styles.spinner} aria-hidden="true" />
          발급 중…
        </>
      ) : (
        label
      )}
    </button>
  );
}
