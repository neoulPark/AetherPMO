# PMS REST API 설계 문서

> **버전**: v1.0.0  
> **작성일**: 2026-06-15  
> **기술 스택**: Spring Boot 3.x, Spring Security, JWT, JPA

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [인증 API](#2-인증-api-auth)
3. [사용자 API](#3-사용자-api-user)
4. [프로젝트 API](#4-프로젝트-api-project)
5. [업무 API](#5-업무-api-task)
6. [산출물 API](#6-산출물-api-deliverable)
7. [이슈/리스크 API](#7-이슈리스크-api-issue)
8. [프로젝트 인력 API](#8-프로젝트-인력-api-member)
9. [근태/공수 API](#9-근태공수-api-time-off)
10. [알림 API](#10-알림-api-notification)
11. [대시보드 API](#11-대시보드-api)
12. [활동 이력 API](#12-활동-이력-api)

---

## 1. 공통 사항

### 1.1 Base URL

```
https://{host}/api/v1
```

### 1.2 인증 방식

모든 API 요청(로그인 제외)은 HTTP `Authorization` 헤더에 JWT Bearer Token을 포함해야 합니다.

```
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

- Access Token 유효기간: **1시간**
- Refresh Token 유효기간: **7일**
- 토큰 만료 시 `/api/v1/auth/refresh`를 통해 재발급

### 1.3 공통 응답 형식

#### 성공 응답

```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공적으로 처리되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 요청 성공 여부 |
| `data` | object \| array \| null | 응답 데이터 |
| `message` | string | 처리 결과 메시지 |
| `timestamp` | string (ISO 8601) | 응답 시각 |

#### 페이징 응답 (목록 조회 시)

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### 1.4 오류 응답 형식

```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "해당 사용자를 찾을 수 없습니다.",
    "details": null
  },
  "timestamp": "2026-06-15T10:30:00Z"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 항상 `false` |
| `error.code` | string | 애플리케이션 에러 코드 |
| `error.message` | string | 사람이 읽을 수 있는 오류 설명 |
| `error.details` | object \| null | 필드별 유효성 오류 상세 (422 시) |
| `timestamp` | string (ISO 8601) | 응답 시각 |

### 1.5 HTTP 상태코드

| 상태코드 | 의미 | 사용 상황 |
|----------|------|-----------|
| `200 OK` | 성공 | 조회, 수정, 삭제 성공 |
| `201 Created` | 생성 성공 | 리소스 생성 성공 |
| `400 Bad Request` | 잘못된 요청 | 파라미터 누락 또는 형식 오류 |
| `401 Unauthorized` | 인증 실패 | 토큰 없음 또는 유효하지 않은 토큰 |
| `403 Forbidden` | 권한 없음 | 접근 권한 부족 |
| `404 Not Found` | 리소스 없음 | 요청한 리소스가 존재하지 않음 |
| `409 Conflict` | 충돌 | 중복 데이터 등 비즈니스 규칙 위반 |
| `422 Unprocessable Entity` | 유효성 검증 실패 | 입력값 유효성 오류 |
| `500 Internal Server Error` | 서버 오류 | 예기치 못한 서버 오류 |

### 1.6 역할별 접근 권한

| 역할 | 코드 | 설명 |
|------|------|------|
| 시스템 관리자 | `ADMIN` | 모든 기능에 접근 가능, 사용자 관리 포함 |
| 프로젝트 관리자 | `PM` | 담당 프로젝트 전체 관리 권한 |
| 프로젝트 멤버 | `MEMBER` | 할당된 업무 조회/수정, 제한적 접근 |

> 권한 표기 예시: `ADMIN` / `PM` / `MEMBER` — 해당 역할 이상의 권한 필요

---

## 2. 인증 API (AUTH)

### 2.1 로그인

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/login` |
| **설명** | 이메일과 비밀번호로 로그인하여 JWT Access/Refresh Token을 발급받습니다. |
| **권한** | 없음 (공개) |

**Request Body**

```json
{
  "email": "hong@example.com",
  "password": "P@ssw0rd123"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | Y | 사용자 이메일 주소 |
| `password` | string | Y | 비밀번호 (8자 이상) |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "name": "홍길동",
      "email": "hong@example.com",
      "role": "PM",
      "department": "개발팀"
    }
  },
  "message": "로그인이 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |
| `ACCOUNT_DISABLED` | 403 | 비활성화된 계정 |
| `ACCOUNT_LOCKED` | 403 | 잠긴 계정 (로그인 5회 실패) |

---

### 2.2 로그아웃

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/logout` |
| **설명** | Refresh Token을 무효화하여 로그아웃 처리합니다. |
| **권한** | 인증된 사용자 |

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "로그아웃되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `INVALID_TOKEN` | 400 | 유효하지 않은 Refresh Token |

---

### 2.3 토큰 갱신

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/refresh` |
| **설명** | Refresh Token으로 새로운 Access Token을 발급받습니다. |
| **권한** | 없음 (Refresh Token 필요) |

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "message": "토큰이 갱신되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `INVALID_TOKEN` | 401 | 유효하지 않은 Refresh Token |
| `EXPIRED_TOKEN` | 401 | 만료된 Refresh Token |
| `TOKEN_REVOKED` | 401 | 무효화된 Refresh Token (로그아웃 후) |

---

### 2.4 내 정보 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/auth/me` |
| **설명** | 현재 로그인한 사용자의 정보를 조회합니다. |
| **권한** | 인증된 사용자 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "PM",
    "department": "개발팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "profileImageUrl": "https://example.com/profiles/1.jpg",
    "lastLoginAt": "2026-06-15T09:00:00Z",
    "createdAt": "2026-01-01T00:00:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `UNAUTHORIZED` | 401 | 유효하지 않은 Access Token |

---

## 3. 사용자 API (USER)

### 3.1 사용자 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/users` |
| **설명** | 전체 사용자 목록을 조회합니다. 이름, 이메일, 역할로 검색 및 페이징을 지원합니다. |
| **권한** | `ADMIN`, `PM` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 (0부터 시작) |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `sort` | string | N | `name,asc` | 정렬 기준 (예: `createdAt,desc`) |
| `keyword` | string | N | - | 이름 또는 이메일 검색어 |
| `role` | string | N | - | 역할 필터 (`ADMIN`, `PM`, `MEMBER`) |
| `department` | string | N | - | 부서 필터 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "홍길동",
        "email": "hong@example.com",
        "role": "PM",
        "department": "개발팀",
        "position": "팀장",
        "phone": "010-1234-5678",
        "active": true,
        "createdAt": "2026-01-01T00:00:00Z"
      },
      {
        "id": 2,
        "name": "김영희",
        "email": "kim@example.com",
        "role": "MEMBER",
        "department": "개발팀",
        "position": "개발자",
        "phone": "010-9876-5432",
        "active": true,
        "createdAt": "2026-01-05T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3,
    "first": true,
    "last": false
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 3.2 사용자 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/users` |
| **설명** | 새 사용자 계정을 생성합니다. |
| **권한** | `ADMIN` |

**Request Body**

```json
{
  "name": "이철수",
  "email": "lee@example.com",
  "password": "TempPass123!",
  "role": "MEMBER",
  "department": "기획팀",
  "position": "기획자",
  "phone": "010-1111-2222"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | Y | 사용자 이름 (2~50자) |
| `email` | string | Y | 이메일 주소 (유니크) |
| `password` | string | Y | 초기 비밀번호 (8자 이상, 영문+숫자+특수문자) |
| `role` | string | Y | 역할 (`ADMIN`, `PM`, `MEMBER`) |
| `department` | string | N | 부서명 |
| `position` | string | N | 직책 |
| `phone` | string | N | 연락처 |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 3,
    "name": "이철수",
    "email": "lee@example.com",
    "role": "MEMBER",
    "department": "기획팀",
    "position": "기획자",
    "phone": "010-1111-2222",
    "active": true,
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "사용자가 생성되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 사용 중인 이메일 |
| `VALIDATION_ERROR` | 422 | 입력값 유효성 오류 |

---

### 3.3 사용자 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/users/{id}` |
| **설명** | 특정 사용자의 상세 정보를 조회합니다. |
| **권한** | `ADMIN`, `PM`, 본인 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 사용자 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "PM",
    "department": "개발팀",
    "position": "팀장",
    "phone": "010-1234-5678",
    "profileImageUrl": "https://example.com/profiles/1.jpg",
    "active": true,
    "lastLoginAt": "2026-06-15T09:00:00Z",
    "assignedProjects": [
      { "id": 10, "name": "ERP 시스템 구축", "role": "PM" }
    ],
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-06-01T00:00:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 3.4 사용자 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/users/{id}` |
| **설명** | 사용자 정보를 수정합니다. 역할 변경은 ADMIN만 가능합니다. |
| **권한** | `ADMIN`, 본인 (역할 변경은 ADMIN만) |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 사용자 ID |

**Request Body**

```json
{
  "name": "홍길동",
  "department": "개발팀",
  "position": "수석팀장",
  "phone": "010-1234-9999",
  "role": "PM"
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "PM",
    "department": "개발팀",
    "position": "수석팀장",
    "phone": "010-1234-9999",
    "active": true,
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "사용자 정보가 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |
| `VALIDATION_ERROR` | 422 | 입력값 유효성 오류 |

---

### 3.5 사용자 삭제 (비활성화)

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/users/{id}` |
| **설명** | 사용자를 비활성화 처리합니다. (Hard delete 미지원, Soft delete) |
| **권한** | `ADMIN` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 사용자 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "사용자가 비활성화되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `CANNOT_DELETE_SELF` | 409 | 본인 계정 삭제 불가 |
| `USER_HAS_ACTIVE_TASKS` | 409 | 진행 중인 업무가 있어 삭제 불가 |

---

## 4. 프로젝트 API (PROJECT)

### 4.1 프로젝트 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects` |
| **설명** | 프로젝트 목록을 조회합니다. ADMIN은 전체, PM/MEMBER는 참여 프로젝트만 조회됩니다. |
| **권한** | 인증된 사용자 |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `sort` | string | N | `startDate,desc` | 정렬 기준 |
| `keyword` | string | N | - | 프로젝트명 검색어 |
| `status` | string | N | - | 상태 필터 (`PLANNING`, `IN_PROGRESS`, `ON_HOLD`, `COMPLETED`, `CANCELLED`) |
| `startDateFrom` | string | N | - | 시작일 범위 (yyyy-MM-dd) |
| `startDateTo` | string | N | - | 시작일 범위 (yyyy-MM-dd) |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10,
        "name": "ERP 시스템 구축",
        "code": "PRJ-2026-001",
        "description": "전사 ERP 시스템 신규 구축 프로젝트",
        "status": "IN_PROGRESS",
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "pmId": 1,
        "pmName": "홍길동",
        "budget": 500000000,
        "progressRate": 35,
        "memberCount": 12,
        "createdAt": "2025-12-01T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `INVALID_PARAMETER` | 400 | 잘못된 쿼리 파라미터 |

---

### 4.2 프로젝트 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects` |
| **설명** | 새 프로젝트를 생성합니다. |
| **권한** | `ADMIN`, `PM` |

**Request Body**

```json
{
  "name": "CRM 포털 개편",
  "code": "PRJ-2026-002",
  "description": "고객관리 포털 시스템 전면 개편",
  "startDate": "2026-07-01",
  "endDate": "2026-12-31",
  "pmId": 1,
  "budget": 200000000,
  "clientName": "(주)ABC기업",
  "priority": "HIGH"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | Y | 프로젝트명 (2~200자) |
| `code` | string | Y | 프로젝트 코드 (유니크) |
| `description` | string | N | 프로젝트 설명 |
| `startDate` | string | Y | 시작일 (yyyy-MM-dd) |
| `endDate` | string | Y | 종료일 (yyyy-MM-dd) |
| `pmId` | integer | Y | PM 사용자 ID |
| `budget` | long | N | 예산 (원) |
| `clientName` | string | N | 고객사명 |
| `priority` | string | N | 우선순위 (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 11,
    "name": "CRM 포털 개편",
    "code": "PRJ-2026-002",
    "description": "고객관리 포털 시스템 전면 개편",
    "status": "PLANNING",
    "startDate": "2026-07-01",
    "endDate": "2026-12-31",
    "pmId": 1,
    "pmName": "홍길동",
    "budget": 200000000,
    "clientName": "(주)ABC기업",
    "priority": "HIGH",
    "progressRate": 0,
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "프로젝트가 생성되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_CODE_DUPLICATE` | 409 | 이미 사용 중인 프로젝트 코드 |
| `PM_NOT_FOUND` | 404 | 지정한 PM 사용자 없음 |
| `INVALID_DATE_RANGE` | 422 | 종료일이 시작일보다 앞선 경우 |
| `VALIDATION_ERROR` | 422 | 입력값 유효성 오류 |

---

### 4.3 프로젝트 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{id}` |
| **설명** | 프로젝트 상세 정보를 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 프로젝트 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "ERP 시스템 구축",
    "code": "PRJ-2026-001",
    "description": "전사 ERP 시스템 신규 구축 프로젝트",
    "status": "IN_PROGRESS",
    "startDate": "2026-01-01",
    "endDate": "2026-12-31",
    "pm": {
      "id": 1,
      "name": "홍길동",
      "email": "hong@example.com"
    },
    "budget": 500000000,
    "usedBudget": 120000000,
    "clientName": "(주)고객사",
    "priority": "HIGH",
    "progressRate": 35,
    "memberCount": 12,
    "taskStats": {
      "total": 80,
      "notStarted": 30,
      "inProgress": 35,
      "completed": 15
    },
    "createdAt": "2025-12-01T00:00:00Z",
    "updatedAt": "2026-06-10T00:00:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 해당 프로젝트 접근 권한 없음 |

---

### 4.4 프로젝트 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/projects/{id}` |
| **설명** | 프로젝트 정보를 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 프로젝트 ID |

**Request Body**

```json
{
  "name": "ERP 시스템 구축 (1차)",
  "description": "전사 ERP 시스템 신규 구축 1차 프로젝트",
  "status": "IN_PROGRESS",
  "endDate": "2026-11-30",
  "budget": 550000000,
  "priority": "CRITICAL"
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "name": "ERP 시스템 구축 (1차)",
    "status": "IN_PROGRESS",
    "endDate": "2026-11-30",
    "budget": 550000000,
    "priority": "CRITICAL",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "프로젝트 정보가 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |
| `INVALID_STATUS_TRANSITION` | 422 | 유효하지 않은 상태 전환 |

---

### 4.5 프로젝트 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/projects/{id}` |
| **설명** | 프로젝트를 삭제합니다. 진행 중인 프로젝트는 삭제 불가합니다. |
| **권한** | `ADMIN` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 프로젝트 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "프로젝트가 삭제되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `PROJECT_IN_PROGRESS` | 409 | 진행 중인 프로젝트 삭제 불가 |

---

### 4.6 프로젝트 요약 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{id}/summary` |
| **설명** | 프로젝트 대시보드용 요약 정보(진행률, 이슈, 일정 현황 등)를 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 프로젝트 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "projectId": 10,
    "projectName": "ERP 시스템 구축",
    "status": "IN_PROGRESS",
    "progressRate": 35,
    "daysRemaining": 199,
    "scheduleStatus": "ON_TIME",
    "taskSummary": {
      "total": 80,
      "notStarted": 30,
      "inProgress": 35,
      "completed": 15,
      "overdue": 3
    },
    "issueSummary": {
      "open": 5,
      "inProgress": 2,
      "resolved": 10,
      "critical": 1
    },
    "deliverableSummary": {
      "total": 20,
      "pending": 8,
      "submitted": 5,
      "approved": 7
    },
    "budgetSummary": {
      "totalBudget": 500000000,
      "usedBudget": 120000000,
      "usageRate": 24
    },
    "recentActivities": [
      {
        "type": "TASK_COMPLETED",
        "description": "요구사항 분석 완료",
        "actorName": "김영희",
        "occurredAt": "2026-06-14T15:00:00Z"
      }
    ]
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |
