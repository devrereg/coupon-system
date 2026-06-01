/**
 * 백엔드 API 클라이언트.
 * 학습 결정(plan 2026-06-02): Vite proxy 가 아니라 CORS 로 8080 을 직접 호출한다.
 */

/** 백엔드 base URL — 단일 상수로 분리. */
export const API_BASE_URL = "http://localhost:8080";

/** GET /api/coupons 응답 한 건. remaining = 잔여 재고. */
export interface Coupon {
  id: number;
  name: string;
  totalQuantity: number;
  remaining: number;
}

/** GET /api/users 응답 한 건(시드 유저 드롭다운용). */
export interface User {
  id: number;
  name: string;
}

/** POST /api/coupons/{id}/issue 성공(201) 응답. issuedAt 은 ISO 문자열. */
export interface IssueResult {
  issueId: number;
  couponId: number;
  userId: number;
  issuedAt: string;
}

/** 백엔드 공통 에러 바디. */
export interface ApiErrorBody {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

/**
 * 비-2xx 응답을 담는 에러. page 는 status + message 로 409 소진/중복을 구분한다.
 */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`);
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as ApiErrorBody | null;
    throw new ApiError(res.status, body?.message ?? res.statusText);
  }
  return (await res.json()) as T;
}

/** 쿠폰 목록 조회. 첫 항목이 활성 쿠폰. */
export function getCoupons(): Promise<Coupon[]> {
  return getJson<Coupon[]>("/api/coupons");
}

/** 시드 유저 목록 조회. */
export function getUsers(): Promise<User[]> {
  return getJson<User[]>("/api/users");
}

/**
 * 쿠폰 발급. 비-2xx 면 ApiError(status, message) 를 throw 한다.
 * 409 는 message 로 소진("재고가 소진"/"재고 소진") vs 중복("이미 발급")을 구분한다.
 */
export async function issueCoupon(
  couponId: number,
  userId: number,
): Promise<IssueResult> {
  const res = await fetch(`${API_BASE_URL}/api/coupons/${couponId}/issue`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId }),
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as ApiErrorBody | null;
    throw new ApiError(res.status, body?.message ?? res.statusText);
  }
  return (await res.json()) as IssueResult;
}
