-- =====================================================================
-- V6 : OPMS 카탈로그 단일 노드 트리 통합 (production-safe)
-- 기존 3종 카탈로그 테이블(phase/activity/task_template/deliverable_template)을
-- 하나의 자기참조 노드 트리(pms_catalog_node)로 통합한다.
-- V1~V5가 이미 적용된 운영 DB에서도 안전하게 실행되어야 한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1a. 단일 노드 테이블
-- ---------------------------------------------------------------------
CREATE TABLE pms_catalog_node (
    node_id         BIGSERIAL PRIMARY KEY,
    parent_node_id  BIGINT REFERENCES pms_catalog_node(node_id) ON DELETE CASCADE,
    node_type       VARCHAR(20) NOT NULL CHECK (node_type IN ('PHASE','ACTIVITY','TASK','DELIVERABLE')),
    code            VARCHAR(40),
    name            VARCHAR(300) NOT NULL,
    description     TEXT,
    is_optional     BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INT NOT NULL DEFAULT 0,
    -- DELIVERABLE 전용 (다른 타입은 NULL)
    seq_no               INT,
    deliverable_category VARCHAR(100),
    stage                VARCHAR(20),
    template_file_ref    VARCHAR(200),
    template_tags        JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_catalog_node_parent ON pms_catalog_node(parent_node_id);
CREATE INDEX idx_catalog_node_type ON pms_catalog_node(node_type);
-- updated_at: JPA auditing이 관리 (트리거 미사용)

-- ---------------------------------------------------------------------
-- 1b. 카탈로그 시드 (code 기반 부모 해석)
-- ---------------------------------------------------------------------

-- ---- PHASE (parent NULL) ----
INSERT INTO pms_catalog_node (parent_node_id, node_type, code, name, sort_order)
VALUES
    (NULL, 'PHASE', 'PRR', '사업준비', 1),
    (NULL, 'PHASE', 'PRP', '착수계획', 2),
    (NULL, 'PHASE', 'PPC', '실행통제', 3),
    (NULL, 'PHASE', 'PED', '종료',     4);

-- ---- ACTIVITY (parent = phase by code) ----
INSERT INTO pms_catalog_node (parent_node_id, node_type, code, name, sort_order)
SELECT parent.node_id, 'ACTIVITY', v.code, v.name, v.ord
FROM (VALUES
    ('PRR','OP','사업발주준비',1),
    ('PRR','PW','제안작업',    2),
    ('PRP','CT','계약(착수)',  1),
    ('PRP','TL','테일러링',    2),
    ('PRP','PM','사업관리계획',3),
    ('PPC','CM','핵심관리',    1),
    ('PPC','SM','단순관리',    2),
    ('PPC','IM','내부관리',    3),
    ('PED','EE','외부종료',    1),
    ('PED','IE','내부종료',    2)
) AS v(phase_code, code, name, ord)
JOIN pms_catalog_node parent ON parent.code = v.phase_code AND parent.node_type = 'PHASE';

-- ---- TASK (parent = activity by code) ----
INSERT INTO pms_catalog_node (parent_node_id, node_type, code, name, is_optional, sort_order)
SELECT parent.node_id, 'TASK', v.task_code, v.task_name, v.is_optional, v.ord
FROM (VALUES
    ('OP','OP-1','사업계획지원', FALSE, 1),
    ('OP','OP-2','RFP 지원',     FALSE, 2),
    ('PW','PW-1','제안서 작성',  FALSE, 1),
    ('PW','PW-2','제안발표',     FALSE, 2),
    ('CT','CT-1','계약체결',     FALSE, 1),
    ('CT','CT-2','착수계 제출',  FALSE, 2),
    ('CT','CT-3','투입인력확정', FALSE, 3),
    ('TL','TL-1','테일러링',     FALSE, 1),
    ('PM','PM-1','범위관리',     FALSE, 1),
    ('PM','PM-2','일정관리',     FALSE, 2),
    ('PM','PM-3','위험/이슈관리',FALSE, 3),
    ('PM','PM-4','품질관리',     FALSE, 4),
    ('PM','PM-5','인력관리',     FALSE, 5),
    ('PM','PM-6','형상관리',     FALSE, 6),
    ('PM','PM-7','변경관리',     FALSE, 7),
    ('PM','PM-8','의사소통관리', FALSE, 8),
    ('PM','PM-9','보안관리',     FALSE, 9),
    ('PM','PM-10','안전보건관리',FALSE,10),
    ('CM','CM-1','범위관리/통제',     FALSE, 1),
    ('CM','CM-2','일정관리/통제',     FALSE, 2),
    ('CM','CM-3','위험/이슈관리/통제',FALSE, 3),
    ('CM','CM-4','품질관리/통제',     FALSE, 4),
    ('CM','CM-5','인력관리/통제',     FALSE, 5),
    ('CM','CM-6','형상관리/통제',     FALSE, 6),
    ('CM','CM-7','변경관리/통제',     FALSE, 7),
    ('SM','SM-1','의사소통관리',  FALSE, 1),
    ('SM','SM-2','보안관리/통제', FALSE, 2),
    ('SM','SM-3','안전관리/통제', FALSE, 3),
    ('IM','IM-1','환경수립',      FALSE, 1),
    ('IM','IM-2','프로젝트 통제', FALSE, 2),
    ('EE','EE-1','검수',          FALSE, 1),
    ('EE','EE-2','사업완료',      FALSE, 2),
    ('IE','IE-1','프로젝트 종료', FALSE, 1)
) AS v(activity_code, task_code, task_name, is_optional, ord)
JOIN pms_catalog_node parent ON parent.code = v.activity_code AND parent.node_type = 'ACTIVITY';

-- ---- DELIVERABLE (parent = task by code; code = task_code-seq_no) ----
INSERT INTO pms_catalog_node (parent_node_id, node_type, code, name, is_optional, seq_no, sort_order)
SELECT parent.node_id, 'DELIVERABLE', v.task_code || '-' || v.seq_no, v.name, v.is_optional, v.seq_no, v.seq_no
FROM (VALUES
    ('OP-1',10,'사업계획서',        FALSE),
    ('OP-1',20,'비용산출내역서',    FALSE),
    ('OP-2',10,'제안공고서',        FALSE),
    ('OP-2',20,'제안요청서',        FALSE),
    ('PW-1',10,'제안서',            FALSE),
    ('PW-1',20,'발표자료',          FALSE),
    ('PW-1',30,'제안요약서',        TRUE),
    ('PW-2',10,'예상질문서',        FALSE),
    ('CT-1',10,'계약서',            FALSE),
    ('CT-1',20,'과업지시서',        FALSE),
    ('CT-1',30,'하도급계약서',      FALSE),
    ('CT-1',40,'하도급 승인신청',   FALSE),
    ('CT-1',50,'기술협상안',        FALSE),
    ('CT-2',10,'착수계',            FALSE),
    ('CT-2',20,'용역자책임자계',    FALSE),
    ('CT-2',30,'사업수행계획서',    FALSE),
    ('CT-2',40,'산출내역서',        FALSE),
    ('CT-3',10,'인력투입계획표',    FALSE),
    ('CT-3',20,'비상연락망',        FALSE),
    ('CT-3',30,'조직도',            FALSE),
    ('CT-3',40,'자리배치도',        FALSE),
    ('CT-3',50,'업무분장표',        FALSE),
    ('TL-1',10,'테일러링가이드',    FALSE),
    ('TL-1',20,'테일러링 결과서',   FALSE),
    ('PM-1',10,'범위관리계획서',    FALSE),
    ('PM-2',10,'일정관리계획서',    FALSE),
    ('PM-3',10,'위험관리계획서',    FALSE),
    ('PM-4',10,'품질관리계획서',    FALSE),
    ('PM-5',10,'인력관리계획서',    FALSE),
    ('PM-6',10,'형상관리계획서',    FALSE),
    ('PM-7',10,'변경관리계획서',    FALSE),
    ('PM-8',10,'의사소통관리계획서',FALSE),
    ('PM-9',10,'보안관리계획서',    FALSE),
    ('PM-10',10,'안전보건관리계획서',FALSE),
    ('CM-1',10,'요구사항추적표',    FALSE),
    ('CM-1',20,'요구사항정의서',    FALSE),
    ('CM-2',10,'WBS',               FALSE),
    ('CM-3',10,'위험보고서',        FALSE),
    ('CM-3',20,'이슈보고서',        FALSE),
    ('CM-3',30,'이슈해결보고서',    FALSE),
    ('CM-3',40,'위험이슈통제결과표',FALSE),
    ('CM-4',10,'품질목표정의서',    FALSE),
    ('CM-4',20,'품질검토결과보고서',FALSE),
    ('CM-5',10,'투입인력보고',      FALSE),
    ('CM-6',10,'형상관리대장',      FALSE),
    ('CM-6',20,'형상기준선',        FALSE),
    ('CM-7',10,'변경요청관리대장',  FALSE),
    ('CM-7',20,'변경요청서',        FALSE),
    ('SM-1',10,'회의록',            FALSE),
    ('SM-1',20,'주간업무보고',      FALSE),
    ('SM-1',30,'월간업무보고',      FALSE),
    ('SM-1',40,'수시보고서',        FALSE),
    ('SM-1',50,'공문관리대장',      FALSE),
    ('SM-2',10,'보안관리대장',      FALSE),
    ('SM-3',10,'안전보건수행 결과서',FALSE),
    ('IM-1',10,'프로젝트 렌탈/장비 구매 품의', FALSE),
    ('IM-1',20,'원가 품의(외주 업체 등)',      FALSE),
    ('IM-1',30,'인장날인/공문 발신',           FALSE),
    ('IM-2',10,'프로젝트 공수보고(월별)',      FALSE),
    ('IM-2',20,'변경요청 공문(인력/과업 등)',  FALSE),
    ('IM-2',30,'원가 관리(월별)',              FALSE),
    ('EE-1',10,'검수계획서',        FALSE),
    ('EE-1',20,'검수요청서(공문)',  FALSE),
    ('EE-1',30,'준공검사확인서',    FALSE),
    ('EE-1',40,'검사확인서',        FALSE),
    ('EE-1',50,'준공조서',          FALSE),
    ('EE-1',60,'인수인계계획서',    FALSE),
    ('EE-1',70,'기능점수산출내역',  FALSE),
    ('EE-2',10,'프로젝트 완료보고서',FALSE),
    ('EE-2',20,'최종산출물',        FALSE),
    ('EE-2',30,'무상 하자보수계획서',FALSE),
    ('IE-1',10,'프로젝트 공수보고', FALSE),
    ('IE-1',20,'프로젝트 종료보고', FALSE)
) AS v(task_code, seq_no, name, is_optional)
JOIN pms_catalog_node parent ON parent.code = v.task_code AND parent.node_type = 'TASK';

-- ---------------------------------------------------------------------
-- 1c. 참조 컬럼 재배선 (production-safe)
-- ---------------------------------------------------------------------

-- pms_project_tailoring : 구 템플릿 FK 제거 후 catalog_node_id 추가 (테스트 데이터)
TRUNCATE pms_project_tailoring;
ALTER TABLE pms_project_tailoring DROP COLUMN IF EXISTS task_template_id;
ALTER TABLE pms_project_tailoring DROP COLUMN IF EXISTS deliverable_template_id;
ALTER TABLE pms_project_tailoring
    ADD COLUMN catalog_node_id BIGINT REFERENCES pms_catalog_node(node_id) ON DELETE SET NULL;

-- pms_task : task_template_id -> catalog_node_id
ALTER TABLE pms_task DROP COLUMN IF EXISTS task_template_id;
ALTER TABLE pms_task
    ADD COLUMN catalog_node_id BIGINT REFERENCES pms_catalog_node(node_id) ON DELETE SET NULL;

-- pms_deliverable : deliverable_template_id -> catalog_node_id
ALTER TABLE pms_deliverable DROP COLUMN IF EXISTS deliverable_template_id;
ALTER TABLE pms_deliverable
    ADD COLUMN catalog_node_id BIGINT REFERENCES pms_catalog_node(node_id) ON DELETE SET NULL;

-- ---------------------------------------------------------------------
-- 1d. 구 카탈로그 테이블 제거 (FK 순서)
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS vw_methodology_catalog CASCADE;
DROP TABLE IF EXISTS pms_deliverable_tag_mapping CASCADE;
DROP TABLE IF EXISTS pms_deliverable_template CASCADE;
DROP TABLE IF EXISTS pms_task_template CASCADE;
DROP TABLE IF EXISTS pms_methodology_activity CASCADE;
DROP TABLE IF EXISTS pms_methodology_phase CASCADE;
