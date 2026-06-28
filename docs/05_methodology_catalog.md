# OPMS 표준 방법론 카탈로그 설계서

> **OPMS** (ORCHESTRO Project Management Standard)
> 공공 SI 사업관리 표준 방법론을 시스템에 내재화하여, 신규 프로젝트 생성 시
> 표준 Task/산출물을 **테일러링(Tailoring)** 방식으로 선택·적용한다.

---

## 1. 개요

### 1.1 목적
- 공공 SI 사업의 표준 산출물 체계를 마스터 카탈로그로 관리
- 신규 프로젝트 생성 시 카탈로그에서 필요한 항목만 선택하여 Task/산출물 자동 생성
- 산출물 누락 방지 및 사업관리 표준화

### 1.2 4계층 구조
```
단계(Phase)  ──→  활동(Activity)  ──→  Task 템플릿(세부활동)  ──→  산출물 템플릿(Deliverable)
  PRR              OP                    OP-1                       [10] 사업계획서
                                                                    [20] 비용산출내역서
```

---

## 2. 방법론 구조 (Master Data)

### 2.1 단계 (Phase) — 4단계

| 코드 | 단계명 | 영문 |
|------|--------|------|
| PRR | 사업준비 | Project Preparation |
| PRP | 착수계획 | Project Plan |
| PPC | 실행통제 | Project Control |
| PED | 종료 | Project End |

### 2.2 활동 (Activity) — 10활동

| 단계 | 코드 | 활동명 | 영문 |
|------|------|--------|------|
| PRR | OP | 사업발주준비 | Order Preparation |
| PRR | PW | 제안작업 | Project Work |
| PRP | CT | 계약(착수) | Contract |
| PRP | TL | 테일러링 | Tailoring |
| PRP | PM | 사업관리계획 | Project Management |
| PPC | CM | 핵심관리 | Core Management |
| PPC | SM | 단순관리 | Simple Management |
| PPC | IM | 내부관리 | Internal Management |
| PED | EE | 외부종료 | External End |
| PED | IE | 내부종료 | Internal End |

### 2.3 Task 템플릿 & 산출물 템플릿 (전체 카탈로그)

#### PRR 사업준비
| Task | 세부활동 | 산출물 |
|------|----------|--------|
| OP-1 | 사업계획지원 | 사업계획서, 비용산출내역서 |
| OP-2 | RFP 지원 | 제안공고서, 제안요청서 |
| PW-1 | 제안서 작성 | 제안서, 발표자료, 제안요약서(선택) |
| PW-2 | 제안발표 | 예상질문서 |

#### PRP 착수계획
| Task | 세부활동 | 산출물 |
|------|----------|--------|
| CT-1 | 계약체결 | 계약서, 과업지시서, 하도급계약서, 하도급 승인신청, 기술협상안 |
| CT-2 | 착수계 제출 | 착수계, 용역자책임자계, 사업수행계획서, 산출내역서 |
| CT-3 | 투입인력확정 | 인력투입계획표, 비상연락망, 조직도, 자리배치도, 업무분장표 |
| TL-1 | 테일러링 | 테일러링가이드, 테일러링 결과서 |
| PM-1 | 범위관리 | 범위관리계획서 |
| PM-2 | 일정관리 | 일정관리계획서 |
| PM-3 | 위험/이슈관리 | 위험관리계획서 |
| PM-4 | 품질관리 | 품질관리계획서 |
| PM-5 | 인력관리 | 인력관리계획서 |
| PM-6 | 형상관리 | 형상관리계획서 |
| PM-7 | 변경관리 | 변경관리계획서 |
| PM-8 | 의사소통관리 | 의사소통관리계획서 |
| PM-9 | 보안관리 | 보안관리계획서 |
| PM-10 | 안전보건관리 | 안전보건관리계획서 |

> PM 활동 = **사업관리 계획서 10종** (범위·일정·위험·품질·인력·형상·변경·의사소통·보안·안전보건)

#### PPC 실행통제
| Task | 세부활동 | 산출물 |
|------|----------|--------|
| CM-1 | 범위관리/통제 | 요구사항추적표, 요구사항정의서 |
| CM-2 | 일정관리/통제 | WBS |
| CM-3 | 위험/이슈관리/통제 | 위험보고서, 이슈보고서, 이슈해결보고서, 위험이슈통제결과표 |
| CM-4 | 품질관리/통제 | 품질목표정의서, 품질검토결과보고서 |
| CM-5 | 인력관리/통제 | 투입인력보고 |
| CM-6 | 형상관리/통제 | 형상관리대장, 형상기준선 |
| CM-7 | 변경관리/통제 | 변경요청관리대장, 변경요청서 |
| SM-1 | 의사소통관리 | 회의록, 주간업무보고, 월간업무보고, 수시보고서, 공문관리대장 |
| SM-2 | 보안관리/통제 | 보안관리대장 |
| SM-3 | 안전관리/통제 | 안전보건수행 결과서 |
| IM-1 | 환경수립 | 프로젝트 렌탈/장비 구매 품의, 원가 품의, 인장날인/공문 발신 |
| IM-2 | 프로젝트 통제 | 프로젝트 공수보고(월별), 변경요청 공문, 원가 관리(월별) |

