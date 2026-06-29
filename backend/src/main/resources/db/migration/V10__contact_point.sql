-- Contact Point (컨택포인트): per-project contacts, INTERNAL (user ref) or EXTERNAL (manual)
CREATE TABLE pms_contact_point (
    contact_id   BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES pms_project(project_id) ON DELETE CASCADE,
    field        VARCHAR(100),                 -- 분야/역할 (영업, 기술, 계약, 법무, 고객사 PM 등)
    contact_type VARCHAR(20) NOT NULL CHECK (contact_type IN ('INTERNAL','EXTERNAL')),
    user_id      BIGINT,                        -- INTERNAL: 아마란스/내부 사용자 ref
    name         VARCHAR(100),                  -- EXTERNAL(또는 INTERNAL 캐시)
    company      VARCHAR(200),
    department   VARCHAR(100),
    title        VARCHAR(100),
    phone        VARCHAR(50),
    email        VARCHAR(150),
    note         TEXT,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_contact_point_project ON pms_contact_point(project_id);

-- Example contacts for project 1 (guarded so re-runs / partial data stay safe)
INSERT INTO pms_contact_point (project_id, field, contact_type, name, company, phone, sort_order)
SELECT 1, '고객사 PM', 'EXTERNAL', '김부장', '국립정보자원관리원', '010-1234-5678', 0
WHERE EXISTS (SELECT 1 FROM pms_project WHERE project_id = 1)
  AND NOT EXISTS (SELECT 1 FROM pms_contact_point WHERE project_id = 1 AND field = '고객사 PM');

INSERT INTO pms_contact_point (project_id, field, contact_type, user_id, sort_order)
SELECT 1, '기술 PM', 'INTERNAL',
       (SELECT user_id FROM pms_user WHERE full_name = '안유경' LIMIT 1), 1
WHERE EXISTS (SELECT 1 FROM pms_project WHERE project_id = 1)
  AND EXISTS (SELECT 1 FROM pms_user WHERE full_name = '안유경')
  AND NOT EXISTS (SELECT 1 FROM pms_contact_point WHERE project_id = 1 AND field = '기술 PM');
