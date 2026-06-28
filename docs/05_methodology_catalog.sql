-- =====================================================================
-- AetherPMO : OPMS 표준 방법론 카탈로그 (Methodology Catalog)
-- ORCHESTRO Project Management Standard
-- =====================================================================
-- 구조: 단계(Phase) → 활동(Activity) → Task 템플릿(세부활동) → 산출물 템플릿(Deliverable)
-- 신규 프로젝트 생성 시 이 카탈로그에서 필요한 항목을 선택(테일러링)하여
-- 실제 프로젝트의 Task / 산출물로 인스턴스화한다.
-- =====================================================================
-- [변경] 구 템플릿 테이블 폐기
--   - pms_document_template  → pms_deliverable_template 로 흡수 (file_path/template_tags)
--   - pms_template_tag_mapping → pms_deliverable_tag_mapping 로 대체
-- =====================================================================

-- ---- 구 템플릿 테이블 폐기 (의존 객체 함께 제거) -------------------
DROP TABLE IF EXISTS pms_template_tag_mapping CASCADE;
DROP TABLE IF EXISTS pms_document_template    CASCADE;

-- ---------------------------------------------------------------------
-- C1. PMS_METHODOLOGY_PHASE (방법론 단계)
-- ---------------------------------------------------------------------
CREATE TABLE pms_methodology_phase (
    phase_id        BIGSERIAL       PRIMARY KEY,
    phase_code      VARCHAR(10)     NOT NULL UNIQUE,     -- PRR / PRP / PPC / PED
    phase_name      VARCHAR(100)    NOT NULL,            -- 사업준비 / 착수계획 / 실행통제 / 종료
    phase_name_en   VARCHAR(100),                        -- Project Preparation 등
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);
COMMENT ON TABLE  pms_methodology_phase IS 'OPMS 방법론 단계 (4단계)';
COMMENT ON COLUMN pms_methodology_phase.phase_code IS 'PRR(사업준비), PRP(착수계획), PPC(실행통제), PED(종료)';

CREATE TRIGGER trg_pms_methodology_phase_updated_at
    BEFORE UPDATE ON pms_methodology_phase
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- ---------------------------------------------------------------------
-- C2. PMS_METHODOLOGY_ACTIVITY (방법론 활동)
-- ---------------------------------------------------------------------
CREATE TABLE pms_methodology_activity (
    activity_id     BIGSERIAL       PRIMARY KEY,
    phase_id        BIGINT          NOT NULL
                        REFERENCES pms_methodology_phase(phase_id) ON DELETE CASCADE,
    activity_code   VARCHAR(10)     NOT NULL UNIQUE,     -- OP / PW / CT / TL / PM / CM / SM / IM / EE / IE
    activity_name   VARCHAR(100)    NOT NULL,            -- 사업발주준비 등
    activity_name_en VARCHAR(100),                       -- Order Preparation 등
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);
COMMENT ON TABLE  pms_methodology_activity IS 'OPMS 방법론 활동 (단계 하위)';
COMMENT ON COLUMN pms_methodology_activity.activity_code IS 'OP/PW/CT/TL/PM/CM/SM/IM/EE/IE';

CREATE TRIGGER trg_pms_methodology_activity_updated_at
    BEFORE UPDATE ON pms_methodology_activity
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE INDEX idx_methodology_activity_phase ON pms_methodology_activity(phase_id);

-- ---------------------------------------------------------------------
-- C3. PMS_TASK_TEMPLATE (Task 템플릿 = 세부활동)
-- ---------------------------------------------------------------------
CREATE TABLE pms_task_template (
    task_template_id BIGSERIAL      PRIMARY KEY,
    activity_id     BIGINT          NOT NULL
                        REFERENCES pms_methodology_activity(activity_id) ON DELETE CASCADE,
    task_code       VARCHAR(20)     NOT NULL UNIQUE,     -- OP-1, PW-1, CT-1 ...
    task_name       VARCHAR(200)    NOT NULL,            -- 사업계획지원 등
    description     TEXT,
    is_optional     BOOLEAN         NOT NULL DEFAULT FALSE,  -- 선택 항목 여부
    default_selected BOOLEAN        NOT NULL DEFAULT TRUE,   -- 신규 프로젝트 기본 선택 여부
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);
COMMENT ON TABLE  pms_task_template IS 'OPMS Task 템플릿 (세부활동, 활동 하위)';
COMMENT ON COLUMN pms_task_template.task_code IS '세부활동 코드 (예: OP-1, CT-2)';
COMMENT ON COLUMN pms_task_template.is_optional IS '테일러링 시 선택적으로 제외 가능한 항목';

