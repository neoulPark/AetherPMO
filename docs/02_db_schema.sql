-- =============================================================================
-- PMS (Project Management System) Database Schema
-- Database: PostgreSQL 14+
-- Created: 2026-06-15
-- Description: 프로젝트 관리 시스템 전체 DDL
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Extension
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 1. TRIGGER FUNCTION: updated_at 자동 갱신
-- =============================================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 2. TABLES (의존성 순서대로 생성)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 2.1 PMS_USER (사용자)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_user (
    user_id         BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(100)    NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(100),
    role            VARCHAR(20)     NOT NULL DEFAULT 'MEMBER'
                        CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'PM', 'MEMBER')),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT uq_user_email    UNIQUE (email)
);

COMMENT ON TABLE  pms_user             IS '시스템 사용자';
COMMENT ON COLUMN pms_user.user_id     IS '사용자 고유 ID';
COMMENT ON COLUMN pms_user.username    IS '로그인 아이디';
COMMENT ON COLUMN pms_user.email       IS '이메일 주소';
COMMENT ON COLUMN pms_user.password    IS 'bcrypt 등으로 암호화된 비밀번호';
COMMENT ON COLUMN pms_user.role        IS '시스템 역할: ADMIN(관리자), PM(프로젝트 관리자), MEMBER(일반 팀원)';
COMMENT ON COLUMN pms_user.is_active   IS '계정 활성화 여부 (소프트 삭제)';
COMMENT ON COLUMN pms_user.last_login_at IS '최종 로그인 일시';

