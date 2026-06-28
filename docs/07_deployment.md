# AetherPMO 배포 가이드 (실서버 구성)

> 백엔드(Spring Boot) + PostgreSQL은 **Render**, 프론트(Vue/정적)는 **Vercel**에 배포한다.
> 기술스택은 변경 없음 — 단지 클라우드에서 실행하기 위한 구성이다.

```
사용자 ──▶ Vercel (프론트, 정적)
              │  VITE_API_BASE_URL
              ▼
          Render (Spring Boot, Docker)  ──▶  Render PostgreSQL
```

---

## 사전 준비
- GitHub 레포: `neoulPark/AetherPMO` (이미 푸시됨)
- Render 계정, Vercel 계정 (각 무료 가입)

---

## 1단계: 백엔드 + DB 배포 (Render)

레포에 **`render.yaml` 블루프린트**가 있어 백엔드와 DB가 함께 생성된다.

1. https://render.com → **New → Blueprint**
2. GitHub 레포 `neoulPark/AetherPMO` 선택 → render.yaml 자동 감지
3. **Apply** → 다음이 자동 생성됨:
   - `aetherpmo-db` (PostgreSQL, free)
   - `aetherpmo-backend` (Docker 웹 서비스, free)
4. 빌드 완료까지 대기 (최초 ~5분). Dockerfile이 Maven 빌드 → `java -jar` 실행.
   - DB 연결: `DATABASE_URL`이 자동 주입되고, `DatabaseUrlPostProcessor`가
     `postgres://` → `jdbc:postgresql://`(sslmode=require)로 변환.
   - Flyway가 V1~V5 마이그레이션 + 시드 자동 실행.
5. 배포되면 백엔드 URL 확인: 예) `https://aetherpmo-backend.onrender.com`
   - 헬스체크: `GET /api/v1/methodology/catalog`

> ⚠️ Render 무료 웹서비스는 15분 무요청 시 슬립 → 첫 요청에 ~1분 콜드스타트.
> 무료 PostgreSQL은 생성 후 90일 제한(이후 유료 또는 재생성). 데이터는 Flyway로 재생성 가능.

---

## 2단계: 프론트 배포 (Vercel)

1. https://vercel.com → **Add New → Project** → `neoulPark/AetherPMO` Import
2. 설정:
   | 항목 | 값 |
   |------|-----|
   | **Root Directory** | `frontend` |
   | Framework | Vite (자동) |
   | Build Command | `npm run build` |
   | Output Directory | `dist` |
3. **Environment Variables** 추가:
   | Key | Value |
   |-----|-------|
   | `VITE_API_BASE_URL` | `https://aetherpmo-backend.onrender.com/api/v1` (1단계 백엔드 URL + `/api/v1`) |
4. **Deploy** → 프론트 URL 확인: 예) `https://aether-pmo.vercel.app`

> 참고: 현재 프론트는 hash 라우터(`/#/...`)라 SPA rewrite 설정 불필요.

---

## 3단계: CORS 연결 (백엔드 ↔ 프론트)

프론트 도메인을 백엔드 CORS 허용 목록에 추가한다.

1. Render → `aetherpmo-backend` → **Environment**
2. `APP_CORS_ORIGINS` 값 설정: `https://aether-pmo.vercel.app` (2단계 프론트 URL)
3. 저장 → 백엔드 재배포(자동)

---

## 4단계: 확인
- 프론트 URL 접속 → 프로젝트 목록/상세/WBS/산출물이 **실데이터**로 표시되면 성공.
- 안 되면: 브라우저 콘솔에서 API 호출 URL·CORS 오류 확인.

---

## 환경변수 요약

### 백엔드 (Render)
| 변수 | 설명 | 출처 |
|------|------|------|
| `DATABASE_URL` | DB 연결 문자열 | 블루프린트 자동 |
| `APP_CORS_ORIGINS` | 허용 오리진(콤마 구분) | 프론트 URL 입력 |
| `PORT` | 서비스 포트 | Render 자동 |

### 프론트 (Vercel)
| 변수 | 설명 |
|------|------|
| `VITE_API_BASE_URL` | 백엔드 API 베이스 URL (`.../api/v1`) |

---

## 로컬 개발 (변경 없음)
```bash
# DB
sudo pg_ctlcluster 16 main start   # 또는 docker postgres

# 백엔드 (localhost:8080)
cd backend && mvn spring-boot:run

# 프론트 (localhost:5173, /api → 8080 프록시)
cd frontend && npm run dev
```
로컬은 `VITE_API_BASE_URL` 미설정 → vite 프록시(`/api` → 8080) 사용.

---

## 대안: Railway
Render 대신 Railway도 가능 (Dockerfile 그대로 사용).
- New Project → Deploy from GitHub → root `backend` → Dockerfile 자동 감지
- PostgreSQL 플러그인 추가 → `DATABASE_URL` 자동 주입 (동일하게 변환됨)
- 프론트는 동일하게 Vercel + `VITE_API_BASE_URL`
