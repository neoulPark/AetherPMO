-- Seed deliverables for project 1 (matching the frontend mock)

DO $$
DECLARE
    pid       BIGINT;
    ahnyk_id  BIGINT;
    parkjm_id BIGINT;
    kimjh_id  BIGINT;
    d1        BIGINT;
BEGIN
    SELECT project_id INTO pid       FROM pms_project WHERE project_code = 'PRJ-2026-001';
    SELECT user_id    INTO ahnyk_id  FROM pms_user    WHERE username = 'ahnyk';
    SELECT user_id    INTO parkjm_id FROM pms_user    WHERE username = 'parkjm';
    SELECT user_id    INTO kimjh_id  FROM pms_user    WHERE username = 'kimjh';

    -- 1. 요구사항정의서 (APPROVED, 안유경)
    INSERT INTO pms_deliverable
        (project_id, deliverable_name, deliverable_type, status, version_no,
         submitted_by, submitted_at, reviewed_by, reviewed_at,
         approved_by, approved_at, approval_comment, created_by, updated_at)
    VALUES
        (pid, 'IoT 플랫폼 요구사항 정의서', '요구사항정의서', 'APPROVED', 'v1.0.0',
         ahnyk_id, TIMESTAMP '2026-03-25 10:00:00', ahnyk_id, TIMESTAMP '2026-03-27 14:00:00',
         ahnyk_id, TIMESTAMP '2026-03-28 09:00:00', '검토 완료, 승인합니다.', ahnyk_id, TIMESTAMP '2026-03-28 09:00:00')
        RETURNING deliverable_id INTO d1;

    -- 2. 시스템설계서 (APPROVED, 박지민)
    INSERT INTO pms_deliverable
        (project_id, deliverable_name, deliverable_type, status, version_no,
         submitted_by, submitted_at, reviewed_by, reviewed_at,
         approved_by, approved_at, approval_comment, created_by, updated_at)
    VALUES
        (pid, '클라우드 인프라 및 DB 아키텍처 설계서', '시스템설계서', 'APPROVED', 'v1.1.0',
         parkjm_id, TIMESTAMP '2026-04-15 10:00:00', ahnyk_id, TIMESTAMP '2026-04-18 14:00:00',
         ahnyk_id, TIMESTAMP '2026-04-19 09:00:00', '아키텍처 검토 완료.', parkjm_id, TIMESTAMP '2026-04-19 09:00:00');

    -- 3. 소스코드 (SUBMITTED / 검토 요청, 김준현)
    INSERT INTO pms_deliverable
        (project_id, deliverable_name, deliverable_type, status, version_no,
         submitted_by, submitted_at, created_by, updated_at)
    VALUES
        (pid, 'IoT API 서버 게이트웨이 모듈 소스코드', '소스코드', 'SUBMITTED', 'v0.9.0',
         kimjh_id, TIMESTAMP '2026-05-24 11:00:00', kimjh_id, TIMESTAMP '2026-05-24 11:00:00');

    -- Attachments for deliverable 1
    INSERT INTO pms_attachment
        (entity_type, entity_id, file_ref, file_name, file_size, content_type, sort_order, uploaded_by, uploaded_at)
    VALUES
        ('DELIVERABLE', d1, 'amaranth-mock-0001', '요구사항정의서_v1.0.0.hwp', 248320, 'application/x-hwp', 0, 'ahnyk', TIMESTAMP '2026-03-25 10:00:00'),
        ('DELIVERABLE', d1, 'amaranth-mock-0002', '요구사항_추적표.xlsx', 51200, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 1, 'ahnyk', TIMESTAMP '2026-03-25 10:05:00');
END $$;
