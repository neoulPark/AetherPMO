-- 프로젝트 1(차세대 스마트홈 IoT) 업무에 기본 일정(계획 시작/종료일) 부여
-- WBS 간트 차트가 데이터 없이 비지 않도록 시드 보강. (parent는 UI에서 자동 롤업되므로 leaf만 설정)
UPDATE pms_task SET planned_start_date = DATE '2026-03-02', planned_end_date = DATE '2026-03-20'
 WHERE project_id = 1 AND task_name = '요구사항 분석';
UPDATE pms_task SET planned_start_date = DATE '2026-03-10', planned_end_date = DATE '2026-03-31'
 WHERE project_id = 1 AND task_name = '현행업무 분석';
UPDATE pms_task SET planned_start_date = DATE '2026-04-01', planned_end_date = DATE '2026-04-20'
 WHERE project_id = 1 AND task_name = '화면설계';
UPDATE pms_task SET planned_start_date = DATE '2026-04-10', planned_end_date = DATE '2026-05-10'
 WHERE project_id = 1 AND task_name = 'DB설계';
UPDATE pms_task SET planned_start_date = DATE '2026-04-25', planned_end_date = DATE '2026-05-20'
 WHERE project_id = 1 AND task_name = '인터페이스설계';
UPDATE pms_task SET planned_start_date = DATE '2026-05-15', planned_end_date = DATE '2026-06-30'
 WHERE project_id = 1 AND task_name = '사용자 기능 개발';
UPDATE pms_task SET planned_start_date = DATE '2026-06-01', planned_end_date = DATE '2026-07-15'
 WHERE project_id = 1 AND task_name = '관리자 기능 개발';
