# 08. AetherPMO 백엔드 현행 명세서 (재개발용 청사진)

> 본 문서는 `/backend` 디렉토리의 **실제 코드(소스/마이그레이션)** 를 기준으로 작성한 현행
> 백엔드 명세입니다. 설계 문서 01~07 은 일부 구버전이며, 충돌 시 **본 문서가 우선**합니다.
> 목적: 동일 백엔드를 다른 환경/컨벤션에서 **동일 동작으로 재구현**하기 위한 청사진.
> 기준 코드 패키지 루트: `com.aetherpmo`

---

## 1. 개요

### 1.1 기술 스택 (버전)

| 항목 | 값 |
|------|----|
| 빌드 | Maven (`spring-boot-starter-parent` 3.2.5) |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.2.5 (Web, Data JPA, Security, Validation) |
| ORM | Hibernate (JPA), `ddl-auto: validate` |
| DB | PostgreSQL (드라이버 `org.postgresql:postgresql`, runtime) |
| 마이그레이션 | Flyway 10.10.0 (`flyway-core` + `flyway-database-postgresql`) |
| 보조 | Lombok (`@RequiredArgsConstructor`, `@Getter` 등, optional/build-excluded) |
| 인증 | Spring Security + 커스텀 Mock 필터 (Stateless) |
| 테스트 | spring-boot-starter-test, spring-security-test |

groupId `com.aetherpmo`, artifactId `aetherpmo-backend`, version `0.0.1-SNAPSHOT`.
메인 클래스: `com.aetherpmo.AetherPmoApplication`.

### 1.2 아키텍처 (레이어드 + 도메인 패키지)

```
com.aetherpmo
├── AetherPmoApplication            (@SpringBootApplication 진입점)
├── common/                         공통 (ApiResponse, ApiError, BaseEntity, GlobalExceptionHandler, HealthController)
├── config/                         SecurityConfig, CorsConfig, JpaConfig, DatabaseUrlPostProcessor
├── security/                       MockAuthFilter, CurrentUser
├── auth/                           AuthController, MockTokenService, UserEntity, UserRepository, dto/
├── adapter/amaranth/              외부연동 포트(UserPort/FilePort/TokenValidator) + dto/ + mock/
└── domain/
    ├── project/                    Project, ProjectController/Service/Repository, dto/
    ├── task/                       Task, TaskAssignmentHistory, Controller/Service/Repository, dto/
    ├── deliverable/                Deliverable, Attachment, Controller/Service/Repository, dto/
    ├── methodology/                CatalogNode, MethodologyController/Service/Repository, dto/
    ├── workflow/                   Workflow/WorkflowStatus/WorkflowTransition, Controller/Service/3 Repository, dto/
    ├── tailoring/                  ProjectTailoring, TailoringController/Service/Repository, dto/
    ├── contact/                    ContactPoint, Controller/Service/Repository, dto/
    ├── company/                    CompanyNameRepository (EntityManager 기반 단순 조회)
    └── user/                       UserController (내부 사용자 검색)
```

레이어 흐름: **Controller (REST, DTO 변환) → Service (트랜잭션·비즈니스 로직) → Repository (Spring Data JPA) → Entity**.
DTO 는 모두 Java `record`. 엔티티→DTO 변환은 DTO의 정적 팩토리 `from(...)` 에서 수행.

### 1.3 공통 규약

#### ApiResponse 래퍼

모든 정상 응답(헬스체크 제외)은 `ApiResponse<T>` 로 감싼다.

```java
record ApiResponse<T>(boolean success, T data, String message, Instant timestamp)
```

성공 예시:
```json
{
  "success": true,
  "data": { "id": 1, "projectName": "..." },
  "message": "Project created",
  "timestamp": "2026-06-29T01:23:45.678Z"
}
```

실패 예시 (`ApiResponse.fail`):
```json
{
  "success": false,
  "data": null,
  "message": "Project not found: 99",
  "timestamp": "2026-06-29T01:23:45.678Z"
}
```

생성자: `ApiResponse.ok(data)`, `ApiResponse.ok(data, message)`, `ApiResponse.fail(message)`.
**예외**: `HealthController` 만 `Map<String,String>`(`{"status":"UP"}`)를 래퍼 없이 반환.

#### 예외 처리 (`GlobalExceptionHandler`, `@RestControllerAdvice`)

| 예외 | HTTP 상태 | 본문 |
|------|-----------|------|
| `ApiError` (status 내장) | `ex.getStatus()` (404/400/401) | `ApiResponse.fail(message)` |
| `MethodArgumentNotValidException` | 400 | `"필드: 메시지"` (첫 필드 오류) |
| `IllegalArgumentException` | 400 | `ApiResponse.fail(message)` |
| `IllegalStateException` | 409 (CONFLICT) | `ApiResponse.fail(message)` |
| 그 외 `Exception` | 500 | `ApiResponse.fail(message)` |

`ApiError` 는 `RuntimeException` + `HttpStatus`. 팩토리: `notFound`(404), `badRequest`(400), `unauthorized`(401).
**관례**: 도메인 not-found/검증은 `ApiError`, 카탈로그/워크플로 검증은 `IllegalArgumentException`, 산출물 상태 위반은 `IllegalStateException`(→409).

#### BaseEntity (JPA 감사)

```java
@MappedSuperclass @EntityListeners(AuditingEntityListener.class)
abstract class BaseEntity {
  @CreatedDate @Column(name="created_at", updatable=false) LocalDateTime createdAt;
  @LastModifiedDate @Column(name="updated_at") LocalDateTime updatedAt;
}
```

`JpaConfig` 에 `@EnableJpaAuditing` 적용. 모든 엔티티가 `BaseEntity` 상속 →
`created_at`/`updated_at` 은 애플리케이션이 채움(DB 트리거 미사용). `created_by`/`updated_by` 는
감사 미연동이며 서비스에서 `currentUser.idOrNull()` 로 수동 세팅.

#### 스키마 관리 정책

- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate 가 스키마를 **생성하지 않고 검증만** 함.
  엔티티 매핑과 실제 테이블이 정확히 일치해야 부팅된다.
- 실제 스키마는 **Flyway 마이그레이션(V1~V10)** 이 단독 소유. `baseline-on-migrate: true`.
- `open-in-view: false`, `show-sql: false`.

---

## 2. 데이터 모델 (V1~V10 합친 최종 스키마)

> 모든 테이블 접두사 `pms_`. PK 는 `BIGSERIAL`. 타임스탬프는 `created_at DEFAULT NOW()`,
> `updated_at` (JPA 가 갱신). 아래는 V10 까지 ALTER 가 모두 반영된 **최종 유효 스키마**.

### 2.1 pms_user (사용자)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| user_id | BIGSERIAL | PK |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(100) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | (bcrypt) |
| full_name | VARCHAR(100) | |
| role | VARCHAR(20) | CHECK IN ('ADMIN','PM','MEMBER') |
| is_active | BOOLEAN | DEFAULT true |
| created_at / updated_at | TIMESTAMP | |

### 2.2 pms_company (회사)

| 컬럼 | 타입 | 제약 |
|------|------|------|
| company_id | BIGSERIAL | PK |
| company_name | VARCHAR(200) | NOT NULL |
| company_type | VARCHAR(20) | CHECK IN ('OWN','PARTNER','CLIENT') |
| is_active | BOOLEAN | DEFAULT true |
| created_at / updated_at | TIMESTAMP | |

### 2.3 pms_project (프로젝트)

| 컬럼 | 타입 | 제약/기본 |
|------|------|-----------|
| project_id | BIGSERIAL | PK |
| project_name | VARCHAR(200) | NOT NULL |
| project_code | VARCHAR(50) | UNIQUE |
| description | TEXT | |
| pm_id | BIGINT | FK→pms_user(user_id) |
| client_company_id | BIGINT | FK→pms_company(company_id) |
| status | VARCHAR(20) | CHECK IN ('PLANNING','IN_PROGRESS','ON_HOLD','COMPLETED','CANCELLED') DEFAULT 'PLANNING' |
| project_stage | VARCHAR(20) | CHECK IN ('BIDDING','EXECUTION','COMPLETED') DEFAULT 'EXECUTION' |
| planned_start_date / planned_end_date | DATE | |
| actual_start_date / actual_end_date | DATE | |
| contract_amount | DECIMAL(15,2) | |
| progress_rate | INT | DEFAULT 0 |
| risk_level | VARCHAR(10) | DEFAULT '보통' |
| team | VARCHAR(100) | |
| location | VARCHAR(200) | |
| business_type | VARCHAR(100) | |
| **bid_status** | VARCHAR(20) | (V9) PREPARING/SUBMITTED/WAITING/WON/LOST |
| **consortium_role** | VARCHAR(100) | (V9) |
| **consortium_share** | DECIMAL(5,2) | (V9) |
| **vrb_status** | VARCHAR(50) | (V9) |
| **announcement_no** | VARCHAR(100) | (V9) |
| **proposal_deadline** | DATE | (V9) |
| created_at/updated_at/created_by/updated_by | | created_by/updated_by 는 BIGINT |

인덱스: `idx_project_pm`, `idx_project_client`, `idx_project_status`, `idx_project_stage`.

### 2.4 pms_task (업무 / WBS, 자기참조 트리)

| 컬럼 | 타입 | 제약/기본 |
|------|------|-----------|
| task_id | BIGSERIAL | PK |
| parent_task_id | BIGINT | FK→pms_task ON DELETE CASCADE |
| project_id | BIGINT | NOT NULL, FK→pms_project ON DELETE CASCADE |
| task_name | VARCHAR(300) | NOT NULL |
| status | VARCHAR(20) | CHECK IN ('TODO','IN_PROGRESS','REVIEW','REJECTED','DONE') DEFAULT 'TODO' |
| progress_rate | INT | DEFAULT 0, CHECK 0~100 |
| assignee_id | BIGINT | FK→pms_user |
| planned/actual_start_date, planned/actual_end_date | DATE | |
| planned_effort / actual_effort | DECIMAL(10,2) | (가중치로 사용) |
| depth | INT | DEFAULT 0 |
| sort_order | INT | DEFAULT 0 |
| description | TEXT | |
| **catalog_node_id** | BIGINT | (V6) FK→pms_catalog_node ON DELETE SET NULL |
| created_at/updated_at/created_by/updated_by | | |

인덱스: `idx_task_parent`, `idx_task_project`, `idx_task_assignee`, `idx_task_status`, `idx_task_task_template`(V5 잔존, 컬럼명만 변경).
※ V5 에서 추가된 `task_template_id` 는 V6 에서 DROP 후 `catalog_node_id` 로 교체됨.

### 2.5 pms_task_assignment_history (담당자 변경 이력)

| 컬럼 | 타입 |
|------|------|
| history_id | BIGSERIAL PK |
| task_id | BIGINT NOT NULL FK→pms_task ON DELETE CASCADE |
| from_user_id / to_user_id / changed_by | BIGINT |
| change_reason | TEXT |
| changed_at | TIMESTAMP DEFAULT NOW() |

인덱스: `idx_assign_hist_task`.

### 2.6 pms_deliverable (산출물)

| 컬럼 | 타입 | 제약/기본 |
|------|------|-----------|
| deliverable_id | BIGSERIAL | PK |
| project_id | BIGINT | NOT NULL FK→pms_project ON DELETE CASCADE |
| task_id | BIGINT | FK→pms_task ON DELETE SET NULL |
| deliverable_name | VARCHAR(300) | NOT NULL |
| deliverable_type | VARCHAR(50) | |
| status | VARCHAR(20) | NOT NULL DEFAULT 'DRAFT' CHECK IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED') |
| version_no | VARCHAR(20) | DEFAULT '1.0' |
| submitted_by / submitted_at | BIGINT / TIMESTAMP | |
| reviewed_by / reviewed_at / review_comment | BIGINT / TIMESTAMP / TEXT | |
| approved_by / approved_at / approval_comment | BIGINT / TIMESTAMP / TEXT | |
| **catalog_node_id** | BIGINT | (V6) FK→pms_catalog_node ON DELETE SET NULL |
| created_at/updated_at/created_by/updated_by | | |