CREATE TRIGGER trg_pms_task_template_updated_at
    BEFORE UPDATE ON pms_task_template
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE INDEX idx_task_template_activity ON pms_task_template(activity_id);

-- ---------------------------------------------------------------------
-- C4. PMS_DELIVERABLE_TEMPLATE (산출물 템플릿)
-- ---------------------------------------------------------------------
CREATE TABLE pms_deliverable_template (
    deliverable_template_id BIGSERIAL PRIMARY KEY,
    task_template_id BIGINT         NOT NULL
                        REFERENCES pms_task_template(task_template_id) ON DELETE CASCADE,
    seq_no          INT             NOT NULL,            -- 10, 20, 30 ...
    deliverable_name VARCHAR(300)   NOT NULL,            -- 사업계획서 등
    deliverable_category VARCHAR(100),                   -- 산출물 구분 (계획서/보고서/대장 등)
    stage           VARCHAR(20)     CHECK (stage IN ('INCEPTION','EXECUTION','CLOSURE')),
                                                          -- 산출물 템플릿 관리 화면 단계 구분 (착수/수행/종료)
    version_no      VARCHAR(20)     DEFAULT '1.0',        -- 템플릿 버전
    description     TEXT,
    is_optional     BOOLEAN         NOT NULL DEFAULT FALSE,  -- (선택) 표기 산출물
    default_selected BOOLEAN        NOT NULL DEFAULT TRUE,
    -- 문서 자동화(태그 치환) : 구 pms_document_template 흡수
    template_file_ref VARCHAR(200),                       -- 표준 양식 파일 (아마란스 file_id, 1개)
    file_name       VARCHAR(300),                         -- 다운로드 파일명(표시용)
    template_tags   JSONB,                                -- 사용 태그 목록 (예: ["PROJECT_NAME","PM_NAME"])
    sort_order      INT             NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW(),
    CONSTRAINT uq_deliverable_template_seq UNIQUE (task_template_id, seq_no)
);
COMMENT ON TABLE  pms_deliverable_template IS 'OPMS 산출물 템플릿 (Task 템플릿 하위, 문서 자동화 템플릿 통합)';
COMMENT ON COLUMN pms_deliverable_template.seq_no IS '산출물 순번 ([10],[20],[30])';
COMMENT ON COLUMN pms_deliverable_template.stage IS 'INCEPTION(착수)/EXECUTION(수행)/CLOSURE(종료)';
COMMENT ON COLUMN pms_deliverable_template.template_tags IS '문서 자동화 태그 목록 JSON 배열';

CREATE TRIGGER trg_pms_deliverable_template_updated_at
    BEFORE UPDATE ON pms_deliverable_template
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE INDEX idx_deliverable_template_task  ON pms_deliverable_template(task_template_id);
CREATE INDEX idx_deliverable_template_stage ON pms_deliverable_template(stage);

-- ---------------------------------------------------------------------
-- C4-1. PMS_DELIVERABLE_TAG_MAPPING (산출물 템플릿 태그 매핑)
-- ---------------------------------------------------------------------
-- 구 pms_template_tag_mapping 대체. 문서 자동화 태그 → 데이터 소스 매핑.
CREATE TABLE pms_deliverable_tag_mapping (
    mapping_id      BIGSERIAL       PRIMARY KEY,
    deliverable_template_id BIGINT  NOT NULL
                        REFERENCES pms_deliverable_template(deliverable_template_id) ON DELETE CASCADE,
    tag_name        VARCHAR(100)    NOT NULL,            -- PROJECT_NAME, PM_NAME 등
    data_source     VARCHAR(200)    NOT NULL,            -- pms_project.project_name 등
    description     VARCHAR(300),
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW(),
    CONSTRAINT uq_deliverable_tag UNIQUE (deliverable_template_id, tag_name)
);
COMMENT ON TABLE  pms_deliverable_tag_mapping IS '산출물 템플릿 문서 자동화 태그 매핑 (구 pms_template_tag_mapping 대체)';
COMMENT ON COLUMN pms_deliverable_tag_mapping.data_source IS '치환 데이터 소스 (테이블.컬럼)';

CREATE TRIGGER trg_pms_deliverable_tag_mapping_updated_at
    BEFORE UPDATE ON pms_deliverable_tag_mapping
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE INDEX idx_deliverable_tag_template ON pms_deliverable_tag_mapping(deliverable_template_id);
CREATE INDEX idx_deliverable_tag_name     ON pms_deliverable_tag_mapping(tag_name);

