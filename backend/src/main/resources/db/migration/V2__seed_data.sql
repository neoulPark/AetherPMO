-- Seed data matching the frontend mock

-- Users (password = bcrypt of "password")
INSERT INTO pms_user (username, email, password, full_name, role) VALUES
    ('ahnyk',  'ahnyk@aetherpmo.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '안유경', 'PM'),
    ('leeyh',  'leeyh@aetherpmo.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이영희', 'PM'),
    ('kimcs',  'kimcs@aetherpmo.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김철수', 'PM'),
    ('kimjh',  'kimjh@aetherpmo.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김준현', 'MEMBER'),
    ('parkjm', 'parkjm@aetherpmo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박지민', 'MEMBER'),
    ('kangdw', 'kangdw@aetherpmo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '강동우', 'MEMBER');

-- Companies
INSERT INTO pms_company (company_name, company_type) VALUES
    ('국립정보자원관리원', 'CLIENT'),
    ('국민건강보험공단',   'CLIENT'),
    ('오케스트로',         'OWN');

-- Projects
INSERT INTO pms_project
    (project_name, project_code, description, pm_id, client_company_id, status, project_stage,
     planned_start_date, planned_end_date, contract_amount, progress_rate, team)
VALUES
    ('차세대 스마트홈 IoT 플랫폼 구축', 'PRJ-2026-001',
     '가정용 디바이스 연동 및 지능형 가전 원격 제어를 위한 고가용성 클라우드 기반 IoT 플랫폼 설계 및 구축 프로젝트.',
     (SELECT user_id FROM pms_user WHERE username = 'ahnyk'),
     (SELECT company_id FROM pms_company WHERE company_name = '국립정보자원관리원'),
     'IN_PROGRESS', 'EXECUTION', DATE '2026-03-02', DATE '2026-08-31', 2450000000, 65, '개발팀'),
    ('AI 기반 다국어 고객 상담 어시스턴트 개발', 'PRJ-2026-002',
     'AI 기반 다국어 고객 상담 어시스턴트 개발 프로젝트.',
     (SELECT user_id FROM pms_user WHERE username = 'leeyh'),
     (SELECT company_id FROM pms_company WHERE company_name = '국민건강보험공단'),
     'IN_PROGRESS', 'EXECUTION', DATE '2026-04-10', DATE '2026-05-31', 1800000000, 45, '기획팀');

-- Tasks for project 1 (tree)
DO $$
DECLARE
    pid      BIGINT;
    ahnyk_id BIGINT;
    parkjm_id BIGINT;
    kimjh_id BIGINT;
    p_analysis BIGINT;
    p_design   BIGINT;
    p_dev      BIGINT;
BEGIN
    SELECT project_id INTO pid FROM pms_project WHERE project_code = 'PRJ-2026-001';
    SELECT user_id INTO ahnyk_id  FROM pms_user WHERE username = 'ahnyk';
    SELECT user_id INTO parkjm_id FROM pms_user WHERE username = 'parkjm';
    SELECT user_id INTO kimjh_id  FROM pms_user WHERE username = 'kimjh';

    -- 분석
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, NULL, '분석', 'DONE', 100, 0, 0) RETURNING task_id INTO p_analysis;
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, assignee_id, depth, sort_order)
    VALUES (pid, p_analysis, '요구사항 분석', 'DONE', 100, ahnyk_id, 1, 0);
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, p_analysis, '현행업무 분석', 'DONE', 100, 1, 1);

    -- 설계
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, NULL, '설계', 'IN_PROGRESS', 67, 0, 1) RETURNING task_id INTO p_design;
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, p_design, '화면설계', 'DONE', 100, 1, 0);
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, assignee_id, depth, sort_order)
    VALUES (pid, p_design, 'DB설계', 'IN_PROGRESS', 70, parkjm_id, 1, 1);
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, p_design, '인터페이스설계', 'IN_PROGRESS', 30, 1, 2);

    -- 개발
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, NULL, '개발', 'IN_PROGRESS', 30, 0, 2) RETURNING task_id INTO p_dev;
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, assignee_id, depth, sort_order)
    VALUES (pid, p_dev, '사용자 기능 개발', 'IN_PROGRESS', 40, kimjh_id, 1, 0);
    INSERT INTO pms_task (project_id, parent_task_id, task_name, status, progress_rate, depth, sort_order)
    VALUES (pid, p_dev, '관리자 기능 개발', 'IN_PROGRESS', 20, 1, 1);
END $$;