인덱스: `idx_deliverable_project`, `idx_deliverable_task`, `idx_deliverable_status`, `idx_deliverable_deliverable_template`.

### 2.7 pms_attachment (첨부파일, 다형성)

| 컬럼 | 타입 |
|------|------|
| attachment_id | BIGSERIAL PK |
| entity_type | VARCHAR(30) NOT NULL (예: 'DELIVERABLE') |
| entity_id | BIGINT NOT NULL |
| file_ref | VARCHAR(200) NOT NULL (아마란스 파일 ID) |
| file_name | VARCHAR(300) |
| file_size | BIGINT |
| content_type | VARCHAR(100) |
| sort_order | INT DEFAULT 0 |
| uploaded_by | VARCHAR(100) (문자열로 저장된 user id) |
| uploaded_at | TIMESTAMP DEFAULT NOW() |
| created_at / updated_at | TIMESTAMP |

인덱스: `idx_attachment_entity(entity_type, entity_id)`, `idx_attachment_fileref`.

### 2.8 pms_catalog_node (방법론 카탈로그 단일 노드 트리) — V6 통합

V5 의 4개 카탈로그 테이블(phase/activity/task_template/deliverable_template)을 **자기참조 단일 트리**로 통합.
V7 에서 `workflow_id` 추가.

| 컬럼 | 타입 | 제약/기본 |
|------|------|-----------|
| node_id | BIGSERIAL | PK |
| parent_node_id | BIGINT | FK→self ON DELETE CASCADE |
| node_type | VARCHAR(20) | NOT NULL CHECK IN ('PHASE','ACTIVITY','TASK','DELIVERABLE') |
| code | VARCHAR(40) | (예: 'PRR','OP','OP-1','OP-1-10') |
| name | VARCHAR(300) | NOT NULL |
| description | TEXT | |
| is_optional | BOOLEAN | NOT NULL DEFAULT false |
| sort_order | INT | NOT NULL DEFAULT 0 |
| seq_no | INT | (DELIVERABLE 전용) |
| deliverable_category | VARCHAR(100) | (DELIVERABLE 전용) |
| stage | VARCHAR(20) | (DELIVERABLE 전용; INCEPTION/EXECUTION/CLOSURE 의도) |
| template_file_ref | VARCHAR(200) | (DELIVERABLE 템플릿 파일) |
| template_tags | JSONB | |
| **workflow_id** | BIGINT | (V7) FK→pms_workflow ON DELETE SET NULL |
| created_at / updated_at | TIMESTAMP | |

인덱스: `idx_catalog_node_parent`, `idx_catalog_node_type`.
트리 구조: **PHASE(parent NULL) → ACTIVITY → TASK → DELIVERABLE**. DELIVERABLE code = `{taskCode}-{seqNo}`.

### 2.9 pms_project_tailoring (테일러링 선택/생성 내역) — V5 생성, V6 재배선

| 컬럼 | 타입 |
|------|------|
| tailoring_id | BIGSERIAL PK |
| project_id | BIGINT NOT NULL FK→pms_project ON DELETE CASCADE |
| **catalog_node_id** | BIGINT FK→pms_catalog_node ON DELETE SET NULL (V6; 구 task/deliverable_template FK 대체) |
| is_selected | BOOLEAN NOT NULL DEFAULT true |
| exclude_reason | TEXT |
| generated_task_id | BIGINT FK→pms_task ON DELETE SET NULL |
| generated_deliverable_id | BIGINT FK→pms_deliverable ON DELETE SET NULL |
| created_at / updated_at | TIMESTAMP |

인덱스: `idx_project_tailoring_project` 등.

### 2.10 워크플로 3테이블 (Jira 유사) — V7

**pms_workflow**

| 컬럼 | 타입 |
|------|------|
| workflow_id | BIGSERIAL PK |
| name | VARCHAR(100) NOT NULL |
| description | TEXT |
| is_default | BOOLEAN NOT NULL DEFAULT false |
| created_at / updated_at | TIMESTAMP |

**pms_workflow_status**

| 컬럼 | 타입 |
|------|------|
| status_id | BIGSERIAL PK |
| workflow_id | BIGINT NOT NULL FK→pms_workflow ON DELETE CASCADE |
| code | VARCHAR(40) |
| name | VARCHAR(100) NOT NULL |
| color | VARCHAR(20) (hex) |
| category | VARCHAR(20) CHECK IN ('TODO','IN_PROGRESS','DONE') |
| is_initial / is_final | BOOLEAN NOT NULL DEFAULT false |
| sort_order | INT NOT NULL DEFAULT 0 |
| created_at / updated_at | TIMESTAMP |

인덱스 `idx_workflow_status_wf`.

**pms_workflow_transition**

| 컬럼 | 타입 |
|------|------|
| transition_id | BIGSERIAL PK |
| workflow_id | BIGINT NOT NULL FK→pms_workflow ON DELETE CASCADE |
| from_status_id | BIGINT NOT NULL FK→pms_workflow_status ON DELETE CASCADE |
| to_status_id | BIGINT NOT NULL FK→pms_workflow_status ON DELETE CASCADE |
| name | VARCHAR(100) |
| created_at / updated_at | TIMESTAMP |

인덱스 `idx_workflow_transition_wf`.

### 2.11 pms_contact_point (컨택포인트) — V10