-- ---------------------------------------------------------------------
-- C5. PMS_PROJECT_TAILORING (프로젝트 테일러링 선택 내역)
-- ---------------------------------------------------------------------
-- 신규 프로젝트 생성 시 카탈로그에서 어떤 Task/산출물을 채택/제외했는지 기록한다.
CREATE TABLE pms_project_tailoring (
    tailoring_id    BIGSERIAL       PRIMARY KEY,
    project_id      BIGINT          NOT NULL
                        REFERENCES pms_project(project_id) ON DELETE CASCADE,
    task_template_id BIGINT         REFERENCES pms_task_template(task_template_id) ON DELETE SET NULL,
    deliverable_template_id BIGINT  REFERENCES pms_deliverable_template(deliverable_template_id) ON DELETE SET NULL,
    is_selected     BOOLEAN         NOT NULL DEFAULT TRUE,   -- 채택 여부
    exclude_reason  TEXT,                                    -- 제외 사유 (테일러링 근거)
    generated_task_id BIGINT        REFERENCES pms_task(task_id) ON DELETE SET NULL,        -- 생성된 실제 Task
    generated_deliverable_id BIGINT REFERENCES pms_deliverable(deliverable_id) ON DELETE SET NULL, -- 생성된 실제 산출물
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);
COMMENT ON TABLE  pms_project_tailoring IS '프로젝트 테일러링 선택 내역 (카탈로그 → 프로젝트 적용 매핑)';
COMMENT ON COLUMN pms_project_tailoring.is_selected IS '해당 항목 채택 여부 (FALSE=테일러링 제외)';
COMMENT ON COLUMN pms_project_tailoring.exclude_reason IS '제외 사유 (테일러링 결과서 근거)';

CREATE TRIGGER trg_pms_project_tailoring_updated_at
    BEFORE UPDATE ON pms_project_tailoring
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE INDEX idx_project_tailoring_project ON pms_project_tailoring(project_id);
CREATE INDEX idx_project_tailoring_task_tpl ON pms_project_tailoring(task_template_id);

-- ---------------------------------------------------------------------
-- C6. 기존 테이블 확장 (템플릿 추적 컬럼)
-- ---------------------------------------------------------------------
ALTER TABLE pms_task
    ADD COLUMN task_template_id BIGINT
        REFERENCES pms_task_template(task_template_id) ON DELETE SET NULL;
COMMENT ON COLUMN pms_task.task_template_id IS '생성 출처 Task 템플릿 (OPMS 카탈로그)';

ALTER TABLE pms_deliverable
    ADD COLUMN deliverable_template_id BIGINT
        REFERENCES pms_deliverable_template(deliverable_template_id) ON DELETE SET NULL;
COMMENT ON COLUMN pms_deliverable.deliverable_template_id IS '생성 출처 산출물 템플릿 (OPMS 카탈로그)';


-- =====================================================================
-- 시드 데이터 : OPMS 표준 방법론 카탈로그
-- =====================================================================

-- ---- 단계 (Phase) -------------------------------------------------
INSERT INTO pms_methodology_phase (phase_code, phase_name, phase_name_en, sort_order) VALUES
('PRR', '사업준비', 'Project Preparation', 1),
('PRP', '착수계획', 'Project Plan',        2),
('PPC', '실행통제', 'Project Control',     3),
('PED', '종료',     'Project End',         4);

-- ---- 활동 (Activity) ----------------------------------------------
INSERT INTO pms_methodology_activity (phase_id, activity_code, activity_name, activity_name_en, sort_order)
SELECT phase_id, v.code, v.name, v.name_en, v.ord
FROM (VALUES
    ('PRR','OP','사업발주준비','Order Preparation',1),
    ('PRR','PW','제안작업',    'Project Work',     2),
    ('PRP','CT','계약(착수)',  'Contract',         1),
    ('PRP','TL','테일러링',    'Tailoring',        2),
    ('PRP','PM','사업관리계획','Project Management',3),
    ('PPC','CM','핵심관리',    'Core Management',  1),
    ('PPC','SM','단순관리',    'Simple Management',2),
    ('PPC','IM','내부관리',    'Internal Management',3),
    ('PED','EE','외부종료',    'External End',     1),
    ('PED','IE','내부종료',    'Internal End',     2)
) AS v(phase_code, code, name, name_en, ord)
JOIN pms_methodology_phase p ON p.phase_code = v.phase_code;

