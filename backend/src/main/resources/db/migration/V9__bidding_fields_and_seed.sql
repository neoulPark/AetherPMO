-- V9: Bidding-stage specific fields + seed of 2 BIDDING projects and a proposal-phase WBS.
-- Production-safe: ADD COLUMN IF NOT EXISTS, FK resolution via subquery, inserts guarded by WHERE NOT EXISTS.

-- 1a. Bidding-specific columns on pms_project
ALTER TABLE pms_project ADD COLUMN IF NOT EXISTS bid_status        VARCHAR(20);   -- PREPARING/SUBMITTED/WAITING/WON/LOST
ALTER TABLE pms_project ADD COLUMN IF NOT EXISTS consortium_role   VARCHAR(100);
ALTER TABLE pms_project ADD COLUMN IF NOT EXISTS consortium_share  DECIMAL(5,2);
ALTER TABLE pms_project ADD COLUMN IF NOT EXISTS vrb_status        VARCHAR(50);
ALTER TABLE pms_project ADD COLUMN IF NOT EXISTS announcement_no   VARCHAR(100);
ALTER TABLE pms_project ADD COLUMN IF NOT EXISTS proposal_deadline DATE;

-- Client company for bidding projects (조달청). Insert if not present so FK can resolve.
INSERT INTO pms_company (company_name, company_type)
SELECT '조달청(행정안전부)', 'CLIENT'
WHERE NOT EXISTS (SELECT 1 FROM pms_company WHERE company_name = '조달청(행정안전부)');

-- 1b. Seed Project A: 전사 통합 ERP 시스템 고도화 및 클라우드 이전
INSERT INTO pms_project (
    project_name, description, pm_id, client_company_id,
    status, project_stage, bid_status, team, contract_amount, risk_level,
    consortium_role, vrb_status, proposal_deadline,
    planned_start_date, planned_end_date
)
SELECT
    '전사 통합 ERP 시스템 고도화 및 클라우드 이전',
    '레거시 온프레미스 ERP 핵심 인프라를 AWS 클라우드로 이관하고 고도화하는 사업.',
    (SELECT user_id FROM pms_user WHERE full_name = '김철수' LIMIT 1),
    (SELECT company_id FROM pms_company WHERE company_name = '조달청(행정안전부)' LIMIT 1),
    'PLANNING', 'BIDDING', 'PREPARING', '개발팀', 8900000000, '보통',
    '미지정', 'VRB : 미상신', DATE '2026-06-30',
    DATE '2026-06-15', DATE '2026-06-30'
WHERE NOT EXISTS (
    SELECT 1 FROM pms_project WHERE project_name = '전사 통합 ERP 시스템 고도화 및 클라우드 이전'
);

-- 1b. Seed Project B: 대법원 차세대 등기정보시스템 구축 및 인프라 보강
INSERT INTO pms_project (
    project_name, description, pm_id, client_company_id,
    status, project_stage, bid_status, team, contract_amount, risk_level,
    consortium_role, vrb_status, proposal_deadline,
    planned_start_date, planned_end_date
)
SELECT
    '대법원 차세대 등기정보시스템 구축 및 인프라 보강',
    '대법원 차세대 등기정보시스템을 구축하고 관련 인프라를 보강하는 사업.',
    (SELECT user_id FROM pms_user WHERE full_name = '안유경' LIMIT 1),
    (SELECT company_id FROM pms_company WHERE company_name = '조달청(행정안전부)' LIMIT 1),
    'PLANNING', 'BIDDING', 'SUBMITTED', '개발팀', 4200000000, '보통',
    '미지정', 'VRB : 미상신', DATE '2026-06-30',
    DATE '2026-06-15', DATE '2026-06-30'
WHERE NOT EXISTS (
    SELECT 1 FROM pms_project WHERE project_name = '대법원 차세대 등기정보시스템 구축 및 인프라 보강'
);

-- 1c. Proposal-phase WBS for Project A
DO $$
DECLARE
    pid           BIGINT;
    p_prep        BIGINT;  -- 사업준비
    p_kickoff     BIGINT;  -- 착수 준비
BEGIN
    SELECT project_id INTO pid
      FROM pms_project
     WHERE project_name = '전사 통합 ERP 시스템 고도화 및 클라우드 이전'
     LIMIT 1;

    IF pid IS NULL THEN
        RETURN;
    END IF;

    -- Skip if WBS already seeded for this project (re-run safety)
    IF EXISTS (SELECT 1 FROM pms_task WHERE project_id = pid) THEN
        RETURN;
    END IF;

    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, NULL, '사업준비', 'TODO', 0, 0, 1)
    RETURNING task_id INTO p_prep;

    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order,
                          planned_start_date, planned_end_date)
    VALUES (pid, p_prep, '제안서 작성', 'TODO', 0, 1, 1, DATE '2026-06-15', DATE '2026-06-25');

    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order,
                          planned_start_date, planned_end_date)
    VALUES (pid, p_prep, '제안 발표 준비', 'TODO', 0, 1, 2, DATE '2026-06-26', DATE '2026-06-30');

    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, NULL, '착수 준비', 'TODO', 0, 0, 2)
    RETURNING task_id INTO p_kickoff;

    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, p_kickoff, '컨소시엄 구성', 'TODO', 0, 1, 1);
END $$;