| 컬럼 | 타입 | 비고 |
|------|------|------|
| contact_id | BIGSERIAL | PK |
| project_id | BIGINT | NOT NULL FK→pms_project ON DELETE CASCADE |
| field | VARCHAR(100) | 역할/분야 (영업, 기술, 고객사 PM 등) |
| contact_type | VARCHAR(20) | NOT NULL CHECK IN ('INTERNAL','EXTERNAL') |
| user_id | BIGINT | INTERNAL: 사용자 ref (FK 제약 없음) |
| name / company / department / title / phone / email | VARCHAR | EXTERNAL 직접입력 또는 INTERNAL 캐시 |
| note | TEXT | |
| sort_order | INT NOT NULL DEFAULT 0 | |
| created_at / updated_at | TIMESTAMP | |

인덱스 `idx_contact_point_project`.

### 2.12 ERD (텍스트 관계도)

```
pms_company 1──* pms_project *──1 pms_user(pm_id)
                  │
                  ├──* pms_task ──(self parent_task_id, CASCADE)── pms_task
                  │        │ assignee_id → pms_user
                  │        │ catalog_node_id → pms_catalog_node (SET NULL)
                  │        └──* pms_task_assignment_history
                  │
                  ├──* pms_deliverable
                  │        │ task_id → pms_task (SET NULL)
                  │        │ catalog_node_id → pms_catalog_node (SET NULL)
                  │        └──* pms_attachment (entity_type='DELIVERABLE', entity_id)
                  │
                  ├──* pms_project_tailoring
                  │        catalog_node_id → pms_catalog_node (SET NULL)
                  │        generated_task_id → pms_task / generated_deliverable_id → pms_deliverable
                  │
                  └──* pms_contact_point (user_id → 내부 사용자, FK 없음)

pms_catalog_node ──(self parent_node_id, CASCADE)── pms_catalog_node
                  workflow_id → pms_workflow (SET NULL)

pms_workflow 1──* pms_workflow_status
             1──* pms_workflow_transition (from/to → status)
```

### 2.13 시드 데이터 요약

- **사용자 6명** (V2): PM `ahnyk(안유경)`,`leeyh(이영희)`,`kimcs(김철수)`; MEMBER `kimjh(김준현)`,`parkjm(박지민)`,`kangdw(강동우)`. 비밀번호 bcrypt("password").
- **회사** (V2): 국립정보자원관리원(CLIENT), 국민건강보험공단(CLIENT), 오케스트로(OWN). (V9) 조달청(행정안전부)(CLIENT).
- **프로젝트** (V2): PRJ-2026-001(스마트홈 IoT, IN_PROGRESS/EXECUTION, 65%), PRJ-2026-002(AI 상담, IN_PROGRESS/EXECUTION, 45%). (V9) BIDDING 2건(ERP 클라우드 이전 PREPARING, 대법원 등기 SUBMITTED).
- **업무 트리** (V2): 프로젝트1 에 분석/설계/개발 3 단계 + 자식들. (V8) leaf 업무에 계획일정 보강. (V9) ERP 프로젝트에 제안단계 WBS.
- **산출물** (V4): 프로젝트1 에 3건(요구사항정의서 APPROVED, 시스템설계서 APPROVED, 소스코드 SUBMITTED) + 첨부 2건.
- **카탈로그** (V6): 4 PHASE / 10 ACTIVITY / 33 TASK / 74 DELIVERABLE 노드 (OPMS 표준 방법론).
- **워크플로** (V7): 기본 워크플로 1개(is_default) — 상태 5개(대기/진행중/검토중/완료/반려), 전이 6개. 모든 TASK 노드에 기본 워크플로 할당.
- **컨택포인트** (V10): 프로젝트1 에 EXTERNAL(고객사 PM 김부장) + INTERNAL(기술 PM 안유경) 2건.

---

## 3. API 명세

> 공통 베이스: `/api/v1`. 응답은 별도 표기 없으면 `ApiResponse<...>` 로 래핑.
> 인증: `public`(불필요) 외 전부 `Bearer mock-token-{userId}` 필요.

### 3.1 인증 / 사용자

| 메서드 | 경로 | 인증 | 요청 | 응답 data | 설명 |
|--------|------|------|------|-----------|------|
| POST | `/api/v1/auth/login` | **public** | `{username, password}` (둘 다 NotBlank) | `{token, user:UserInfo}` | 로그인. 비번 고정 `"password"`. 토큰 `mock-token-{id}` |
| GET | `/api/v1/auth/me` | 필요 | – | `UserInfo` | 현재 사용자 |
| GET | `/api/v1/users/search?q=` | 필요 | q (선택) | `List<UserInfo>` | username/fullName 부분일치(대소문자 무시). q 없으면 전체 |
| GET | `/api/v1/health` | **public** | – | `{"status":"UP"}` (래퍼 없음) | 헬스체크 |

`UserInfo` = `{id, username, email, fullName, role, active}`.

### 3.2 프로젝트 (project)

| 메서드 | 경로 | 요청 | 응답 data | 설명 |
|--------|------|------|-----------|------|
| GET | `/api/v1/projects?stage=` | stage(선택: BIDDING/EXECUTION/COMPLETED) | `List<ProjectDto>` | stage 필터, id 오름차순, enrich(pmName/clientName) |
| POST | `/api/v1/projects` | `ProjectCreateRequest` | `ProjectDto` | 생성 (enrich 없음) |
| GET | `/api/v1/projects/{id}` | – | `ProjectDto` | 단건 (enrich) |
| PUT | `/api/v1/projects/{id}` | `ProjectUpdateRequest` (부분) | `ProjectDto` | 수정 (null 필드 무시) |
| GET | `/api/v1/projects/{id}/summary` | – | `ProjectSummaryDto` | 업무 상태별 카운트 집계 |
| POST | `/api/v1/projects/with-tailoring` | `ProjectCreateWithTailoringRequest` | `ProjectDto` | 테일러링 기반 프로젝트+WBS+산출물 일괄 생성 |