-- ---- Task 템플릿 (세부활동) ---------------------------------------
INSERT INTO pms_task_template (activity_id, task_code, task_name, is_optional, sort_order)
SELECT a.activity_id, v.task_code, v.task_name, v.is_optional, v.ord
FROM (VALUES
    -- OP 사업발주준비
    ('OP','OP-1','사업계획지원', FALSE, 1),
    ('OP','OP-2','RFP 지원',     FALSE, 2),
    -- PW 제안작업
    ('PW','PW-1','제안서 작성',  FALSE, 1),
    ('PW','PW-2','제안발표',     FALSE, 2),
    -- CT 계약(착수)
    ('CT','CT-1','계약체결',     FALSE, 1),
    ('CT','CT-2','착수계 제출',  FALSE, 2),
    ('CT','CT-3','투입인력확정', FALSE, 3),
    -- TL 테일러링
    ('TL','TL-1','테일러링',     FALSE, 1),
    -- PM 사업관리계획 (10종)
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
    -- CM 핵심관리
    ('CM','CM-1','범위관리/통제',     FALSE, 1),
    ('CM','CM-2','일정관리/통제',     FALSE, 2),
    ('CM','CM-3','위험/이슈관리/통제',FALSE, 3),
    ('CM','CM-4','품질관리/통제',     FALSE, 4),
    ('CM','CM-5','인력관리/통제',     FALSE, 5),
    ('CM','CM-6','형상관리/통제',     FALSE, 6),
    ('CM','CM-7','변경관리/통제',     FALSE, 7),
    -- SM 단순관리
    ('SM','SM-1','의사소통관리',  FALSE, 1),
    ('SM','SM-2','보안관리/통제', FALSE, 2),
    ('SM','SM-3','안전관리/통제', FALSE, 3),
    -- IM 내부관리
    ('IM','IM-1','환경수립',      FALSE, 1),
    ('IM','IM-2','프로젝트 통제', FALSE, 2),
    -- EE 외부종료
    ('EE','EE-1','검수',          FALSE, 1),
    ('EE','EE-2','사업완료',      FALSE, 2),
    -- IE 내부종료
    ('IE','IE-1','프로젝트 종료', FALSE, 1)
) AS v(activity_code, task_code, task_name, is_optional, ord)
JOIN pms_methodology_activity a ON a.activity_code = v.activity_code;

