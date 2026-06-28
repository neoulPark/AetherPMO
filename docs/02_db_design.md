# PMS 데이터베이스 설계 문서

> 버전: 1.0  
> 작성일: 2026-06-15  
> 작성자: 시스템 설계팀

---

## 목차

1. [개요](#1-개요)
2. [테이블 목록](#2-테이블-목록)
3. [ERD 관계도](#3-erd-관계도)
4. [테이블 상세 정의](#4-테이블-상세-정의)
5. [인덱스 목록](#5-인덱스-목록)
6. [주요 관계 설명](#6-주요-관계-설명)

---

## 1. 개요

본 문서는 PMS(Project Management System)의 데이터베이스 설계를 기술한다.  
PostgreSQL을 기반으로 설계되었으며, 프로젝트·업무·이슈·산출물·알림 등 PMS의 핵심 도메인을 포괄한다.

### 설계 원칙

- 모든 테이블에 감사 컬럼(created_at, updated_at, created_by, updated_by) 적용
- 소프트 삭제(is_active) 방식을 기본으로 채택
- 상태 코드는 PMS_STATUS 마스터 테이블로 중앙 관리
- 상태 전이 규칙은 PMS_STATUS_TRANSITION으로 정의
- 알림 규칙은 PMS_NOTIFICATION_RULE로 유연하게 설정

---

## 2. 테이블 목록

| 번호 | 테이블명 | 설명 |
|------|----------|------|
| 1 | PMS_USER | 시스템 사용자 |
| 2 | PMS_COMPANY | 회사(자사/파트너/고객) |
| 3 | PMS_PROJECT | 프로젝트 |
| 4 | PMS_TASK | 업무(WBS 계층 구조 지원) |
| 5 | PMS_TASK_ASSIGNMENT_HISTORY | 업무 담당자 변경 이력 |
| 6 | PMS_STATUS | 상태 마스터 |
| 7 | PMS_STATUS_TRANSITION | 상태 전이 규칙 |
| 8 | PMS_DELIVERABLE | 산출물 |
| 9 | PMS_ISSUE | 이슈/리스크 |
| 10 | PMS_ACTION_ITEM | 조치 항목 |
| 11 | PMS_PROJECT_MEMBER | 프로젝트 구성원 |
| 12 | PMS_MEMBER_TIME_OFF | 구성원 휴가/부재 |
| 13 | PMS_NOTIFICATION_RULE | 알림 규칙 |
| 14 | PMS_NOTIFICATION | 알림 |
| 15 | PMS_ACTIVITY_LOG | 활동 로그 |
| 16 | ~~PMS_DOCUMENT_TEMPLATE~~ | **폐기** → OPMS 카탈로그(pms_deliverable_template)로 통합 |
| 17 | ~~PMS_TEMPLATE_TAG_MAPPING~~ | **폐기** → pms_deliverable_tag_mapping로 대체 |
| 18 | PMS_MEETING | 회의록 관리 |
| 19 | PMS_OFFICIAL_DOC | 공문(수발신) 관리 |

#### OPMS 표준 방법론 카탈로그 (별도 문서: 05_methodology_catalog)

| 번호 | 테이블명 | 설명 |
|------|----------|------|
| C1 | PMS_METHODOLOGY_PHASE | 방법론 단계 (PRR/PRP/PPC/PED) |
| C2 | PMS_METHODOLOGY_ACTIVITY | 방법론 활동 (OP/PW/CT/TL/PM/CM/SM/IM/EE/IE) |
| C3 | PMS_TASK_TEMPLATE | Task 템플릿 (세부활동) |
| C4 | PMS_DELIVERABLE_TEMPLATE | 산출물 템플릿 (문서 자동화 통합) |
| C4-1 | PMS_DELIVERABLE_TAG_MAPPING | 산출물 템플릿 태그 매핑 |
| C5 | PMS_PROJECT_TAILORING | 프로젝트 테일러링 선택 내역 |

---

## 3. ERD 관계도

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              PMS ERD (논리 관계도)                               │
└─────────────────────────────────────────────────────────────────────────────────┘

  PMS_COMPANY                PMS_USER
  ┌──────────────┐           ┌──────────────┐
  │ company_id PK│           │ user_id PK   │
  │ company_name │           │ username     │
  │ company_type │           │ email        │
  │ is_active    │           │ role         │
  └──────┬───────┘           └──────┬───────┘
         │                          │
         │ 1                        │ 1
         │ ┌────────────────────────┤
         │ │                        │
         ▼ ▼                        │
  PMS_PROJECT                       │
  ┌──────────────────┐              │
  │ project_id PK    │              │
  │ project_name     │◄─────────────┘ (pm_id)
  │ project_code     │
  │ status           │
  │ pm_id FK         │
  │ client_company_id│
  └──────┬───────────┘
         │ 1
         ├──────────────────────────────────────────────┐
         │                                              │
         │ N                                            │ N
  ┌──────▼───────────┐              ┌───────────────────▼──┐
  │ PMS_TASK         │              │ PMS_PROJECT_MEMBER   │
  │ task_id PK       │              │ project_member_id PK │
  │ parent_task_id FK│◄─┐ (self)    │ project_id FK        │
  │ project_id FK    │  │           │ user_id FK           │
  │ task_name        │──┘           │ company_id FK        │
  │ status           │              │ role_type            │
  │ assignee_id FK   │              │ is_active            │
  │ depth            │              └──────────┬───────────┘
  └──────┬───────────┘                         │ 1
         │ 1                                   │ N
         ├──────────────┐             ┌────────▼───────────┐
         │              │             │ PMS_MEMBER_TIME_OFF │
         │ N            │ N           │ time_off_id PK     │
  ┌──────▼───────┐  ┌───▼──────────┐ │ project_member_id  │
  │PMS_DELIVERABLE│ │ PMS_ISSUE    │ │ off_date           │
  │deliverable_id│  │ issue_id PK  │ │ off_type           │
  │project_id FK │  │ project_id FK│ └────────────────────┘
  │task_id FK    │  │ task_id FK   │
  │status        │  │ issue_type   │
  │version_no    │  │ severity     │
  └──────────────┘  └──────┬───────┘
                            │ 1
                            │ N
                    ┌───────▼──────────┐
                    │ PMS_ACTION_ITEM  │
                    │ action_id PK     │
                    │ issue_id FK      │
                    │ assignee_id FK   │
                    │ status           │
                    └──────────────────┘

  PMS_STATUS                 PMS_STATUS_TRANSITION
  ┌──────────────┐           ┌─────────────────────────┐
  │ status_id PK │           │ transition_id PK        │
  │ domain       │           │ domain                  │
  │ status_code  │◄──────────│ from_status_code        │
  │ status_name  │◄──────────│ to_status_code          │
  │ is_initial   │           │ condition_json          │
  │ is_final     │           └─────────────────────────┘
  └──────────────┘

  PMS_NOTIFICATION_RULE      PMS_NOTIFICATION
  ┌──────────────────┐       ┌──────────────────────┐
  │ rule_id PK       │       │ notification_id PK   │
  │ project_id FK    │──── N │ rule_id FK (nullable)│
  │ rule_name        │       │ user_id FK           │
  │ rule_type        │       │ title                │
  │ target_type      │       │ is_read              │
  │ is_active        │       │ entity_type          │
  └──────────────────┘       │ entity_id            │
                             └──────────────────────┘

  PMS_ACTIVITY_LOG
  ┌──────────────────┐
  │ log_id PK        │
  │ entity_type      │       ※ 문서 템플릿 계열(PMS_DOCUMENT_TEMPLATE,
  │ entity_id        │          PMS_TEMPLATE_TAG_MAPPING)은 폐기되어
  │ project_id FK    │          OPMS 방법론 카탈로그로 통합됨.
  │ action           │          → 05_methodology_catalog 참조
  │ performed_by FK  │
  │ details (JSONB)  │
  └──────────────────┘

  PMS_TASK_ASSIGNMENT_HISTORY
  ┌─────────────────────────┐
  │ history_id PK           │
  │ task_id FK              │
  │ from_user_id FK         │
  │ to_user_id FK           │
  │ changed_by FK           │
  │ changed_at              │
  └─────────────────────────┘
```

---

## 4. 테이블 상세 정의

> 모든 테이블에는 다음 감사 컬럼이 포함된다.

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| created_at | TIMESTAMP | 생성일시 (DEFAULT NOW()) |
| updated_at | TIMESTAMP | 수정일시 (트리거로 자동 갱신) |
| created_by | BIGINT | 생성자 user_id |
| updated_by | BIGINT | 수정자 user_id |

---

### 4.1 PMS_USER (사용자)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| user_id | BIGSERIAL | PK | 사용자 고유 ID |
| username | VARCHAR(50) | UNIQUE NOT NULL | 로그인 아이디 |
| email | VARCHAR(100) | UNIQUE NOT NULL | 이메일 주소 |
| password | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 |
| full_name | VARCHAR(100) | | 사용자 실명 |
| role | VARCHAR(20) | CHECK ('ADMIN','PM','MEMBER') | 시스템 역할 |
| is_active | BOOLEAN | DEFAULT true | 활성 여부 |
| last_login_at | TIMESTAMP | | 최종 로그인 일시 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**역할 설명**
- ADMIN: 시스템 전체 관리자
- PM: 프로젝트 관리자
- MEMBER: 일반 팀원

---

### 4.2 PMS_COMPANY (회사)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| company_id | BIGSERIAL | PK | 회사 고유 ID |
| company_name | VARCHAR(200) | NOT NULL | 회사명 |
| company_type | VARCHAR(20) | CHECK ('OWN','PARTNER','CLIENT') | 회사 유형 |
| business_no | VARCHAR(20) | | 사업자등록번호 |
| contact_name | VARCHAR(100) | | 담당자명 |
| contact_email | VARCHAR(100) | | 담당자 이메일 |
| contact_phone | VARCHAR(20) | | 담당자 연락처 |
| is_active | BOOLEAN | DEFAULT true | 활성 여부 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**회사 유형 설명**
- OWN: 자사
- PARTNER: 협력사
- CLIENT: 고객사

---

### 4.3 PMS_PROJECT (프로젝트)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| project_id | BIGSERIAL | PK | 프로젝트 고유 ID |
| project_name | VARCHAR(200) | NOT NULL | 프로젝트명 |
| project_code | VARCHAR(50) | UNIQUE | 프로젝트 코드 |
| description | TEXT | | 프로젝트 설명 |
| pm_id | BIGINT | FK→PMS_USER | PM 사용자 ID |
| client_company_id | BIGINT | FK→PMS_COMPANY | 고객사 ID |
| status | VARCHAR(20) | CHECK ('PLANNING','IN_PROGRESS','ON_HOLD','COMPLETED','CANCELLED') | 프로젝트 상태 |
| planned_start_date | DATE | | 계획 시작일 |
| planned_end_date | DATE | | 계획 종료일 |
| actual_start_date | DATE | | 실제 시작일 |
| actual_end_date | DATE | | 실제 종료일 |
| total_budget | DECIMAL(15,2) | | 총 예산 |
| contract_amount | DECIMAL(15,2) | | 계약 금액 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**프로젝트 상태 흐름**
```
PLANNING → IN_PROGRESS → COMPLETED
                       → ON_HOLD → IN_PROGRESS
                       → CANCELLED
```

**입찰단계 전용 컬럼 (v1.1 추가)**

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| project_stage | VARCHAR(20) | CHECK ('BIDDING','EXECUTION','COMPLETED') DEFAULT 'EXECUTION' | 프로젝트 단계 구분 |
| bid_status | VARCHAR(20) | CHECK ('PREPARING','SUBMITTED','WAITING','WON','LOST') | 입찰 상태 |
| consortium_role | VARCHAR(100) | | 컨소시엄 역할 |
| consortium_share | DECIMAL(5,2) | | 컨소시엄 지분율(%) |
| vrb_status | VARCHAR(20) | | VRB 상태 |
| announcement_no | VARCHAR(100) | | 나라장터 공고번호 |
| proposal_deadline | DATE | | 제안서 제출마감일 |
| risk_level | VARCHAR(10) | CHECK ('높음','보통','낮음') DEFAULT '보통' | 사업 위험도 |

---

### 4.4 PMS_TASK (업무)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| task_id | BIGSERIAL | PK | 업무 고유 ID |
| parent_task_id | BIGINT | FK→PMS_TASK (nullable) | 상위 업무 ID (계층 구조) |
| project_id | BIGINT | FK→PMS_PROJECT NOT NULL | 소속 프로젝트 |
| task_name | VARCHAR(300) | NOT NULL | 업무명 |
| task_type | VARCHAR(20) | DEFAULT 'TASK' | 업무 유형 (TASK/MILESTONE/PHASE) |
| status | VARCHAR(20) | CHECK ('TODO','IN_PROGRESS','REVIEW','REJECTED','DONE') | 업무 상태 |
| progress_rate | INT | DEFAULT 0, CHECK (0~100) | 진행률(%) |
| assignee_id | BIGINT | FK→PMS_USER | 담당자 ID |
| planned_start_date | DATE | | 계획 시작일 |
| planned_end_date | DATE | | 계획 종료일 |
| actual_start_date | DATE | | 실제 시작일 |
| actual_end_date | DATE | | 실제 종료일 |
| planned_effort | DECIMAL(10,2) | | 계획 공수(MM) |
| actual_effort | DECIMAL(10,2) | | 실적 공수(MM) |
| depth | INT | DEFAULT 0 | WBS 계층 깊이 (0=최상위) |
| sort_order | INT | DEFAULT 0 | 동일 레벨 내 정렬 순서 |
| description | TEXT | | 업무 상세 설명 |
| delay_days | INT | GENERATED ALWAYS | 지연일수 (계산 컬럼) |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

> `delay_days`는 생성 컬럼(GENERATED ALWAYS AS)으로, `status != 'DONE' AND planned_end_date < CURRENT_DATE` 일 때 `CURRENT_DATE - planned_end_date`, 아니면 0을 반환한다.

**업무 상태 흐름**
```
TODO → IN_PROGRESS → REVIEW → DONE
                   ↑    └→ REJECTED → IN_PROGRESS
```

---

### 4.5 PMS_TASK_ASSIGNMENT_HISTORY (업무 담당자 변경 이력)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| history_id | BIGSERIAL | PK | 이력 고유 ID |
| task_id | BIGINT | FK→PMS_TASK NOT NULL | 업무 ID |
| from_user_id | BIGINT | FK→PMS_USER (nullable) | 이전 담당자 (최초 배정 시 NULL) |
| to_user_id | BIGINT | FK→PMS_USER NOT NULL | 신규 담당자 |
| changed_by | BIGINT | FK→PMS_USER NOT NULL | 변경자 |
| change_reason | TEXT | | 변경 사유 |
| changed_at | TIMESTAMP | DEFAULT NOW() | 변경 일시 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

---

### 4.6 PMS_STATUS (상태 마스터)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| status_id | BIGSERIAL | PK | 상태 고유 ID |
| domain | VARCHAR(30) | NOT NULL | 적용 도메인 (TASK/DELIVERABLE/ISSUE/PROJECT) |
| status_code | VARCHAR(50) | NOT NULL | 상태 코드 |
| status_name | VARCHAR(100) | NOT NULL | 상태명(표시용) |
| is_initial | BOOLEAN | DEFAULT false | 초기 상태 여부 |
| is_final | BOOLEAN | DEFAULT false | 최종 상태 여부 |
| sort_order | INT | DEFAULT 0 | 정렬 순서 |
| color_code | VARCHAR(10) | | UI 표시 색상 (예: #FF5733) |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**복합 유니크**: `UNIQUE(domain, status_code)`

---

### 4.7 PMS_STATUS_TRANSITION (상태 전이 규칙)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| transition_id | BIGSERIAL | PK | 전이 규칙 고유 ID |
| domain | VARCHAR(30) | NOT NULL | 적용 도메인 |
| from_status_code | VARCHAR(50) | NOT NULL | 전이 시작 상태 코드 |
| to_status_code | VARCHAR(50) | NOT NULL | 전이 대상 상태 코드 |
| condition_json | JSONB | | 전이 조건 트리 (JSON) |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**복합 유니크**: `UNIQUE(domain, from_status_code, to_status_code)`

---

### 4.8 PMS_DELIVERABLE (산출물)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| deliverable_id | BIGSERIAL | PK | 산출물 고유 ID |
| project_id | BIGINT | FK→PMS_PROJECT NOT NULL | 소속 프로젝트 |
| task_id | BIGINT | FK→PMS_TASK (nullable) | 연관 업무 |
| deliverable_name | VARCHAR(300) | NOT NULL | 산출물명 |
| deliverable_type | VARCHAR(50) | | 산출물 유형 (설계서/보고서/계획서 등) |
| status | VARCHAR(20) | CHECK ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED') | 산출물 상태 |
| version_no | VARCHAR(20) | DEFAULT '1.0' | 버전 번호 |
| file_path | VARCHAR(500) | | 파일 저장 경로 |
| file_name | VARCHAR(300) | | 원본 파일명 |
| file_size | BIGINT | | 파일 크기(bytes) |
| submitted_by | BIGINT | FK→PMS_USER | 제출자 |
| submitted_at | TIMESTAMP | | 제출 일시 |
| reviewed_by | BIGINT | FK→PMS_USER | 검토자 |
| reviewed_at | TIMESTAMP | | 검토 일시 |
| review_comment | TEXT | | 검토 의견 |
| approved_by | BIGINT | FK→PMS_USER | 승인자 |
| approved_at | TIMESTAMP | | 승인 일시 |
| approval_comment | TEXT | | 승인 의견 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**산출물 상태 흐름**
```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                               └→ REJECTED → DRAFT (재작성)
```

---

### 4.9 PMS_ISSUE (이슈/리스크)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| issue_id | BIGSERIAL | PK | 이슈 고유 ID |
| project_id | BIGINT | FK→PMS_PROJECT NOT NULL | 소속 프로젝트 |
| task_id | BIGINT | FK→PMS_TASK (nullable) | 연관 업무 |
| issue_type | VARCHAR(20) | CHECK ('ISSUE','RISK') | 이슈 유형 |
| title | VARCHAR(300) | NOT NULL | 이슈 제목 |
| description | TEXT | | 이슈 상세 설명 |
| severity | VARCHAR(10) | CHECK ('HIGH','MEDIUM','LOW') | 심각도 |
| status | VARCHAR(20) | CHECK ('OPEN','IN_PROGRESS','RESOLVED','CLOSED') | 이슈 상태 |
| assignee_id | BIGINT | FK→PMS_USER | 담당자 |
| reporter_id | BIGINT | FK→PMS_USER | 보고자 |
| due_date | DATE | | 처리 기한 |
| resolved_at | TIMESTAMP | | 해결 일시 |
| resolution | TEXT | | 해결 내용 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

---

### 4.10 PMS_ACTION_ITEM (조치 항목)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| action_id | BIGSERIAL | PK | 조치 항목 고유 ID |
| issue_id | BIGINT | FK→PMS_ISSUE (nullable) | 연관 이슈 (NULL 허용 - 독립적 Action Item 가능) |
| description | TEXT | NOT NULL | 조치 내용 |
| assignee_id | BIGINT | FK→PMS_USER | 담당자 |
| due_date | DATE | | 완료 기한 |
| status | VARCHAR(20) | CHECK ('TODO','IN_PROGRESS','DONE') | 진행 상태 |
| completed_at | TIMESTAMP | | 완료 일시 |
| result_description | TEXT | | 결과 설명 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

---

### 4.11 PMS_PROJECT_MEMBER (프로젝트 구성원)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| project_member_id | BIGSERIAL | PK | 구성원 고유 ID |
| project_id | BIGINT | FK→PMS_PROJECT NOT NULL | 프로젝트 |
| user_id | BIGINT | FK→PMS_USER NOT NULL | 사용자 |
| company_id | BIGINT | FK→PMS_COMPANY | 소속 회사 |
| role_type | VARCHAR(50) | NOT NULL | 프로젝트 역할 |
| allocation_rate | DECIMAL(5,2) | DEFAULT 100.00 | 투입률(%) |
| join_date | DATE | NOT NULL | 합류일 |
| leave_date | DATE | | 이탈일 |
| is_active | BOOLEAN | DEFAULT true | 활성 여부 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**부분 유니크 인덱스**: `UNIQUE(project_id, user_id) WHERE is_active = true`

**역할 유형 예시**
- PM, TECH_LEAD, DEVELOPER, ANALYST, QA, DESIGNER, CONSULTANT

---

### 4.12 PMS_MEMBER_TIME_OFF (구성원 휴가/부재)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| time_off_id | BIGSERIAL | PK | 휴가 고유 ID |
| project_member_id | BIGINT | FK→PMS_PROJECT_MEMBER NOT NULL | 프로젝트 구성원 |
| off_date | DATE | NOT NULL | 휴가 날짜 |
| off_type | VARCHAR(20) | CHECK ('ANNUAL','HALF','QUARTER','SICK','PUBLIC','COMPENSATORY') | 휴가 유형 |
| off_hours | DECIMAL(4,2) | NOT NULL, CHECK (>0 AND <=8) | 휴가 시간 |
| reason | TEXT | | 휴가 사유 |
| approval_status | VARCHAR(20) | CHECK ('PENDING','APPROVED','REJECTED') | 승인 상태 |
| approved_by | BIGINT | FK→PMS_USER | 승인자 |
| approved_at | TIMESTAMP | | 승인 일시 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**휴가 유형 설명**
- ANNUAL: 연차, HALF: 반차, QUARTER: 반반차, SICK: 병가, PUBLIC: 공휴일, COMPENSATORY: 대체휴가

---

### 4.13 PMS_NOTIFICATION_RULE (알림 규칙)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| rule_id | BIGSERIAL | PK | 규칙 고유 ID |
| project_id | BIGINT | FK→PMS_PROJECT (nullable) | 프로젝트 (NULL=전역 규칙) |
| rule_name | VARCHAR(200) | NOT NULL | 규칙명 |
| rule_type | VARCHAR(50) | NOT NULL | 규칙 유형 |
| target_type | VARCHAR(20) | NOT NULL | 알림 대상 유형 |
| condition_json | JSONB | | 조건 정의 JSON |
| is_active | BOOLEAN | DEFAULT true | 활성 여부 |
| advance_days | INT | DEFAULT 3 | 마감 N일 전 알림 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

**rule_type 유형**
- DELAY: 지연 감지
- APPROVAL_PENDING: 승인 대기
- DELIVERABLE_MISSING: 산출물 미제출
- DEADLINE_APPROACHING: 마감 임박
- STATUS_CHANGE: 상태 변경

**target_type 유형**
- ASSIGNEE: 담당자, PM: 프로젝트 관리자, REVIEWER: 검토자, ALL: 전체

---

### 4.14 PMS_NOTIFICATION (알림)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| notification_id | BIGSERIAL | PK | 알림 고유 ID |
| rule_id | BIGINT | FK→PMS_NOTIFICATION_RULE (nullable) | 생성 규칙 |
| user_id | BIGINT | FK→PMS_USER NOT NULL | 수신자 |
| title | VARCHAR(300) | NOT NULL | 알림 제목 |
| message | TEXT | | 알림 내용 |
| notification_type | VARCHAR(50) | | 알림 유형 |
| entity_type | VARCHAR(50) | | 연관 엔티티 유형 (TASK/ISSUE/DELIVERABLE 등) |
| entity_id | BIGINT | | 연관 엔티티 ID |
| is_read | BOOLEAN | DEFAULT false | 읽음 여부 |
| read_at | TIMESTAMP | | 읽은 일시 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| created_by | BIGINT | | 생성자 |
| updated_by | BIGINT | | 수정자 |

---

### 4.15 PMS_ACTIVITY_LOG (활동 로그)

> 이 테이블은 불변(immutable) 로그이므로 `updated_at`, `updated_by`, `created_by`를 제외한다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| log_id | BIGSERIAL | PK | 로그 고유 ID |
| entity_type | VARCHAR(50) | NOT NULL | 대상 엔티티 유형 |
| entity_id | BIGINT | NOT NULL | 대상 엔티티 ID |
| project_id | BIGINT | FK→PMS_PROJECT (nullable) | 관련 프로젝트 |
| action | VARCHAR(100) | NOT NULL | 수행 액션 (CREATE/UPDATE/DELETE/STATUS_CHANGE 등) |
| performed_by | BIGINT | FK→PMS_USER NOT NULL | 수행자 |
| details | JSONB | | 변경 상세 (이전값/이후값 등) |
| ip_address | VARCHAR(45) | | 접속 IP (IPv6 포함) |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |

---

### 4.16 PMS_DOCUMENT_TEMPLATE (문서 템플릿) — **폐기됨**

> OPMS 표준 방법론 카탈로그 도입으로 폐기되었다.
> 문서 자동화(태그 치환) 기능은 `PMS_DELIVERABLE_TEMPLATE`의
> `file_path` / `file_name` / `template_tags` 컬럼으로 흡수되었다.
> 상세 정의는 `05_methodology_catalog`(C4) 참조.

---

### 4.17 PMS_TEMPLATE_TAG_MAPPING (템플릿 태그 매핑) — **폐기됨**

> `PMS_DELIVERABLE_TAG_MAPPING`(C4-1)로 대체되었다.
> 상세 정의는 `05_methodology_catalog` 참조.

---

### 4.18 PMS_MEETING (회의록)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| meeting_id | BIGSERIAL | PK | 회의록 고유 ID |
| project_id | BIGINT | FK→PMS_PROJECT NOT NULL | 소속 프로젝트 |
| title | VARCHAR(300) | NOT NULL | 회의 제목 |
| meeting_type | VARCHAR(50) | NOT NULL | 회의 유형 (의사결정 회의/정기 회의/킥오프/검토 회의 등) |
| meeting_date | TIMESTAMP | NOT NULL | 회의 일시 |
| location | VARCHAR(300) | | 회의 장소 |
| attendees | TEXT | | 참석자 목록 (쉼표 구분 또는 JSON) |
| agenda | TEXT | | 회의 안건 |
| minutes | TEXT | | 회의록 본문 |
| status | VARCHAR(20) | CHECK ('DRAFT','COMPLETED','CANCELLED') DEFAULT 'DRAFT' | 회의록 상태 |
| author_id | BIGINT | FK→PMS_USER (nullable) | 작성자 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT NOW() | 수정일시 |
| created_by | BIGINT | FK→PMS_USER | 생성자 |
| updated_by | BIGINT | FK→PMS_USER | 수정자 |

**인덱스**: `idx_pms_meeting_project(project_id)`, `idx_pms_meeting_date(meeting_date)`, `idx_pms_meeting_status(status)`

---

### 4.19 PMS_OFFICIAL_DOC (공문)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| doc_id | BIGSERIAL | PK | 공문 고유 ID |
| project_id | BIGINT | FK→PMS_PROJECT NOT NULL | 소속 프로젝트 |
| doc_no | VARCHAR(100) | UNIQUE | 품의번호 (예: OKE-202603-000312) |
| title | VARCHAR(500) | NOT NULL | 공문 제목 |
| direction | VARCHAR(10) | CHECK ('INBOUND','OUTBOUND') NOT NULL | 수발신 구분 |
| sender_org | VARCHAR(200) | | 발신 기관 |
| receiver_org | VARCHAR(200) | | 수신 기관 |
| drafter_id | BIGINT | FK→PMS_USER (nullable) | 기안자 |
| draft_dept | VARCHAR(100) | | 기안부서 |
| sent_date | DATE | | 발신일/시행일자 |
| approval_status | VARCHAR(20) | CHECK ('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING' | 결재 상태 |
| review_status | VARCHAR(20) | CHECK ('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' | 협의 상태 |
| attachment_count | INT | DEFAULT 0 | 첨부파일 수 |
| content | TEXT | | 공문 내용 |
| created_at | TIMESTAMP | DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT NOW() | 수정일시 |
| created_by | BIGINT | FK→PMS_USER | 생성자 |
| updated_by | BIGINT | FK→PMS_USER | 수정자 |

**direction 설명**
- INBOUND: 수신 공문
- OUTBOUND: 발신 공문

**인덱스**: `idx_pms_official_doc_project(project_id)`, `idx_pms_official_doc_no(doc_no)`, `idx_pms_official_doc_approval(approval_status)`, `idx_pms_official_doc_direction(direction)`

---

## 5. 인덱스 목록

### 5.1 기본 인덱스 (FK, 상태, 날짜 컬럼)

| 인덱스명 | 테이블 | 컬럼 | 유형 | 목적 |
|----------|--------|------|------|------|
| idx_project_pm | PMS_PROJECT | pm_id | BTREE | PM별 프로젝트 조회 |
| idx_project_client | PMS_PROJECT | client_company_id | BTREE | 고객사별 프로젝트 조회 |
| idx_project_status | PMS_PROJECT | status | BTREE | 상태별 프로젝트 필터 |
| idx_project_dates | PMS_PROJECT | planned_start_date, planned_end_date | BTREE | 기간별 프로젝트 조회 |
| idx_task_project | PMS_TASK | project_id | BTREE | 프로젝트별 업무 조회 |
| idx_task_parent | PMS_TASK | parent_task_id | BTREE | 하위 업무 조회 |
| idx_task_assignee | PMS_TASK | assignee_id | BTREE | 담당자별 업무 조회 |
| idx_task_status | PMS_TASK | status | BTREE | 상태별 업무 필터 |
| idx_task_dates | PMS_TASK | planned_end_date | BTREE | 기한 관리 |
| idx_task_history_task | PMS_TASK_ASSIGNMENT_HISTORY | task_id | BTREE | 업무별 이력 조회 |
| idx_task_history_from | PMS_TASK_ASSIGNMENT_HISTORY | from_user_id | BTREE | 이전 담당자별 조회 |
| idx_task_history_to | PMS_TASK_ASSIGNMENT_HISTORY | to_user_id | BTREE | 신규 담당자별 조회 |
| idx_status_domain | PMS_STATUS | domain | BTREE | 도메인별 상태 조회 |
| idx_deliverable_project | PMS_DELIVERABLE | project_id | BTREE | 프로젝트별 산출물 |
| idx_deliverable_task | PMS_DELIVERABLE | task_id | BTREE | 업무별 산출물 |
| idx_deliverable_status | PMS_DELIVERABLE | status | BTREE | 상태별 산출물 |
| idx_issue_project | PMS_ISSUE | project_id | BTREE | 프로젝트별 이슈 |
| idx_issue_task | PMS_ISSUE | task_id | BTREE | 업무별 이슈 |
| idx_issue_assignee | PMS_ISSUE | assignee_id | BTREE | 담당자별 이슈 |
| idx_issue_status | PMS_ISSUE | status | BTREE | 상태별 이슈 |
| idx_action_issue | PMS_ACTION_ITEM | issue_id | BTREE | 이슈별 조치 항목 |
| idx_action_assignee | PMS_ACTION_ITEM | assignee_id | BTREE | 담당자별 조치 항목 |
| idx_member_project | PMS_PROJECT_MEMBER | project_id | BTREE | 프로젝트별 구성원 |
| idx_member_user | PMS_PROJECT_MEMBER | user_id | BTREE | 사용자별 프로젝트 |
| idx_member_company | PMS_PROJECT_MEMBER | company_id | BTREE | 회사별 구성원 |
| idx_timeoff_member | PMS_MEMBER_TIME_OFF | project_member_id | BTREE | 구성원별 휴가 |
| idx_timeoff_date | PMS_MEMBER_TIME_OFF | off_date | BTREE | 날짜별 휴가 조회 |
| idx_noti_rule_project | PMS_NOTIFICATION_RULE | project_id | BTREE | 프로젝트별 알림 규칙 |
| idx_noti_user | PMS_NOTIFICATION | user_id | BTREE | 수신자별 알림 |
| idx_noti_rule | PMS_NOTIFICATION | rule_id | BTREE | 규칙별 알림 |
| idx_actlog_entity | PMS_ACTIVITY_LOG | entity_type, entity_id | BTREE | 엔티티별 로그 |
| idx_actlog_project | PMS_ACTIVITY_LOG | project_id | BTREE | 프로젝트별 로그 |
| idx_actlog_performed | PMS_ACTIVITY_LOG | performed_by | BTREE | 수행자별 로그 |
| idx_actlog_created | PMS_ACTIVITY_LOG | created_at | BTREE | 시간순 로그 조회 |
| (방법론 카탈로그 인덱스는 05_methodology_catalog 참조) | — | — | — | — |

### 5.2 부분 인덱스 (Partial Index)

| 인덱스명 | 테이블 | 컬럼 | 조건 | 목적 |
|----------|--------|------|------|------|
| idx_user_active | PMS_USER | user_id | WHERE is_active = true | 활성 사용자만 검색 |
| idx_project_member_active_unique | PMS_PROJECT_MEMBER | project_id, user_id | WHERE is_active = true | 활성 구성원 유니크 보장 |
| idx_noti_unread | PMS_NOTIFICATION | user_id, created_at | WHERE is_read = false | 읽지 않은 알림 빠른 조회 |
| idx_task_delayed | PMS_TASK | project_id, planned_end_date | WHERE status != 'DONE' | 지연 업무 조회 최적화 |
| idx_noti_rule_active | PMS_NOTIFICATION_RULE | rule_type | WHERE is_active = true | 활성 알림 규칙만 처리 |

---

## 6. 주요 관계 설명

### 6.1 사용자 ↔ 프로젝트 (N:M)
- PMS_USER와 PMS_PROJECT는 PMS_PROJECT_MEMBER를 통해 다대다 관계
- 한 사용자는 여러 프로젝트에 참여 가능
- 한 프로젝트에 여러 사용자 참여 가능
- 프로젝트별 역할(role_type)과 투입률(allocation_rate) 별도 관리
- `is_active=true` 조건의 부분 유니크 인덱스로 동일 프로젝트 내 중복 활성 구성원 방지

### 6.2 업무 계층 구조 (자기 참조)
- PMS_TASK의 `parent_task_id`가 동일 테이블의 `task_id`를 참조
- `depth` 컬럼으로 계층 깊이 추적 (0=최상위 Phase, 1=Task, 2=Sub-Task 등)
- `sort_order`로 동일 레벨 내 순서 관리
- WBS(Work Breakdown Structure) 구조 표현 가능

### 6.3 상태 관리 (도메인별 중앙화)
- PMS_STATUS: 도메인별(TASK/PROJECT/DELIVERABLE/ISSUE) 상태 마스터 관리
- PMS_STATUS_TRANSITION: 허용된 상태 전이 경로 정의
- `condition_json` JSONB 컬럼으로 복잡한 전이 조건 표현 가능
- 각 도메인 테이블의 status 컬럼은 CHECK 제약으로도 보호

### 6.4 산출물 워크플로우
- DRAFT(초안) → SUBMITTED(제출) → UNDER_REVIEW(검토중) → APPROVED(승인)/REJECTED(반려)
- submitted_by/submitted_at, reviewed_by/reviewed_at, approved_by/approved_at으로 각 단계 추적
- task_id는 nullable: 업무와 독립적인 프로젝트 수준 산출물도 관리 가능

### 6.5 이슈 ↔ 조치 항목 (1:N)
- 하나의 이슈에 여러 조치 항목(PMS_ACTION_ITEM) 연결
- 이슈가 RESOLVED/CLOSED 되기 위해 연관 조치 항목 완료 여부 체크 가능
- ISSUE와 RISK를 동일 테이블에서 `issue_type`으로 구분

### 6.6 알림 규칙 ↔ 알림 (1:N)
- PMS_NOTIFICATION_RULE: 알림 발생 조건 정의 (전역 또는 프로젝트 특정)
- PMS_NOTIFICATION: 실제 발송된 알림 기록 (rule_id는 nullable — 수동 알림도 가능)
- 스케줄러가 활성 규칙을 주기적으로 평가하여 알림 생성

### 6.7 담당자 변경 추적
- PMS_TASK_ASSIGNMENT_HISTORY로 업무 담당자 변경 이력 완전 추적
- `from_user_id` NULL 허용 (최초 배정 시)
- `changed_by`로 누가 변경했는지 기록

### 6.8 활동 로그 (감사 추적)
- PMS_ACTIVITY_LOG는 모든 엔티티의 생성/수정/삭제/상태변경 이력 기록
- `entity_type + entity_id`로 어떤 객체에 대한 로그인지 식별
- `details JSONB`로 변경 전후 값 등 상세 정보 보관
- 불변(append-only) 테이블: updated_at, updated_by 없음

### 6.9 OPMS 방법론 카탈로그 ↔ 프로젝트 테일러링
- 표준 방법론 카탈로그(C1~C4-1)는 마스터 데이터로, 단계→활동→Task템플릿→산출물템플릿 4계층 구성
- 신규 프로젝트 생성 시 PMS_PROJECT_TAILORING(C5)에 채택/제외 내역을 기록(테일러링)
- 채택된 Task템플릿/산출물템플릿은 실제 PMS_TASK / PMS_DELIVERABLE로 인스턴스화
  (PMS_TASK.task_template_id, PMS_DELIVERABLE.deliverable_template_id로 출처 추적)
- 문서 자동화(태그 치환)는 PMS_DELIVERABLE_TEMPLATE의 file_path/template_tags +
  PMS_DELIVERABLE_TAG_MAPPING(data_source: "pms_project.project_name" 형식)로 통합 관리
- 상세 정의는 별도 문서 `05_methodology_catalog` 참조