`ProjectDto` 필드: id, projectName, projectCode, description, pmId, clientCompanyId, status, projectStage, planned/actual start/end date, contractAmount, progressRate, riskLevel, team, location, businessType, bidStatus, consortiumRole, consortiumShare, vrbStatus, announcementNo, proposalDeadline, pmName, clientName.
`ProjectCreateRequest`: projectName(NotBlank), projectCode, description, pmId, clientCompanyId, status, projectStage, plannedStart/EndDate, contractAmount, team, location, businessType.
`ProjectUpdateRequest`: 위 + actualStart/EndDate, progressRate, riskLevel (전부 선택).
`ProjectSummaryDto`: projectId, projectName, progressRate, totalTasks, todoTasks, inProgressTasks, reviewTasks, doneTasks.

### 3.3 업무 (task)

| 메서드 | 경로 | 요청 | 응답 data | 설명 |
|--------|------|------|-----------|------|
| GET | `/api/v1/projects/{projectId}/tasks` | – | `List<TaskTreeDto>` | 프로젝트 업무 트리 (workflowId 포함) |
| POST | `/api/v1/projects/{projectId}/tasks` | `TaskCreateRequest` | `TaskDto` | 업무 생성 (parent 지정 시 depth 자동, 상위 재집계) |
| GET | `/api/v1/tasks/{id}` | – | `TaskDto` | 단건 (workflowId 해석 포함) |
| PUT | `/api/v1/tasks/{id}` | `TaskUpdateRequest` (부분) | `TaskDto` | 수정 |
| DELETE | `/api/v1/tasks/{id}` | – | null | 삭제 (자식 CASCADE, 상위 재집계) |
| PUT | `/api/v1/tasks/{id}/progress` | `{progressRate}` (0~100, NotNull) | `TaskDto` | leaf 만 허용. 100→DONE, >0&TODO→IN_PROGRESS, 상위 전파 |
| POST | `/api/v1/tasks/{id}/assign` | `{assigneeId(NotNull), reason}` | `TaskDto` | 담당자 변경 + 이력 기록 |
| GET | `/api/v1/tasks/{id}/assignment-history` | – | `List<TaskAssignmentHistory>` | 변경 이력(changedAt 내림차순) |
| GET | `/api/v1/tasks/{id}/workflow` | – | `WorkflowDto` | 업무 워크플로 (catalog_node→없으면 기본 폴백) |
| GET | `/api/v1/tasks/{id}/deliverables` | – | `List<DeliverableDto>` | 해당 업무의 산출물 |
| POST | `/api/v1/tasks/{id}/deliverables` | `{deliverableName(NotBlank), deliverableType}` | `DeliverableDto` | 업무에 산출물 생성(version 1.0, DRAFT) |
| GET | `/api/v1/tasks/{id}/template-deliverables` | – | `List<TemplateDeliverableDto>` | catalog_node 자식 중 DELIVERABLE 템플릿 |

`TaskDto`/`TaskTreeDto` 필드: id, parentTaskId, projectId, taskName, status, progressRate, assigneeId, planned/actual start/end date, planned/actual Effort, depth, sortOrder, description, catalogNodeId, workflowId (+ TaskTreeDto: children).
`TemplateDeliverableDto`: name, templateFileRef, seqNo.

### 3.4 산출물 (deliverable)

| 메서드 | 경로 | 요청 | 응답 data | 설명 |
|--------|------|------|-----------|------|
| GET | `/api/v1/projects/{projectId}/deliverables` | – | `List<DeliverableDto>` | 프로젝트 산출물 목록 |
| POST | `/api/v1/projects/{projectId}/deliverables` | `DeliverableCreateRequest` | `DeliverableDto` | 생성 |
| GET | `/api/v1/deliverables/{id}` | – | `DeliverableDto` | 단건 |
| PUT | `/api/v1/deliverables/{id}` | `DeliverableUpdateRequest` | `DeliverableDto` | 메타 수정 |
| POST | `/api/v1/deliverables/{id}/submit` | – | `DeliverableDto` | DRAFT→SUBMITTED |
| POST | `/api/v1/deliverables/{id}/review` | `ReviewRequest`(선택) | `DeliverableDto` | decision 없으면 SUBMITTED→UNDER_REVIEW, APPROVE/REJECT |
| POST | `/api/v1/deliverables/{id}/approve` | `ReviewRequest`(선택) | `DeliverableDto` | →APPROVED |
| GET | `/api/v1/deliverables/{id}/attachments` | – | `List<AttachmentDto>` | 첨부 목록 |
| POST | `/api/v1/deliverables/{id}/attachments` | `{fileName(NotBlank), contentType, fileSize}` | `AttachmentDto` | FilePort.upload 후 첨부 등록 |

`DeliverableDto`: id, projectId, taskId, deliverableName, deliverableType, status, versionNo, submittedBy, submittedAt, reviewedAt, approvedAt, authorName, attachmentCount, updatedAt.
`DeliverableCreateRequest`: deliverableName(NotBlank), deliverableType, versionNo, taskId, status.
`ReviewRequest`: decision(APPROVE/REJECT), comment.
`AttachmentDto`: id, entityType, entityId, fileRef, fileName, fileSize, contentType, sortOrder, uploadedBy, uploadedAt.

### 3.5 방법론 카탈로그 (methodology)

| 메서드 | 경로 | 요청 | 응답 data | 설명 |
|--------|------|------|-----------|------|
| GET | `/api/v1/methodology/catalog` | – | `List<CatalogNodeDto>` | 전체 노드 트리(PHASE 루트, 재귀 children) |
| POST | `/api/v1/methodology/nodes` | `CatalogNodeRequest` | `CatalogNodeDto` | 노드 생성(계층 검증) |
| PUT | `/api/v1/methodology/nodes/{id}` | `CatalogNodeRequest` (부분) | `CatalogNodeDto` | 수정 |
| DELETE | `/api/v1/methodology/nodes/{id}` | – | null | 삭제(자식 CASCADE) |