-- ---- 산출물 템플릿 (Deliverable) ----------------------------------
INSERT INTO pms_deliverable_template (task_template_id, seq_no, deliverable_name, is_optional, sort_order)
SELECT t.task_template_id, v.seq_no, v.name, v.is_optional, v.seq_no
FROM (VALUES
    -- OP-1 사업계획지원
    ('OP-1',10,'사업계획서',        FALSE),
    ('OP-1',20,'비용산출내역서',    FALSE),
    -- OP-2 RFP 지원
    ('OP-2',10,'제안공고서',        FALSE),
    ('OP-2',20,'제안요청서',        FALSE),
    -- PW-1 제안서 작성
    ('PW-1',10,'제안서',            FALSE),
    ('PW-1',20,'발표자료',          FALSE),
    ('PW-1',30,'제안요약서',        TRUE),
    -- PW-2 제안발표
    ('PW-2',10,'예상질문서',        FALSE),
    -- CT-1 계약체결
    ('CT-1',10,'계약서',            FALSE),
    ('CT-1',20,'과업지시서',        FALSE),
    ('CT-1',30,'하도급계약서',      FALSE),
    ('CT-1',40,'하도급 승인신청',   FALSE),
    ('CT-1',50,'기술협상안',        FALSE),
    -- CT-2 착수계 제출
    ('CT-2',10,'착수계',            FALSE),
    ('CT-2',20,'용역자책임자계',    FALSE),
    ('CT-2',30,'사업수행계획서',    FALSE),
    ('CT-2',40,'산출내역서',        FALSE),
    -- CT-3 투입인력확정
    ('CT-3',10,'인력투입계획표',    FALSE),
    ('CT-3',20,'비상연락망',        FALSE),
    ('CT-3',30,'조직도',            FALSE),
    ('CT-3',40,'자리배치도',        FALSE),
    ('CT-3',50,'업무분장표',        FALSE),
    -- TL-1 테일러링
    ('TL-1',10,'테일러링가이드',    FALSE),
    ('TL-1',20,'테일러링 결과서',   FALSE),
    -- PM 사업관리 계획서 10종
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
    -- CM-1 범위관리/통제
    ('CM-1',10,'요구사항추적표',    FALSE),
    ('CM-1',20,'요구사항정의서',    FALSE),
    -- CM-2 일정관리/통제
    ('CM-2',10,'WBS',               FALSE),
    -- CM-3 위험/이슈관리/통제
    ('CM-3',10,'위험보고서',        FALSE),
    ('CM-3',20,'이슈보고서',        FALSE),
    ('CM-3',30,'이슈해결보고서',    FALSE),
    ('CM-3',40,'위험이슈통제결과표',FALSE),
    -- CM-4 품질관리/통제
    ('CM-4',10,'품질목표정의서',    FALSE),
    ('CM-4',20,'품질검토결과보고서',FALSE),
    -- CM-5 인력관리/통제
    ('CM-5',10,'투입인력보고',      FALSE),
    -- CM-6 형상관리/통제
    ('CM-6',10,'형상관리대장',      FALSE),
    ('CM-6',20,'형상기준선',        FALSE),
    -- CM-7 변경관리/통제
    ('CM-7',10,'변경요청관리대장',  FALSE),
    ('CM-7',20,'변경요청서',        FALSE),
    -- SM-1 의사소통관리
    ('SM-1',10,'회의록',            FALSE),
    ('SM-1',20,'주간업무보고',      FALSE),
    ('SM-1',30,'월간업무보고',      FALSE),
    ('SM-1',40,'수시보고서',        FALSE),
    ('SM-1',50,'공문관리대장',      FALSE),
    -- SM-2 보안관리/통제
    ('SM-2',10,'보안관리대장',      FALSE),
    -- SM-3 안전관리/통제
    ('SM-3',10,'안전보건수행 결과서',FALSE),
    -- IM-1 환경수립
    ('IM-1',10,'프로젝트 렌탈/장비 구매 품의', FALSE),
    ('IM-1',20,'원가 품의(외주 업체 등)',      FALSE),
    ('IM-1',30,'인장날인/공문 발신',           FALSE),
    -- IM-2 프로젝트 통제
    ('IM-2',10,'프로젝트 공수보고(월별)',      FALSE),
    ('IM-2',20,'변경요청 공문(인력/과업 등)',  FALSE),
    ('IM-2',30,'원가 관리(월별)',              FALSE),
    -- EE-1 검수
    ('EE-1',10,'검수계획서',        FALSE),
    ('EE-1',20,'검수요청서(공문)',  FALSE),
    ('EE-1',30,'준공검사확인서',    FALSE),
    ('EE-1',40,'검사확인서',        FALSE),
    ('EE-1',50,'준공조서',          FALSE),
    ('EE-1',60,'인수인계계획서',    FALSE),
    ('EE-1',70,'기능점수산출내역',  FALSE),
    -- EE-2 사업완료
    ('EE-2',10,'프로젝트 완료보고서',FALSE),
    ('EE-2',20,'최종산출물',        FALSE),
    ('EE-2',30,'무상 하자보수계획서',FALSE),
    -- IE-1 프로젝트 종료
    ('IE-1',10,'프로젝트 공수보고', FALSE),
    ('IE-1',20,'프로젝트 종료보고', FALSE)
) AS v(task_code, seq_no, name, is_optional)
JOIN pms_task_template t ON t.task_code = v.task_code;

-- =====================================================================
-- 카탈로그 조회 뷰 : 단계 → 활동 → Task → 산출물 평면화
-- =====================================================================
CREATE OR REPLACE VIEW vw_methodology_catalog AS
SELECT
    p.phase_code, p.phase_name, p.sort_order   AS phase_order,
    a.activity_code, a.activity_name, a.sort_order AS activity_order,
    t.task_code, t.task_name, t.is_optional    AS task_optional, t.sort_order AS task_order,
    d.seq_no, d.deliverable_name, d.is_optional AS deliverable_optional,
    d.deliverable_template_id, t.task_template_id
FROM pms_methodology_phase p
JOIN pms_methodology_activity a ON a.phase_id = p.phase_id
JOIN pms_task_template t        ON t.activity_id = a.activity_id
LEFT JOIN pms_deliverable_template d ON d.task_template_id = t.task_template_id
ORDER BY p.sort_order, a.sort_order, t.sort_order, d.seq_no;

COMMENT ON VIEW vw_methodology_catalog IS 'OPMS 카탈로그 평면화 뷰 (테일러링 선택 화면용)';