CREATE TRIGGER trg_pms_user_updated_at
    BEFORE UPDATE ON pms_user
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.2 PMS_COMPANY (회사)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_company (
    company_id      BIGSERIAL       PRIMARY KEY,
    company_name    VARCHAR(200)    NOT NULL,
    company_type    VARCHAR(20)     NOT NULL
                        CONSTRAINT chk_company_type CHECK (company_type IN ('OWN', 'PARTNER', 'CLIENT')),
    business_no     VARCHAR(20),
    contact_name    VARCHAR(100),
    contact_email   VARCHAR(100),
    contact_phone   VARCHAR(20),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

COMMENT ON TABLE  pms_company              IS '회사 (자사/협력사/고객사)';
COMMENT ON COLUMN pms_company.company_type IS '회사 유형: OWN(자사), PARTNER(협력사), CLIENT(고객사)';
COMMENT ON COLUMN pms_company.business_no  IS '사업자등록번호';

CREATE TRIGGER trg_pms_company_updated_at
    BEFORE UPDATE ON pms_company
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.3 PMS_PROJECT (프로젝트)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_project (
    project_id          BIGSERIAL       PRIMARY KEY,
    project_name        VARCHAR(200)    NOT NULL,
    project_code        VARCHAR(50),
    description         TEXT,
    pm_id               BIGINT
                            CONSTRAINT fk_project_pm
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    client_company_id   BIGINT
                            CONSTRAINT fk_project_company
                            REFERENCES pms_company(company_id)
                            ON DELETE SET NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PLANNING'
                            CONSTRAINT chk_project_status
                            CHECK (status IN ('PLANNING','IN_PROGRESS','ON_HOLD','COMPLETED','CANCELLED')),
    planned_start_date  DATE,
    planned_end_date    DATE,
    actual_start_date   DATE,
    actual_end_date     DATE,
    total_budget        DECIMAL(15,2),
    contract_amount     DECIMAL(15,2),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    CONSTRAINT uq_project_code UNIQUE (project_code),
    CONSTRAINT chk_project_dates CHECK (
        planned_end_date IS NULL OR planned_start_date IS NULL OR planned_end_date >= planned_start_date
    )
);

COMMENT ON TABLE  pms_project                  IS '프로젝트';
COMMENT ON COLUMN pms_project.project_code     IS '프로젝트 고유 코드 (업무용 식별자)';
COMMENT ON COLUMN pms_project.pm_id            IS '프로젝트 관리자 (PM) 사용자 ID';
COMMENT ON COLUMN pms_project.client_company_id IS '고객사 ID';
COMMENT ON COLUMN pms_project.status           IS '프로젝트 상태: PLANNING/IN_PROGRESS/ON_HOLD/COMPLETED/CANCELLED';
COMMENT ON COLUMN pms_project.total_budget     IS '총 예산 (원)';
COMMENT ON COLUMN pms_project.contract_amount  IS '계약 금액 (원)';

CREATE TRIGGER trg_pms_project_updated_at
    BEFORE UPDATE ON pms_project
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.4 PMS_STATUS (상태 마스터)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_status (
    status_id       BIGSERIAL       PRIMARY KEY,
    domain          VARCHAR(30)     NOT NULL,
    status_code     VARCHAR(50)     NOT NULL,
    status_name     VARCHAR(100)    NOT NULL,
    is_initial      BOOLEAN         NOT NULL DEFAULT false,
    is_final        BOOLEAN         NOT NULL DEFAULT false,
    sort_order      INT             NOT NULL DEFAULT 0,
    color_code      VARCHAR(10),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_status_domain_code UNIQUE (domain, status_code)
);

COMMENT ON TABLE  pms_status             IS '상태 마스터 (도메인별 상태 코드 중앙 관리)';
COMMENT ON COLUMN pms_status.domain      IS '적용 도메인: TASK, PROJECT, DELIVERABLE, ISSUE';
COMMENT ON COLUMN pms_status.status_code IS '상태 코드 (도메인 내 유일)';
COMMENT ON COLUMN pms_status.is_initial  IS '초기(시작) 상태 여부';
COMMENT ON COLUMN pms_status.is_final    IS '최종(종료) 상태 여부';
COMMENT ON COLUMN pms_status.color_code  IS 'UI 표시 색상 HEX 코드 (예: #4CAF50)';

CREATE TRIGGER trg_pms_status_updated_at
    BEFORE UPDATE ON pms_status
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.5 PMS_STATUS_TRANSITION (상태 전이 규칙)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_status_transition (
    transition_id       BIGSERIAL       PRIMARY KEY,
    domain              VARCHAR(30)     NOT NULL,
    from_status_code    VARCHAR(50)     NOT NULL,
    to_status_code      VARCHAR(50)     NOT NULL,
    condition_json      JSONB,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    CONSTRAINT uq_transition UNIQUE (domain, from_status_code, to_status_code)
);

COMMENT ON TABLE  pms_status_transition                IS '상태 전이 규칙 (허용된 상태 변경 경로 정의)';
COMMENT ON COLUMN pms_status_transition.domain         IS '적용 도메인: TASK, PROJECT, DELIVERABLE, ISSUE';
COMMENT ON COLUMN pms_status_transition.from_status_code IS '전이 시작 상태 코드';
COMMENT ON COLUMN pms_status_transition.to_status_code   IS '전이 목표 상태 코드';
COMMENT ON COLUMN pms_status_transition.condition_json   IS '전이 조건 JSON 트리 (예: {"role": "PM"})';

CREATE TRIGGER trg_pms_status_transition_updated_at
    BEFORE UPDATE ON pms_status_transition
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.6 PMS_TASK (업무 - WBS 계층 구조)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_task (
    task_id             BIGSERIAL       PRIMARY KEY,
    parent_task_id      BIGINT
                            CONSTRAINT fk_task_parent
                            REFERENCES pms_task(task_id)
                            ON DELETE RESTRICT,
    project_id          BIGINT          NOT NULL
                            CONSTRAINT fk_task_project
                            REFERENCES pms_project(project_id)
                            ON DELETE RESTRICT,
    task_name           VARCHAR(300)    NOT NULL,
    task_type           VARCHAR(20)     NOT NULL DEFAULT 'TASK',
    status              VARCHAR(20)     NOT NULL DEFAULT 'TODO'
                            CONSTRAINT chk_task_status
                            CHECK (status IN ('TODO','IN_PROGRESS','REVIEW','REJECTED','DONE')),
    progress_rate       INT             NOT NULL DEFAULT 0
                            CONSTRAINT chk_task_progress CHECK (progress_rate BETWEEN 0 AND 100),
    assignee_id         BIGINT
                            CONSTRAINT fk_task_assignee
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    planned_start_date  DATE,
    planned_end_date    DATE,
    actual_start_date   DATE,
    actual_end_date     DATE,
    planned_effort      DECIMAL(10,2),
    actual_effort       DECIMAL(10,2),
    depth               INT             NOT NULL DEFAULT 0,
    sort_order          INT             NOT NULL DEFAULT 0,
    description         TEXT,
    delay_days          INT             GENERATED ALWAYS AS (
                            CASE
                                WHEN status != 'DONE' AND planned_end_date IS NOT NULL AND planned_end_date < CURRENT_DATE
                                THEN CAST(CURRENT_DATE - planned_end_date AS INT)
                                ELSE 0
                            END
                        ) STORED,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE  pms_task                 IS '업무 (WBS 계층 구조 지원)';
COMMENT ON COLUMN pms_task.parent_task_id  IS '상위 업무 ID (NULL이면 최상위 업무)';
COMMENT ON COLUMN pms_task.task_type       IS '업무 유형: TASK(일반), MILESTONE(마일스톤), PHASE(단계)';
COMMENT ON COLUMN pms_task.progress_rate   IS '진행률 (0~100%)';
COMMENT ON COLUMN pms_task.planned_effort  IS '계획 공수 (MM, Man-Month)';
COMMENT ON COLUMN pms_task.actual_effort   IS '실적 공수 (MM)';
COMMENT ON COLUMN pms_task.depth           IS 'WBS 계층 깊이 (0=최상위)';
COMMENT ON COLUMN pms_task.sort_order      IS '동일 레벨 내 정렬 순서';
COMMENT ON COLUMN pms_task.delay_days      IS '지연 일수 (생성 컬럼: 미완료 && 계획 종료일 초과 시 자동 계산)';

CREATE TRIGGER trg_pms_task_updated_at
    BEFORE UPDATE ON pms_task
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.7 PMS_TASK_ASSIGNMENT_HISTORY (업무 담당자 변경 이력)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_task_assignment_history (
    history_id      BIGSERIAL       PRIMARY KEY,
    task_id         BIGINT          NOT NULL
                        CONSTRAINT fk_tah_task
                        REFERENCES pms_task(task_id)
                        ON DELETE RESTRICT,
    from_user_id    BIGINT
                        CONSTRAINT fk_tah_from_user
                        REFERENCES pms_user(user_id)
                        ON DELETE SET NULL,
    to_user_id      BIGINT          NOT NULL
                        CONSTRAINT fk_tah_to_user
                        REFERENCES pms_user(user_id)
                        ON DELETE RESTRICT,
    changed_by      BIGINT          NOT NULL
                        CONSTRAINT fk_tah_changed_by
                        REFERENCES pms_user(user_id)
                        ON DELETE RESTRICT,
    change_reason   TEXT,
    changed_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

COMMENT ON TABLE  pms_task_assignment_history              IS '업무 담당자 변경 이력';
COMMENT ON COLUMN pms_task_assignment_history.from_user_id IS '이전 담당자 (최초 배정 시 NULL)';
COMMENT ON COLUMN pms_task_assignment_history.to_user_id   IS '신규 담당자';
COMMENT ON COLUMN pms_task_assignment_history.changed_by   IS '변경을 수행한 사용자';
COMMENT ON COLUMN pms_task_assignment_history.changed_at   IS '담당자 변경 일시';

CREATE TRIGGER trg_pms_tah_updated_at
    BEFORE UPDATE ON pms_task_assignment_history
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.8 PMS_DELIVERABLE (산출물)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_deliverable (
    deliverable_id      BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL
                            CONSTRAINT fk_deliverable_project
                            REFERENCES pms_project(project_id)
                            ON DELETE RESTRICT,
    task_id             BIGINT
                            CONSTRAINT fk_deliverable_task
                            REFERENCES pms_task(task_id)
                            ON DELETE SET NULL,
    deliverable_name    VARCHAR(300)    NOT NULL,
    deliverable_type    VARCHAR(50),
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                            CONSTRAINT chk_deliverable_status
                            CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED')),
    version_no          VARCHAR(20)     NOT NULL DEFAULT '1.0',
    file_path           VARCHAR(500),
    file_name           VARCHAR(300),
    file_size           BIGINT,
    submitted_by        BIGINT
                            CONSTRAINT fk_deliverable_submitted_by
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    submitted_at        TIMESTAMP,
    reviewed_by         BIGINT
                            CONSTRAINT fk_deliverable_reviewed_by
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    reviewed_at         TIMESTAMP,
    review_comment      TEXT,
    approved_by         BIGINT
                            CONSTRAINT fk_deliverable_approved_by
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    approved_at         TIMESTAMP,
    approval_comment    TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE  pms_deliverable                  IS '산출물 (프로젝트/업무 단위 문서 및 파일)';
COMMENT ON COLUMN pms_deliverable.deliverable_type IS '산출물 유형 (예: 요구사항정의서, 설계서, 보고서)';
COMMENT ON COLUMN pms_deliverable.version_no       IS '버전 번호 (예: 1.0, 2.1)';
COMMENT ON COLUMN pms_deliverable.file_size        IS '파일 크기 (bytes)';
COMMENT ON COLUMN pms_deliverable.status           IS '산출물 상태: DRAFT→SUBMITTED→UNDER_REVIEW→APPROVED/REJECTED';

CREATE TRIGGER trg_pms_deliverable_updated_at
    BEFORE UPDATE ON pms_deliverable
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.9 PMS_ISSUE (이슈/리스크)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_issue (
    issue_id        BIGSERIAL       PRIMARY KEY,
    project_id      BIGINT          NOT NULL
                        CONSTRAINT fk_issue_project
                        REFERENCES pms_project(project_id)
                        ON DELETE RESTRICT,
    task_id         BIGINT
                        CONSTRAINT fk_issue_task
                        REFERENCES pms_task(task_id)
                        ON DELETE SET NULL,
    issue_type      VARCHAR(20)     NOT NULL DEFAULT 'ISSUE'
                        CONSTRAINT chk_issue_type
                        CHECK (issue_type IN ('ISSUE', 'RISK')),
    title           VARCHAR(300)    NOT NULL,
    description     TEXT,
    severity        VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM'
                        CONSTRAINT chk_issue_severity
                        CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                        CONSTRAINT chk_issue_status
                        CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    assignee_id     BIGINT
                        CONSTRAINT fk_issue_assignee
                        REFERENCES pms_user(user_id)
                        ON DELETE SET NULL,
    reporter_id     BIGINT
                        CONSTRAINT fk_issue_reporter
                        REFERENCES pms_user(user_id)
                        ON DELETE SET NULL,
    due_date        DATE,
    resolved_at     TIMESTAMP,
    resolution      TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

COMMENT ON TABLE  pms_issue             IS '이슈 및 리스크';
COMMENT ON COLUMN pms_issue.issue_type  IS '유형: ISSUE(이슈), RISK(리스크)';
COMMENT ON COLUMN pms_issue.severity    IS '심각도: HIGH(높음), MEDIUM(보통), LOW(낮음)';
COMMENT ON COLUMN pms_issue.resolution  IS '해결 방법 및 결과 설명';

CREATE TRIGGER trg_pms_issue_updated_at
    BEFORE UPDATE ON pms_issue
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.10 PMS_ACTION_ITEM (조치 항목)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_action_item (
    action_id           BIGSERIAL       PRIMARY KEY,
    issue_id            BIGINT          NOT NULL
                            CONSTRAINT fk_action_issue
                            REFERENCES pms_issue(issue_id)
                            ON DELETE CASCADE,
    description         TEXT            NOT NULL,
    assignee_id         BIGINT
                            CONSTRAINT fk_action_assignee
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    due_date            DATE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'TODO'
                            CONSTRAINT chk_action_status
                            CHECK (status IN ('TODO','IN_PROGRESS','DONE')),
    completed_at        TIMESTAMP,
    result_description  TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE  pms_action_item              IS '이슈 조치 항목';
COMMENT ON COLUMN pms_action_item.description  IS '조치 내용 설명';
COMMENT ON COLUMN pms_action_item.completed_at IS '조치 완료 일시';
COMMENT ON COLUMN pms_action_item.result_description IS '조치 결과 설명';

CREATE TRIGGER trg_pms_action_item_updated_at
    BEFORE UPDATE ON pms_action_item
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.11 PMS_PROJECT_MEMBER (프로젝트 구성원)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_project_member (
    project_member_id   BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL
                            CONSTRAINT fk_pm_project
                            REFERENCES pms_project(project_id)
                            ON DELETE RESTRICT,
    user_id             BIGINT          NOT NULL
                            CONSTRAINT fk_pm_user
                            REFERENCES pms_user(user_id)
                            ON DELETE RESTRICT,
    company_id          BIGINT
                            CONSTRAINT fk_pm_company
                            REFERENCES pms_company(company_id)
                            ON DELETE SET NULL,
    role_type           VARCHAR(50)     NOT NULL,
    allocation_rate     DECIMAL(5,2)    NOT NULL DEFAULT 100.00
                            CONSTRAINT chk_pm_allocation CHECK (allocation_rate BETWEEN 0.01 AND 100.00),
    join_date           DATE            NOT NULL,
    leave_date          DATE,
    is_active           BOOLEAN         NOT NULL DEFAULT true,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    CONSTRAINT chk_pm_dates CHECK (leave_date IS NULL OR leave_date >= join_date)
);

COMMENT ON TABLE  pms_project_member                IS '프로젝트 구성원';
COMMENT ON COLUMN pms_project_member.role_type      IS '프로젝트 역할: PM, TECH_LEAD, DEVELOPER, ANALYST, QA, DESIGNER 등';
COMMENT ON COLUMN pms_project_member.allocation_rate IS '투입률 (%) - 0.01~100.00';
COMMENT ON COLUMN pms_project_member.join_date      IS '프로젝트 합류일';
COMMENT ON COLUMN pms_project_member.leave_date     IS '프로젝트 이탈일 (NULL이면 현재 참여 중)';
COMMENT ON COLUMN pms_project_member.is_active      IS '활성 구성원 여부';

CREATE TRIGGER trg_pms_project_member_updated_at
    BEFORE UPDATE ON pms_project_member
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.12 PMS_MEMBER_TIME_OFF (구성원 휴가/부재)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_member_time_off (
    time_off_id         BIGSERIAL       PRIMARY KEY,
    project_member_id   BIGINT          NOT NULL
                            CONSTRAINT fk_timeoff_member
                            REFERENCES pms_project_member(project_member_id)
                            ON DELETE RESTRICT,
    off_date            DATE            NOT NULL,
    off_type            VARCHAR(20)     NOT NULL
                            CONSTRAINT chk_timeoff_type
                            CHECK (off_type IN ('ANNUAL','HALF','QUARTER','SICK','PUBLIC','COMPENSATORY')),
    off_hours           DECIMAL(4,2)    NOT NULL
                            CONSTRAINT chk_timeoff_hours CHECK (off_hours > 0 AND off_hours <= 8),
    reason              TEXT,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                            CONSTRAINT chk_timeoff_approval
                            CHECK (approval_status IN ('PENDING','APPROVED','REJECTED')),
    approved_by         BIGINT
                            CONSTRAINT fk_timeoff_approved_by
                            REFERENCES pms_user(user_id)
                            ON DELETE SET NULL,
    approved_at         TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE  pms_member_time_off              IS '프로젝트 구성원 휴가/부재 기록';
COMMENT ON COLUMN pms_member_time_off.off_type     IS '휴가 유형: ANNUAL(연차), HALF(반차), QUARTER(반반차), SICK(병가), PUBLIC(공휴일), COMPENSATORY(대체휴가)';
COMMENT ON COLUMN pms_member_time_off.off_hours    IS '휴가 시간 (0 초과 ~ 8 이하)';
COMMENT ON COLUMN pms_member_time_off.approval_status IS '승인 상태: PENDING(대기), APPROVED(승인), REJECTED(반려)';

CREATE TRIGGER trg_pms_member_time_off_updated_at
    BEFORE UPDATE ON pms_member_time_off
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.13 PMS_NOTIFICATION_RULE (알림 규칙)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_notification_rule (
    rule_id         BIGSERIAL       PRIMARY KEY,
    project_id      BIGINT
                        CONSTRAINT fk_noti_rule_project
                        REFERENCES pms_project(project_id)
                        ON DELETE CASCADE,
    rule_name       VARCHAR(200)    NOT NULL,
    rule_type       VARCHAR(50)     NOT NULL,
    target_type     VARCHAR(20)     NOT NULL,
    condition_json  JSONB,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    advance_days    INT             NOT NULL DEFAULT 3
                        CONSTRAINT chk_noti_advance CHECK (advance_days >= 0),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

COMMENT ON TABLE  pms_notification_rule              IS '알림 규칙 (전역 또는 프로젝트 특정)';
COMMENT ON COLUMN pms_notification_rule.project_id   IS '프로젝트 ID (NULL이면 전역 규칙)';
COMMENT ON COLUMN pms_notification_rule.rule_type    IS '규칙 유형: DELAY, APPROVAL_PENDING, DELIVERABLE_MISSING, DEADLINE_APPROACHING, STATUS_CHANGE';
COMMENT ON COLUMN pms_notification_rule.target_type  IS '알림 수신 대상: ASSIGNEE(담당자), PM, REVIEWER(검토자), ALL(전체)';
COMMENT ON COLUMN pms_notification_rule.condition_json IS '알림 발생 조건 JSON';
COMMENT ON COLUMN pms_notification_rule.advance_days IS '마감 N일 전 알림 발송';

CREATE TRIGGER trg_pms_notification_rule_updated_at
    BEFORE UPDATE ON pms_notification_rule
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.14 PMS_NOTIFICATION (알림)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_notification (
    notification_id     BIGSERIAL       PRIMARY KEY,
    rule_id             BIGINT
                            CONSTRAINT fk_noti_rule
                            REFERENCES pms_notification_rule(rule_id)
                            ON DELETE SET NULL,
    user_id             BIGINT          NOT NULL
                            CONSTRAINT fk_noti_user
                            REFERENCES pms_user(user_id)
                            ON DELETE CASCADE,
    title               VARCHAR(300)    NOT NULL,
    message             TEXT,
    notification_type   VARCHAR(50),
    entity_type         VARCHAR(50),
    entity_id           BIGINT,
    is_read             BOOLEAN         NOT NULL DEFAULT false,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

COMMENT ON TABLE  pms_notification                   IS '알림 (수신자별 알림 메시지)';
COMMENT ON COLUMN pms_notification.rule_id           IS '생성 규칙 ID (NULL이면 수동 발송 알림)';
COMMENT ON COLUMN pms_notification.entity_type       IS '연관 엔티티 유형 (TASK, ISSUE, DELIVERABLE 등)';
COMMENT ON COLUMN pms_notification.entity_id         IS '연관 엔티티 ID';
COMMENT ON COLUMN pms_notification.is_read           IS '읽음 여부';
COMMENT ON COLUMN pms_notification.read_at           IS '읽은 일시';

CREATE TRIGGER trg_pms_notification_updated_at
    BEFORE UPDATE ON pms_notification
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.15 PMS_ACTIVITY_LOG (활동 로그 - 불변 이력 테이블)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_activity_log (
    log_id          BIGSERIAL       PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    project_id      BIGINT
                        CONSTRAINT fk_actlog_project
                        REFERENCES pms_project(project_id)
                        ON DELETE SET NULL,
    action          VARCHAR(100)    NOT NULL,
    performed_by    BIGINT          NOT NULL
                        CONSTRAINT fk_actlog_user
                        REFERENCES pms_user(user_id)
                        ON DELETE RESTRICT,
    details         JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  pms_activity_log             IS '활동 로그 (불변 감사 이력 - updated_at/updated_by 없음)';
COMMENT ON COLUMN pms_activity_log.entity_type IS '대상 엔티티 유형 (PROJECT, TASK, ISSUE, DELIVERABLE 등)';
COMMENT ON COLUMN pms_activity_log.entity_id   IS '대상 엔티티 ID';
COMMENT ON COLUMN pms_activity_log.action      IS '수행 액션 (CREATE, UPDATE, DELETE, STATUS_CHANGE 등)';
COMMENT ON COLUMN pms_activity_log.details     IS '변경 상세 정보 (이전값/이후값 포함 JSON)';
COMMENT ON COLUMN pms_activity_log.ip_address  IS '접속 IP 주소 (IPv4/IPv6 최대 45자)';

-- 활동 로그는 불변이므로 UPDATE 방지 트리거
CREATE OR REPLACE FUNCTION fn_deny_actlog_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'pms_activity_log is immutable. Updates are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pms_activity_log_immutable
    BEFORE UPDATE OR DELETE ON pms_activity_log
    FOR EACH ROW EXECUTE FUNCTION fn_deny_actlog_update();

-- -----------------------------------------------------------------------------
-- 2.16 PMS_DOCUMENT_TEMPLATE (문서 템플릿)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_document_template (
    template_id     BIGSERIAL       PRIMARY KEY,
    template_name   VARCHAR(200)    NOT NULL,
    template_type   VARCHAR(50),
    description     TEXT,
    template_tags   JSONB,
    file_path       VARCHAR(500),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

COMMENT ON TABLE  pms_document_template              IS '문서 템플릿';
COMMENT ON COLUMN pms_document_template.template_type IS '템플릿 유형 (예: 계획서, 보고서, 회의록)';
COMMENT ON COLUMN pms_document_template.template_tags IS '사용 태그 목록 JSON 배열 (예: ["PROJECT_NAME","PM_NAME"])';
COMMENT ON COLUMN pms_document_template.file_path    IS '템플릿 파일 저장 경로';

CREATE TRIGGER trg_pms_document_template_updated_at
    BEFORE UPDATE ON pms_document_template
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -----------------------------------------------------------------------------
-- 2.17 PMS_TEMPLATE_TAG_MAPPING (템플릿 태그 매핑)
-- -----------------------------------------------------------------------------
CREATE TABLE pms_template_tag_mapping (
    mapping_id      BIGSERIAL       PRIMARY KEY,
    template_id     BIGINT          NOT NULL
                        CONSTRAINT fk_tagmap_template
                        REFERENCES pms_document_template(template_id)
                        ON DELETE CASCADE,
    tag_name        VARCHAR(100)    NOT NULL,
    data_source     VARCHAR(100)    NOT NULL,
    description     VARCHAR(300),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT
);

COMMENT ON TABLE  pms_template_tag_mapping             IS '문서 템플릿 태그 매핑';
COMMENT ON COLUMN pms_template_tag_mapping.tag_name    IS '템플릿 내 태그명 (예: PROJECT_NAME, PM_NAME)';
COMMENT ON COLUMN pms_template_tag_mapping.data_source IS '데이터 소스 경로 (예: pms_project.project_name)';

CREATE TRIGGER trg_pms_template_tag_mapping_updated_at
    BEFORE UPDATE ON pms_template_tag_mapping
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- =============================================================================
-- 3. INDEXES
-- =============================================================================

-- PMS_USER
CREATE INDEX idx_user_email     ON pms_user (email);
CREATE INDEX idx_user_role      ON pms_user (role);
CREATE INDEX idx_user_active    ON pms_user (user_id) WHERE is_active = true;

-- PMS_COMPANY
CREATE INDEX idx_company_type   ON pms_company (company_type);
CREATE INDEX idx_company_active ON pms_company (is_active);

-- PMS_PROJECT
CREATE INDEX idx_project_pm         ON pms_project (pm_id);
CREATE INDEX idx_project_client     ON pms_project (client_company_id);
CREATE INDEX idx_project_status     ON pms_project (status);
CREATE INDEX idx_project_start_date ON pms_project (planned_start_date);
CREATE INDEX idx_project_end_date   ON pms_project (planned_end_date);

-- PMS_STATUS
CREATE INDEX idx_status_domain      ON pms_status (domain);
CREATE INDEX idx_status_is_initial  ON pms_status (domain, is_initial) WHERE is_initial = true;
CREATE INDEX idx_status_is_final    ON pms_status (domain, is_final)   WHERE is_final   = true;

-- PMS_STATUS_TRANSITION
CREATE INDEX idx_st_domain_from     ON pms_status_transition (domain, from_status_code);

-- PMS_TASK
CREATE INDEX idx_task_project       ON pms_task (project_id);
CREATE INDEX idx_task_parent        ON pms_task (parent_task_id);
CREATE INDEX idx_task_assignee      ON pms_task (assignee_id);
CREATE INDEX idx_task_status        ON pms_task (status);
CREATE INDEX idx_task_end_date      ON pms_task (planned_end_date);
CREATE INDEX idx_task_depth_sort    ON pms_task (project_id, depth, sort_order);
CREATE INDEX idx_task_delayed       ON pms_task (project_id, planned_end_date)
    WHERE status != 'DONE';

-- PMS_TASK_ASSIGNMENT_HISTORY
CREATE INDEX idx_tah_task       ON pms_task_assignment_history (task_id);
CREATE INDEX idx_tah_from_user  ON pms_task_assignment_history (from_user_id);
CREATE INDEX idx_tah_to_user    ON pms_task_assignment_history (to_user_id);
CREATE INDEX idx_tah_changed_at ON pms_task_assignment_history (changed_at);

-- PMS_DELIVERABLE
CREATE INDEX idx_deliverable_project    ON pms_deliverable (project_id);
CREATE INDEX idx_deliverable_task       ON pms_deliverable (task_id);
CREATE INDEX idx_deliverable_status     ON pms_deliverable (status);
CREATE INDEX idx_deliverable_submitted  ON pms_deliverable (submitted_by);

-- PMS_ISSUE
CREATE INDEX idx_issue_project      ON pms_issue (project_id);
CREATE INDEX idx_issue_task         ON pms_issue (task_id);
CREATE INDEX idx_issue_assignee     ON pms_issue (assignee_id);
CREATE INDEX idx_issue_reporter     ON pms_issue (reporter_id);
CREATE INDEX idx_issue_status       ON pms_issue (status);
CREATE INDEX idx_issue_severity     ON pms_issue (severity);
CREATE INDEX idx_issue_type         ON pms_issue (issue_type);
CREATE INDEX idx_issue_due_date     ON pms_issue (due_date);

-- PMS_ACTION_ITEM
CREATE INDEX idx_action_issue       ON pms_action_item (issue_id);
CREATE INDEX idx_action_assignee    ON pms_action_item (assignee_id);
CREATE INDEX idx_action_status      ON pms_action_item (status);
CREATE INDEX idx_action_due_date    ON pms_action_item (due_date);

-- PMS_PROJECT_MEMBER
CREATE INDEX idx_pm_project     ON pms_project_member (project_id);
CREATE INDEX idx_pm_user        ON pms_project_member (user_id);
CREATE INDEX idx_pm_company     ON pms_project_member (company_id);
CREATE INDEX idx_pm_is_active   ON pms_project_member (project_id, is_active);
-- 부분 유니크 인덱스: 활성 구성원은 프로젝트 내 중복 불가
CREATE UNIQUE INDEX idx_pm_active_unique
    ON pms_project_member (project_id, user_id)
    WHERE is_active = true;

-- PMS_MEMBER_TIME_OFF
CREATE INDEX idx_timeoff_member         ON pms_member_time_off (project_member_id);
CREATE INDEX idx_timeoff_date           ON pms_member_time_off (off_date);
CREATE INDEX idx_timeoff_approval       ON pms_member_time_off (approval_status);
CREATE INDEX idx_timeoff_approved_by    ON pms_member_time_off (approved_by);

-- PMS_NOTIFICATION_RULE
CREATE INDEX idx_noti_rule_project      ON pms_notification_rule (project_id);
CREATE INDEX idx_noti_rule_type         ON pms_notification_rule (rule_type);
CREATE INDEX idx_noti_rule_active       ON pms_notification_rule (rule_type)
    WHERE is_active = true;

-- PMS_NOTIFICATION
CREATE INDEX idx_noti_user          ON pms_notification (user_id);
CREATE INDEX idx_noti_rule_fk       ON pms_notification (rule_id);
CREATE INDEX idx_noti_entity        ON pms_notification (entity_type, entity_id);
CREATE INDEX idx_noti_created       ON pms_notification (user_id, created_at DESC);
-- 부분 인덱스: 읽지 않은 알림 빠른 조회
CREATE INDEX idx_noti_unread        ON pms_notification (user_id, created_at DESC)
    WHERE is_read = false;

-- PMS_ACTIVITY_LOG
CREATE INDEX idx_actlog_entity      ON pms_activity_log (entity_type, entity_id);
CREATE INDEX idx_actlog_project     ON pms_activity_log (project_id);
CREATE INDEX idx_actlog_performed   ON pms_activity_log (performed_by);
CREATE INDEX idx_actlog_created     ON pms_activity_log (created_at DESC);
CREATE INDEX idx_actlog_action      ON pms_activity_log (action);

-- PMS_DOCUMENT_TEMPLATE
CREATE INDEX idx_template_type      ON pms_document_template (template_type);
CREATE INDEX idx_template_active    ON pms_document_template (is_active);

-- PMS_TEMPLATE_TAG_MAPPING
CREATE INDEX idx_tagmap_template    ON pms_template_tag_mapping (template_id);
CREATE INDEX idx_tagmap_tag_name    ON pms_template_tag_mapping (tag_name);

-- =============================================================================
-- 4. SEED DATA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 4.1 PMS_STATUS 초기 데이터
-- -----------------------------------------------------------------------------

-- TASK 도메인 상태
INSERT INTO pms_status (domain, status_code, status_name, is_initial, is_final, sort_order, color_code, created_by) VALUES
    ('TASK', 'TODO',        '대기',     true,  false, 10, '#9E9E9E', 1),
    ('TASK', 'IN_PROGRESS', '진행중',   false, false, 20, '#2196F3', 1),
    ('TASK', 'REVIEW',      '검토중',   false, false, 30, '#FF9800', 1),
    ('TASK', 'REJECTED',    '반려',     false, false, 40, '#F44336', 1),
    ('TASK', 'DONE',        '완료',     false, true,  50, '#4CAF50', 1);

-- PROJECT 도메인 상태
INSERT INTO pms_status (domain, status_code, status_name, is_initial, is_final, sort_order, color_code, created_by) VALUES
    ('PROJECT', 'PLANNING',     '기획중',   true,  false, 10, '#9C27B0', 1),
    ('PROJECT', 'IN_PROGRESS',  '진행중',   false, false, 20, '#2196F3', 1),
    ('PROJECT', 'ON_HOLD',      '보류',     false, false, 30, '#FF9800', 1),
    ('PROJECT', 'COMPLETED',    '완료',     false, true,  40, '#4CAF50', 1),
    ('PROJECT', 'CANCELLED',    '취소',     false, true,  50, '#9E9E9E', 1);

-- DELIVERABLE 도메인 상태
INSERT INTO pms_status (domain, status_code, status_name, is_initial, is_final, sort_order, color_code, created_by) VALUES
    ('DELIVERABLE', 'DRAFT',        '초안',     true,  false, 10, '#9E9E9E', 1),
    ('DELIVERABLE', 'SUBMITTED',    '제출됨',   false, false, 20, '#2196F3', 1),
    ('DELIVERABLE', 'UNDER_REVIEW', '검토중',   false, false, 30, '#FF9800', 1),
    ('DELIVERABLE', 'APPROVED',     '승인됨',   false, true,  40, '#4CAF50', 1),
    ('DELIVERABLE', 'REJECTED',     '반려됨',   false, false, 50, '#F44336', 1);

-- ISSUE 도메인 상태
INSERT INTO pms_status (domain, status_code, status_name, is_initial, is_final, sort_order, color_code, created_by) VALUES
    ('ISSUE', 'OPEN',        '오픈',     true,  false, 10, '#F44336', 1),
    ('ISSUE', 'IN_PROGRESS', '처리중',   false, false, 20, '#2196F3', 1),
    ('ISSUE', 'RESOLVED',    '해결됨',   false, false, 30, '#8BC34A', 1),
    ('ISSUE', 'CLOSED',      '종료',     false, true,  40, '#9E9E9E', 1);

-- -----------------------------------------------------------------------------
-- 4.2 PMS_STATUS_TRANSITION 초기 데이터
-- -----------------------------------------------------------------------------

-- TASK 상태 전이
INSERT INTO pms_status_transition (domain, from_status_code, to_status_code, created_by) VALUES
    ('TASK', 'TODO',        'IN_PROGRESS', 1),
    ('TASK', 'IN_PROGRESS', 'REVIEW',      1),
    ('TASK', 'IN_PROGRESS', 'DONE',        1),
    ('TASK', 'REVIEW',      'DONE',        1),
    ('TASK', 'REVIEW',      'REJECTED',    1),
    ('TASK', 'REJECTED',    'IN_PROGRESS', 1),
    ('TASK', 'DONE',        'IN_PROGRESS', 1); -- 재오픈

-- PROJECT 상태 전이
INSERT INTO pms_status_transition (domain, from_status_code, to_status_code, created_by) VALUES
    ('PROJECT', 'PLANNING',    'IN_PROGRESS', 1),
    ('PROJECT', 'PLANNING',    'CANCELLED',   1),
    ('PROJECT', 'IN_PROGRESS', 'ON_HOLD',     1),
    ('PROJECT', 'IN_PROGRESS', 'COMPLETED',   1),
    ('PROJECT', 'IN_PROGRESS', 'CANCELLED',   1),
    ('PROJECT', 'ON_HOLD',     'IN_PROGRESS', 1),
    ('PROJECT', 'ON_HOLD',     'CANCELLED',   1);

-- DELIVERABLE 상태 전이
INSERT INTO pms_status_transition (domain, from_status_code, to_status_code, created_by) VALUES
    ('DELIVERABLE', 'DRAFT',        'SUBMITTED',    1),
    ('DELIVERABLE', 'SUBMITTED',    'UNDER_REVIEW', 1),
    ('DELIVERABLE', 'UNDER_REVIEW', 'APPROVED',     1),
    ('DELIVERABLE', 'UNDER_REVIEW', 'REJECTED',     1),
    ('DELIVERABLE', 'REJECTED',     'DRAFT',        1);

-- ISSUE 상태 전이
INSERT INTO pms_status_transition (domain, from_status_code, to_status_code, created_by) VALUES
    ('ISSUE', 'OPEN',        'IN_PROGRESS', 1),
    ('ISSUE', 'IN_PROGRESS', 'RESOLVED',   1),
    ('ISSUE', 'IN_PROGRESS', 'CLOSED',     1),
    ('ISSUE', 'RESOLVED',    'CLOSED',     1),
    ('ISSUE', 'RESOLVED',    'IN_PROGRESS',1), -- 재오픈
    ('ISSUE', 'CLOSED',      'IN_PROGRESS',1); -- 재오픈

-- -----------------------------------------------------------------------------
-- 4.3 PMS_NOTIFICATION_RULE 기본 전역 규칙
-- -----------------------------------------------------------------------------
INSERT INTO pms_notification_rule
    (project_id, rule_name, rule_type, target_type, condition_json, is_active, advance_days, created_by)
VALUES
    -- 전역: 마감 3일 전 담당자 알림
    (NULL,
     '업무 마감 임박 알림 (3일 전)',
     'DEADLINE_APPROACHING',
     'ASSIGNEE',
     '{"entity": "TASK", "days_before": 3}',
     true,
     3,
     1),

    -- 전역: 업무 지연 발생 시 PM 알림
    (NULL,
     '업무 지연 PM 알림',
     'DELAY',
     'PM',
     '{"entity": "TASK", "condition": "delay_days > 0"}',
     true,
     0,
     1),

    -- 전역: 산출물 승인 대기 검토자 알림
    (NULL,
     '산출물 승인 대기 알림',
     'APPROVAL_PENDING',
     'REVIEWER',
     '{"entity": "DELIVERABLE", "status": "UNDER_REVIEW", "wait_hours": 24}',
     true,
     0,
     1),

    -- 전역: 이슈 마감 2일 전 담당자 알림
    (NULL,
     '이슈 처리 기한 임박 알림 (2일 전)',
     'DEADLINE_APPROACHING',
     'ASSIGNEE',
     '{"entity": "ISSUE", "days_before": 2}',
     true,
     2,
     1);

-- =============================================================================
-- 5. 유용한 VIEW (선택적)
-- =============================================================================

-- 지연 업무 현황 뷰
CREATE OR REPLACE VIEW v_delayed_tasks AS
SELECT
    t.task_id,
    t.project_id,
    p.project_name,
    t.task_name,
    t.status,
    t.planned_end_date,
    t.delay_days,
    t.assignee_id,
    u.full_name     AS assignee_name,
    pm_user.full_name AS pm_name
FROM pms_task t
JOIN pms_project p ON t.project_id = p.project_id
LEFT JOIN pms_user u  ON t.assignee_id  = u.user_id
LEFT JOIN pms_user pm_user ON p.pm_id   = pm_user.user_id
WHERE t.status != 'DONE'
  AND t.planned_end_date IS NOT NULL
  AND t.planned_end_date < CURRENT_DATE;

COMMENT ON VIEW v_delayed_tasks IS '지연 업무 현황 (완료되지 않았고 계획 종료일이 지난 업무)';

-- 프로젝트별 진행률 요약 뷰
CREATE OR REPLACE VIEW v_project_progress_summary AS
SELECT
    p.project_id,
    p.project_name,
    p.status,
    p.planned_start_date,
    p.planned_end_date,
    COUNT(t.task_id)                                                AS total_tasks,
    COUNT(t.task_id) FILTER (WHERE t.status = 'DONE')              AS done_tasks,
    COUNT(t.task_id) FILTER (WHERE t.delay_days > 0)               AS delayed_tasks,
    ROUND(
        COUNT(t.task_id) FILTER (WHERE t.status = 'DONE')::NUMERIC
        / NULLIF(COUNT(t.task_id), 0) * 100, 1
    )                                                               AS completion_rate,
    COALESCE(AVG(t.progress_rate), 0)                              AS avg_progress_rate
FROM pms_project p
LEFT JOIN pms_task t ON t.project_id = p.project_id
GROUP BY p.project_id, p.project_name, p.status, p.planned_start_date, p.planned_end_date;

COMMENT ON VIEW v_project_progress_summary IS '프로젝트별 업무 진행률 요약';

-- 미읽은 알림 뷰
CREATE OR REPLACE VIEW v_unread_notifications AS
SELECT
    n.notification_id,
    n.user_id,
    u.full_name     AS user_name,
    n.title,
    n.message,
    n.notification_type,
    n.entity_type,
    n.entity_id,
    n.created_at
FROM pms_notification n
JOIN pms_user u ON n.user_id = u.user_id
WHERE n.is_read = false
ORDER BY n.created_at DESC;

COMMENT ON VIEW v_unread_notifications IS '읽지 않은 알림 목록';

-- =============================================================================
-- END OF SCHEMA
-- =============================================================================
