-- V7: Status workflow (Jira-like) feature.
-- Defines workflows (statuses + transitions), assignable to catalog TASK nodes.
-- NOTE: No fn_set_updated_at trigger is used; columns rely on DEFAULT NOW() + JPA auditing.

CREATE TABLE pms_workflow (
    workflow_id  BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE pms_workflow_status (
    status_id    BIGSERIAL PRIMARY KEY,
    workflow_id  BIGINT NOT NULL REFERENCES pms_workflow(workflow_id) ON DELETE CASCADE,
    code         VARCHAR(40),
    name         VARCHAR(100) NOT NULL,
    color        VARCHAR(20),                 -- hex or token
    category     VARCHAR(20) CHECK (category IN ('TODO','IN_PROGRESS','DONE')),
    is_initial   BOOLEAN NOT NULL DEFAULT FALSE,
    is_final     BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_workflow_status_wf ON pms_workflow_status(workflow_id);

CREATE TABLE pms_workflow_transition (
    transition_id  BIGSERIAL PRIMARY KEY,
    workflow_id    BIGINT NOT NULL REFERENCES pms_workflow(workflow_id) ON DELETE CASCADE,
    from_status_id BIGINT NOT NULL REFERENCES pms_workflow_status(status_id) ON DELETE CASCADE,
    to_status_id   BIGINT NOT NULL REFERENCES pms_workflow_status(status_id) ON DELETE CASCADE,
    name           VARCHAR(100),
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_workflow_transition_wf ON pms_workflow_transition(workflow_id);

ALTER TABLE pms_catalog_node
    ADD COLUMN workflow_id BIGINT REFERENCES pms_workflow(workflow_id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- Seed a default workflow + assign to all TASK nodes
-- ---------------------------------------------------------------------------
INSERT INTO pms_workflow (name, description, is_default)
VALUES ('기본 업무 워크플로', '기본 제공 업무 상태 워크플로', TRUE);

INSERT INTO pms_workflow_status (workflow_id, code, name, color, category, is_initial, is_final, sort_order)
SELECT w.workflow_id, v.code, v.name, v.color, v.category, v.is_initial, v.is_final, v.sort_order
FROM pms_workflow w
CROSS JOIN (VALUES
    ('TODO',        '대기',   '#4a5568', 'TODO',        TRUE,  FALSE, 1),
    ('IN_PROGRESS', '진행중', '#0984e3', 'IN_PROGRESS', FALSE, FALSE, 2),
    ('REVIEW',      '검토중', '#f39c12', 'IN_PROGRESS', FALSE, FALSE, 3),
    ('DONE',        '완료',   '#00b894', 'DONE',        FALSE, TRUE,  4),
    ('REJECTED',    '반려',   '#e74c3c', 'IN_PROGRESS', FALSE, FALSE, 5)
) AS v(code, name, color, category, is_initial, is_final, sort_order)
WHERE w.is_default;

INSERT INTO pms_workflow_transition (workflow_id, from_status_id, to_status_id, name)
SELECT fs.workflow_id, fs.status_id, ts.status_id, t.name
FROM (VALUES
    ('대기',   '진행중', '시작'),
    ('진행중', '검토중', '검토 요청'),
    ('검토중', '완료',   '승인'),
    ('검토중', '반려',   '반려'),
    ('반려',   '진행중', '재작업'),
    ('진행중', '완료',   '완료 처리')
) AS t(from_name, to_name, name)
JOIN pms_workflow_status fs
  ON fs.name = t.from_name
 AND fs.workflow_id = (SELECT workflow_id FROM pms_workflow WHERE is_default LIMIT 1)
JOIN pms_workflow_status ts
  ON ts.name = t.to_name
 AND ts.workflow_id = (SELECT workflow_id FROM pms_workflow WHERE is_default LIMIT 1);

UPDATE pms_catalog_node
SET workflow_id = (SELECT workflow_id FROM pms_workflow WHERE is_default LIMIT 1)
WHERE node_type = 'TASK';