`CatalogNodeDto`: nodeId, parentNodeId, nodeType, code, name, isOptional, seqNo, sortOrder, workflowId, children.
`CatalogNodeRequest`: parentNodeId, nodeType, code, name, isOptional, sortOrder, seqNo, description, deliverableCategory, stage, workflowId.

### 3.6 워크플로 (workflow)

| 메서드 | 경로 | 요청 | 응답 data |
|--------|------|------|-----------|
| GET | `/api/v1/workflows` | – | `List<WorkflowDto>` |
| GET | `/api/v1/workflows/{id}` | – | `WorkflowDto` |
| POST | `/api/v1/workflows` | `WorkflowRequest{name,description,isDefault}` | `WorkflowDto` |
| PUT | `/api/v1/workflows/{id}` | `WorkflowRequest`(부분) | `WorkflowDto` |
| DELETE | `/api/v1/workflows/{id}` | – | null (statuses/transitions CASCADE) |
| POST | `/api/v1/workflows/{id}/statuses` | `WorkflowStatusRequest` | `WorkflowStatusDto` |
| PUT | `/api/v1/workflow-statuses/{statusId}` | `WorkflowStatusRequest`(부분) | `WorkflowStatusDto` |
| DELETE | `/api/v1/workflow-statuses/{statusId}` | – | null |
| POST | `/api/v1/workflows/{id}/transitions` | `WorkflowTransitionRequest{fromStatusId,toStatusId,name}` | `WorkflowTransitionDto` |
| DELETE | `/api/v1/workflow-transitions/{transitionId}` | – | null |

`WorkflowDto`: workflowId, name, description, isDefault, statuses[], transitions[].
`WorkflowStatusDto`: statusId, code, name, color, category, isInitial, isFinal, sortOrder.
`WorkflowTransitionDto`: transitionId, fromStatusId, toStatusId, name.

### 3.7 컨택포인트 (contact)

| 메서드 | 경로 | 요청 | 응답 data |
|--------|------|------|-----------|
| GET | `/api/v1/projects/{projectId}/contacts` | – | `List<ContactPointDto>` |
| POST | `/api/v1/projects/{projectId}/contacts` | `ContactPointRequest` | `ContactPointDto` |
| PUT | `/api/v1/contacts/{id}` | `ContactPointRequest` | `ContactPointDto` |
| DELETE | `/api/v1/contacts/{id}` | – | null |

`ContactPointDto`/`ContactPointRequest`: field, contactType(INTERNAL/EXTERNAL), userId, name, company, department, title, phone, email, note, sortOrder. (Dto 는 contactId, projectId 추가)

---

## 4. 도메인 비즈니스 로직 (재구현 필수)

### 4.1 진척률 자동 집계 (`TaskService`)

규칙:
- **leaf(자식 없는 업무)** 만 `progressRate` 직접 입력 가능. parent 에 progress 직접 설정 시 400.
- parent 진척률 = 자식들의 **가중 평균** 후 정수 반올림.

```
weightedProgress(children):
  if 모든 자식의 plannedEffort > 0:
     return round( Σ(effort_i × progress_i) / Σ(effort_i) )   # HALF_UP, 0자리
  else:
     return round( Σ(progress_i) / count )                    # 단순 평균
progressOf(t) = t.progressRate ?? 0
```

상위 전파 (`recalcAncestors(taskId)`):
```
current = taskId
while current != null:
   task = load(current)
   children = childrenOf(current)
   if children not empty:
      task.progressRate = weightedProgress(children)
      applyDerivedStatus(task)   # >=100→DONE, >0→IN_PROGRESS, else→TODO
   current = task.parentTaskId
```

트리거 시점: progress 갱신, 업무 생성(자식 추가 시 parent 재계산), 업무 삭제 후.
leaf progress 갱신 시: 100→status DONE, (>0 & 기존 TODO)→IN_PROGRESS.

### 4.2 테일러링: 프로젝트 일괄 생성 (`TailoringService.createProjectWithTailoring`)

```
1. 요청으로 Project 생성·저장 (created_by/updated_by = currentUser).
2. selectedNodeIds 비면 프로젝트만 반환.
3. 선택 노드 로드 + 전체 노드 인덱싱(부모 탐색용). TASK/DELIVERABLE 로 분리.
4. TASK 노드를 조상 PHASE 별로 그룹핑 (taskNode → activity(parent) → phase(parent)).
5. PHASE 를 sortOrder, id 순 정렬. 각 PHASE 마다:
     - 부모 Task(phaseTask, depth=0, status TODO, progress 0) 생성.
     - 그 PHASE 의 TASK 들을 (activity.sortOrder, task.sortOrder, id) 순 정렬.
     - 각 TASK 노드 → 자식 Task(depth=1, catalogNodeId=노드id, TODO) 생성.
       generatedTaskIdByNode[node.id] = 저장된 childTask.id
       ProjectTailoring(projectId, catalogNodeId, isSelected, generatedTaskId) 저장.
6. 선택 DELIVERABLE 노드마다:
     - Deliverable 생성 (name, type=deliverableCategory, DRAFT, version 1.0, catalogNodeId).
     - 부모 TASK 노드가 함께 선택돼 생성됐으면 그 generatedTaskId 로 taskId 링크.
     - ProjectTailoring(catalogNodeId, generatedDeliverableId) 저장.
```
핵심: PHASE 만 parent Task 로 만들고 ACTIVITY 는 Task 로 만들지 않음(TASK 노드만 leaf 업무). catalog_node_id 링크가 워크플로 해석과 템플릿 산출물 조회의 기반.

### 4.3 카탈로그 트리 구성 (`MethodologyService.getCatalog`)

N+1 방지: `findAll()` 1회 → parentNodeId 로 그룹핑한 Map → 루트(parent null)부터 메모리에서 재귀 트리 구성.
정렬: `sortOrder(null→0)` then `id`. 생성/수정 응답은 해당 노드의 하위만 조회하는 단건 변환 사용.
계층 검증(`createNode`): PHASE 는 parent 없어야 하고, 그 외 타입은 parent 필수.

