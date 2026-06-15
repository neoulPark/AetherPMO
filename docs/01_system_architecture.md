# 시스템 아키텍처 문서

**프로젝트명:** okePMS - 공공기관 SI 프로젝트 관리 시스템  
**버전:** 1.0.0  
**작성일:** 2026-06-15  
**작성자:** 개발팀  

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
9. [개발 환경 구성 (Docker Compose)](#9-개발-환경-구성-docker-compose)
10. [전체 디렉토리 구조](#10-전체-디렉토리-구조)

---

## 1. 시스템 개요

### 1.1 목적

okePMS는 **공공기관 SI(System Integration) 프로젝트**의 전 생애주기를 체계적으로 관리하기 위한 웹 기반 프로젝트 관리 시스템이다.

공공 SI 프로젝트는 발주기관의 요구사항, 계약 기반 산출물 의무, 다수의 협력업체 참여, 엄격한 품질 기준(PMO/QA 검수) 등 일반 민간 프로젝트와 구별되는 복잡한 특성을 갖는다. 본 시스템은 이러한 특성에 최적화된 업무 관리, 진척 모니터링, 산출물 추적, 인력·공수 관리 기능을 제공하여 프로젝트 성공률을 높이고 행정 부담을 최소화하는 것을 목적으로 한다.

### 1.2 대상 사용자

| 역할 | 설명 | 주요 권한 |
|------|------|-----------|
| **ADMIN** (시스템 관리자) | 전체 시스템 및 회사/사용자 마스터 데이터 관리자 | 전체 기능 접근, 사용자 관리, 회사 관리, 시스템 설정 |
| **PM** (프로젝트 관리자) | 프로젝트 책임자로서 계획 수립, 인력 배치, 진척 관리 수행 | 프로젝트 생성/수정, WBS 관리, 인력 배치, 산출물 승인, 이슈 관리 |
| **MEMBER** (프로젝트 구성원) | 실무 담당자(개발자, 분석가, 설계자 등), 업무 실행 및 보고 | 할당된 업무 조회/업데이트, 근태 입력, 산출물 등록, 이슈 제기 |

> PMO(Project Management Office) 및 QA 담당자는 별도 역할 없이 ADMIN 또는 전용 뷰어 권한으로 접근하며, 향후 `PMO` 역할 추가를 고려한다.

### 1.3 주요 기능 요약

| 기능 영역 | 세부 기능 |
|-----------|-----------|
| **프로젝트 관리** | 프로젝트 등록/수정/삭제, 상태 관리, 발주기관·수행기관 정보 관리, 계약 정보 |
| **업무(WBS) 관리** | 트리 구조 WBS 생성, 업무 할당, 진척률 자동 계산, 일정 관리, 상태 전이 |
| **산출물 관리** | 산출물 목록 정의, 파일 첨부, 검토/승인 워크플로우, 버전 관리 |
| **이슈/리스크 관리** | 이슈 등록, 심각도/우선순위 분류, 담당자 지정, 해결 이력 추적 |
| **인력/공수 관리** | 프로젝트 인력 배치, 역할 지정, 투입 기간 관리, MM(Man-Month) 산출 |
| **근태 관리** | 일별 공수 입력, 월별 집계, 초과근무 관리 |
| **대시보드** | 프로젝트 현황 요약, 진척률 현황, 이슈 현황, 일정 위험도 시각화 |
| **알림** | 업무 할당, 기한 임박, 이슈 변경, 산출물 승인 요청 등 실시간 알림 |
| **활동 이력** | 모든 주요 변경 사항 자동 감사 로그 기록 |

---

## 2. 전체 시스템 구성도

### 2.1 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                          사용자 (Browser)                            │
│                PM / MEMBER / ADMIN                                   │
└────────────────────────┬────────────────────────────────────────────┘
                         │ HTTPS (443)
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     프론트엔드 레이어                                 │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Vue 3 SPA (Vite)                           │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐  │   │
│  │  │ Vue Router│  │  Pinia   │  │  Axios   │  │ TypeScript │  │   │
│  │  │ (라우팅)  │  │ (상태관리)│  │ (HTTP)   │  │  (타입정의) │  │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └────────────┘  │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                      Port 5173 (dev) / 80 (prod)                    │
└────────────────────────┬────────────────────────────────────────────┘
                         │ HTTP REST API (JSON)
                         │ Authorization: Bearer <JWT>
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      백엔드 레이어                                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                 Spring Boot 3.x Application                   │   │
│  │                                                               │   │
│  │  ┌────────────┐  ┌──────────────┐  ┌──────────────────────┐ │   │
│  │  │Spring      │  │ REST         │  │  Business Logic       │ │   │
│  │  │Security    │  │ Controllers  │  │  (Service Layer)      │ │   │
│  │  │(JWT Filter)│  │ (@RestCtrl)  │  │                       │ │   │
│  │  └────────────┘  └──────────────┘  └──────────────────────┘ │   │
│  │  ┌────────────────────────────────────────────────────────┐  │   │
│  │  │         Spring Data JPA (Repository Layer)              │  │   │
│  │  └────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                          Port 8080                                   │
└────────────────────────┬────────────────────────────────────────────┘
                         │ JDBC / JPA (5432)
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      데이터베이스 레이어                               │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    PostgreSQL 15                               │   │
│  │                                                               │   │
│  │   users │ companies │ projects │ tasks │ deliverables         │   │
│  │   issues │ project_members │ time_records │ notifications      │   │
│  │   activity_logs │ ...                                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                          Port 5432                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 모듈 구성도

```
┌─────────────────────────────────────────────────────────────────────┐
│                       okePMS 시스템 모듈                              │
├─────────────────┬───────────────────────────────────────────────────┤
│                 │                                                     │
│   인증/보안     │   auth module ──→ JWT 발급/검증/갱신               │
│                 │   security module ──→ Spring Security 설정          │
├─────────────────┼───────────────────────────────────────────────────┤
│                 │                                                     │
│   마스터 관리   │   user module ──→ 사용자 계정 관리                 │
│                 │   company module ──→ 발주/수행 회사 관리            │
├─────────────────┼───────────────────────────────────────────────────┤
│                 │   project module ──→ 프로젝트 CRUD, 상태 관리      │
│                 │        │                                            │
│   프로젝트      │   member module ──→ 인력 배치, 역할 관리           │
│   핵심 기능     │        │                                            │
│                 │   task module ──→ WBS 트리, 진척률 계산            │
│                 │        │                                            │
│                 │   deliverable module ──→ 산출물, 승인 워크플로우   │
│                 │        │                                            │
│                 │   issue module ──→ 이슈/리스크 추적                │
│                 │        │                                            │
│                 │   timeoff module ──→ 근태/공수 관리                │
├─────────────────┼───────────────────────────────────────────────────┤
│                 │                                                     │
│   공통 지원     │   notification module ──→ 실시간 알림              │
│                 │   activity module ──→ 감사 로그                    │
│                 │   common module ──→ 공통 응답, 예외, BaseEntity    │
└─────────────────┴───────────────────────────────────────────────────┘
```

---

## 3. 기술 스택 상세

### 3.1 백엔드

| 기술 | 버전 | 선택 이유 |
|------|------|-----------|
| **Java** | 17 LTS | 공공기관 표준 언어, LTS 버전 장기 지원 보장, Record·Sealed class 등 최신 문법 활용, Spring Boot 3.x 요구 최소 버전 |
| **Spring Boot** | 3.3.x | 공공 SI 시장 표준 프레임워크, 방대한 레퍼런스, 빠른 개발 생산성, Jakarta EE 10 지원 |
| **Spring Security** | 6.x (Boot 내장) | Spring 생태계 표준 보안 프레임워크, JWT 필터 체인 커스터마이징 용이, 역할 기반 접근 제어(RBAC) 지원 |
| **Spring Data JPA** | 3.x (Boot 내장) | ORM 추상화로 데이터베이스 종속성 최소화, 복잡 쿼리는 JPQL/QueryDSL 병행 사용 |
| **Hibernate** | 6.x (JPA 구현체) | 성숙한 ORM, N+1 문제 해결을 위한 fetch 전략 세밀 제어 가능 |
| **Maven** | 3.9.x | 공공 SI 프로젝트 표준 빌드 도구, 풍부한 플러그인 생태계, CI/CD 파이프라인 연동 용이 |
| **Lombok** | 1.18.x | 반복 코드(Getter/Setter/Builder) 제거, 생산성 향상 |
| **MapStruct** | 1.5.x | Entity ↔ DTO 변환 자동화, 컴파일 타임 검증으로 런타임 오류 방지 |
| **JJWT** | 0.12.x | JWT 생성/파싱 표준 라이브러리, 활발한 유지보수 |

### 3.2 프론트엔드

| 기술 | 버전 | 선택 이유 |
|------|------|-----------|
| **Vue 3** | 3.4.x | Composition API로 코드 재사용성 향상, 한국 공공/SI 시장 높은 점유율, 가벼운 학습 곡선 |
| **TypeScript** | 5.x | 대규모 팀 협업 시 타입 안정성 확보, IDE 자동완성 지원, 런타임 오류 사전 방지 |
| **Pinia** | 2.x | Vue 공식 상태관리 라이브러리(Vuex 후속), TypeScript 친화적, DevTools 지원 |
| **Vue Router** | 4.x | Vue 공식 라우터, 중첩 라우트·내비게이션 가드 지원 |
| **Vite** | 5.x | 빠른 개발 서버 HMR, ESM 기반 빌드, 번들 최적화 |
| **Axios** | 1.x | HTTP 클라이언트 표준, 인터셉터를 통한 토큰 자동 첨부 및 갱신 처리 |
| **Element Plus** | 2.x | Vue 3 호환 엔터프라이즈 UI 컴포넌트 라이브러리, 테이블·폼·날짜피커 풍부 |

### 3.3 데이터베이스

| 기술 | 버전 | 선택 이유 |
|------|------|-----------|
| **PostgreSQL** | 15.x | 오픈소스 RDBMS, 공공기관 라이선스 비용 절감, JSON 타입 지원, 트리 구조 쿼리(RECURSIVE CTE) 지원, MVCC 기반 동시성 처리 |

### 3.4 인프라/DevOps

| 기술 | 버전 | 선택 이유 |
|------|------|-----------|
| **Docker** | 25.x | 로컬 개발 환경 일관성 보장, 배포 이식성 |
| **Docker Compose** | 2.x | 멀티 컨테이너 로컬 환경 구성 간편화 |
| **pgAdmin** | 8.x | PostgreSQL 웹 관리 도구, 개발 편의성 |

---

## 4. 백엔드 모듈 구조

### 4.1 패키지 트리

```
com.okepms
│
├── config/                          # 전역 설정
│   ├── SecurityConfig.java          # Spring Security 필터 체인 설정
│   ├── CorsConfig.java              # CORS 허용 도메인/메서드 설정
│   ├── JpaConfig.java               # JPA AuditingEntityListener 활성화
│   └── SwaggerConfig.java           # OpenAPI 3.0 문서 설정
│
├── common/                          # 공통 컴포넌트
│   ├── response/
│   │   ├── ApiResponse.java         # 표준 응답 래퍼: {success, message, data}
│   │   └── PageResponse.java        # 페이지네이션 응답 래퍼
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice 전역 예외 처리
│   │   ├── BusinessException.java       # 비즈니스 로직 예외 기반 클래스
│   │   └── ErrorCode.java               # 오류 코드 enum (HTTP 상태 + 메시지)
│   └── entity/
│       └── BaseEntity.java          # createdAt, updatedAt, createdBy, updatedBy (JPA Auditing)
│
├── security/                        # 인증/보안
│   ├── JwtTokenProvider.java        # JWT 생성, 파싱, 검증
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter: 요청마다 JWT 검증
│   ├── CustomUserDetails.java       # UserDetails 구현체
│   └── CustomUserDetailsService.java # UserDetailsService 구현체 (DB 조회)
│
└── domain/                          # 비즈니스 도메인
    │
    ├── auth/                        # 인증 도메인
    │   ├── controller/
    │   │   └── AuthController.java  # POST /api/auth/login, /logout, /refresh
    │   ├── service/
    │   │   └── AuthService.java     # 로그인 검증, 토큰 발급/갱신/폐기
    │   ├── dto/
    │   │   ├── LoginRequest.java
    │   │   ├── LoginResponse.java   # accessToken, refreshToken, userInfo
    │   │   └── TokenRefreshRequest.java
    │   └── repository/
    │       └── RefreshTokenRepository.java  # Redis 또는 DB 기반 Refresh Token 저장
    │
    ├── user/                        # 사용자 관리
    │   ├── controller/
    │   │   └── UserController.java  # GET/POST/PUT/DELETE /api/users
    │   ├── service/
    │   │   └── UserService.java
    │   ├── repository/
    │   │   └── UserRepository.java
    │   ├── entity/
    │   │   └── User.java            # users 테이블 엔티티
    │   └── dto/
    │       ├── UserCreateRequest.java
    │       ├── UserUpdateRequest.java
    │       └── UserResponse.java
    │
    ├── company/                     # 회사 관리
    │   ├── controller/
    │   │   └── CompanyController.java  # GET/POST/PUT/DELETE /api/companies
    │   ├── service/
    │   │   └── CompanyService.java
    │   ├── repository/
    │   │   └── CompanyRepository.java
    │   ├── entity/
    │   │   └── Company.java         # companies 테이블 (발주기관/수행기관)
    │   └── dto/
    │       └── CompanyDto.java
    │
    ├── project/                     # 프로젝트 관리
    │   ├── controller/
    │   │   └── ProjectController.java  # /api/projects
    │   ├── service/
    │   │   └── ProjectService.java
    │   ├── repository/
    │   │   └── ProjectRepository.java
    │   ├── entity/
    │   │   └── Project.java         # projects 테이블
    │   └── dto/
    │       ├── ProjectCreateRequest.java
    │       ├── ProjectUpdateRequest.java
    │       └── ProjectResponse.java
    │
    ├── task/                        # 업무(WBS) 관리
    │   ├── controller/
    │   │   └── TaskController.java  # /api/projects/{id}/tasks
    │   ├── service/
    │   │   ├── TaskService.java
    │   │   └── TaskProgressService.java  # 진척률 자동 계산 로직
    │   ├── repository/
    │   │   └── TaskRepository.java
    │   ├── entity/
    │   │   └── Task.java            # tasks 테이블 (자기참조: parent_id)
    │   └── dto/
    │       ├── TaskCreateRequest.java
    │       ├── TaskUpdateRequest.java
    │       ├── TaskProgressUpdateRequest.java
    │       └── TaskTreeResponse.java  # 트리 구조 응답
    │
    ├── deliverable/                 # 산출물 관리
    │   ├── controller/
    │   │   └── DeliverableController.java  # /api/projects/{id}/deliverables
    │   ├── service/
    │   │   └── DeliverableService.java
    │   ├── repository/
    │   │   └── DeliverableRepository.java
    │   ├── entity/
    │   │   └── Deliverable.java
    │   └── dto/
    │       └── DeliverableDto.java
    │
    ├── issue/                       # 이슈/리스크 관리
    │   ├── controller/
    │   │   └── IssueController.java  # /api/projects/{id}/issues
    │   ├── service/
    │   │   └── IssueService.java
    │   ├── repository/
    │   │   └── IssueRepository.java
    │   ├── entity/
    │   │   └── Issue.java
    │   └── dto/
    │       └── IssueDto.java
    │
    ├── member/                      # 프로젝트 인력 관리
    │   ├── controller/
    │   │   └── MemberController.java  # /api/projects/{id}/members
    │   ├── service/
    │   │   └── MemberService.java
    │   ├── repository/
    │   │   └── ProjectMemberRepository.java
    │   ├── entity/
    │   │   └── ProjectMember.java
    │   └── dto/
    │       └── MemberDto.java
    │
    ├── timeoff/                     # 근태/공수 관리
    │   ├── controller/
    │   │   └── TimeRecordController.java  # /api/projects/{id}/time-records
    │   ├── service/
    │   │   └── TimeRecordService.java
    │   ├── repository/
    │   │   └── TimeRecordRepository.java
    │   ├── entity/
    │   │   └── TimeRecord.java
    │   └── dto/
    │       └── TimeRecordDto.java
    │
    ├── notification/                # 알림
    │   ├── controller/
    │   │   └── NotificationController.java  # /api/notifications
    │   ├── service/
    │   │   └── NotificationService.java     # 알림 생성, 읽음 처리
    │   ├── repository/
    │   │   └── NotificationRepository.java
    │   ├── entity/
    │   │   └── Notification.java
    │   └── dto/
    │       └── NotificationDto.java
    │
    └── activity/                    # 활동 이력 (감사 로그)
        ├── service/
        │   └── ActivityService.java  # AOP 기반 자동 로깅
        ├── repository/
        │   └── ActivityRepository.java
        ├── entity/
        │   └── ActivityLog.java
        └── dto/
            └── ActivityLogDto.java
```

### 4.2 공통 응답 형식

모든 API는 아래 표준 응답 구조를 따른다.

```json
// 성공 응답
{
  "success": true,
  "message": "프로젝트가 생성되었습니다.",
  "data": { ... }
}

// 오류 응답
{
  "success": false,
  "message": "프로젝트를 찾을 수 없습니다.",
  "errorCode": "PROJECT_NOT_FOUND",
  "data": null
}

// 페이지네이션 응답
{
  "success": true,
  "message": null,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### 4.3 BaseEntity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private Long createdBy;      // users.id FK (논리적 참조)

    @LastModifiedBy
    @Column(nullable = false)
    private Long updatedBy;

    @Column(nullable = false)
    private boolean deleted = false;  // 소프트 딜리트
}
```

---

## 5. 프론트엔드 모듈 구조

### 5.1 디렉토리 구조

```
src/
│
├── assets/                          # 정적 자원
│   ├── images/
│   ├── icons/
│   └── styles/
│       ├── main.css                 # 전역 스타일
│       └── variables.css            # CSS 변수 (색상, 폰트)
│
├── components/                      # 재사용 가능 컴포넌트
│   ├── common/                      # 공통 컴포넌트
│   │   ├── AppHeader.vue            # 상단 네비게이션 바
│   │   ├── AppSidebar.vue           # 사이드바 메뉴
│   │   ├── AppBreadcrumb.vue        # 브레드크럼
│   │   ├── DataTable.vue            # 공통 데이터 테이블 (검색/페이지네이션)
│   │   ├── ConfirmDialog.vue        # 확인/취소 다이얼로그
│   │   ├── FileUpload.vue           # 파일 업로드 컴포넌트
│   │   ├── StatusBadge.vue          # 상태 뱃지 (색상 표시)
│   │   └── LoadingSpinner.vue
│   │
│   ├── project/
│   │   ├── ProjectCard.vue          # 프로젝트 목록 카드
│   │   ├── ProjectForm.vue          # 프로젝트 생성/수정 폼
│   │   └── ProjectStatusBar.vue     # 프로젝트 진척률 막대
│   │
│   ├── task/
│   │   ├── TaskTree.vue             # WBS 트리 컴포넌트
│   │   ├── TaskForm.vue             # 업무 생성/수정 폼
│   │   ├── TaskProgressBar.vue      # 진척률 표시
│   │   ├── GanttChart.vue           # 간트 차트
│   │   └── TaskStatusChip.vue       # 상태 칩
│   │
│   ├── deliverable/
│   │   ├── DeliverableList.vue
│   │   ├── DeliverableForm.vue
│   │   └── DeliverableApproval.vue  # 승인 워크플로우
│   │
│   ├── issue/
│   │   ├── IssueList.vue
│   │   ├── IssueForm.vue
│   │   └── IssueSeverityTag.vue
│   │
│   ├── member/
│   │   ├── MemberList.vue
│   │   ├── MemberAssignForm.vue
│   │   └── ManMonthSummary.vue      # 공수 요약 테이블
│   │
│   └── dashboard/
│       ├── ProjectSummaryWidget.vue
│       ├── ProgressDonutChart.vue
│       ├── UpcomingDeadlineWidget.vue
│       └── IssueStatusWidget.vue
│
├── views/                           # 페이지 컴포넌트 (라우터와 1:1 매핑)
│   ├── auth/
│   │   └── LoginView.vue
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
│   │   └── DeliverableListView.vue
│   ├── issue/
│   │   └── IssueListView.vue
│   ├── member/
│   │   └── MemberListView.vue
│   ├── timeoff/
│   │   └── TimeRecordView.vue
│   ├── notification/
│   │   └── NotificationView.vue
│   └── admin/
│       ├── UserManagementView.vue
│       └── CompanyManagementView.vue
│
├── router/
│   └── index.ts                     # Vue Router 설정, 인증 가드
│
├── stores/                          # Pinia 상태 관리
│   ├── auth.ts                      # 인증 상태 (user, token, role)
│   ├── project.ts                   # 현재 프로젝트 상태
│   ├── task.ts                      # 업무 트리 상태
│   ├── notification.ts              # 알림 목록 및 읽음 상태
│   └── ui.ts                        # UI 상태 (사이드바 열림, 로딩)
│
├── api/                             # Axios API 호출 모듈
│   ├── axios.ts                     # Axios 인스턴스 + 인터셉터 설정
│   ├── auth.ts                      # 인증 API
│   ├── project.ts                   # 프로젝트 API
│   ├── task.ts                      # 업무 API
│   ├── deliverable.ts               # 산출물 API
│   ├── issue.ts                     # 이슈 API
│   ├── member.ts                    # 인력 API
│   ├── timeRecord.ts                # 근태 API
│   └── notification.ts              # 알림 API
│
├── types/                           # TypeScript 타입 정의
│   ├── auth.ts                      # User, LoginRequest, TokenResponse
│   ├── project.ts                   # Project, ProjectStatus enum
│   ├── task.ts                      # Task, TaskStatus enum, TaskTree
│   ├── deliverable.ts
│   ├── issue.ts                     # Issue, IssueSeverity, IssueStatus enum
│   ├── member.ts
│   ├── timeRecord.ts
│   └── common.ts                    # ApiResponse, PageResponse, Pagination
│
├── utils/
│   ├── date.ts                      # 날짜 포매팅 유틸리티
│   ├── number.ts                    # 숫자/퍼센트 포매팅
│   ├── storage.ts                   # LocalStorage 래퍼 (토큰 저장)
│   └── validator.ts                 # 폼 유효성 검증 함수
│
├── App.vue
└── main.ts
```

### 5.2 Axios 인터셉터 구조

```typescript
// src/api/axios.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
});

// 요청 인터셉터: Access Token 자동 첨부
apiClient.interceptors.request.use((config) => {
  const token = storage.getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 응답 인터셉터: 401 시 Refresh Token으로 자동 갱신
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      await authStore.refreshToken();
      return apiClient(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 5.3 라우터 인증 가드

```typescript
// src/router/index.ts
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const requiresAuth = to.meta.requiresAuth !== false;

  if (requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } });
    return;
  }

  if (to.meta.roles && !to.meta.roles.includes(authStore.userRole)) {
    next({ name: 'forbidden' });
    return;
  }

  next();
});
```

---

## 6. 데이터베이스 구성

### 6.1 주요 테이블 목록

| 테이블명 | 설명 | 주요 컬럼 |
|----------|------|-----------|
| `users` | 시스템 사용자 | id, email, password, name, role, company_id, is_active |
| `companies` | 발주/수행 회사 | id, name, type(ORDERING/PERFORMING), business_no, contact |
| `projects` | 프로젝트 | id, name, code, status, start_date, end_date, ordering_company_id, performing_company_id, pm_id, budget |
| `project_members` | 프로젝트 인력 배치 | id, project_id, user_id, role, join_date, leave_date, planned_mm |
| `tasks` | 업무(WBS) | id, project_id, parent_id, title, level, order_num, status, progress, assignee_id, planned_start, planned_end, actual_start, actual_end, weight |
| `deliverables` | 산출물 | id, project_id, task_id, name, status, due_date, reviewer_id, approver_id, file_path, version |
| `issues` | 이슈/리스크 | id, project_id, task_id, title, type(ISSUE/RISK), severity, priority, status, reporter_id, assignee_id, due_date, resolved_at |
| `time_records` | 공수 기록 | id, project_id, user_id, task_id, work_date, hours, work_type(NORMAL/OVERTIME), description |
| `notifications` | 알림 | id, user_id, type, title, message, ref_type, ref_id, is_read, created_at |
| `activity_logs` | 감사 로그 | id, user_id, action, entity_type, entity_id, before_value(JSON), after_value(JSON), ip_address, created_at |
| `refresh_tokens` | Refresh Token 관리 | id, user_id, token, expires_at, is_revoked |

### 6.2 ERD 관계 개요

```
companies ──────────────────────────────────────────────────┐
     │ (ordering/performing)                                  │
     │                                                        ▼
     └──────────────────────► projects ◄──────── users (PM)
                                  │
              ┌───────────────────┼───────────────────────┐
              │                   │                        │
              ▼                   ▼                        ▼
      project_members           tasks                 deliverables
       (user_id FK)          (self-ref                (task_id FK)
              │               parent_id)
              │                   │
              ▼                   ├──────────► issues
            users                 │            (task_id FK)
                                  │
                                  └──────────► time_records
                                               (task_id FK)

