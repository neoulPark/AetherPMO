# AetherPMO 시스템 아키텍처 문서

> **문서 버전**: v1.0.0
> **최초 작성일**: 2026-06-15
> **대상 독자**: 개발팀, 기술 아키텍트, PMO

---

## 목차

1. [시스템 개요](#1-시스템-개요)
2. [전체 시스템 구성도](#2-전체-시스템-구성도)
3. [기술 스택 상세](#3-기술-스택-상세)
4. [백엔드 모듈 구조](#4-백엔드-모듈-구조)
5. [프론트엔드 모듈 구조](#5-프론트엔드-모듈-구조)
6. [데이터베이스 구성](#6-데이터베이스-구성)
7. [인증/인가 흐름](#7-인증인가-흐름)
8. [주요 비즈니스 로직](#8-주요-비즈니스-로직)
9. [개발 환경 구성](#9-개발-환경-구성)
10. [전체 디렉토리 구조](#10-전체-디렉토리-구조)

---

## 1. 시스템 개요

### 1.1 목적

**AetherPMO(Project Management System)**는 대한민국 공공기관 SI(System Integration) 사업을 대상으로 설계된 프로젝트 관리 전문 플랫폼이다. 공공 SI 사업의 특성상 복잡한 납품 체계, 다수 이해관계자, 엄격한 품질 요구 사항, 정부 조달 기준 준수 등이 필요하며, 이를 체계적으로 관리하기 위한 통합 솔루션을 제공한다.

주요 목표는 다음과 같다:

- **가시성 확보**: 프로젝트 진척률, 인력 투입 현황, 이슈 현황을 실시간으로 파악
- **산출물 관리**: 공공 SI 요구 산출물(제안서, 분석서, 설계서, 테스트 결과서 등)의 생명주기 추적
- **리스크 조기 감지**: 일정 지연, 품질 문제, 인력 이탈 등을 자동으로 감지하고 알림
- **공수 관리**: 투입 인력별 공수 계산 및 근태 관리
- **감사 추적**: 모든 변경 이력을 기록하여 감리/감사 대응

### 1.2 대상 사용자

| 역할 | 코드 | 설명 | 주요 기능 |
|------|------|------|-----------|
| 시스템 관리자 | `ADMIN` | 전체 시스템을 관리하는 최고 권한자 | 회사/사용자 관리, 시스템 설정, 전체 프로젝트 조회 |
| 프로젝트 관리자 | `PM` | 개별 프로젝트를 담당하는 책임자 | 프로젝트 생성/수정, 인력 배정, 업무 계획 수립, 진척 관리 |
| 실무자 | `MEMBER` | 프로젝트에 투입된 개발자/분석가/설계자 등 | 업무 진척 입력, 산출물 등록, 이슈 보고, 공수 입력 |
| PMO/QA | `PMO` | 품질 보증 및 프로젝트 관리 지원 조직 | 전체 프로젝트 모니터링, 품질 검토, 보고서 생성 |

> 참고: `PMO` 역할은 `MEMBER` 권한을 상속하되, 추가적으로 교차 프로젝트 조회 권한을 보유한다.

### 1.3 주요 기능 요약

#### 프로젝트 관리
- 프로젝트 CRUD (생성, 조회, 수정, 삭제)
- 프로젝트 상태 관리: `계획중 → 진행중 → 완료 → 종료`
- 예산, 기간, 계약 정보 관리
- 발주처 / 수행사 / 협력사 정보 연계

#### 업무(Task) 관리
- 무제한 계층 트리 구조 (WBS 지원)
- 업무별 담당자, 기간, 우선순위 설정
- 진척률 자동 계산 (하위 업무 가중 평균)
- Gantt 차트 연동

#### 산출물(Deliverable) 관리
- 산출물 등록/버전 관리
- 검토/승인 워크플로우
- 파일 첨부 및 이력 관리
- 공공 표준 산출물 템플릿 지원

#### 이슈/리스크 관리
- 이슈 등록 및 추적
- 리스크 매트릭스 (발생 가능성 × 영향도)
- 담당자 지정 및 에스컬레이션
- 이슈-업무 연계

#### 인력(Member) 관리
- 프로젝트별 인력 투입 계획
- 역할/직급/투입률 설정
- 회사별 인력 관리 (원청/협력사 구분)

#### 근태/공수 관리
- 일별/주별 공수 입력
- 연차/휴가 관리
- 공수 집계 및 보고서 생성

#### 알림 및 활동 이력
- 실시간 알림 (마감 임박, 이슈 발생, 상태 변경)
- 전체 활동 이력 감사 로그
- 이메일/인앱 알림 지원

---

## 2. 전체 시스템 구성도

### 2.1 시스템 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           클라이언트 영역                                │
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                      Vue 3 SPA (Vite)                           │   │
│   │                                                                 │   │
│   │  ┌───────────┐  ┌───────────┐  ┌──────────┐  ┌─────────────┐  │   │
│   │  │ Vue Router│  │   Pinia   │  │  Axios   │  │  TypeScript │  │   │
│   │  │  (라우팅) │  │  (상태관리)│  │ (HTTP)   │  │  (타입안전) │  │   │
│   │  └───────────┘  └───────────┘  └──────────┘  └─────────────┘  │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                               │  HTTPS (REST API)                       │
└───────────────────────────────┼─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          서버 영역 (Docker)                              │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              Spring Boot 3.x API Server (Port: 8080)            │   │
│  │                                                                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │   │
│  │  │Spring Security│  │  JWT Filter  │  │  Exception Handler   │  │   │
│  │  │  (인증/인가)  │  │ (토큰 검증)  │  │   (전역 예외 처리)   │  │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘  │   │
│  │                           │                                     │   │
│  │  ┌────────────────────────▼────────────────────────────────┐   │   │
│  │  │                   도메인 레이어                           │   │   │
│  │  │  auth │ user │ company │ project │ task │ deliverable   │   │   │
│  │  │  issue │ member │ timeoff │ notification │ activity     │   │   │
│  │  └────────────────────────┬────────────────────────────────┘   │   │
│  │                           │                                     │   │
│  │  ┌────────────────────────▼────────────────────────────────┐   │   │
│  │  │              Spring Data JPA / Hibernate                 │   │   │
│  │  └────────────────────────┬────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│  ┌───────────────────────────▼─────────────────────────────────────┐   │
│  │              PostgreSQL 15 (Port: 5432)                         │   │
│  │                                                                 │   │
│  │   projects │ tasks │ deliverables │ issues │ members            │   │
│  │   users │ companies │ timeoffs │ notifications │ activity_logs  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 모듈 구성도

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         AetherPMO 모듈 구성                                 │
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │                     공통 모듈 (Common)                           │   │
│   │   ApiResponse │ BaseEntity │ GlobalExceptionHandler │ Constants  │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│   ┌────────────────────┐     ┌────────────────────┐                     │
│   │   인증/인가 모듈    │     │   사용자/회사 모듈  │                     │
│   │  auth / security   │────▶│   user / company   │                     │
│   └────────────────────┘     └────────────────────┘                     │
│              │                         │                                 │
│              ▼                         ▼                                 │
│   ┌─────────────────────────────────────────────────────────────┐       │
│   │                   프로젝트 핵심 모듈                          │       │
│   │                                                             │       │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │       │
│   │  │ project  │  │   task   │  │deliverable│  │  issue   │  │       │
│   │  │ (프로젝트)│  │(업무/WBS)│  │ (산출물) │  │ (이슈)   │  │       │
│   │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │       │
│   │                                                             │       │
│   │  ┌──────────┐  ┌──────────┐                               │       │
│   │  │  member  │  │ timeoff  │                               │       │
│   │  │ (인력)   │  │ (근태)   │                               │       │
│   │  └──────────┘  └──────────┘                               │       │
│   └─────────────────────────────────────────────────────────────┘       │
│              │                                                           │
│              ▼                                                           │
│   ┌──────────────────────────────────────────┐                          │
│   │          지원 모듈 (Support)              │                          │
│   │  notification (알림) │ activity (이력)   │                          │
│   └──────────────────────────────────────────┘                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.3 레이어 아키텍처 (도메인 내부)

각 도메인은 다음과 같은 계층 구조를 따른다:

```
Controller (REST API 진입점)
    │
    ▼
Service (비즈니스 로직)
    │
    ▼
Repository (데이터 접근, Spring Data JPA)
    │
    ▼
Entity (JPA 엔티티, 테이블 매핑)
```

---

## 3. 기술 스택 상세

### 3.1 백엔드

| 기술 | 버전 | 역할 | 선택 이유 |
|------|------|------|-----------|
| Java | 17 LTS | 언어 | LTS 버전으로 안정성 보장, Record/Sealed class 등 최신 문법 활용, Spring Boot 3.x 필수 조건 |
| Spring Boot | 3.2.x | 애플리케이션 프레임워크 | 공공 SI 표준 기술 스택, 자동 설정으로 빠른 개발, 방대한 레퍼런스 |
| Spring Security | 6.x | 인증/인가 | JWT 기반 Stateless 인증 구현, 역할 기반 접근 제어(RBAC) |
| Spring Data JPA | 3.2.x | ORM/데이터 접근 | 반복적 CRUD 코드 제거, 쿼리 메서드 자동 생성, JPQL/네이티브 쿼리 지원 |
| Hibernate | 6.x | JPA 구현체 | Spring Data JPA 기본 구현체, 검증된 ORM 성능 |
| QueryDSL | 5.x | 동적 쿼리 | 타입 안전 동적 쿼리 생성, 복잡한 검색 조건 처리 |
| JWT (jjwt) | 0.12.x | 토큰 기반 인증 | Stateless 서버, 수평 확장 용이 |
| Maven | 3.9.x | 빌드 도구 | 공공 SI 환경에서 Maven 표준화 요구 많음, 의존성 관리 안정성 |
| Lombok | 1.18.x | 코드 생성 | Getter/Setter/Builder 보일러플레이트 코드 제거 |
| MapStruct | 1.5.x | DTO 매핑 | Entity-DTO 변환 코드 자동 생성, 타입 안전 |

### 3.2 프론트엔드

| 기술 | 버전 | 역할 | 선택 이유 |
|------|------|------|-----------|
| Vue 3 | 3.4.x | UI 프레임워크 | Composition API로 로직 재사용성 향상, TypeScript 친화적 |
| TypeScript | 5.x | 언어 | 대규모 팀 개발 시 타입 안전성 확보, IDE 지원 강력 |
| Pinia | 2.x | 상태 관리 | Vue 3 공식 권장 상태 관리 라이브러리, TypeScript 완전 지원 |
| Vue Router | 4.x | 라우팅 | Vue 3 공식 라우터, 중첩 라우트/가드 지원 |
| Vite | 5.x | 빌드 도구 | 극히 빠른 HMR, ES Module 기반, 개발 생산성 향상 |
| Axios | 1.x | HTTP 클라이언트 | 인터셉터 기반 JWT 자동 첨부, 공통 에러 처리 |
| Element Plus | 2.x | UI 컴포넌트 | 기업용 컴포넌트 라이브러리, 한국어 지원 |
| ECharts | 5.x | 차트 | Gantt 차트, 진척률 차트, 대시보드 시각화 |
| Day.js | 1.x | 날짜 처리 | 경량 날짜 라이브러리, Moment.js 대체 |

### 3.3 데이터베이스

| 기술 | 버전 | 역할 | 선택 이유 |
|------|------|------|-----------|
| PostgreSQL | 15 | 주 데이터베이스 | JSON 컬럼 지원, 트리 구조 쿼리(ltree), 공공 오픈소스 정책 부합 |
| HikariCP | 5.x | 커넥션 풀 | Spring Boot 기본 커넥션 풀, 고성능 |

### 3.4 인프라/운영

| 기술 | 버전 | 역할 | 선택 이유 |
|------|------|------|-----------|
| Docker | 24.x | 컨테이너화 | 환경 일관성 보장, 개발/운영 환경 통일 |
| Docker Compose | 2.x | 로컬 개발 오케스트레이션 | 멀티 컨테이너 로컬 환경 구성 간소화 |
| Nginx | 1.25.x | 리버스 프록시 | 정적 파일 서빙, SSL 종료, API 프록시 |

---

## 4. 백엔드 모듈 구조

### 4.1 패키지 구조 개요

```
com.aetherpmo
├── config/
├── common/
├── security/
└── domain/
    ├── auth/
    ├── user/
    ├── company/
    ├── project/
    ├── task/
    ├── deliverable/
    ├── issue/
    ├── member/
    ├── timeoff/
    ├── notification/
    └── activity/
```

### 4.2 config/ - 설정 패키지

Spring Boot 애플리케이션 전역 설정을 담당한다.

| 클래스 | 설명 |
|--------|------|
| `SecurityConfig` | Spring Security FilterChain 설정, 공개/보호 엔드포인트 구분, CSRF 비활성화, Stateless 세션 설정 |
| `CorsConfig` | CORS 허용 도메인, 메서드, 헤더 설정. 개발(localhost:5173)/운영 환경 분리 |
| `JpaConfig` | JPA AuditingEntityListener 활성화, `@EnableJpaAuditing` |
| `WebMvcConfig` | 정적 리소스 경로, 인터셉터 등록 |
| `SwaggerConfig` | SpringDoc OpenAPI 3.0 문서화 설정, JWT Bearer 인증 스키마 등록 |
| `AppProperties` | `@ConfigurationProperties`로 `application.yml` 바인딩 (jwt.secret, file.upload-dir 등) |

### 4.3 common/ - 공통 패키지

시스템 전반에서 재사용되는 공통 컴포넌트를 정의한다.

| 클래스/인터페이스 | 설명 |
|------------------|------|
| `ApiResponse<T>` | 표준 API 응답 래퍼. `success`, `message`, `data`, `timestamp` 필드 포함 |
| `PageResponse<T>` | 페이지네이션 응답 래퍼. `content`, `totalElements`, `totalPages`, `currentPage` 포함 |
| `BaseEntity` | `@MappedSuperclass`. `createdAt`, `updatedAt`, `createdBy`, `updatedBy` 공통 감사 필드 |
| `GlobalExceptionHandler` | `@RestControllerAdvice`. 비즈니스 예외, 유효성 검증 실패, JWT 오류 등 전역 처리 |
| `BusinessException` | 비즈니스 로직 예외 기반 클래스. `ErrorCode` enum 포함 |
| `ErrorCode` | 에러 코드 enum. HTTP 상태 코드 + 한국어 메시지 매핑 (`PROJECT_NOT_FOUND`, `UNAUTHORIZED_ACCESS` 등) |
| `ErrorResponse` | 에러 응답 DTO. `code`, `message`, `details` 필드 |
| `Constants` | 전역 상수 정의 (페이지 크기, 날짜 포맷, 파일 크기 제한 등) |
| `enums/` | 시스템 공통 Enum: `Status`, `Priority`, `ProjectStatus`, `TaskStatus`, `IssueStatus` |

### 4.4 security/ - 보안 패키지

JWT 기반 인증 처리 컴포넌트를 담당한다.

| 클래스 | 설명 |
|--------|------|
| `JwtTokenProvider` | JWT 생성(`generateToken`), 검증(`validateToken`), 클레임 추출(`getUserId`, `getRole`) |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` 확장. HTTP 요청에서 JWT 추출 후 SecurityContext 설정 |
| `CustomUserDetailsService` | `UserDetailsService` 구현. DB에서 사용자 정보 로드 |
| `CustomUserDetails` | `UserDetails` 구현. 사용자 ID, 이메일, 역할 포함 |
| `SecurityUtil` | SecurityContext에서 현재 인증 사용자 정보 추출 유틸리티 |
| `JwtAccessDeniedHandler` | 권한 없는 접근 시 403 응답 처리 |
| `JwtAuthenticationEntryPoint` | 인증 실패 시 401 응답 처리 |

### 4.5 domain/auth/ - 인증 도메인

| 클래스 | 설명 |
|--------|------|
| `AuthController` | `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/refresh` |
| `AuthService` | 로그인 검증, JWT 발급, RefreshToken 관리, 로그아웃 처리 |
| `LoginRequest` | 로그인 요청 DTO (`email`, `password`) |
| `LoginResponse` | 로그인 응답 DTO (`accessToken`, `refreshToken`, `user`) |
| `RefreshTokenRequest` | 토큰 재발급 요청 DTO |
| `RefreshToken` | RefreshToken 엔티티 (DB 또는 Redis 저장) |
| `RefreshTokenRepository` | RefreshToken JPA 레포지토리 |

### 4.6 domain/user/ - 사용자 도메인

| 클래스 | 설명 |
|--------|------|
| `UserController` | 사용자 CRUD API. `GET /api/v1/users`, `POST /api/v1/users`, `PUT /api/v1/users/{id}` |
| `UserService` | 사용자 생성/수정/삭제, 비밀번호 변경, 역할 변경 비즈니스 로직 |
| `UserRepository` | 이메일/역할/회사별 사용자 조회 쿼리 메서드 |
| `User` | 사용자 JPA 엔티티. `email`, `password`, `name`, `phone`, `role`, `company`, `enabled` |
| `UserRole` | 역할 Enum: `ADMIN`, `PM`, `MEMBER`, `PMO` |
| `CreateUserRequest` | 사용자 생성 요청 DTO |
| `UpdateUserRequest` | 사용자 수정 요청 DTO |
| `UserResponse` | 사용자 응답 DTO (비밀번호 제외) |
| `UserMapper` | MapStruct 기반 Entity-DTO 변환 |

### 4.7 domain/company/ - 회사 도메인

| 클래스 | 설명 |
|--------|------|
| `CompanyController` | 회사 CRUD API. `GET /api/v1/companies`, `POST /api/v1/companies` |
| `CompanyService` | 회사 등록, 수정, 사용자 연계 비즈니스 로직 |
| `CompanyRepository` | 회사명/사업자번호 검색 쿼리 |
| `Company` | 회사 JPA 엔티티. `name`, `businessNumber`, `ceo`, `address`, `type(발주처/수행사/협력사)` |
| `CompanyType` | Enum: `CLIENT(발주처)`, `CONTRACTOR(수행사)`, `SUBCONTRACTOR(협력사)` |
| `CompanyResponse` | 회사 응답 DTO |

### 4.8 domain/project/ - 프로젝트 도메인

| 클래스 | 설명 |
|--------|------|
| `ProjectController` | 프로젝트 CRUD 및 상태 변경 API |
| `ProjectService` | 프로젝트 생성, 상태 전이 검증, 진척률 집계, PM 권한 검증 |
| `ProjectRepository` | 상태/기간/PM별 프로젝트 조회. QueryDSL 동적 쿼리 포함 |
| `Project` | 프로젝트 JPA 엔티티. `name`, `code`, `status`, `startDate`, `endDate`, `budget`, `clientCompany`, `contractorCompany`, `pm` |
| `ProjectStatus` | Enum: `PLANNING(계획중)`, `IN_PROGRESS(진행중)`, `COMPLETED(완료)`, `CLOSED(종료)` |
| `ProjectQueryRepository` | QueryDSL 기반 복잡한 프로젝트 검색 쿼리 |
| `CreateProjectRequest` | 프로젝트 생성 요청 DTO |
| `UpdateProjectRequest` | 프로젝트 수정 요청 DTO |
| `ProjectResponse` | 프로젝트 응답 DTO (진척률 포함) |
| `ProjectSummaryResponse` | 대시보드용 프로젝트 요약 DTO |

### 4.9 domain/task/ - 업무 도메인 (핵심)

업무는 트리 구조(WBS)를 지원하는 가장 복잡한 도메인이다.

| 클래스 | 설명 |
|--------|------|
| `TaskController` | 업무 CRUD, 트리 조회, 진척률 업데이트 API |
| `TaskService` | 트리 구조 업무 관리, 진척률 자동 계산, 순환 참조 방지, 하위 업무 전파 |
| `TaskRepository` | 프로젝트/부모별 업무 조회, 루트 업무 조회 |
| `Task` | 업무 JPA 엔티티. `title`, `description`, `status`, `priority`, `startDate`, `endDate`, `progress`, `weight`, `parent`, `children`, `assignee`, `project` |
| `TaskStatus` | Enum: `TODO(예정)`, `IN_PROGRESS(진행중)`, `REVIEW(검토중)`, `DONE(완료)`, `ON_HOLD(보류)` |
| `TaskPriority` | Enum: `CRITICAL(긴급)`, `HIGH(높음)`, `MEDIUM(보통)`, `LOW(낮음)` |
| `TaskTreeResponse` | 트리 구조 응답 DTO (재귀 children 포함) |
| `ProgressUpdateRequest` | 진척률 업데이트 요청 DTO |
| `TaskQueryRepository` | QueryDSL 기반 업무 검색 (담당자/상태/기간 필터) |

### 4.10 domain/deliverable/ - 산출물 도메인

| 클래스 | 설명 |
|--------|------|
| `DeliverableController` | 산출물 CRUD, 승인/반려 API, 파일 업로드/다운로드 |
| `DeliverableService` | 산출물 등록, 버전 관리, 검토/승인 워크플로우 처리 |
| `DeliverableRepository` | 프로젝트/업무/상태별 산출물 조회 |
| `Deliverable` | 산출물 엔티티. `title`, `type`, `status`, `version`, `filePath`, `fileSize`, `task`, `project`, `reviewer`, `approver` |
| `DeliverableStatus` | Enum: `DRAFT(작성중)`, `REVIEW(검토중)`, `APPROVED(승인)`, `REJECTED(반려)` |
| `DeliverableType` | Enum: `ANALYSIS(분석)`, `DESIGN(설계)`, `CODE(소스코드)`, `TEST(테스트)`, `MANUAL(매뉴얼)` 등 |
| `FileStorageService` | 로컬 스토리지 파일 저장/조회/삭제 서비스 |
| `ApprovalRequest` | 승인/반려 요청 DTO (`comment` 포함) |

### 4.11 domain/issue/ - 이슈/리스크 도메인

| 클래스 | 설명 |
|--------|------|
| `IssueController` | 이슈 CRUD, 상태 변경, 에스컬레이션 API |
| `IssueService` | 이슈 등록, 담당자 지정, 마감 기한 알림 연동 |
| `IssueRepository` | 심각도/상태/프로젝트별 이슈 조회 |
| `Issue` | 이슈 엔티티. `title`, `description`, `type(이슈/리스크)`, `severity`, `status`, `dueDate`, `reporter`, `assignee`, `task`, `project` |
| `IssueType` | Enum: `ISSUE(이슈)`, `RISK(리스크)`, `DEFECT(결함)`, `CHANGE(변경요청)` |
| `IssueSeverity` | Enum: `CRITICAL(치명적)`, `HIGH(높음)`, `MEDIUM(보통)`, `LOW(낮음)` |
| `IssueStatus` | Enum: `OPEN(오픈)`, `IN_PROGRESS(처리중)`, `RESOLVED(해결)`, `CLOSED(종료)` |
| `RiskMatrix` | 리스크 가능성 × 영향도 계산 유틸리티 클래스 |

### 4.12 domain/member/ - 프로젝트 인력 도메인

| 클래스 | 설명 |
|--------|------|
| `MemberController` | 프로젝트 인력 배정/해제/수정 API |
| `MemberService` | 인력 투입률 검증, 중복 배정 방지, 회사별 인력 집계 |
| `MemberRepository` | 프로젝트/사용자/회사별 인력 조회 |
| `ProjectMember` | 프로젝트-사용자 매핑 엔티티. `role(역할명)`, `joinDate`, `leaveDate`, `inputRate(투입률%)`, `company` |
| `MemberRole` | 업무 역할 Enum: `PM`, `PL`, `DEVELOPER`, `DESIGNER`, `ANALYST`, `QA`, `DBA` |
| `AssignMemberRequest` | 인력 배정 요청 DTO |
| `MemberResponse` | 인력 응답 DTO (사용자 정보 포함) |

### 4.13 domain/timeoff/ - 근태/공수 도메인

| 클래스 | 설명 |
|--------|------|
| `TimeoffController` | 공수 입력/수정, 휴가 신청/승인 API |
| `TimeoffService` | 공수 유효성 검증(일 8시간 초과 방지), 월별 집계, 승인 처리 |
| `TimeoffRepository` | 사용자/프로젝트/기간별 공수 조회 |
| `WorkLog` | 공수 기록 엔티티. `date`, `hours`, `description`, `task`, `user`, `project`, `approved` |
| `Leave` | 휴가 엔티티. `type(연차/반차/기타)`, `startDate`, `endDate`, `reason`, `status` |
| `LeaveType` | Enum: `ANNUAL(연차)`, `HALF(반차)`, `SICK(병가)`, `OTHER(기타)` |
| `WorkLogSummary` | 기간별 공수 집계 응답 DTO |

### 4.14 domain/notification/ - 알림 도메인

| 클래스 | 설명 |
|--------|------|
| `NotificationController` | 알림 목록 조회, 읽음 처리 API |
| `NotificationService` | 알림 생성 (다른 도메인 서비스에서 호출), 푸시/이메일 발송 |
| `NotificationRepository` | 수신자/읽음 여부별 알림 조회 |
| `Notification` | 알림 엔티티. `type`, `title`, `message`, `recipient`, `referenceId`, `referenceType`, `isRead` |
| `NotificationType` | Enum: `TASK_ASSIGNED`, `ISSUE_OPENED`, `DELIVERABLE_APPROVED`, `PROJECT_DEADLINE`, `MENTION` |
| `NotificationEvent` | Spring 이벤트 기반 비동기 알림 처리 |

### 4.15 domain/activity/ - 활동 이력 도메인

| 클래스 | 설명 |
|--------|------|
| `ActivityController` | 프로젝트/엔티티별 활동 이력 조회 API |
| `ActivityService` | 활동 이력 기록 (AOP 또는 직접 호출) |
| `ActivityRepository` | 프로젝트/엔티티/사용자별 이력 조회 |
| `ActivityLog` | 활동 이력 엔티티. `action`, `entityType`, `entityId`, `projectId`, `actor`, `before`, `after` (JSON) |
| `ActivityAction` | Enum: `CREATE`, `UPDATE`, `DELETE`, `STATUS_CHANGE`, `ASSIGN`, `APPROVE` |
| `ActivityAspect` | `@Around` AOP로 서비스 메서드 자동 감사 로그 기록 |

---

## 5. 프론트엔드 모듈 구조

### 5.1 디렉토리 구조 개요

```
src/
├── assets/              # 정적 자원
├── components/          # 재사용 가능한 UI 컴포넌트
│   ├── common/          # 공통 컴포넌트
│   ├── project/         # 프로젝트 컴포넌트
│   ├── task/            # 업무 컴포넌트
│   ├── deliverable/     # 산출물 컴포넌트
│   ├── issue/           # 이슈 컴포넌트
│   ├── member/          # 인력 컴포넌트
│   └── dashboard/       # 대시보드 컴포넌트
├── views/               # 페이지 뷰 컴포넌트
├── router/              # Vue Router 설정
├── stores/              # Pinia 상태 관리
├── api/                 # Axios API 호출 함수
├── types/               # TypeScript 타입 정의
└── utils/               # 유틸리티 함수
```

### 5.2 components/common/ - 공통 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `AppHeader.vue` | 상단 네비게이션 바. 사용자 정보, 알림 뱃지, 로그아웃 |
| `AppSidebar.vue` | 좌측 사이드바 메뉴. 권한별 메뉴 노출 |
| `AppBreadcrumb.vue` | 현재 위치 표시 경로 |
| `BaseTable.vue` | 공통 테이블 컴포넌트. 정렬/페이지네이션 포함 |
| `BaseModal.vue` | 공통 모달 다이얼로그 |
| `BaseForm.vue` | 공통 폼 컴포넌트. 유효성 검증 통합 |
| `StatusBadge.vue` | 상태 표시 뱃지 (색상 자동 매핑) |
| `PriorityBadge.vue` | 우선순위 표시 뱃지 |
| `UserAvatar.vue` | 사용자 아바타 (이니셜/이미지) |
| `FileUpload.vue` | 파일 업로드 드래그앤드롭 컴포넌트 |
| `DateRangePicker.vue` | 날짜 범위 선택기 |
| `ConfirmDialog.vue` | 삭제/중요 작업 확인 다이얼로그 |
| `EmptyState.vue` | 데이터 없음 상태 표시 |
| `LoadingSpinner.vue` | 로딩 인디케이터 |
| `NotificationPanel.vue` | 알림 패널 슬라이드오버 |

### 5.3 components/project/ - 프로젝트 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `ProjectCard.vue` | 프로젝트 목록 카드 뷰 |
| `ProjectStatusChip.vue` | 프로젝트 상태 칩 |
| `ProjectProgressBar.vue` | 프로젝트 전체 진척률 프로그레스 바 |
| `ProjectInfoPanel.vue` | 프로젝트 기본 정보 패널 |
| `ProjectCreateForm.vue` | 프로젝트 생성 폼 |
| `ProjectEditForm.vue` | 프로젝트 수정 폼 |
| `ProjectSummaryWidget.vue` | 대시보드용 프로젝트 요약 위젯 |

### 5.4 components/task/ - 업무 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `TaskTree.vue` | 업무 트리 구조 렌더링 (재귀 컴포넌트) |
| `TaskTreeNode.vue` | 트리 개별 노드. 드래그앤드롭 지원 |
| `TaskCard.vue` | 업무 카드 뷰 |
| `TaskDetailPanel.vue` | 업무 상세 정보 사이드 패널 |
| `TaskCreateForm.vue` | 업무 생성 폼 |
| `TaskProgressSlider.vue` | 진척률 입력 슬라이더 |
| `GanttChart.vue` | ECharts 기반 Gantt 차트 |
| `TaskStatusSelect.vue` | 업무 상태 변경 드롭다운 |

### 5.5 components/deliverable/ - 산출물 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `DeliverableList.vue` | 산출물 목록 테이블 |
| `DeliverableCard.vue` | 산출물 카드 뷰 |
| `DeliverableUploadForm.vue` | 산출물 업로드 폼 |
| `ApprovalWorkflow.vue` | 검토/승인 워크플로우 시각화 |
| `VersionHistory.vue` | 버전 이력 타임라인 |

### 5.6 components/issue/ - 이슈 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `IssueList.vue` | 이슈 목록 (필터링/정렬 포함) |
| `IssueCard.vue` | 이슈 카드 뷰 |
| `IssueCreateForm.vue` | 이슈 생성 폼 |
| `RiskMatrix.vue` | 리스크 매트릭스 시각화 (2D 그리드) |
| `IssueSeverityBadge.vue` | 이슈 심각도 뱃지 |

### 5.7 components/member/ - 인력 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `MemberList.vue` | 프로젝트 인력 목록 |
| `MemberCard.vue` | 인력 카드 (역할, 투입률 표시) |
| `AssignMemberForm.vue` | 인력 배정 폼 |
| `InputRateChart.vue` | 인력별 투입률 차트 |
| `WorkLogCalendar.vue` | 공수 입력 캘린더 뷰 |

### 5.8 components/dashboard/ - 대시보드 컴포넌트

| 컴포넌트 | 설명 |
|----------|------|
| `ProjectOverviewWidget.vue` | 전체 프로젝트 현황 위젯 |
| `TaskProgressWidget.vue` | 업무 진척률 현황 위젯 |
| `IssueStatusWidget.vue` | 이슈 상태 현황 위젯 |
| `UpcomingDeadlineWidget.vue` | 마감 임박 알림 위젯 |
| `TeamWorkloadWidget.vue` | 팀원 업무 부하 차트 위젯 |
| `ActivityFeedWidget.vue` | 최근 활동 피드 위젯 |

### 5.9 stores/ - Pinia 상태 관리

| 스토어 | 파일 | 설명 |
|--------|------|------|
| 인증 스토어 | `auth.store.ts` | 로그인 상태, 사용자 정보, JWT 토큰 관리 |
| 프로젝트 스토어 | `project.store.ts` | 현재 선택 프로젝트, 프로젝트 목록 캐시 |
| 업무 스토어 | `task.store.ts` | 업무 트리 데이터, 선택된 업무 |
| 알림 스토어 | `notification.store.ts` | 읽지 않은 알림 목록, 실시간 카운트 |
| UI 스토어 | `ui.store.ts` | 사이드바 상태, 테마, 로딩 상태 |

### 5.10 api/ - API 호출 모듈

| 파일 | 설명 |
|------|------|
| `axios.instance.ts` | Axios 인스턴스. 기본 URL, 타임아웃, 인터셉터(JWT 자동 첨부, 401 재발급) 설정 |
| `auth.api.ts` | 로그인/로그아웃/토큰 재발급 API 함수 |
| `user.api.ts` | 사용자 CRUD API 함수 |
| `company.api.ts` | 회사 CRUD API 함수 |
| `project.api.ts` | 프로젝트 CRUD/상태 변경 API 함수 |
| `task.api.ts` | 업무 트리 조회/CRUD/진척률 API 함수 |
| `deliverable.api.ts` | 산출물 업로드/승인/다운로드 API 함수 |
| `issue.api.ts` | 이슈 CRUD/상태 변경 API 함수 |
| `member.api.ts` | 인력 배정/조회 API 함수 |
| `timeoff.api.ts` | 공수 입력/조회/집계 API 함수 |
| `notification.api.ts` | 알림 조회/읽음 처리 API 함수 |

### 5.11 types/ - TypeScript 타입 정의

| 파일 | 설명 |
|------|------|
| `auth.types.ts` | 로그인 요청/응답, JWT 페이로드 타입 |
| `user.types.ts` | User, UserRole, UserResponse 타입 |
| `company.types.ts` | Company, CompanyType 타입 |
| `project.types.ts` | Project, ProjectStatus, ProjectResponse 타입 |
| `task.types.ts` | Task, TaskStatus, TaskTreeNode, ProgressUpdate 타입 |
| `deliverable.types.ts` | Deliverable, DeliverableStatus, ApprovalRequest 타입 |
| `issue.types.ts` | Issue, IssueType, IssueSeverity 타입 |
| `member.types.ts` | ProjectMember, MemberRole 타입 |
| `timeoff.types.ts` | WorkLog, Leave, WorkLogSummary 타입 |
| `common.types.ts` | ApiResponse, PageResponse, PaginationParams 공통 타입 |

---

## 6. 데이터베이스 구성

### 6.1 주요 테이블 목록

| 테이블명 | 설명 | 주요 컬럼 |
|----------|------|-----------|
| `users` | 시스템 사용자 | `id`, `email`, `password`, `name`, `phone`, `role`, `company_id`, `enabled` |
| `companies` | 회사 정보 | `id`, `name`, `business_number`, `ceo`, `address`, `type` |
| `projects` | 프로젝트 | `id`, `name`, `code`, `status`, `start_date`, `end_date`, `budget`, `pm_id`, `client_company_id`, `contractor_company_id` |
| `tasks` | 업무 (트리) | `id`, `title`, `status`, `priority`, `progress`, `weight`, `start_date`, `end_date`, `parent_id`, `project_id`, `assignee_id` |
| `deliverables` | 산출물 | `id`, `title`, `type`, `status`, `version`, `file_path`, `file_size`, `task_id`, `project_id`, `reviewer_id`, `approver_id` |
| `issues` | 이슈/리스크 | `id`, `title`, `type`, `severity`, `status`, `due_date`, `project_id`, `task_id`, `reporter_id`, `assignee_id` |
| `project_members` | 프로젝트 인력 | `id`, `project_id`, `user_id`, `company_id`, `role`, `join_date`, `leave_date`, `input_rate` |
| `work_logs` | 공수 기록 | `id`, `date`, `hours`, `description`, `task_id`, `user_id`, `project_id`, `approved` |
| `leaves` | 휴가/근태 | `id`, `type`, `start_date`, `end_date`, `reason`, `status`, `user_id`, `approver_id` |
| `notifications` | 알림 | `id`, `type`, `title`, `message`, `is_read`, `recipient_id`, `reference_id`, `reference_type` |
| `activity_logs` | 활동 이력 | `id`, `action`, `entity_type`, `entity_id`, `project_id`, `actor_id`, `before_data`, `after_data` |
| `refresh_tokens` | JWT 리프레시 토큰 | `id`, `token`, `user_id`, `expiry_date` |

### 6.2 테이블 관계 개요

```
companies ────────────── users (N:1, company_id)
     │
     ├─────── projects.client_company_id (N:1)
     └─────── projects.contractor_company_id (N:1)

projects ──────────────── tasks (1:N, project_id)
     │                      │
     │                      └── tasks (자기참조, parent_id, 트리구조)
     │
     ├─────── deliverables (1:N, project_id)
     ├─────── issues (1:N, project_id)
     ├─────── project_members (1:N, project_id)
     ├─────── work_logs (1:N, project_id)
     ├─────── notifications (1:N, reference)
     └─────── activity_logs (1:N, project_id)

tasks ──────────────────── deliverables (1:N, task_id)
tasks ──────────────────── issues (1:N, task_id)
tasks ──────────────────── work_logs (1:N, task_id)

users ──────────────────── project_members (1:N, user_id)
users ──────────────────── work_logs (1:N, user_id)
users ──────────────────── leaves (1:N, user_id)
users ──────────────────── notifications (1:N, recipient_id)
```

### 6.3 인덱스 전략

| 테이블 | 인덱스 컬럼 | 인덱스 유형 | 용도 |
|--------|-------------|-------------|------|
| `users` | `email` | UNIQUE | 로그인 조회 |
| `users` | `company_id` | B-TREE | 회사별 사용자 조회 |
| `projects` | `status` | B-TREE | 상태별 프로젝트 목록 |
| `projects` | `pm_id` | B-TREE | PM별 프로젝트 조회 |
| `projects` | `(start_date, end_date)` | B-TREE | 기간별 프로젝트 조회 |
| `tasks` | `project_id` | B-TREE | 프로젝트별 업무 조회 (가장 빈번) |
| `tasks` | `parent_id` | B-TREE | 자식 업무 조회 (트리 탐색) |
| `tasks` | `assignee_id` | B-TREE | 담당자별 업무 조회 |
| `tasks` | `status` | B-TREE | 상태별 업무 필터 |
| `deliverables` | `project_id` | B-TREE | 프로젝트별 산출물 |
| `deliverables` | `task_id` | B-TREE | 업무별 산출물 |
| `issues` | `(project_id, status)` | 복합 B-TREE | 프로젝트 내 상태별 이슈 |
| `work_logs` | `(user_id, date)` | 복합 B-TREE | 사용자 일별 공수 조회 |
| `work_logs` | `(project_id, date)` | 복합 B-TREE | 프로젝트 일별 공수 집계 |
| `notifications` | `(recipient_id, is_read)` | 복합 B-TREE | 미읽은 알림 조회 |
| `activity_logs` | `(project_id, created_at)` | 복합 B-TREE | 프로젝트 최근 활동 |
| `refresh_tokens` | `token` | UNIQUE | 토큰 검증 |

### 6.4 특수 데이터 타입 활용

```sql
-- activity_logs의 before/after 변경 데이터는 JSONB 타입 사용
ALTER TABLE activity_logs
    ADD COLUMN before_data JSONB,
    ADD COLUMN after_data JSONB;

-- JSONB 인덱스 (GIN)
CREATE INDEX idx_activity_logs_before_gin ON activity_logs USING GIN (before_data);
CREATE INDEX idx_activity_logs_after_gin ON activity_logs USING GIN (after_data);
```

---

## 7. 인증/인가 흐름

### 7.1 역할 기반 접근 제어 (RBAC)

| 기능 영역 | ADMIN | PM | PMO | MEMBER |
|-----------|-------|-----|-----|--------|
| 시스템 설정 | O | X | X | X |
| 회사/사용자 관리 | O | X | X | X |
| 프로젝트 생성 | O | O | X | X |
| 프로젝트 수정 | O | O(본인) | X | X |
| 전체 프로젝트 조회 | O | X | O | X |
| 업무 생성/수정 | O | O | X | O(본인) |
| 진척률 입력 | O | O | X | O(본인) |
| 산출물 등록 | O | O | X | O |
| 산출물 승인 | O | O | O | X |
| 이슈 등록 | O | O | O | O |
| 공수 입력 | O | O | O | O(본인) |

### 7.2 JWT 발급/검증 흐름

```
클라이언트                    API 서버                         DB
    │                            │                              │
    │ POST /api/v1/auth/login     │                              │
    │ {email, password}           │                              │
    │────────────────────────────▶│                              │
    │                            │ SELECT * FROM users           │
    │                            │ WHERE email = ?               │
    │                            │──────────────────────────────▶│
    │                            │                              │
    │                            │◀──────────────────────────────│
    │                            │ User 엔티티 반환              │
    │                            │                              │
    │                            │ BCrypt.matches(password)      │
    │                            │ 비밀번호 검증                  │
    │                            │                              │
    │                            │ JWT AccessToken 생성          │
    │                            │ (유효기간: 30분)              │
    │                            │                              │
    │                            │ JWT RefreshToken 생성         │
    │                            │ (유효기간: 7일)               │
    │                            │                              │
    │                            │ INSERT INTO refresh_tokens    │
    │                            │──────────────────────────────▶│
    │                            │                              │
    │◀────────────────────────────│                              │
    │ 200 OK                      │                              │
    │ {accessToken, refreshToken} │                              │
    │                            │                              │
    │ [이후 API 요청]              │                              │
    │ Authorization: Bearer {accessToken}                        │
    │────────────────────────────▶│                              │
    │                            │                              │
    │                            │ JwtAuthenticationFilter       │
    │                            │ 1. 헤더에서 토큰 추출          │
    │                            │ 2. 서명 검증                  │
    │                            │ 3. 만료 여부 확인             │
    │                            │ 4. SecurityContext 설정       │
    │                            │                              │
    │◀────────────────────────────│                              │
    │ API 응답                    │                              │
    │                            │                              │
    │ [토큰 만료 시 재발급]        │                              │
    │ POST /api/v1/auth/refresh   │                              │
    │ {refreshToken}              │                              │
    │────────────────────────────▶│                              │
    │                            │ SELECT * FROM refresh_tokens  │
    │                            │ WHERE token = ? AND expiry > NOW()
    │                            │──────────────────────────────▶│
    │                            │◀──────────────────────────────│
    │                            │ 유효한 RefreshToken 확인      │
    │                            │ 새 AccessToken 발급           │
    │◀────────────────────────────│                              │
    │ 200 OK {newAccessToken}     │                              │
```

### 7.3 JWT 토큰 구조

```
Header: {"alg": "HS256", "typ": "JWT"}

Payload:
{
  "sub": "12345",           // userId
  "email": "user@oke.com",
  "role": "PM",
  "iat": 1718400000,        // 발급 시각
  "exp": 1718401800         // 만료 시각 (30분 후)
}
```

### 7.4 Axios 인터셉터 (프론트엔드)

```typescript
// 요청 인터셉터: 모든 요청에 JWT 자동 첨부
axiosInstance.interceptors.request.use((config) => {
  const token = authStore.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 응답 인터셉터: 401 발생 시 토큰 재발급 후 원래 요청 재시도
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      await authStore.refreshToken();
      return axiosInstance(error.config);
    }
    return Promise.reject(error);
  }
);
```

---

## 8. 주요 비즈니스 로직

### 8.1 Task 진척률 자동 계산 알고리즘

업무(Task)는 트리 구조로 구성되며, 자식 업무들의 가중 평균을 부모 업무의 진척률로 자동 계산한다.

#### 알고리즘 개요

```
진척률 계산 규칙:
1. 리프 노드(자식 없는 업무): 담당자가 직접 진척률 입력 (0~100%)
2. 중간 노드(자식 있는 업무): 자식들의 가중 평균으로 자동 계산
3. 루트 노드: 최종 프로젝트 전체 진척률

가중치(weight): 각 업무의 상대적 중요도/규모. 기본값 = 1
```

#### Java 구현

```java
/**
 * 업무 진척률 재귀 계산
 * @param task 대상 업무 (children 로드 필요)
 * @return 계산된 진척률 (0.0 ~ 100.0)
 */
public double calculateProgress(Task task) {
    List<Task> children = task.getChildren();

    // 리프 노드: 입력된 진척률 그대로 반환
    if (children == null || children.isEmpty()) {
        return task.getProgress();
    }

    // 중간/루트 노드: 자식들의 가중 평균 계산
    double totalWeight = 0.0;
    double weightedProgressSum = 0.0;

    for (Task child : children) {
        double childProgress = calculateProgress(child);  // 재귀 호출
        double weight = child.getWeight() != null ? child.getWeight() : 1.0;

        weightedProgressSum += childProgress * weight;
        totalWeight += weight;
    }

    if (totalWeight == 0) return 0.0;

    double calculatedProgress = weightedProgressSum / totalWeight;

    // 계산 결과를 DB에 저장 (캐시 역할)
    task.setProgress(calculatedProgress);
    taskRepository.save(task);

    return calculatedProgress;
}
```

#### 계산 예시

```
프로젝트 A (전체 진척률: ?)
│
├── 분석 단계 [weight=2] (진척률: ?)
│   ├── 요구사항 분석 [weight=1] → 진척률: 100%
│   └── AS-IS 분석   [weight=1] → 진척률: 80%
│   분석 단계 계산: (100*1 + 80*1) / (1+1) = 90%
│
├── 설계 단계 [weight=3] (진척률: ?)
│   ├── 아키텍처 설계 [weight=2] → 진척률: 60%
│   └── DB 설계      [weight=1] → 진척률: 40%
│   설계 단계 계산: (60*2 + 40*1) / (2+1) = 53.3%
│
└── 개발 단계 [weight=5] → 진척률: 0%

프로젝트 전체: (90*2 + 53.3*3 + 0*5) / (2+3+5) = 33.99% ≈ 34%
```

#### 진척률 업데이트 전파 처리

```java
@Transactional
public void updateProgress(Long taskId, double newProgress) {
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

    // 1. 리프 노드만 직접 수정 가능
    if (!task.getChildren().isEmpty()) {
        throw new BusinessException(ErrorCode.CANNOT_UPDATE_PARENT_PROGRESS);
    }

    task.setProgress(newProgress);

    // 2. 부모 업무로 거슬러 올라가며 재계산
    propagateProgressToParent(task.getParent());
}

private void propagateProgressToParent(Task parent) {
    if (parent == null) return;  // 루트에 도달하면 종료

    // 부모의 모든 자식들로 진척률 재계산
    List<Task> siblings = taskRepository.findByParentId(parent.getId());
    double weightedSum = siblings.stream()
        .mapToDouble(t -> t.getProgress() * (t.getWeight() != null ? t.getWeight() : 1.0))
        .sum();
    double totalWeight = siblings.stream()
        .mapToDouble(t -> t.getWeight() != null ? t.getWeight() : 1.0)
        .sum();

    parent.setProgress(totalWeight > 0 ? weightedSum / totalWeight : 0);
    taskRepository.save(parent);

    // 재귀: 조부모로 전파
    propagateProgressToParent(parent.getParent());
}
```

### 8.2 상태 전이 규칙 (State Machine)

#### 프로젝트 상태 전이

```
                    ┌──────────────┐
                    │   PLANNING   │ ← 초기 상태
                    │   (계획중)   │
                    └──────┬───────┘
                           │ PM/ADMIN 승인
                           ▼
                    ┌──────────────┐
              ┌────▶│  IN_PROGRESS │
              │     │  (진행중)    │
              │     └──────┬───────┘
              │ 재개        │ 프로젝트 완성
              │            ▼
              │     ┌──────────────┐
              │     │  COMPLETED   │
              │     │  (완료)      │
              │     └──────┬───────┘
              │            │ 행정 종료 처리
              │            ▼
              │     ┌──────────────┐
              │     │    CLOSED    │
              │     │   (종료)     │
              │     └──────────────┘
              │
        (보류 상태는 별도 관리)
```

#### 업무 상태 전이

```
     ┌─────────────────────────────────────────────┐
     │                                             │
     ▼                                             │
  ┌─────┐    담당자 착수    ┌─────────────┐        │
  │ TODO│──────────────────▶│ IN_PROGRESS │        │
  │(예정)│                  │  (진행중)   │        │
  └─────┘                  └──────┬──────┘        │
     ▲                            │                │
     │                   검토 요청 │                │
     │                            ▼                │
     │                    ┌───────────┐            │
     │           반려      │  REVIEW   │            │
     │◀───────────────────│  (검토중) │            │
     │                    └─────┬─────┘            │
     │                          │ 승인              │
     │                          ▼                  │
     │                    ┌──────────┐             │
     │                    │   DONE   │             │
     │                    │  (완료)  │             │
     │                    └──────────┘             │
     │                                             │
     │   보류 처리                                  │
     └──────────────── ON_HOLD ────────────────────┘
                        (보류)
```

#### 산출물 상태 전이

```
  ┌────────┐  제출    ┌────────┐  승인    ┌──────────┐
  │ DRAFT  │─────────▶│ REVIEW │─────────▶│ APPROVED │
  │(작성중)│          │(검토중)│          │  (승인)  │
  └────────┘          └───┬────┘          └──────────┘
       ▲                  │ 반려
       │                  ▼
       │            ┌──────────┐
       └────────────│ REJECTED │
         수정 후     │  (반려)  │
         재제출      └──────────┘
```

### 8.3 공수 계산 방식

공수(Man-Month, MM) 계산은 공공 SI 사업의 과업대가 산정 기준을 따른다.

#### 기본 단위 정의

| 단위 | 기준 | 설명 |
|------|------|------|
| 1 MD (Man-Day) | 8시간 | 하루 기준 노동 시간 |
| 1 MW (Man-Week) | 40시간 = 5MD | 주 5일 기준 |
| 1 MM (Man-Month) | 160시간 = 20MD | 월 20 영업일 기준 |

#### 공수 집계 로직

```java
/**
 * 특정 기간의 사용자 공수 집계
 */
public WorkLogSummary calculateWorkload(
        Long userId, Long projectId,
        LocalDate startDate, LocalDate endDate) {

    List<WorkLog> logs = workLogRepository
        .findByUserIdAndProjectIdAndDateBetween(userId, projectId, startDate, endDate);

    double totalHours = logs.stream()
        .mapToDouble(WorkLog::getHours)
        .sum();

    // 공수 단위 변환
    double md = totalHours / 8.0;
    double mw = totalHours / 40.0;
    double mm = totalHours / 160.0;

    return WorkLogSummary.builder()
        .totalHours(totalHours)
        .manDay(Math.round(md * 100.0) / 100.0)
        .manWeek(Math.round(mw * 100.0) / 100.0)
        .manMonth(Math.round(mm * 100.0) / 100.0)
        .logCount(logs.size())
        .build();
}
```

#### 일별 공수 유효성 검증

```java
@Transactional
public WorkLog registerWorkLog(WorkLogRequest request, Long userId) {
    // 당일 등록된 공수 합계 계산
    double existingHours = workLogRepository
        .sumHoursByUserAndDate(userId, request.getDate());

    // 신규 입력 공수 포함 시 8시간 초과 검증
    if (existingHours + request.getHours() > 8.0) {
        throw new BusinessException(ErrorCode.WORK_LOG_EXCEEDS_DAILY_LIMIT,
            String.format("현재 공수 %.1f시간 + 입력 %.1f시간 > 8시간",
                existingHours, request.getHours()));
    }

    WorkLog workLog = WorkLog.builder()
        .date(request.getDate())
        .hours(request.getHours())
        .description(request.getDescription())
        .task(taskRepository.getReferenceById(request.getTaskId()))
        .user(userRepository.getReferenceById(userId))
        .project(projectRepository.getReferenceById(request.getProjectId()))
        .approved(false)
        .build();

    return workLogRepository.save(workLog);
}
```

---

## 9. 개발 환경 구성

### 9.1 Docker Compose 설정

```yaml
# docker-compose.yml
version: '3.8'

services:
  # PostgreSQL 데이터베이스
  postgres:
    image: postgres:15-alpine
    container_name: aetherpmo-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: aetherpmo
      POSTGRES_USER: aetherpmo
      POSTGRES_PASSWORD: aetherpmo1234!
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=ko_KR.UTF-8"
      TZ: Asia/Seoul
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init.sql:/docker-entrypoint-initdb.d/01_init.sql
    networks:
      - aetherpmo-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U aetherpmo -d aetherpmo"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot 백엔드 API
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: aetherpmo-backend
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/aetherpmo
      SPRING_DATASOURCE_USERNAME: aetherpmo
      SPRING_DATASOURCE_PASSWORD: aetherpmo1234!
      JWT_SECRET: aetherpmo-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256
      JWT_ACCESS_TOKEN_EXPIRY: 1800000
      JWT_REFRESH_TOKEN_EXPIRY: 604800000
      FILE_UPLOAD_DIR: /app/uploads
      TZ: Asia/Seoul
    ports:
      - "8080:8080"
    volumes:
      - upload_data:/app/uploads
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - aetherpmo-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # Vue 3 프론트엔드 (개발 모드)
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    container_name: aetherpmo-frontend
    restart: unless-stopped
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api/v1
      TZ: Asia/Seoul
    ports:
      - "5173:5173"
    volumes:
      - ./frontend:/app
      - /app/node_modules
    depends_on:
      - backend
    networks:
      - aetherpmo-network

  # Nginx 리버스 프록시 (선택적 - 통합 테스트용)
  nginx:
    image: nginx:1.25-alpine
    container_name: aetherpmo-nginx
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - backend
      - frontend
    networks:
      - aetherpmo-network
    profiles:
      - prod

volumes:
  postgres_data:
    driver: local
  upload_data:
    driver: local

networks:
  aetherpmo-network:
    driver: bridge
```

### 9.2 백엔드 Dockerfile

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /build/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown appuser:appgroup /app/uploads
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", "app.jar"]
```

### 9.3 프론트엔드 개발용 Dockerfile

```dockerfile
# frontend/Dockerfile.dev
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

### 9.4 application.yml 설정

```yaml
# backend/src/main/resources/application.yml
spring:
  application:
    name: aetherpmo
  datasource:
    url: jdbc:postgresql://localhost:5432/aetherpmo
    username: aetherpmo
    password: aetherpmo1234!
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Seoul
    default-property-inclusion: non_null

server:
  port: 8080
  servlet:
    context-path: /api/v1

jwt:
  secret: ${JWT_SECRET:aetherpmo-local-dev-secret-key-256bits-placeholder}
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

file:
  upload-dir: ${FILE_UPLOAD_DIR:./uploads}
  max-size: 52428800

logging:
  level:
    com.aetherpmo: DEBUG
    org.springframework.security: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

---
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    show-sql: false
```

### 9.5 로컬 개발 시작 명령

```bash
# 전체 스택 실행
docker-compose up -d

# 백엔드만 실행 (DB는 Docker, 백엔드는 로컬 IDE)
docker-compose up -d postgres
./mvnw spring-boot:run -pl backend

# 프론트엔드만 실행
cd frontend && npm run dev

# 로그 확인
docker-compose logs -f backend
docker-compose logs -f postgres

# 서비스 재시작
docker-compose restart backend

# 전체 정리
docker-compose down -v
```

---

## 10. 전체 디렉토리 구조

### 10.1 프로젝트 루트 구조

```
aetherpmo/
├── backend/                    # Spring Boot 백엔드
├── frontend/                   # Vue 3 프론트엔드
├── docs/                       # 프로젝트 문서
│   ├── 01_system_architecture.md
│   ├── 02_api_specification.md
│   ├── 03_database_schema.md
│   └── 04_deployment_guide.md
├── nginx/                      # Nginx 설정
│   └── nginx.conf
├── scripts/                    # 운영 스크립트
│   ├── init.sql                # DB 초기화 SQL
│   └── backup.sh               # 백업 스크립트
├── docker-compose.yml          # 로컬 개발 환경
├── docker-compose.prod.yml     # 운영 환경
└── README.md
```

### 10.2 백엔드 전체 디렉토리 구조

```
backend/
├── Dockerfile
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── aetherpmo/
    │   │           ├── AetherPmoApplication.java
    │   │           ├── config/
    │   │           │   ├── SecurityConfig.java
    │   │           │   ├── CorsConfig.java
    │   │           │   ├── JpaConfig.java
    │   │           │   ├── WebMvcConfig.java
    │   │           │   ├── SwaggerConfig.java
    │   │           │   └── AppProperties.java
    │   │           ├── common/
    │   │           │   ├── response/
    │   │           │   │   ├── ApiResponse.java
    │   │           │   │   └── PageResponse.java
    │   │           │   ├── entity/
    │   │           │   │   └── BaseEntity.java
    │   │           │   ├── exception/
    │   │           │   │   ├── BusinessException.java
    │   │           │   │   ├── ErrorCode.java
    │   │           │   │   ├── ErrorResponse.java
    │   │           │   │   └── GlobalExceptionHandler.java
    │   │           │   ├── enums/
    │   │           │   │   ├── Status.java
    │   │           │   │   └── Priority.java
    │   │           │   └── Constants.java
    │   │           ├── security/
    │   │           │   ├── JwtTokenProvider.java
    │   │           │   ├── JwtAuthenticationFilter.java
    │   │           │   ├── CustomUserDetailsService.java
    │   │           │   ├── CustomUserDetails.java
    │   │           │   ├── SecurityUtil.java
    │   │           │   ├── JwtAccessDeniedHandler.java
    │   │           │   └── JwtAuthenticationEntryPoint.java
    │   │           └── domain/
    │   │               ├── auth/
    │   │               │   ├── controller/AuthController.java
    │   │               │   ├── service/AuthService.java
    │   │               │   ├── repository/RefreshTokenRepository.java
    │   │               │   ├── entity/RefreshToken.java
    │   │               │   └── dto/
    │   │               │       ├── LoginRequest.java
    │   │               │       ├── LoginResponse.java
    │   │               │       └── RefreshTokenRequest.java
    │   │               ├── user/
    │   │               │   ├── controller/UserController.java
    │   │               │   ├── service/UserService.java
    │   │               │   ├── repository/UserRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── User.java
    │   │               │   │   └── UserRole.java
    │   │               │   ├── dto/
    │   │               │   │   ├── CreateUserRequest.java
    │   │               │   │   ├── UpdateUserRequest.java
    │   │               │   │   └── UserResponse.java
    │   │               │   └── mapper/UserMapper.java
    │   │               ├── company/
    │   │               │   ├── controller/CompanyController.java
    │   │               │   ├── service/CompanyService.java
    │   │               │   ├── repository/CompanyRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── Company.java
    │   │               │   │   └── CompanyType.java
    │   │               │   └── dto/CompanyResponse.java
    │   │               ├── project/
    │   │               │   ├── controller/ProjectController.java
    │   │               │   ├── service/ProjectService.java
    │   │               │   ├── repository/
    │   │               │   │   ├── ProjectRepository.java
    │   │               │   │   └── ProjectQueryRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── Project.java
    │   │               │   │   └── ProjectStatus.java
    │   │               │   └── dto/
    │   │               │       ├── CreateProjectRequest.java
    │   │               │       ├── UpdateProjectRequest.java
    │   │               │       ├── ProjectResponse.java
    │   │               │       └── ProjectSummaryResponse.java
    │   │               ├── task/
    │   │               │   ├── controller/TaskController.java
    │   │               │   ├── service/TaskService.java
    │   │               │   ├── repository/
    │   │               │   │   ├── TaskRepository.java
    │   │               │   │   └── TaskQueryRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── Task.java
    │   │               │   │   ├── TaskStatus.java
    │   │               │   │   └── TaskPriority.java
    │   │               │   └── dto/
    │   │               │       ├── CreateTaskRequest.java
    │   │               │       ├── UpdateTaskRequest.java
    │   │               │       ├── TaskResponse.java
    │   │               │       ├── TaskTreeResponse.java
    │   │               │       └── ProgressUpdateRequest.java
    │   │               ├── deliverable/
    │   │               │   ├── controller/DeliverableController.java
    │   │               │   ├── service/
    │   │               │   │   ├── DeliverableService.java
    │   │               │   │   └── FileStorageService.java
    │   │               │   ├── repository/DeliverableRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── Deliverable.java
    │   │               │   │   ├── DeliverableStatus.java
    │   │               │   │   └── DeliverableType.java
    │   │               │   └── dto/
    │   │               │       ├── DeliverableResponse.java
    │   │               │       └── ApprovalRequest.java
    │   │               ├── issue/
    │   │               │   ├── controller/IssueController.java
    │   │               │   ├── service/IssueService.java
    │   │               │   ├── repository/IssueRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── Issue.java
    │   │               │   │   ├── IssueType.java
    │   │               │   │   ├── IssueSeverity.java
    │   │               │   │   └── IssueStatus.java
    │   │               │   └── dto/
    │   │               │       ├── CreateIssueRequest.java
    │   │               │       └── IssueResponse.java
    │   │               ├── member/
    │   │               │   ├── controller/MemberController.java
    │   │               │   ├── service/MemberService.java
    │   │               │   ├── repository/MemberRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── ProjectMember.java
    │   │               │   │   └── MemberRole.java
    │   │               │   └── dto/
    │   │               │       ├── AssignMemberRequest.java
    │   │               │       └── MemberResponse.java
    │   │               ├── timeoff/
    │   │               │   ├── controller/TimeoffController.java
    │   │               │   ├── service/TimeoffService.java
    │   │               │   ├── repository/
    │   │               │   │   ├── WorkLogRepository.java
    │   │               │   │   └── LeaveRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── WorkLog.java
    │   │               │   │   ├── Leave.java
    │   │               │   │   └── LeaveType.java
    │   │               │   └── dto/
    │   │               │       ├── WorkLogRequest.java
    │   │               │       ├── WorkLogResponse.java
    │   │               │       └── WorkLogSummary.java
    │   │               ├── notification/
    │   │               │   ├── controller/NotificationController.java
    │   │               │   ├── service/NotificationService.java
    │   │               │   ├── repository/NotificationRepository.java
    │   │               │   ├── entity/
    │   │               │   │   ├── Notification.java
    │   │               │   │   └── NotificationType.java
    │   │               │   └── dto/NotificationResponse.java
    │   │               └── activity/
    │   │                   ├── controller/ActivityController.java
    │   │                   ├── service/ActivityService.java
    │   │                   ├── repository/ActivityRepository.java
    │   │                   ├── entity/
    │   │                   │   ├── ActivityLog.java
    │   │                   │   └── ActivityAction.java
    │   │                   ├── dto/ActivityLogResponse.java
    │   │                   └── aspect/ActivityAspect.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-docker.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__init_schema.sql
    │           ├── V2__seed_data.sql
    │           └── V3__add_indexes.sql
    └── test/
        └── java/com/aetherpmo/
            ├── domain/
            │   ├── auth/AuthServiceTest.java
            │   ├── project/ProjectServiceTest.java
            │   └── task/
            │       ├── TaskServiceTest.java
            │       └── TaskProgressTest.java
            └── integration/
                ├── AuthIntegrationTest.java
                └── ProjectIntegrationTest.java
```

### 10.3 프론트엔드 전체 디렉토리 구조

```
frontend/
├── Dockerfile.dev
├── package.json
├── vite.config.ts
├── tsconfig.json
├── .eslintrc.cjs
├── .prettierrc
├── index.html
├── env.d.ts
├── public/
│   ├── favicon.ico
│   └── logo.png
└── src/
    ├── main.ts
    ├── App.vue
    ├── assets/
    │   ├── styles/
    │   │   ├── main.css
    │   │   ├── variables.css
    │   │   └── element-override.css
    │   ├── images/
    │   │   ├── logo.svg
    │   │   └── empty-state.svg
    │   └── fonts/
    │       └── NotoSansKR/
    ├── components/
    │   ├── common/
    │   │   ├── AppHeader.vue
    │   │   ├── AppSidebar.vue
    │   │   ├── AppBreadcrumb.vue
    │   │   ├── BaseTable.vue
    │   │   ├── BaseModal.vue
    │   │   ├── BaseForm.vue
    │   │   ├── StatusBadge.vue
    │   │   ├── PriorityBadge.vue
    │   │   ├── UserAvatar.vue
    │   │   ├── FileUpload.vue
    │   │   ├── DateRangePicker.vue
    │   │   ├── ConfirmDialog.vue
    │   │   ├── EmptyState.vue
    │   │   ├── LoadingSpinner.vue
    │   │   └── NotificationPanel.vue
    │   ├── project/
    │   │   ├── ProjectCard.vue
    │   │   ├── ProjectStatusChip.vue
    │   │   ├── ProjectProgressBar.vue
    │   │   ├── ProjectInfoPanel.vue
    │   │   ├── ProjectCreateForm.vue
    │   │   ├── ProjectEditForm.vue
    │   │   └── ProjectSummaryWidget.vue
    │   ├── task/
    │   │   ├── TaskTree.vue
    │   │   ├── TaskTreeNode.vue
    │   │   ├── TaskCard.vue
    │   │   ├── TaskDetailPanel.vue
    │   │   ├── TaskCreateForm.vue
    │   │   ├── TaskProgressSlider.vue
    │   │   ├── GanttChart.vue
    │   │   └── TaskStatusSelect.vue
    │   ├── deliverable/
    │   │   ├── DeliverableList.vue
    │   │   ├── DeliverableCard.vue
    │   │   ├── DeliverableUploadForm.vue
    │   │   ├── ApprovalWorkflow.vue
    │   │   └── VersionHistory.vue
    │   ├── issue/
    │   │   ├── IssueList.vue
    │   │   ├── IssueCard.vue
    │   │   ├── IssueCreateForm.vue
    │   │   ├── RiskMatrix.vue
    │   │   └── IssueSeverityBadge.vue
    │   ├── member/
    │   │   ├── MemberList.vue
    │   │   ├── MemberCard.vue
    │   │   ├── AssignMemberForm.vue
    │   │   ├── InputRateChart.vue
    │   │   └── WorkLogCalendar.vue
    │   └── dashboard/
    │       ├── ProjectOverviewWidget.vue
    │       ├── TaskProgressWidget.vue
    │       ├── IssueStatusWidget.vue
    │       ├── UpcomingDeadlineWidget.vue
    │       ├── TeamWorkloadWidget.vue
    │       └── ActivityFeedWidget.vue
    ├── views/
    │   ├── auth/
    │   │   ├── LoginView.vue
    │   │   └── ChangePasswordView.vue
    │   ├── dashboard/
    │   │   └── DashboardView.vue
    │   ├── project/
    │   │   ├── ProjectListView.vue
    │   │   ├── ProjectDetailView.vue
    │   │   └── ProjectCreateView.vue
    │   ├── task/
    │   │   ├── TaskListView.vue
    │   │   └── TaskDetailView.vue
    │   ├── deliverable/
    │   │   ├── DeliverableListView.vue
    │   │   └── DeliverableDetailView.vue
    │   ├── issue/
    │   │   ├── IssueListView.vue
    │   │   └── IssueDetailView.vue
    │   ├── member/
    │   │   └── MemberListView.vue
    │   ├── timeoff/
    │   │   ├── WorkLogView.vue
    │   │   └── LeaveView.vue
    │   ├── admin/
    │   │   ├── UserManagementView.vue
    │   │   └── CompanyManagementView.vue
    │   └── error/
    │       ├── NotFoundView.vue
    │       └── ForbiddenView.vue
    ├── router/
    │   ├── index.ts
    │   ├── guards.ts
    │   └── routes/
    │       ├── auth.routes.ts
    │       ├── project.routes.ts
    │       ├── task.routes.ts
    │       ├── deliverable.routes.ts
    │       ├── issue.routes.ts
    │       ├── member.routes.ts
    │       ├── timeoff.routes.ts
    │       └── admin.routes.ts
    ├── stores/
    │   ├── auth.store.ts
    │   ├── project.store.ts
    │   ├── task.store.ts
    │   ├── notification.store.ts
    │   └── ui.store.ts
    ├── api/
    │   ├── axios.instance.ts
    │   ├── auth.api.ts
    │   ├── user.api.ts
    │   ├── company.api.ts
    │   ├── project.api.ts
    │   ├── task.api.ts
    │   ├── deliverable.api.ts
    │   ├── issue.api.ts
    │   ├── member.api.ts
    │   ├── timeoff.api.ts
    │   └── notification.api.ts
    ├── types/
    │   ├── auth.types.ts
    │   ├── user.types.ts
    │   ├── company.types.ts
    │   ├── project.types.ts
    │   ├── task.types.ts
    │   ├── deliverable.types.ts
    │   ├── issue.types.ts
    │   ├── member.types.ts
    │   ├── timeoff.types.ts
    │   └── common.types.ts
    └── utils/
        ├── date.utils.ts
        ├── progress.utils.ts
        ├── permission.utils.ts
        ├── file.utils.ts
        ├── format.utils.ts
        └── validation.utils.ts
```

---

## 부록

### A. API 엔드포인트 목록 (요약)

| 도메인 | 메서드 | URL | 설명 |
|--------|--------|-----|------|
| 인증 | POST | `/auth/login` | 로그인 |
| 인증 | POST | `/auth/logout` | 로그아웃 |
| 인증 | POST | `/auth/refresh` | 토큰 재발급 |
| 사용자 | GET | `/users` | 사용자 목록 |
| 사용자 | POST | `/users` | 사용자 등록 |
| 사용자 | PUT | `/users/{id}` | 사용자 수정 |
| 프로젝트 | GET | `/projects` | 프로젝트 목록 |
| 프로젝트 | POST | `/projects` | 프로젝트 생성 |
| 프로젝트 | GET | `/projects/{id}` | 프로젝트 상세 |
| 프로젝트 | PUT | `/projects/{id}` | 프로젝트 수정 |
| 프로젝트 | PATCH | `/projects/{id}/status` | 상태 변경 |
| 업무 | GET | `/projects/{id}/tasks/tree` | 업무 트리 조회 |
| 업무 | POST | `/projects/{id}/tasks` | 업무 생성 |
| 업무 | PATCH | `/tasks/{id}/progress` | 진척률 업데이트 |
| 산출물 | GET | `/projects/{id}/deliverables` | 산출물 목록 |
| 산출물 | POST | `/projects/{id}/deliverables` | 산출물 등록 |
| 산출물 | POST | `/deliverables/{id}/approve` | 승인 |
| 산출물 | POST | `/deliverables/{id}/reject` | 반려 |
| 이슈 | GET | `/projects/{id}/issues` | 이슈 목록 |
| 이슈 | POST | `/projects/{id}/issues` | 이슈 등록 |
| 인력 | GET | `/projects/{id}/members` | 인력 목록 |
| 인력 | POST | `/projects/{id}/members` | 인력 배정 |
| 공수 | GET | `/projects/{id}/worklogs` | 공수 목록 |
| 공수 | POST | `/projects/{id}/worklogs` | 공수 등록 |
| 알림 | GET | `/notifications` | 알림 목록 |
| 알림 | PATCH | `/notifications/{id}/read` | 읽음 처리 |
| 활동 | GET | `/projects/{id}/activities` | 활동 이력 |

### B. 개발 컨벤션

#### 백엔드 컨벤션
- 패키지명: 소문자, 단수형 (`controller`, `service`, `repository`, `entity`, `dto`)
- 클래스명: PascalCase
- 메서드명: camelCase, 동사 시작 (`findProjectById`, `createTask`)
- DTO는 `Request` / `Response` 접미사 사용
- 서비스 메서드는 `@Transactional` 명시
- 예외는 반드시 `BusinessException`과 `ErrorCode` enum 사용

#### 프론트엔드 컨벤션
- 컴포넌트: PascalCase (`.vue`)
- 스토어 파일: `{domain}.store.ts`
- API 파일: `{domain}.api.ts`
- 타입 파일: `{domain}.types.ts`
- Composition API (`<script setup lang="ts">`) 사용 필수
- 컴포넌트 `props`는 TypeScript 인터페이스로 정의

### C. 보안 고려 사항

| 항목 | 조치 |
|------|------|
| SQL Injection | Spring Data JPA / QueryDSL 파라미터 바인딩으로 방지 |
| XSS | Vue 3 기본 이스케이프 + DOMPurify 민감 영역 적용 |
| CSRF | Stateless JWT 방식으로 세션 기반 CSRF 무관, SameSite 쿠키 설정 |
| 파일 업로드 | 확장자 화이트리스트, 파일 크기 제한(50MB), 저장 경로 난수화 |
| 비밀번호 | BCrypt(strength=12) 해시 저장 |
| JWT 비밀키 | 환경변수로 관리, 256비트 이상 |
| 접근 제어 | 메서드 레벨 `@PreAuthorize` + 서비스 레이어 권한 검증 이중화 |
| 민감 정보 로깅 | 비밀번호, 토큰은 로그에서 마스킹 처리 |

---

*본 문서는 AetherPMO 개발 팀의 기술 아키텍처 기준 문서이며, 시스템 변경 시 지속적으로 업데이트되어야 한다.*

*최종 수정: 2026-06-15*
