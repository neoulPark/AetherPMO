# AetherPMO MSA 아키텍처 설계서

> 아마란스(Amaranth) API 연동을 전제로 한 마이크로서비스 분리 설계.
> 인증·사용자·파일은 아마란스가 원천(原泉)이며, PMS 도메인은 단일 코어 서비스로 유지한다.

---

## 1. 설계 전제 (확정 사항)

| 항목 | 결정 | 의미 |
|------|------|------|
| 인증 | 아마란스 IdP | 토큰 발급은 아마란스, 우리는 **검증만** |
| 사용자 | 조회형(call-through) + 권한 단기 캐시 | 우리 DB에 사용자 미저장, 매 요청 아마란스 조회 |
| 파일 | 아마란스 저장소 | 우리는 **참조(file_ref)만** 보관, 바이너리 미보관 |
| 장애 정책 | **Fail-closed** | 권한 조회 실패 시 차단(짧은 캐시 grace 허용) |
| 신뢰 경계 | 게이트웨이 → **내부 JWT 재서명** → 서비스 신뢰 | 내부 헤더 위조 차단 |
| 인가 | **2단계** | 게이트웨이(라우트 권한) + 서비스(도메인 권한) |
| 배포 | PMS Core(상태) + 아마란스 어댑터(무상태) | 어댑터는 추출 용이 |
| 토큰 타입 | **미정** → 추상화 | JWT(JWKS) / 불투명 토큰(introspection) 양쪽 대응 |
| 다운로드 방식 | **미정** → 추상화 | 프록시 스트리밍 / presigned URL 양쪽 대응 |

---

## 2. 서비스 구성

```
                         ┌─────────────────────────────┐
                         │        API Gateway           │
   클라이언트  ─────────▶│  - 아마란스 토큰 검증         │
   (Vue 3 SPA)           │    (TokenValidator 추상화)    │
                         │  - 라우트 단위 인가(1차)      │
                         │  - 내부 JWT 재서명            │
                         │  - 라우팅                     │
                         └───────────────┬─────────────┘
                  ┌──────────────────────┼─────────────────────────┐
                  ▼                                                 ▼
      ┌────────────────────────┐                  ┌────────────────────────────┐
      │     PMS Core 서비스     │ ──인터페이스──▶  │     아마란스 어댑터(ACL)      │
      │  (유일한 상태/DB 보유)   │  UserPort/FilePort │  - UserPort  : 사용자/권한/그룹 │
      │                         │                  │  - FilePort  : 업로드/다운/메타  │
      │  도메인 모듈:            │                  │  - TokenValidator : 토큰검증     │
      │   project, task,        │                  │                              │
      │   deliverable, issue,   │                  │  구현:                        │
      │   member, meeting,      │                  │   ▶ 지금: Mock                │
      │   officialdoc,          │                  │   ▶ 나중: 실제 아마란스 호출   │
      │   methodology(catalog), │                  │  + 서킷브레이커/타임아웃/캐시  │
      │   notification, activity│                  └───────────────┬────────────┘
      │                         │                                  ▼
      │  user_id/file_ref 참조만 │                        ┌──────────────────┐
      └───────────┬────────────┘                        │   아마란스         │
                  ▼                                       │  External API     │
            ┌──────────┐                                  │  - IdP(토큰)       │
            │ PMS DB    │                                  │  - 사용자/권한/그룹 │
            │(Postgres) │                                  │  - 파일 저장소      │
            └──────────┘                                  └──────────────────┘
```

### 2.1 서비스 책임

| 서비스 | 상태 | 책임 |
|--------|------|------|
| **API Gateway** | 무상태 | 토큰 검증, 1차 인가, 내부 JWT 재서명, 라우팅 |
| **PMS Core** | **상태(DB)** | 모든 PMS 도메인 로직, 도메인 권한(2차 인가) |
| **아마란스 어댑터** | 무상태 | 아마란스 ACL(사용자·파일·토큰), 회복탄력성 |

---

## 3. 인증/인가 흐름

### 3.1 요청 처리 시퀀스
```
클라 ──(아마란스 토큰)──▶ Gateway
                          │ ① TokenValidator로 토큰 검증
                          │    - JWT면: JWKS 공개키 로컬 검증 (키 1h 캐시)
                          │    - 불투명이면: 아마란스 introspection 호출
                          │ ② 라우트 권한 1차 확인
                          │ ③ 내부 JWT 재서명 (X-Internal-Token)
                          │    클레임: sub, groups, roles, exp(short)
                          ▼
                       PMS Core
                          │ ④ 내부 JWT 서명 검증 (게이트웨이 공개키)
                          │ ⑤ 도메인 권한 2차 확인
                          │    (예: 이 프로젝트의 PM인가?)
                          │ ⑥ 필요 시 어댑터로 상세 권한 조회
                          ▼
                       아마란스 어댑터 ──▶ 아마란스
```

### 3.2 2단계 인가
| 단계 | 위치 | 판정 대상 | 예시 |
|------|------|----------|------|
| 1차 | Gateway | 라우트/역할 | `/admin/**`는 ADMIN만 |
| 2차 | PMS Core | 도메인/리소스 | "이 프로젝트 PM만 수정" |

---

## 4. 포트 인터페이스 (어댑터 추상화)

> 아마란스 스펙 미정 구간을 인터페이스로 격리. 지금은 Mock, 스펙 확정 시 구현 교체.