users ──────────────────────────────────────────────────────────────┐
     └──► notifications (user_id FK)                                │
     └──► activity_logs (user_id FK)                                │
     └──► refresh_tokens (user_id FK)                               │
                                                                    │
     ◄──────────────────── project_members (user_id FK) ───────────┘
```

### 6.3 tasks 테이블 트리 구조

```sql
-- tasks 테이블 (자기 참조 구조)
CREATE TABLE tasks (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES projects(id),
    parent_id       BIGINT REFERENCES tasks(id),       -- NULL이면 루트 노드
    title           VARCHAR(200) NOT NULL,
    level           SMALLINT NOT NULL DEFAULT 1,       -- 1: 대분류, 2: 중분류, 3: 소분류
    order_num       INTEGER NOT NULL DEFAULT 0,        -- 같은 부모 내 순서
    status          VARCHAR(20) NOT NULL DEFAULT 'READY',
    progress        SMALLINT NOT NULL DEFAULT 0,       -- 0~100
    weight          NUMERIC(5,2) NOT NULL DEFAULT 1.0, -- 진척률 계산 가중치
    assignee_id     BIGINT REFERENCES users(id),
    planned_start   DATE,
    planned_end     DATE,
    actual_start    DATE,
    actual_end      DATE,
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

-- 재귀 CTE로 전체 트리 조회
WITH RECURSIVE task_tree AS (
    -- 앵커: 루트 노드
    SELECT id, parent_id, title, level, progress, weight, 0 AS depth
    FROM tasks
    WHERE project_id = :projectId AND parent_id IS NULL AND deleted = FALSE

    UNION ALL

    -- 재귀: 자식 노드
    SELECT t.id, t.parent_id, t.title, t.level, t.progress, t.weight, tt.depth + 1
    FROM tasks t
    INNER JOIN task_tree tt ON t.parent_id = tt.id
    WHERE t.deleted = FALSE
)
SELECT * FROM task_tree ORDER BY depth, order_num;
```

### 6.4 인덱스 전략

```sql
-- 프로젝트 기반 조회 (가장 빈번한 패턴)
CREATE INDEX idx_tasks_project_id ON tasks(project_id) WHERE deleted = FALSE;
CREATE INDEX idx_tasks_parent_id ON tasks(parent_id) WHERE deleted = FALSE;
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_deliverables_project_id ON deliverables(project_id);
CREATE INDEX idx_issues_project_id ON issues(project_id);
CREATE INDEX idx_time_records_project_user ON time_records(project_id, user_id, work_date);

-- 사용자 조회
CREATE UNIQUE INDEX idx_users_email ON users(email) WHERE deleted = FALSE;

-- 알림 조회 (읽지 않은 알림 빠른 조회)
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;

-- 감사 로그 (엔티티별 이력 조회)
CREATE INDEX idx_activity_entity ON activity_logs(entity_type, entity_id, created_at DESC);

-- 프로젝트 조회 (상태, 기간 필터)
CREATE INDEX idx_projects_status ON projects(status) WHERE deleted = FALSE;
CREATE INDEX idx_projects_dates ON projects(start_date, end_date);
```

---

## 7. 인증/인가 흐름

### 7.1 JWT 토큰 전략

| 토큰 유형 | 유효 기간 | 저장 위치 | 용도 |
|-----------|-----------|-----------|------|
| **Access Token** | 30분 | 메모리 (Pinia store) | API 요청 인증 |
| **Refresh Token** | 7일 | HttpOnly Cookie + DB | Access Token 갱신 |

> Access Token은 LocalStorage가 아닌 메모리(Pinia)에 저장하여 XSS 공격 노출을 최소화한다.  
> Refresh Token은 HttpOnly Cookie로 전달하여 JavaScript에서 직접 접근을 차단하고, DB의 `refresh_tokens` 테이블에 저장하여 강제 무효화(로그아웃, 보안 이벤트)를 지원한다.

### 7.2 로그인 시퀀스 다이어그램

```
Client (Vue3)        Spring Boot API        PostgreSQL
     │                      │                    │
     │  POST /api/auth/login │                    │
     │  {email, password}    │                    │
     │──────────────────────►│                    │
     │                       │  SELECT * FROM     │
     │                       │  users WHERE email │
     │                       │──────────────────►│
     │                       │◄──────────────────│
     │                       │  (user 조회 결과)  │
     │                       │                    │
     │                       │  BCrypt.matches()  │
     │                       │  (비밀번호 검증)    │
     │                       │                    │
     │                       │  JWT 생성:          │
     │                       │  - Access Token    │
     │                       │    (30분, payload: │
     │                       │     userId, role)  │
     │                       │  - Refresh Token   │
     │                       │    (7일)           │
     │                       │                    │
     │                       │  INSERT INTO       │
     │                       │  refresh_tokens    │
     │                       │──────────────────►│
     │                       │◄──────────────────│
     │                       │                    │
     │◄──────────────────────│                    │
     │  200 OK               │                    │
     │  {accessToken, user}  │                    │
     │  Set-Cookie:          │                    │
     │  refreshToken=...;    │                    │
     │  HttpOnly; Secure     │                    │
     │                       │                    │
```

### 7.3 API 요청 인증 흐름

```
Client (Vue3)        JwtAuthFilter         Spring Security       Controller
     │                    │                      │                    │
     │  GET /api/projects │                      │                    │
     │  Authorization:    │                      │                    │
     │  Bearer <token>    │                      │                    │
     │───────────────────►│                      │                    │
     │                    │  JWT 파싱/검증        │                    │
     │                    │  (서명 검증, 만료 확인)│                    │
     │                    │                      │                    │
     │                    │  유효한 경우:          │                    │
     │                    │  SecurityContext에    │                    │
     │                    │  Authentication 저장  │                    │
     │                    │─────────────────────►│                    │
     │                    │                      │  @PreAuthorize 검사 │
     │                    │                      │───────────────────►│
     │                    │                      │                    │
     │                    │                      │◄───────────────────│
     │◄───────────────────────────────────────────────────────────────│
     │  200 OK + data     │                      │                    │
     │                    │                      │                    │
     │  만료된 경우:       │                      │                    │
     │◄───────────────────│                      │                    │
     │  401 Unauthorized  │                      │                    │
```

### 7.4 Token Refresh 흐름

```
Client (Vue3)        Spring Boot API        PostgreSQL
     │                      │                    │
     │  401 응답 수신 후     │                    │
     │  자동으로 갱신 요청   │                    │
     │                      │                    │
     │  POST /api/auth/refresh                   │
     │  Cookie: refreshToken=...                 │
     │──────────────────────►│                    │
     │                       │  refresh_tokens    │
     │                       │  유효성 검증        │
     │                       │──────────────────►│
     │                       │◄──────────────────│
     │                       │                    │
     │                       │  새 Access Token   │
     │                       │  생성              │
     │                       │                    │
     │◄──────────────────────│                    │
     │  200 OK               │                    │
     │  {accessToken: "..."}│                    │
     │                       │                    │
     │  원래 요청 재시도     │                    │
     │──────────────────────►│                    │
```

### 7.5 역할별 권한 매트릭스

| 기능 | ADMIN | PM | MEMBER |
|------|-------|----|--------|
| 사용자 관리 | ✅ | ❌ | ❌ |
| 회사 관리 | ✅ | ❌ | ❌ |
| 프로젝트 생성 | ✅ | ✅ | ❌ |
| 프로젝트 수정 | ✅ | ✅ (본인 PM) | ❌ |
| 프로젝트 조회 | ✅ | ✅ | ✅ (참여 프로젝트) |
| 업무 생성/수정 | ✅ | ✅ | ❌ |
| 업무 진척률 업데이트 | ✅ | ✅ | ✅ (담당 업무) |
| 산출물 등록 | ✅ | ✅ | ✅ |
| 산출물 승인 | ✅ | ✅ | ❌ |
| 이슈 등록 | ✅ | ✅ | ✅ |
| 이슈 종결 | ✅ | ✅ | ❌ |
| 공수 입력 | ✅ | ✅ | ✅ (본인) |
| 인력 배치 | ✅ | ✅ | ❌ |

### 7.6 Spring Security 필터 체인

```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│  CorsFilter                      │  CORS 헤더 처리
├─────────────────────────────────┤
│  JwtAuthenticationFilter         │  JWT 검증 → SecurityContext 설정
│  (OncePerRequestFilter)          │
├─────────────────────────────────┤
│  UsernamePasswordAuthFilter      │  (비활성화, JWT 방식 사용)
├─────────────────────────────────┤
│  ExceptionTranslationFilter      │  401/403 예외 처리
├─────────────────────────────────┤
│  FilterSecurityInterceptor       │  URL 권한 검사
└─────────────────────────────────┘
     │
     ▼
  Controller (@PreAuthorize 세밀 권한 제어)
```

---

## 8. 주요 비즈니스 로직

### 8.1 Task 진척률 자동 계산 알고리즘

업무는 트리 구조로 구성되며, **리프 노드(자식이 없는 업무)**의 진척률을 직접 입력하면 **상위 업무의 진척률은 자동으로 가중 평균 계산**된다.

#### 알고리즘 정의

```
진척률(부모) = Σ(자식.진척률 × 자식.weight) / Σ(자식.weight)
```

예시:
```
프로젝트 (자동 계산)
├── 1. 분석 단계 (자동 계산)          weight=3
│   ├── 1.1 현황 분석 (60%)           weight=1
│   ├── 1.2 요구사항 정의 (80%)        weight=2
│   └── 1.3 인터페이스 정의 (40%)      weight=1
├── 2. 설계 단계 (자동 계산)          weight=3
│   ├── 2.1 아키텍처 설계 (100%)       weight=2
│   └── 2.2 DB 설계 (50%)             weight=2
└── 3. 개발 단계 (자동 계산)          weight=4
    └── ...

1. 분석 단계 진척률:
   = (60×1 + 80×2 + 40×1) / (1+2+1)
   = (60 + 160 + 40) / 4
   = 260 / 4 = 65%
```

#### 구현 로직 (Java 의사코드)

```java
// TaskProgressService.java
@Transactional
public void recalculateProgress(Long taskId) {
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

    List<Task> children = taskRepository.findByParentIdAndDeletedFalse(taskId);

    if (children.isEmpty()) {
        // 리프 노드: 직접 입력된 진척률 사용 (변경 없음)
        return;
    }

    // 가중 평균 계산
    double totalWeight = children.stream()
        .mapToDouble(Task::getWeight)
        .sum();

    double weightedSum = children.stream()
        .mapToDouble(c -> c.getProgress() * c.getWeight())
        .sum();

    int calculatedProgress = (int) Math.round(weightedSum / totalWeight);
    task.setProgress(calculatedProgress);
    taskRepository.save(task);

    // 재귀: 부모로 거슬러 올라가며 갱신
    if (task.getParentId() != null) {
        recalculateProgress(task.getParentId());
    }
}
```

### 8.2 업무 상태 전이 규칙

```
                    ┌─────────┐
              ┌────►│  READY  │◄────────────────────┐
              │     └────┬────┘                      │
              │          │ (담당자 지정 또는 PM 시작)  │
              │          ▼                            │
              │     ┌───────────┐                    │
              │     │IN_PROGRESS│                    │
              │     └─────┬─────┘                    │
              │           │                          │
              │    ┌──────┴──────┐                   │
              │    │             │                   │
              │    ▼             ▼                   │
              │ ┌──────┐    ┌────────┐               │
              │ │REVIEW│    │  HOLD  │──────────────►│
              │ └──┬───┘    └────────┘  (재개 시)    │
              │    │                                  │
              │    │ (검토 통과)                       │
              │    ▼                                  │
              │  ┌────┐                               │
              └──│DONE│                               │
                 └────┘                               │
                                                      │
            어느 상태에서든 ──────────────────► CANCELLED
```

#### 상태 전이 규칙 상세

| 현재 상태 | 가능한 다음 상태 | 조건 | 권한 |
|-----------|-----------------|------|------|
| READY | IN_PROGRESS | 시작일 도래 또는 PM 수동 시작 | PM, MEMBER(담당자) |
| READY | CANCELLED | - | PM, ADMIN |
| IN_PROGRESS | REVIEW | 진척률 100% 또는 수동 전환 | MEMBER(담당자), PM |
| IN_PROGRESS | HOLD | 사유 입력 필수 | PM, ADMIN |
| IN_PROGRESS | CANCELLED | - | PM, ADMIN |
| REVIEW | DONE | 검토자 승인 | PM, ADMIN |
| REVIEW | IN_PROGRESS | 검토 반려 (재작업) | PM, ADMIN |
| HOLD | IN_PROGRESS | 재개 | PM, ADMIN |
| HOLD | CANCELLED | - | PM, ADMIN |

#### 하위 업무 존재 시 제약

- 모든 자식 업무가 `DONE`이 아니면 부모 업무를 `DONE`으로 전환 불가
- `CANCELLED` 처리 시 모든 자식 업무도 연쇄 `CANCELLED` 처리 (확인 다이얼로그 표시)

### 8.3 공수 계산 방식

#### 단위 정의

| 단위 | 정의 | 비고 |
|------|------|------|
| **MD (Man-Day)** | 1인이 1일 근무 = 8시간 | 기본 공수 단위 |
| **MM (Man-Month)** | 1인이 1개월 근무 = 20 MD (영업일 기준) | 계약 공수 단위 |
| **MW (Man-Week)** | 1인이 1주 근무 = 5 MD | 주간 보고 단위 |

#### 계산 공식

```
월별 투입 MM = Σ(일별 근무시간) / 8 / 20(영업일)

실적 MM = Σ time_records.hours / 8 / 20

계획 MM = (leave_date - join_date의 영업일 수) / 20

MM 달성률 = 실적 MM / 계획 MM × 100
```

#### SQL 예시 (월별 공수 집계)

```sql
SELECT
    u.name AS 성명,
    pm.planned_mm AS 계획MM,
    ROUND(SUM(tr.hours) / 8.0 / 20.0, 2) AS 실적MM,
    ROUND(SUM(tr.hours) / 8.0 / 20.0 / pm.planned_mm * 100, 1) AS MM달성률
FROM project_members pm
JOIN users u ON pm.user_id = u.id
LEFT JOIN time_records tr ON tr.project_id = pm.project_id
    AND tr.user_id = pm.user_id
    AND DATE_TRUNC('month', tr.work_date) = DATE_TRUNC('month', CURRENT_DATE)
WHERE pm.project_id = :projectId
GROUP BY u.name, pm.planned_mm;
```

### 8.4 프로젝트 전체 진척률 계산

```
프로젝트 진척률 = 루트 업무들의 가중 평균 진척률

= Σ(루트_업무.진척률 × 루트_업무.weight) / Σ(루트_업무.weight)
```

---

## 9. 개발 환경 구성 (Docker Compose)

### 9.1 docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL 15 데이터베이스
  postgres:
    image: postgres:15-alpine
    container_name: okepms-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: okepms
      POSTGRES_USER: okepms
      POSTGRES_PASSWORD: okepms1234
      TZ: Asia/Seoul
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d  # 초기 DDL 스크립트
    networks:
      - okepms-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U okepms -d okepms"]
      interval: 10s
      timeout: 5s
      retries: 5

  # pgAdmin - PostgreSQL 웹 관리 도구
  pgadmin:
    image: dpage/pgadmin4:8
    container_name: okepms-pgadmin
    restart: unless-stopped
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@okepms.com
      PGADMIN_DEFAULT_PASSWORD: admin1234
      TZ: Asia/Seoul
    ports:
      - "5050:80"
    volumes:
      - pgadmin_data:/var/lib/pgadmin
    networks:
      - okepms-network
    depends_on:
      postgres:
        condition: service_healthy

  # Spring Boot 백엔드
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: okepms-backend
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/okepms
      SPRING_DATASOURCE_USERNAME: okepms
      SPRING_DATASOURCE_PASSWORD: okepms1234
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      JWT_SECRET: your-256-bit-secret-key-here-change-in-production
      JWT_ACCESS_EXPIRATION: 1800000     # 30분 (ms)
      JWT_REFRESH_EXPIRATION: 604800000  # 7일 (ms)
      TZ: Asia/Seoul
      JAVA_OPTS: "-Xms512m -Xmx1g"
    ports:
      - "8080:8080"
    networks:
      - okepms-network
    depends_on:
      postgres:
        condition: service_healthy
    profiles:
      - full  # docker compose --profile full up 으로 활성화

  # Vue 3 프론트엔드 (개발 서버)
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    container_name: okepms-frontend
    restart: unless-stopped
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api
      CHOKIDAR_USEPOLLING: "true"  # Docker 내 HMR을 위한 파일 감시 폴링
    ports:
      - "5173:5173"
    volumes:
      - ./frontend:/app
      - /app/node_modules  # node_modules는 컨테이너 내부 것 사용
    networks:
      - okepms-network
    profiles:
      - full

volumes:
  postgres_data:
    driver: local
  pgadmin_data:
    driver: local

networks:
  okepms-network:
    driver: bridge
```

### 9.2 백엔드 Dockerfile

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 9.3 프론트엔드 Dockerfile (개발용)

```dockerfile
# frontend/Dockerfile.dev
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
```

### 9.4 환경 변수 설정 (.env 파일)

```bash
# backend/src/main/resources/application-local.yml
# 로컬 개발용 설정 (git에 커밋 금지, .gitignore 추가 필수)

# frontend/.env.local
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=okePMS
```

### 9.5 개발 환경 시작 명령어

```bash
# 1. DB + pgAdmin만 실행 (기본 개발 모드 - 백엔드/프론트엔드는 IDE에서 실행)
docker compose up -d postgres pgadmin

# 2. 전체 스택 실행
docker compose --profile full up -d

# 3. 로그 확인
docker compose logs -f postgres
docker compose logs -f backend

# 4. DB 초기화 (주의: 데이터 삭제)
docker compose down -v
docker compose up -d postgres pgadmin

# 5. 백엔드 재빌드
docker compose --profile full build backend
docker compose --profile full up -d backend
```

### 9.6 application.yml 주요 설정

```yaml
# backend/src/main/resources/application.yml
spring:
  application:
    name: okepms
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/okepms}
    username: ${SPRING_DATASOURCE_USERNAME:okepms}
    password: ${SPRING_DATASOURCE_PASSWORD:okepms1234}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_batch_fetch_size: 100  # N+1 방지
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 8080
  servlet:
    context-path: /

jwt:
  secret: ${JWT_SECRET}
  access-expiration: ${JWT_ACCESS_EXPIRATION:1800000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

logging:
  level:
    com.okepms: DEBUG
    org.springframework.security: INFO
```

---

## 10. 전체 디렉토리 구조

```
okePms/                                  # 프로젝트 루트
│
├── docs/                                # 문서
│   ├── 01_system_architecture.md       # 본 문서
│   ├── 02_api_specification.md         # API 명세
│   ├── 03_database_erd.md              # ERD 상세
│   └── 04_deployment_guide.md          # 배포 가이드
│
├── docker/                              # Docker 관련 설정
│   └── postgres/
│       └── init/
│           ├── 01_create_tables.sql     # 테이블 생성 DDL
│           └── 02_insert_master.sql     # 초기 마스터 데이터
│
├── backend/                             # Spring Boot 프로젝트
│   ├── pom.xml
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/okepms/
│       │   │       ├── OkePmsApplication.java
│       │   │       ├── config/
│       │   │       │   ├── SecurityConfig.java
│       │   │       │   ├── CorsConfig.java
│       │   │       │   ├── JpaConfig.java
│       │   │       │   └── SwaggerConfig.java
│       │   │       ├── common/
│       │   │       │   ├── response/
│       │   │       │   │   ├── ApiResponse.java
│       │   │       │   │   └── PageResponse.java
│       │   │       │   ├── exception/
│       │   │       │   │   ├── GlobalExceptionHandler.java
│       │   │       │   │   ├── BusinessException.java
│       │   │       │   │   └── ErrorCode.java
│       │   │       │   └── entity/
│       │   │       │       └── BaseEntity.java
│       │   │       ├── security/
│       │   │       │   ├── JwtTokenProvider.java
│       │   │       │   ├── JwtAuthenticationFilter.java
│       │   │       │   ├── CustomUserDetails.java
│       │   │       │   └── CustomUserDetailsService.java
│       │   │       └── domain/
│       │   │           ├── auth/
│       │   │           │   ├── controller/AuthController.java
│       │   │           │   ├── service/AuthService.java
│       │   │           │   ├── repository/RefreshTokenRepository.java
│       │   │           │   └── dto/
│       │   │           │       ├── LoginRequest.java
│       │   │           │       ├── LoginResponse.java
│       │   │           │       └── TokenRefreshRequest.java
│       │   │           ├── user/
│       │   │           │   ├── controller/UserController.java
│       │   │           │   ├── service/UserService.java
│       │   │           │   ├── repository/UserRepository.java
│       │   │           │   ├── entity/User.java
│       │   │           │   └── dto/
│       │   │           ├── company/
│       │   │           │   └── ...
│       │   │           ├── project/
│       │   │           │   └── ...
│       │   │           ├── task/
│       │   │           │   ├── controller/TaskController.java
│       │   │           │   ├── service/
│       │   │           │   │   ├── TaskService.java
│       │   │           │   │   └── TaskProgressService.java
│       │   │           │   ├── repository/TaskRepository.java
│       │   │           │   ├── entity/Task.java
│       │   │           │   └── dto/
│       │   │           │       ├── TaskCreateRequest.java
│       │   │           │       ├── TaskUpdateRequest.java
│       │   │           │       └── TaskTreeResponse.java
│       │   │           ├── deliverable/
│       │   │           │   └── ...
│       │   │           ├── issue/
│       │   │           │   └── ...
│       │   │           ├── member/
│       │   │           │   └── ...
│       │   │           ├── timeoff/
│       │   │           │   └── ...
│       │   │           ├── notification/
│       │   │           │   └── ...
│       │   │           └── activity/
│       │   │               └── ...
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml   # 로컬 개발용 (git 제외)
│       │       └── db/
│       │           └── migration/          # Flyway 마이그레이션 스크립트
│       │               ├── V1__init_schema.sql
│       │               ├── V2__add_indexes.sql
│       │               └── V3__insert_master_data.sql
│       └── test/
│           └── java/
│               └── com/okepms/
│                   ├── domain/
│                   │   ├── auth/AuthServiceTest.java
│                   │   ├── project/ProjectServiceTest.java
│                   │   └── task/TaskProgressServiceTest.java
│                   └── integration/
│                       └── ProjectIntegrationTest.java
│
├── frontend/                            # Vue 3 프로젝트
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── tsconfig.app.json
│   ├── tsconfig.node.json
│   ├── index.html
│   ├── Dockerfile.dev
│   ├── .env                             # 기본 환경 변수
│   ├── .env.local                       # 로컬 환경 변수 (git 제외)
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── assets/
│       │   ├── images/
│       │   └── styles/
│       │       ├── main.css
│       │       └── variables.css
│       ├── components/
│       │   ├── common/
│       │   ├── project/
│       │   ├── task/
│       │   ├── deliverable/
│       │   ├── issue/
│       │   ├── member/
│       │   └── dashboard/
│       ├── views/
│       │   ├── auth/LoginView.vue
│       │   ├── dashboard/DashboardView.vue
│       │   ├── project/
│       │   ├── task/
│       │   ├── deliverable/
│       │   ├── issue/
│       │   ├── member/
│       │   ├── timeoff/
│       │   ├── notification/
│       │   └── admin/
│       ├── router/
│       │   └── index.ts
│       ├── stores/
│       │   ├── auth.ts
│       │   ├── project.ts
│       │   ├── task.ts
│       │   ├── notification.ts
│       │   └── ui.ts
│       ├── api/
│       │   ├── axios.ts
│       │   ├── auth.ts
│       │   ├── project.ts
│       │   ├── task.ts
│       │   ├── deliverable.ts
│       │   ├── issue.ts
│       │   ├── member.ts
│       │   ├── timeRecord.ts
│       │   └── notification.ts
│       ├── types/
│       │   ├── auth.ts
│       │   ├── project.ts
│       │   ├── task.ts
│       │   ├── deliverable.ts
│       │   ├── issue.ts
│       │   ├── member.ts
│       │   ├── timeRecord.ts
│       │   └── common.ts
│       └── utils/
│           ├── date.ts
│           ├── number.ts
│           ├── storage.ts
│           └── validator.ts
│
├── docker-compose.yml                   # 로컬 개발 환경 구성
├── .gitignore
└── README.md
```

---

## 부록

### A. API 엔드포인트 요약

| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/auth/login | 로그인 | 공개 |
| POST | /api/auth/logout | 로그아웃 | 인증 |
| POST | /api/auth/refresh | 토큰 갱신 | 인증 |
| GET | /api/users | 사용자 목록 | ADMIN |
| POST | /api/users | 사용자 생성 | ADMIN |
| GET | /api/companies | 회사 목록 | ADMIN |
| GET | /api/projects | 프로젝트 목록 | 인증 |
| POST | /api/projects | 프로젝트 생성 | PM, ADMIN |
| GET | /api/projects/{id} | 프로젝트 상세 | 인증 |
| PUT | /api/projects/{id} | 프로젝트 수정 | PM, ADMIN |
| GET | /api/projects/{id}/tasks | 업무 트리 조회 | 인증 |
| POST | /api/projects/{id}/tasks | 업무 생성 | PM, ADMIN |
| PUT | /api/tasks/{id}/progress | 진척률 업데이트 | 인증 |
| GET | /api/projects/{id}/members | 인력 목록 | 인증 |
| POST | /api/projects/{id}/members | 인력 배치 | PM, ADMIN |
| GET | /api/projects/{id}/deliverables | 산출물 목록 | 인증 |
| POST | /api/projects/{id}/deliverables | 산출물 등록 | 인증 |
| PUT | /api/deliverables/{id}/approve | 산출물 승인 | PM, ADMIN |
| GET | /api/projects/{id}/issues | 이슈 목록 | 인증 |
| POST | /api/projects/{id}/issues | 이슈 등록 | 인증 |
| GET | /api/notifications | 내 알림 목록 | 인증 |
| PUT | /api/notifications/{id}/read | 알림 읽음 처리 | 인증 |

### B. 오류 코드 정의

| 오류 코드 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| AUTH_INVALID_CREDENTIALS | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| AUTH_TOKEN_EXPIRED | 401 | 토큰이 만료되었습니다 |
| AUTH_TOKEN_INVALID | 401 | 유효하지 않은 토큰입니다 |
| AUTH_ACCESS_DENIED | 403 | 접근 권한이 없습니다 |
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없습니다 |
| USER_DUPLICATE_EMAIL | 409 | 이미 사용 중인 이메일입니다 |
| PROJECT_NOT_FOUND | 404 | 프로젝트를 찾을 수 없습니다 |
| TASK_NOT_FOUND | 404 | 업무를 찾을 수 없습니다 |
| TASK_INVALID_STATUS_TRANSITION | 400 | 허용되지 않는 상태 전환입니다 |
| DELIVERABLE_NOT_FOUND | 404 | 산출물을 찾을 수 없습니다 |
| ISSUE_NOT_FOUND | 404 | 이슈를 찾을 수 없습니다 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류가 발생했습니다 |

### C. 개발 컨벤션

#### 백엔드

- **패키지 구조:** 도메인 중심 패키지 구성 (domain 하위 각 도메인별 분리)
- **네이밍:** 클래스 PascalCase, 메서드/변수 camelCase, 상수 UPPER_SNAKE_CASE
- **REST API:** 복수 명사 사용 (`/projects`, `/tasks`), 동사는 HTTP 메서드로 표현
- **트랜잭션:** Service 레이어에서 `@Transactional` 적용, 읽기 전용은 `@Transactional(readOnly = true)`
- **소프트 딜리트:** `deleted = true` 처리, `@Where(clause = "deleted = false")` 글로벌 필터 적용

#### 프론트엔드

- **컴포넌트 네이밍:** PascalCase (예: `ProjectCard.vue`)
- **Composition API:** `<script setup>` 문법 사용
- **상태 관리:** 전역 상태는 Pinia, 로컬 상태는 `ref`/`reactive`
- **API 호출:** `src/api/` 모듈 통해서만 호출, 컴포넌트에서 직접 axios 사용 금지
- **타입:** 모든 API 응답 및 요청 DTO에 TypeScript 타입 정의 필수

---

*문서 버전: 1.0.0 | 최종 수정: 2026-06-15*