### 4.4 워크플로 전이 해석 (`TaskService.resolveWorkflowId` / `workflow`)

```
resolveWorkflowId(catalogNodeId):
   if null → null
   return catalogNode.workflowId (없으면 null)

workflow(taskId):                       # GET /tasks/{id}/workflow
   wfId = resolveWorkflowId(task.catalogNodeId)
   if wfId == null:
       wfId = workflowRepository.findFirstByIsDefaultTrue()?.id   # 기본 워크플로 폴백
   return wfId == null ? null : workflowService.getWorkflow(wfId)
```
트리/단건 응답의 `workflowId` 필드도 `resolveWorkflowId` 로 채움(이 경우 폴백 없음 → null 가능).

### 4.5 산출물 상태 워크플로 (`DeliverableService`)

상태: `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED|REJECTED`.

- `submit`: DRAFT 에서만 → SUBMITTED (submittedBy/At 기록). 위반 시 IllegalStateException(409).
- `review(req)`:
  - decision 없음 → `startReview`: SUBMITTED 에서만 → UNDER_REVIEW.
  - `"APPROVE"` → approve, `"REJECT"` → reject, 그 외 → 400.
- `approve(comment)`: 현재 상태 SUBMITTED 또는 UNDER_REVIEW 에서만 → APPROVED (approvedBy/At/Comment).
- `reject(comment)`: 동일 전제 → REJECTED (reviewedBy/At, reviewComment).

DTO 의 `authorName` = submittedBy(없으면 createdBy)의 fullName. `attachmentCount` 는 attachment 카운트.

### 4.6 컨택포인트 INTERNAL/EXTERNAL (`ContactPointService`)

- `contactType` 은 INTERNAL/EXTERNAL 만 허용(아니면 400).
- 저장 시 INTERNAL & userId 있으면 `userPort.getUser(userId)` 로 name 캐시(필요 시 email 보강).
- 조회(DTO) 시 INTERNAL & userId 있으면 다시 UserPort 로 resolve → fullName/email 이 있으면 우선 적용. EXTERNAL 은 행 값 그대로.

### 4.7 프로젝트 enrichment + 입찰 필드 (`ProjectService`)

- list/get 응답에 `pmName`(pms_user.full_name), `clientName`(CompanyNameRepository.findNameById, EntityManager 단순 조회) 보강. create/update 응답은 보강 안 함.
- 입찰(BIDDING) 전용 필드(bidStatus, consortiumRole, consortiumShare, vrbStatus, announcementNo, proposalDeadline)는 엔티티/DTO 에 존재하나 create/update 요청 DTO 에는 일부만 노출 — 주로 시드/직접 수정으로 채움.

---

## 5. 아마란스 연동 (어댑터)

`adapter/amaranth` 패키지. 헥사고날 포트로 정의, 현재는 Mock 구현이 빈으로 주입됨. 실제 연동 시 Mock 을 교체.

### 5.1 UserPort (사용자)
```java
UserInfo getUser(Long id);
Optional<UserInfo> findByUsername(String username);
List<UserInfo> searchUsers(String keyword);
```
**MockUserAdapter**: 로컬 `pms_user`(UserRepository) 조회. searchUsers 는 username/fullName 부분일치(대소문자 무시).
`UserInfo` = (id, username, email, fullName, role, active).

### 5.2 FilePort (파일)
```java
FileRef upload(String fileName, byte[] content);
FileMeta getMeta(String fileId);
byte[] download(String fileId);
```
**MockFileAdapter**: upload 시 UUID fileId 와 `https://mock.amaranth.local/files/{id}` URL 반환(실제 저장 안 함). getMeta/download 는 더미.
`FileRef`=(fileId,url), `FileMeta`=(fileId,fileName,contentType,size).
DeliverableService.addAttachment 는 `filePort.upload(fileName, new byte[0])` 호출 후 반환된 fileId 를 `file_ref` 로 저장.

### 5.3 TokenValidator (토큰)
```java
Optional<Long> validate(String token);   // 유효 시 userId
```
**MockTokenValidator**: `mock-token-{id}` 형식만 인정, 숫자 부분을 userId 로 파싱.

### 5.4 실제 연동 시 교체 지점
- 세 Mock 클래스를 실제 아마란스 API 호출 구현으로 대체(인터페이스 시그니처 유지).
- 토큰 발급 경로(`MockTokenService.login`)도 실제 인증 토큰 발급으로 교체.
- 첨부 업로드: addAttachment 가 빈 바이트 대신 실제 파일 바이트를 받도록 멀티파트 수용 변경 필요.

---

## 6. 인증 / 보안 / CORS / DB 연결

### 6.1 Mock 인증 흐름
1. `POST /auth/login` → `MockTokenService`: username 존재 + password=="password" 검증 → `mock-token-{userId}` 반환.
2. 이후 요청 헤더 `Authorization: Bearer mock-token-{userId}`.
3. `MockAuthFilter`(OncePerRequest): Bearer 토큰 추출 → `TokenValidator.validate` → userId 로 `UserRepository.findById` → 인증 객체(principal=userId(Long), authority=`ROLE_{role}`) 세팅.
4. `CurrentUser.id()` / `idOrNull()` 로 컨트롤러/서비스에서 현재 userId 획득(principal 이 Long 일 때만).

### 6.2 SecurityConfig
- CSRF off, CORS on, 세션 STATELESS.
- **공개 경로**: `/api/v1/auth/login`, `/api/v1/health`. 그 외 전부 `authenticated()`.
- `MockAuthFilter` 를 `UsernamePasswordAuthenticationFilter` 앞에 추가.
- `BCryptPasswordEncoder` 빈 제공(시드 비번 검증용이나 로그인은 평문 "password" 비교).

### 6.3 CORS (`CorsConfig`)
- 허용 오리진: 환경변수 `APP_CORS_ORIGINS`(콤마 구분), 기본 `http://localhost:5173,http://localhost:*`.
- `setAllowedOriginPatterns` 사용(패턴 허용), methods/headers `*`, `allowCredentials=true`, `maxAge=3600`(프리플라이트 1시간 캐시). `/**` 적용.