#### PED 종료
| Task | 세부활동 | 산출물 |
|------|----------|--------|
| EE-1 | 검수 | 검수계획서, 검수요청서(공문), 준공검사확인서, 검사확인서, 준공조서, 인수인계계획서, 기능점수산출내역 |
| EE-2 | 사업완료 | 프로젝트 완료보고서, 최종산출물, 무상 하자보수계획서 |
| IE-1 | 프로젝트 종료 | 프로젝트 공수보고, 프로젝트 종료보고 |

**합계: 4단계 / 10활동 / 33 Task 템플릿 / 73 산출물 템플릿**

---

## 3. 데이터 모델

### 3.1 테이블 목록

| 코드 | 테이블 | 설명 |
|------|--------|------|
| C1 | pms_methodology_phase | 단계 (PRR/PRP/PPC/PED) |
| C2 | pms_methodology_activity | 활동 (OP/PW/…/IE) |
| C3 | pms_task_template | Task 템플릿 (세부활동) |
| C4 | pms_deliverable_template | 산출물 템플릿 (문서 자동화 통합) |
| C4-1 | pms_deliverable_tag_mapping | 산출물 템플릿 태그 매핑 |
| C5 | pms_project_tailoring | 프로젝트 테일러링 선택 내역 |

### 3.2 관계도

```
pms_methodology_phase (C1)
   └─< pms_methodology_activity (C2)
          └─< pms_task_template (C3)
                 └─< pms_deliverable_template (C4)
                        ├─< pms_deliverable_tag_mapping (C4-1)
                        └─ (문서자동화: file_path, template_tags)

pms_project (기존)
   └─< pms_project_tailoring (C5)
          ├─ task_template_id          → C3 (채택한 Task 템플릿)
          ├─ deliverable_template_id   → C4 (채택한 산출물 템플릿)
          ├─ generated_task_id         → pms_task        (생성된 실제 Task)
          └─ generated_deliverable_id  → pms_deliverable  (생성된 실제 산출물)

pms_task.task_template_id               → C3 (출처 추적)
pms_deliverable.deliverable_template_id → C4 (출처 추적)
```

### 3.3 기존 테이블 폐기

| 폐기 테이블 | 대체 |
|-------------|------|
| pms_document_template | pms_deliverable_template (file_path/file_name/template_tags 흡수) |
| pms_template_tag_mapping | pms_deliverable_tag_mapping |

> 문서 자동화(태그 치환, `${PROJECT_NAME}` 등)는 산출물 템플릿에 직접 귀속되어
> 1:1로 관리된다. 별도 범용 문서 템플릿 테이블은 두지 않는다.

---

## 4. 테일러링(Tailoring) 흐름

```
신규 프로젝트 생성
   │
   ▼
① 카탈로그 로드 (vw_methodology_catalog)
   - 단계 → 활동 → Task → 산출물 트리 표시
   │
   ▼
② 사용자 선택 (체크박스)
   - is_optional 항목은 제외 가능
   - 제외 시 exclude_reason 입력 (테일러링 결과서 근거)
   │
   ▼
③ pms_project_tailoring INSERT (선택/제외 내역 기록)
   │
   ▼
④ 채택 항목 인스턴스화
   - 선택된 Task 템플릿  → pms_task 생성 (task_template_id 세팅)
   - 선택된 산출물 템플릿 → pms_deliverable 생성 (deliverable_template_id 세팅)
   - generated_task_id / generated_deliverable_id 역참조 기록
```

### 4.1 단계 ↔ 산출물 템플릿 관리 화면 매핑
- `pms_deliverable_template.stage` (INCEPTION/EXECUTION/CLOSURE)로
  "산출물 템플릿 관리" 화면의 착수/수행/종료 단계 탭과 연결
- `version_no`, `file_name`으로 템플릿 버전·다운로드 파일 표시

---

## 5. 향후 구현 범위 (참고)

> 본 문서/시드는 **DB 설계 + 시드데이터** 범위. 아래는 차기 구현 대상.

- [ ] 카탈로그 조회 API (`GET /api/v1/methodology/catalog`)
- [ ] 신규 프로젝트 생성 시 테일러링 선택 UI (트리 + 체크박스)
- [ ] 테일러링 → Task/산출물 자동 생성 서비스 로직
- [ ] 산출물 템플릿 관리 화면(착수/수행/종료 탭)과 카탈로그 연동
- [ ] 문서 자동화(태그 치환) 엔진
