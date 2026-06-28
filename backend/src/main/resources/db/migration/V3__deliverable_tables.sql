-- Deliverable (산출물) vertical slice

CREATE TABLE pms_deliverable (
    deliverable_id   BIGSERIAL PRIMARY KEY,
    project_id       BIGINT NOT NULL REFERENCES pms_project(project_id) ON DELETE CASCADE,
    task_id          BIGINT REFERENCES pms_task(task_id) ON DELETE SET NULL,
    deliverable_name VARCHAR(300) NOT NULL,
    deliverable_type VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED')),
    version_no       VARCHAR(20) DEFAULT '1.0',
    submitted_by     BIGINT,
    submitted_at     TIMESTAMP,
    reviewed_by      BIGINT,
    reviewed_at      TIMESTAMP,
    review_comment   TEXT,
    approved_by      BIGINT,
    approved_at      TIMESTAMP,
    approval_comment TEXT,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP,
    created_by       BIGINT,
    updated_by       BIGINT
);

CREATE TABLE pms_attachment (
    attachment_id BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(30) NOT NULL,
    entity_id     BIGINT NOT NULL,
    file_ref      VARCHAR(200) NOT NULL,
    file_name     VARCHAR(300),
    file_size     BIGINT,
    content_type  VARCHAR(100),
    sort_order    INT DEFAULT 0,
    uploaded_by   VARCHAR(100),
    uploaded_at   TIMESTAMP DEFAULT NOW(),
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP
);

-- Indexes
CREATE INDEX idx_deliverable_project ON pms_deliverable(project_id);
CREATE INDEX idx_deliverable_task    ON pms_deliverable(task_id);
CREATE INDEX idx_deliverable_status  ON pms_deliverable(status);
CREATE INDEX idx_attachment_entity   ON pms_attachment(entity_type, entity_id);
CREATE INDEX idx_attachment_fileref  ON pms_attachment(file_ref);