### 6.4 운영 DB 연결 (`DatabaseUrlPostProcessor`, EnvironmentPostProcessor)
우선순위:
1. OS 환경변수 `SPRING_DATASOURCE_URL` 있으면 그대로 사용.
2. `DATABASE_URL`(`postgres://` 또는 `postgresql://`) 있으면 `jdbc:postgresql://host:port/db` + username/password 로 변환, 쿼리에 `sslmode=prefer&connectTimeout=15&socketTimeout=30` 부가. `addFirst` 로 yml 기본값보다 우선.
3. 둘 다 없으면 application.yml 기본값(localhost) 사용.

> ⚠️ EnvironmentPostProcessor 는 `META-INF/spring.factories` 등록이 필요(Spring Boot 가 자동 스캔하지 않음). 재구현 시 등록 누락 주의.

### 6.5 application.yml 핵심
- datasource: `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/aetherpmo}`, user/pass 기본 `aetherpmo`/`aetherpmo1234!`, Hikari connection-timeout 15s, initialization-fail-timeout 15s.
- jpa: `ddl-auto: validate`, `open-in-view: false`, show-sql false.
- flyway: enabled true, baseline-on-migrate true.
- server.port: `${PORT:8080}`.

---

## 7. 마이그레이션 순서 / 시드 (V1~V10)

| 버전 | 내용 |
|------|------|
| V1 | 코어 테이블: pms_user, pms_company, pms_project, pms_task(자기참조 트리), pms_task_assignment_history + 인덱스 |
| V2 | 시드: 사용자 6, 회사 3, 프로젝트 2, 프로젝트1 업무 트리(분석/설계/개발) |
| V3 | pms_deliverable, pms_attachment(다형성) + 인덱스 |
| V4 | 산출물 시드(프로젝트1 3건) + 첨부 2건 |
| V5 | 구 카탈로그 4테이블(phase/activity/task_template/deliverable_template) + pms_project_tailoring 생성, pms_task/pms_deliverable 에 *_template_id 추가, OPMS 방법론 카탈로그 시드 |
| V6 | **단일 노드 트리 통합**: pms_catalog_node 생성·시드, project_tailoring TRUNCATE 후 catalog_node_id 로 재배선, task/deliverable 의 *_template_id → catalog_node_id 교체, 구 4테이블 DROP (production-safe) |
| V7 | 워크플로 3테이블(pms_workflow/_status/_transition), catalog_node.workflow_id 추가, 기본 워크플로(상태5/전이6) 시드 + 전 TASK 노드에 할당 |
| V8 | 프로젝트1 leaf 업무 계획일정(planned_start/end_date) 보강 |
| V9 | pms_project 입찰 컬럼 6종(ADD COLUMN IF NOT EXISTS), 조달청 회사, BIDDING 프로젝트 2건 + 제안단계 WBS (재실행 안전 가드) |
| V10 | pms_contact_point 생성 + 프로젝트1 컨택 2건(INTERNAL/EXTERNAL) 시드 |

V6~V10 은 운영 DB 재실행 안전성(`IF NOT EXISTS`, `WHERE NOT EXISTS`, code 기반 FK 해석)을 고려해 작성됨.

---

## 8. 재개발 시 주의 (환경 의존 지점)

1. **패키지 네임스페이스**: 전 코드가 `com.aetherpmo`. 다른 루트 사용 시 컴포넌트 스캔/엔티티 스캔/`spring.factories` 의 PostProcessor 경로 일괄 변경.
2. **Flyway 버전 번호**: V1~V10 연속. 다른 리포 마이그레이션과 합칠 때 버전 충돌 시 재배치/재명명 필요. V6 의 파괴적 ALTER/DROP 순서(테이블 통합) 보존 필수.
3. **ddl-auto=validate**: 엔티티와 스키마가 1:1 일치해야 부팅. 컬럼명(snake↔camel 매핑), 타입, nullable 이 마이그레이션과 어긋나면 기동 실패. 새 컬럼은 반드시 마이그레이션+엔티티 동시 반영.
4. **응답 래퍼/예외 규약**: 모든 성공은 `ApiResponse<T>`(헬스 제외), 예외 매핑(ApiError→상태내장, IllegalArgument→400, IllegalState→409). 프론트가 이 형식에 의존.
5. **Mock 교체 지점**: UserPort/FilePort/TokenValidator 의 Mock 3종 + MockTokenService 의 평문 비번/`mock-token-{id}` 발급 → 실제 인증·파일 저장으로 교체. 인터페이스 시그니처는 유지.
6. **Spring Boot 3.2.5 / Java 21 / Jakarta**: `jakarta.*` 네임스페이스, record DTO, `@RequiredArgsConstructor` 생성자 주입. 다른 버전 정렬 시 validation/security API 변경 주의.
7. **EnvironmentPostProcessor 등록**: `DatabaseUrlPostProcessor` 는 자동 등록 아님 — `META-INF/spring.factories`(또는 `spring.factories`) 의 `org.springframework.boot.env.EnvironmentPostProcessor` 항목 유지 필요.
8. **CORS/포트 환경변수**: `APP_CORS_ORIGINS`, `PORT`, `DATABASE_URL`/`SPRING_DATASOURCE_*` 가 배포 환경(Render 등)에 맞게 주입돼야 함.
9. **감사 컬럼 이원화**: created_at/updated_at 은 JPA auditing 자동, created_by/updated_by 는 서비스에서 수동(`currentUser.idOrNull()`). 재구현 시 동일 분리 유지.
10. **catalog_node_id 의존**: 워크플로 해석·템플릿 산출물 조회·테일러링 추적이 모두 task/deliverable 의 `catalog_node_id` 링크에 의존. 통합 노드 트리(V6) 구조를 반드시 보존.
