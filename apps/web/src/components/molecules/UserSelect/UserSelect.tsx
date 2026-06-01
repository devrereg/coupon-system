import styles from "./UserSelect.module.css";

export interface UserOption {
  id: number;
  name: string;
}

export interface UserSelectProps {
  options: UserOption[];
  /** 선택된 유저 id. 미선택이면 null. */
  value: number | null;
  onChange: (userId: number | null) => void;
  disabled?: boolean;
}

/**
 * molecule — 라벨 + native select 의 발급 대상 드롭다운.
 * 범용 프레젠테이션: 옵션/값/핸들러를 props 로만 받는다.
 */
export function UserSelect({ options, value, onChange, disabled = false }: UserSelectProps) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor="user-select">
        발급 대상
      </label>
      <select
        id="user-select"
        className={styles.select}
        value={value ?? ""}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value === "" ? null : Number(e.target.value))}
      >
        <option value="" disabled className={styles.placeholder}>
          유저를 선택하세요
        </option>
        {options.map((user) => (
          <option key={user.id} value={user.id}>
            {user.name}
          </option>
        ))}
      </select>
    </div>
  );
}