### 4.1 UserPort
```java
public interface UserPort {
    UserInfo getUser(String userId);            // 사용자 기본정보
    List<String> getGroups(String userId);      // 소속 그룹
    Set<String> getPermissions(String userId);  // 권한 코드
    List<UserInfo> searchUsers(String keyword); // 사용자 검색(담당자 지정용)
}
```

### 4.2 FilePort
```java
public interface FilePort {
    FileRef upload(UploadCommand cmd);          // 아마란스 업로드 → file_ref 반환
    FileMeta getMeta(String fileRef);           // 메타데이터
    DownloadResult download(String fileRef);    // 프록시 스트리밍 또는
    String getPresignedUrl(String fileRef);     //   presigned URL (둘 중 지원되는 방식)
    void delete(String fileRef);
}
```

### 4.3 TokenValidator
```java
public interface TokenValidator {
    Principal validate(String token);   // JWT 로컬검증 or introspection
    // 구현체: JwksTokenValidator / IntrospectionTokenValidator
}
```

---

## 5. 회복탄력성 (아마란스 의존 완충)

전제: 인증·사용자·파일이 모두 아마란스 → **아마란스 장애 = 전체 영향**. 완충 필수.

| 대상 | 전략 | 값(초안) |
|------|------|---------|
| 토큰 검증(JWT) | JWKS 공개키 **로컬 캐시** | 키 1시간 캐시 |
| 권한/그룹 조회 | 단기 캐시 + 서킷브레이커 | TTL 30초, 타임아웃 2초 |
| 파일 다운로드 | 타임아웃 + 재시도 | 타임아웃 10초, 재시도 1회 |
| 공통 | 서킷브레이커(Resilience4j) | 실패율 50% → open, 10초 후 half-open |

### 5.1 장애 정책: Fail-closed
- 권한 조회 실패 시 → **차단**(보안 우선, 공공 SI 기준)
- 단, 캐시 TTL(30초) 내 값은 grace로 허용하여 순간 장애 흡수
- 토큰 검증은 JWKS 로컬 캐시로 아마란스 단기 장애에도 동작

---

## 6. 파일 참조 모델 (스키마 영향)

### 6.1 두 종류의 파일 구분 ⚠️
| 구분 | 무엇 | 저장 위치 |
|------|------|----------|
| **표준 양식 파일** (빈 템플릿, 1개) | "사업계획서 양식.hwp" | `pms_deliverable_template.template_file_ref` |
| **실제 산출물 파일** (작성본, N개) | "A프로젝트_사업계획서_v1.2.hwp" + 증빙 | `pms_attachment` (N) |

### 6.2 공통 첨부 테이블 (PMS_ATTACHMENT)
- 산출물·공문·회의록 등 여러 엔티티가 공유하는 **다형(polymorphic) 첨부**
- 우리 DB는 아마란스 `file_ref`와 표시용 메타만 보관 (바이너리 X)

```
pms_attachment
 ├ attachment_id   PK
 ├ entity_type     ('DELIVERABLE' | 'OFFICIAL_DOC' | 'MEETING' | ...)
 ├ entity_id       (대상 엔티티 ID)
 ├ file_ref        (아마란스 file_id — 원천 키)
 ├ file_name       (표시용 캐시)
 ├ file_size       (표시용 캐시)
 ├ content_type
 ├ uploaded_by     (아마란스 user subject)
 └ uploaded_at
```

> DDL은 `02_db_schema.sql`에 반영, 템플릿 파일 컬럼명 변경은 `05_methodology_catalog.sql` 참조.

---

## 7. 배포 구성

```
① API Gateway        (무상태, N replica)
② PMS Core           (상태, DB 연결, 내부 모듈화)
③ 아마란스 어댑터     (무상태, 추출 용이)
```

### 7.1 PMS Core 내부 모듈 경계
```
com.aetherpmo.core
 ├ project/   task/   deliverable/   issue/
 ├ member/    meeting/   officialdoc/
 ├ methodology/  (카탈로그·테일러링)
 ├ notification/ (← 미래 추출 후보: 비동기/이벤트)
 └ activity/     (← 미래 추출 후보: append-only)
```

### 7.2 분리 원칙
- **나누지 않음**: 결합 높고 트랜잭션·조인 겹치는 도메인(코어 전체)
- **나눔**: 무상태 외부 의존(아마란스: 사용자·파일·인증)
- **미래 후보**: notification, activity-log — "느슨한 결합 + 다른 특성"일 때만, 지금은 모듈

---

## 8. 미확정 → 추상화로 흡수한 항목

| 미정 | 추상화 | 스펙 확정 시 |
|------|--------|------------|
| 토큰 타입(JWT/불투명) | `TokenValidator` | 구현체 선택/교체 |
| 다운로드 방식(프록시/presigned) | `FilePort` | 메서드 구현 |
| 아마란스 사용자/권한 스키마 | `UserPort` + Mock | 실제 호출 매핑 |

---

## 9. 차기 작업 (참고)
- [ ] API Gateway 라우팅/토큰검증/내부JWT 구현
- [ ] 아마란스 어댑터 Mock 구현 (UserPort/FilePort/TokenValidator)
- [ ] PMS Core ↔ 어댑터 인터페이스 연결
- [ ] pms_attachment 기반 첨부 업로드/다운로드 플로우
- [ ] 아마란스 실제 API 스펙 수령 후 어댑터 구현 교체
