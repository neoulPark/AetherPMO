-- AetherPMO core tables (vertical slice)

CREATE TABLE pms_user (
    user_id    BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255),
    full_name  VARCHAR(100),
    role       VARCHAR(20) CHECK (role IN ('ADMIN', 'PM', 'MEMBER')),
    is_active  BOOLEAN   DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE pms_company (
    company_id   BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL,
    company_type VARCHAR(20) CHECK (company_type IN ('OWN', 'PARTNER', 'CLIENT')),
    is_active    BOOLEAN   DEFAULT true,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP
);

CREATE TABLE pms_project (
    project_id         BIGSERIAL PRIMARY KEY,
    project_name       VARCHAR(200) NOT NULL,
    project_code       VARCHAR(50) UNIQUE,
    description        TEXT,
    pm_id              BIGINT REFERENCES pms_user(user_id),
    client_company_id  BIGINT REFERENCES pms_company(company_id),
    status             VARCHAR(20) CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED')) DEFAULT 'PLANNING',
    project_stage      VARCHAR(20) CHECK (project_stage IN ('BIDDING', 'EXECUTION', 'COMPLETED')) DEFAULT 'EXECUTION',
    planned_start_date DATE,
    planned_end_date   DATE,
    actual_start_date  DATE,
    actual_end_date    DATE,
    contract_amount    DECIMAL(15, 2),
    progress_rate      INT DEFAULT 0,
    risk_level         VARCHAR(10) DEFAULT '보통',
    team               VARCHAR(100),
    location           VARCHAR(200),
    business_type      VARCHAR(100),
    created_at         TIMESTAMP DEFAULT NOW(),
    updated_at         TIMESTAMP,
    created_by         BIGINT,
    updated_by         BIGINT
);

CREATE TABLE pms_task (
    task_id            BIGSERIAL PRIMARY KEY,
    parent_task_id     BIGINT REFERENCES pms_task(task_id) ON DELETE CASCADE,
    project_id         BIGINT NOT NULL REFERENCES pms_project(project_id) ON DELETE CASCADE,
    task_name          VARCHAR(300) NOT NULL,
    status             VARCHAR(20) CHECK (status IN ('TODO', 'IN_PROGRESS', 'REVIEW', 'REJECTED', 'DONE')) DEFAULT 'TODO',
    progress_rate      INT DEFAULT 0 CHECK (progress_rate BETWEEN 0 AND 100),
    assignee_id        BIGINT REFERENCES pms_user(user_id),
    planned_start_date DATE,
    planned_end_date   DATE,
    actual_start_date  DATE,
    actual_end_date    DATE,
    planned_effort     DECIMAL(10, 2),
    actual_effort      DECIMAL(10, 2),
    depth              INT DEFAULT 0,
    sort_order         INT DEFAULT 0,
    description        TEXT,
    created_at         TIMESTAMP DEFAULT NOW(),
    updated_at         TIMESTAMP,
    created_by         BIGINT,
    updated_by         BIGINT
);

CREATE TABLE pms_task_assignment_history (
    history_id    BIGSERIAL PRIMARY KEY,
    task_id       BIGINT NOT NULL REFERENCES pms_task(task_id) ON DELETE CASCADE,
    from_user_id  BIGINT,
    to_user_id    BIGINT,
    changed_by    BIGINT,
    change_reason TEXT,
    changed_at    TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_project_pm           ON pms_project(pm_id);
CREATE INDEX idx_project_client       ON pms_project(client_company_id);
CREATE INDEX idx_project_status       ON pms_project(status);
CREATE INDEX idx_project_stage        ON pms_project(project_stage);
CREATE INDEX idx_task_parent          ON pms_task(parent_task_id);
CREATE INDEX idx_task_project         ON pms_task(project_id);
CREATE INDEX idx_task_assignee        ON pms_task(assignee_id);
CREATE INDEX idx_task_status          ON pms_task(status);
CREATE INDEX idx_assign_hist_task     ON pms_task_assignment_history(task_id);
