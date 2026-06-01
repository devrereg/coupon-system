# coupon-system (monorepo)

선착순 쿠폰 발급 시스템 — 백엔드 역량 강화용 사이드 프로젝트.
동시성 제어, AOP, Kafka, Spring Batch, Kubernetes, 모니터링을 단계적으로 학습한다.

## 구조

```
coupon-system/
├── apps/
│   ├── api/          # 백엔드 — Spring Boot 3.5 + Kotlin, 레이어드 아키텍처
│   └── web/          # 프론트 — Vite + React + TS, atomic design
├── docker-compose.yml  # 공용 인프라 (PostgreSQL + Redis)
└── .sdkmanrc           # JDK/Gradle 툴체인 (레포 전체)
```

## 공용 인프라 (레포 루트에서 실행)

```bash
docker compose up -d        # PostgreSQL(호스트 15432) + Redis(6379)
docker compose ps           # healthy 확인
docker compose down         # 종료 (데이터 유지)
```

## 백엔드 (apps/api)

```bash
sdk env                     # .sdkmanrc 기준 JDK 21 적용 (최초 1회 sdk env install)
cd apps/api && ./gradlew build   # 인프라가 떠 있어야 통과
cd apps/api && ./gradlew bootRun
```

## 프론트엔드 (apps/web)

```bash
cd apps/web && npm install
cd apps/web && npm run dev
```
디자인 시스템 기준 문서: [apps/web/DESIGN.md](apps/web/DESIGN.md) (Uber, 흑백 + pill).

## 학습 로드맵

- **1주차** 동시성 제어 + Spring AOP (분산 락)
- **2주차** Kafka / Event-Driven Architecture
- **3주차** Spring Batch (대용량 정산)
- **4주차** Kubernetes + Prometheus/Grafana 모니터링
