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
13. [공문 API](#13-공문-api-official-doc)
14. [회의록 API](#14-회의록-api-meeting)

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

---

## 5. 업무 API (TASK)

### 5.1 Task 트리 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/tasks` |
| **설명** | 프로젝트의 업무(Task)를 계층 트리 구조로 조회합니다. 단계(Phase) > WBS > Task > Sub-Task 구조입니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `assigneeId` | integer | N | - | 담당자 ID 필터 |
| `status` | string | N | - | 상태 필터 (`NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `ON_HOLD`) |
| `includeCompleted` | boolean | N | true | 완료 Task 포함 여부 |

**Response Body (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": 100,
      "name": "1단계: 요구사항 분석",
      "type": "PHASE",
      "status": "COMPLETED",
      "startDate": "2026-01-01",
      "endDate": "2026-02-28",
      "progressRate": 100,
      "children": [
        {
          "id": 101,
          "name": "현황 분석",
          "type": "WBS",
          "status": "COMPLETED",
          "startDate": "2026-01-01",
          "endDate": "2026-01-31",
          "progressRate": 100,
          "children": [
            {
              "id": 102,
              "name": "AS-IS 프로세스 분석",
              "type": "TASK",
              "status": "COMPLETED",
              "startDate": "2026-01-01",
              "endDate": "2026-01-15",
              "progressRate": 100,
              "assignee": {
                "id": 2,
                "name": "김영희"
              },
              "priority": "HIGH",
              "estimatedHours": 40,
              "actualHours": 38,
              "children": []
            }
          ]
        }
      ]
    },
    {
      "id": 200,
      "name": "2단계: 설계",
      "type": "PHASE",
      "status": "IN_PROGRESS",
      "startDate": "2026-03-01",
      "endDate": "2026-05-31",
      "progressRate": 60,
      "children": []
    }
  ],
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |

---

### 5.2 Task 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/tasks` |
| **설명** | 프로젝트에 새 업무를 생성합니다. parentId를 통해 계층 구조를 설정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Request Body**

```json
{
  "name": "DB 설계",
  "description": "시스템 데이터베이스 설계 및 ERD 작성",
  "type": "TASK",
  "parentId": 201,
  "assigneeId": 3,
  "startDate": "2026-06-16",
  "endDate": "2026-06-30",
  "priority": "HIGH",
  "estimatedHours": 32,
  "sortOrder": 1
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | Y | 업무명 (2~200자) |
| `description` | string | N | 업무 설명 |
| `type` | string | Y | 업무 유형 (`PHASE`, `WBS`, `TASK`, `SUBTASK`) |
| `parentId` | integer | N | 상위 Task ID (최상위이면 null) |
| `assigneeId` | integer | N | 담당자 ID |
| `startDate` | string | Y | 시작일 (yyyy-MM-dd) |
| `endDate` | string | Y | 종료일 (yyyy-MM-dd) |
| `priority` | string | N | 우선순위 (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) |
| `estimatedHours` | number | N | 예상 공수 (시간) |
| `sortOrder` | integer | N | 정렬 순서 |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 210,
    "name": "DB 설계",
    "description": "시스템 데이터베이스 설계 및 ERD 작성",
    "type": "TASK",
    "parentId": 201,
    "projectId": 10,
    "status": "NOT_STARTED",
    "assignee": {
      "id": 3,
      "name": "이철수"
    },
    "startDate": "2026-06-16",
    "endDate": "2026-06-30",
    "priority": "HIGH",
    "estimatedHours": 32,
    "actualHours": 0,
    "progressRate": 0,
    "sortOrder": 1,
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "업무가 생성되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `PARENT_TASK_NOT_FOUND` | 404 | 상위 Task를 찾을 수 없음 |
| `ASSIGNEE_NOT_PROJECT_MEMBER` | 422 | 담당자가 프로젝트 멤버가 아님 |
| `INVALID_DATE_RANGE` | 422 | 유효하지 않은 날짜 범위 |

---

### 5.3 Task 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/tasks/{id}` |
| **설명** | 업무의 상세 정보를 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Task ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 210,
    "name": "DB 설계",
    "description": "시스템 데이터베이스 설계 및 ERD 작성",
    "type": "TASK",
    "status": "IN_PROGRESS",
    "project": {
      "id": 10,
      "name": "ERP 시스템 구축"
    },
    "parent": {
      "id": 201,
      "name": "시스템 설계"
    },
    "assignee": {
      "id": 3,
      "name": "이철수",
      "email": "lee@example.com"
    },
    "startDate": "2026-06-16",
    "endDate": "2026-06-30",
    "priority": "HIGH",
    "estimatedHours": 32,
    "actualHours": 10,
    "progressRate": 30,
    "sortOrder": 1,
    "childCount": 3,
    "attachments": [],
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TASK_NOT_FOUND` | 404 | 업무를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |

---

### 5.4 Task 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/tasks/{id}` |
| **설명** | 업무 정보를 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`, 본인 담당 업무 `MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Task ID |

**Request Body**

```json
{
  "name": "DB 설계 (ERD)",
  "description": "시스템 데이터베이스 설계 및 ERD 작성 - 논리/물리 모델 포함",
  "assigneeId": 3,
  "startDate": "2026-06-16",
  "endDate": "2026-07-05",
  "priority": "CRITICAL",
  "estimatedHours": 40
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 210,
    "name": "DB 설계 (ERD)",
    "status": "IN_PROGRESS",
    "endDate": "2026-07-05",
    "priority": "CRITICAL",
    "estimatedHours": 40,
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "업무가 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TASK_NOT_FOUND` | 404 | 업무를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 5.5 Task 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/tasks/{id}` |
| **설명** | 업무를 삭제합니다. 하위 Task가 있으면 삭제 불가합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Task ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "업무가 삭제되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TASK_NOT_FOUND` | 404 | 업무를 찾을 수 없음 |
| `TASK_HAS_CHILDREN` | 409 | 하위 Task가 존재하여 삭제 불가 |
| `TASK_COMPLETED` | 409 | 완료된 Task는 삭제 불가 |

---

### 5.6 Task 진행률 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/tasks/{id}/progress` |
| **설명** | 업무의 진행률과 실제 공수를 갱신합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`, 담당 `MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Task ID |

**Request Body**

```json
{
  "progressRate": 50,
  "actualHours": 16,
  "status": "IN_PROGRESS",
  "comment": "논리 ERD 완료, 물리 모델 진행 중"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `progressRate` | integer | Y | 진행률 (0~100) |
| `actualHours` | number | N | 실제 투입 공수 누적 (시간) |
| `status` | string | N | 업무 상태 |
| `comment` | string | N | 진행 상황 코멘트 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 210,
    "progressRate": 50,
    "actualHours": 16,
    "status": "IN_PROGRESS",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "진행률이 업데이트되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TASK_NOT_FOUND` | 404 | 업무를 찾을 수 없음 |
| `INVALID_PROGRESS_RATE` | 422 | 진행률이 0~100 범위 초과 |

---

### 5.7 Task 담당자 배정

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/tasks/{id}/assign` |
| **설명** | Task 담당자를 배정하거나 변경합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Task ID |

**Request Body**

```json
{
  "assigneeId": 5,
  "notifyAssignee": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `assigneeId` | integer | Y | 담당자 사용자 ID (null이면 담당자 해제) |
| `notifyAssignee` | boolean | N | 담당자에게 알림 발송 여부 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "taskId": 210,
    "taskName": "DB 설계 (ERD)",
    "assignee": {
      "id": 5,
      "name": "박민수",
      "email": "park@example.com"
    },
    "assignedAt": "2026-06-15T10:30:00Z"
  },
  "message": "담당자가 배정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TASK_NOT_FOUND` | 404 | 업무를 찾을 수 없음 |
| `USER_NOT_FOUND` | 404 | 담당자 사용자를 찾을 수 없음 |
| `ASSIGNEE_NOT_PROJECT_MEMBER` | 422 | 담당자가 프로젝트 멤버가 아님 |

---

### 5.8 Task 변경 이력 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/tasks/{id}/history` |
| **설명** | Task의 변경 이력(상태 변경, 담당자 변경, 진행률 변경 등)을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Task ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 500,
        "taskId": 210,
        "changeType": "PROGRESS_UPDATED",
        "fieldName": "progressRate",
        "oldValue": "30",
        "newValue": "50",
        "comment": "논리 ERD 완료, 물리 모델 진행 중",
        "changedBy": {
          "id": 3,
          "name": "이철수"
        },
        "changedAt": "2026-06-15T10:30:00Z"
      },
      {
        "id": 499,
        "taskId": 210,
        "changeType": "STATUS_CHANGED",
        "fieldName": "status",
        "oldValue": "NOT_STARTED",
        "newValue": "IN_PROGRESS",
        "comment": null,
        "changedBy": {
          "id": 3,
          "name": "이철수"
        },
        "changedAt": "2026-06-16T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 8,
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
| `TASK_NOT_FOUND` | 404 | 업무를 찾을 수 없음 |

---

### 5.9 Task 하위 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/tasks/{id}/children` |
| **설명** | Task의 직접 하위(1단계) Task 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 부모 Task ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": 211,
      "name": "논리 ERD 작성",
      "type": "SUBTASK",
      "status": "COMPLETED",
      "assignee": { "id": 3, "name": "이철수" },
      "startDate": "2026-06-16",
      "endDate": "2026-06-22",
      "progressRate": 100,
      "priority": "HIGH"
    },
    {
      "id": 212,
      "name": "물리 ERD 작성",
      "type": "SUBTASK",
      "status": "IN_PROGRESS",
      "assignee": { "id": 3, "name": "이철수" },
      "startDate": "2026-06-23",
      "endDate": "2026-07-05",
      "progressRate": 30,
      "priority": "HIGH"
    }
  ],
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TASK_NOT_FOUND` | 404 | 상위 Task를 찾을 수 없음 |

---

## 6. 산출물 API (DELIVERABLE)

### 6.1 산출물 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/deliverables` |
| **설명** | 프로젝트의 산출물 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `status` | string | N | - | 상태 필터 (`PENDING`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`) |
| `category` | string | N | - | 분류 필터 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 300,
        "name": "요구사항 정의서",
        "category": "분석",
        "status": "APPROVED",
        "dueDate": "2026-02-28",
        "submittedAt": "2026-02-25T10:00:00Z",
        "approvedAt": "2026-02-27T14:00:00Z",
        "submitter": { "id": 2, "name": "김영희" },
        "reviewer": { "id": 1, "name": "홍길동" },
        "version": "v1.2"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 20,
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
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |

---

### 6.2 산출물 등록

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/deliverables` |
| **설명** | 프로젝트에 산출물 항목을 등록합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Request Body**

```json
{
  "name": "시스템 설계서",
  "category": "설계",
  "description": "논리/물리 아키텍처 설계 문서",
  "dueDate": "2026-05-31",
  "submitterId": 3,
  "reviewerId": 1,
  "relatedTaskId": 201
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | Y | 산출물명 |
| `category` | string | Y | 산출물 분류 |
| `description` | string | N | 설명 |
| `dueDate` | string | Y | 제출 기한 (yyyy-MM-dd) |
| `submitterId` | integer | Y | 제출 담당자 ID |
| `reviewerId` | integer | Y | 검토자 ID |
| `relatedTaskId` | integer | N | 연관 Task ID |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 320,
    "name": "시스템 설계서",
    "category": "설계",
    "status": "PENDING",
    "dueDate": "2026-05-31",
    "submitter": { "id": 3, "name": "이철수" },
    "reviewer": { "id": 1, "name": "홍길동" },
    "version": "v0.1",
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "산출물이 등록되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `USER_NOT_PROJECT_MEMBER` | 422 | 제출자 또는 검토자가 프로젝트 멤버가 아님 |

---

### 6.3 산출물 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/deliverables/{id}` |
| **설명** | 산출물 상세 정보 및 제출/검토 이력을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 산출물 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 320,
    "name": "시스템 설계서",
    "category": "설계",
    "description": "논리/물리 아키텍처 설계 문서",
    "status": "UNDER_REVIEW",
    "dueDate": "2026-05-31",
    "submitter": { "id": 3, "name": "이철수" },
    "reviewer": { "id": 1, "name": "홍길동" },
    "version": "v1.0",
    "fileUrl": "https://storage.example.com/deliverables/320/v1.0.pdf",
    "submittedAt": "2026-05-28T10:00:00Z",
    "reviewHistory": [
      {
        "action": "SUBMITTED",
        "comment": "v1.0 제출합니다.",
        "actor": { "id": 3, "name": "이철수" },
        "occurredAt": "2026-05-28T10:00:00Z"
      }
    ],
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `DELIVERABLE_NOT_FOUND` | 404 | 산출물을 찾을 수 없음 |

---

### 6.4 산출물 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/deliverables/{id}` |
| **설명** | 산출물 기본 정보를 수정합니다. PENDING 상태에서만 수정 가능합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 산출물 ID |

**Request Body**

```json
{
  "name": "시스템 설계서 (개정)",
  "dueDate": "2026-06-10",
  "reviewerId": 2
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 320,
    "name": "시스템 설계서 (개정)",
    "dueDate": "2026-06-10",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "산출물 정보가 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `DELIVERABLE_NOT_FOUND` | 404 | 산출물을 찾을 수 없음 |
| `INVALID_STATUS_FOR_EDIT` | 422 | PENDING 상태가 아니어서 수정 불가 |

---

### 6.5 산출물 제출

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/deliverables/{id}/submit` |
| **설명** | 산출물을 제출합니다. 상태가 PENDING → SUBMITTED로 변경됩니다. |
| **권한** | 제출 담당자 (`MEMBER`), `PM`, `ADMIN` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 산출물 ID |

**Request Body**

```json
{
  "comment": "v1.0 최종본 제출합니다.",
  "fileUrl": "https://storage.example.com/deliverables/320/v1.0.pdf",
  "version": "v1.0"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `comment` | string | N | 제출 코멘트 |
| `fileUrl` | string | Y | 제출 파일 URL |
| `version` | string | Y | 버전 표기 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 320,
    "status": "SUBMITTED",
    "version": "v1.0",
    "submittedAt": "2026-06-15T10:30:00Z"
  },
  "message": "산출물이 제출되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `DELIVERABLE_NOT_FOUND` | 404 | 산출물을 찾을 수 없음 |
| `INVALID_STATUS_TRANSITION` | 422 | 현재 상태에서 제출 불가 |

---

### 6.6 산출물 검토

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/deliverables/{id}/review` |
| **설명** | 제출된 산출물에 대한 검토 의견을 등록합니다. 반려 시 REJECTED, 승인 요청 시 UNDER_REVIEW 상태로 변경됩니다. |
| **권한** | 검토자 (`PM`), `ADMIN` |

**Request Body**

```json
{
  "action": "REQUEST_REVISION",
  "comment": "3장 데이터 흐름도 보완 필요. 외부 연동 시스템 누락됨."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `action` | string | Y | `APPROVE_REQUEST` (승인 요청) / `REQUEST_REVISION` (보완 요청) |
| `comment` | string | Y | 검토 의견 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 320,
    "status": "PENDING",
    "reviewedAt": "2026-06-15T10:30:00Z"
  },
  "message": "검토 의견이 등록되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `DELIVERABLE_NOT_FOUND` | 404 | 산출물을 찾을 수 없음 |
| `NOT_REVIEWER` | 403 | 검토자 권한 없음 |
| `INVALID_STATUS_TRANSITION` | 422 | SUBMITTED 상태가 아님 |

---

### 6.7 산출물 최종 승인

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/deliverables/{id}/approve` |
| **설명** | 산출물을 최종 승인합니다. 상태가 APPROVED로 변경됩니다. |
| **권한** | `ADMIN`, `PM` |

**Request Body**

```json
{
  "comment": "검토 완료. 최종 승인합니다."
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 320,
    "status": "APPROVED",
    "approvedAt": "2026-06-15T10:30:00Z",
    "approver": { "id": 1, "name": "홍길동" }
  },
  "message": "산출물이 최종 승인되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `DELIVERABLE_NOT_FOUND` | 404 | 산출물을 찾을 수 없음 |
| `INVALID_STATUS_TRANSITION` | 422 | 승인 가능한 상태가 아님 |

---

## 7. 이슈/리스크 API (ISSUE)

### 7.1 이슈 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/issues` |
| **설명** | 프로젝트의 이슈/리스크 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `type` | string | N | - | 유형 필터 (`ISSUE`, `RISK`) |
| `status` | string | N | - | 상태 필터 (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) |
| `severity` | string | N | - | 심각도 필터 (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 400,
        "title": "DB 성능 이슈 - 조회 응답시간 5초 초과",
        "type": "ISSUE",
        "status": "IN_PROGRESS",
        "severity": "HIGH",
        "reporter": { "id": 3, "name": "이철수" },
        "assignee": { "id": 2, "name": "김영희" },
        "reportedAt": "2026-06-10T14:00:00Z",
        "dueDate": "2026-06-20",
        "actionItemCount": 3
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 7,
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
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |

---

### 7.2 이슈 등록

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/issues` |
| **설명** | 프로젝트에 이슈 또는 리스크를 등록합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Request Body**

```json
{
  "title": "인증 모듈 세션 만료 버그",
  "description": "특정 조건에서 세션 토큰이 조기 만료되어 사용자가 강제 로그아웃됨",
  "type": "ISSUE",
  "severity": "HIGH",
  "assigneeId": 2,
  "dueDate": "2026-06-25",
  "relatedTaskId": 210
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | string | Y | 이슈 제목 |
| `description` | string | Y | 상세 내용 |
| `type` | string | Y | `ISSUE` 또는 `RISK` |
| `severity` | string | Y | 심각도 (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) |
| `assigneeId` | integer | N | 담당자 ID |
| `dueDate` | string | N | 해결 기한 (yyyy-MM-dd) |
| `relatedTaskId` | integer | N | 연관 Task ID |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 410,
    "title": "인증 모듈 세션 만료 버그",
    "type": "ISSUE",
    "status": "OPEN",
    "severity": "HIGH",
    "reporter": { "id": 3, "name": "이철수" },
    "assignee": { "id": 2, "name": "김영희" },
    "dueDate": "2026-06-25",
    "reportedAt": "2026-06-15T10:30:00Z"
  },
  "message": "이슈가 등록되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `VALIDATION_ERROR` | 422 | 입력값 유효성 오류 |

---

### 7.3 이슈 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/issues/{id}` |
| **설명** | 이슈 상세 정보를 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 이슈 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 410,
    "title": "인증 모듈 세션 만료 버그",
    "description": "특정 조건에서 세션 토큰이 조기 만료되어 사용자가 강제 로그아웃됨",
    "type": "ISSUE",
    "status": "IN_PROGRESS",
    "severity": "HIGH",
    "project": { "id": 10, "name": "ERP 시스템 구축" },
    "reporter": { "id": 3, "name": "이철수" },
    "assignee": { "id": 2, "name": "김영희" },
    "relatedTask": { "id": 210, "name": "DB 설계 (ERD)" },
    "dueDate": "2026-06-25",
    "resolvedAt": null,
    "actionItems": [
      {
        "id": 600,
        "description": "세션 만료 로직 코드 리뷰",
        "status": "COMPLETED",
        "assignee": { "id": 2, "name": "김영희" },
        "dueDate": "2026-06-18"
      }
    ],
    "reportedAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ISSUE_NOT_FOUND` | 404 | 이슈를 찾을 수 없음 |

---

### 7.4 이슈 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/issues/{id}` |
| **설명** | 이슈 정보 및 상태를 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`, 이슈 담당자 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 이슈 ID |

**Request Body**

```json
{
  "status": "RESOLVED",
  "severity": "MEDIUM",
  "assigneeId": 2,
  "dueDate": "2026-06-22",
  "resolutionComment": "세션 만료 타이머 버그 수정 완료. 핫픽스 배포함."
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 410,
    "status": "RESOLVED",
    "severity": "MEDIUM",
    "resolvedAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "이슈가 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ISSUE_NOT_FOUND` | 404 | 이슈를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 7.5 이슈 액션 아이템 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/issues/{id}/action-items` |
| **설명** | 이슈에 등록된 조치 계획(Action Item) 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 이슈 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": 600,
      "issueId": 410,
      "description": "세션 만료 로직 코드 리뷰",
      "status": "COMPLETED",
      "assignee": { "id": 2, "name": "김영희" },
      "dueDate": "2026-06-18",
      "completedAt": "2026-06-17T16:00:00Z"
    },
    {
      "id": 601,
      "issueId": 410,
      "description": "핫픽스 개발 및 테스트",
      "status": "IN_PROGRESS",
      "assignee": { "id": 2, "name": "김영희" },
      "dueDate": "2026-06-22",
      "completedAt": null
    }
  ],
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ISSUE_NOT_FOUND` | 404 | 이슈를 찾을 수 없음 |

---

### 7.6 이슈 액션 아이템 등록

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/issues/{id}/action-items` |
| **설명** | 이슈에 조치 계획(Action Item)을 등록합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Request Body**

```json
{
  "description": "핫픽스 운영 배포 및 검증",
  "assigneeId": 2,
  "dueDate": "2026-06-25"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `description` | string | Y | 조치 내용 |
| `assigneeId` | integer | Y | 담당자 ID |
| `dueDate` | string | Y | 완료 기한 (yyyy-MM-dd) |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 602,
    "issueId": 410,
    "description": "핫픽스 운영 배포 및 검증",
    "status": "OPEN",
    "assignee": { "id": 2, "name": "김영희" },
    "dueDate": "2026-06-25",
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "액션 아이템이 등록되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ISSUE_NOT_FOUND` | 404 | 이슈를 찾을 수 없음 |
| `ISSUE_CLOSED` | 422 | 종료된 이슈에는 추가 불가 |

---

### 7.7 액션 아이템 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/action-items/{id}` |
| **설명** | 조치 계획 상태 및 내용을 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`, 담당자 `MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | Action Item ID |

**Request Body**

```json
{
  "description": "핫픽스 운영 배포 및 모니터링",
  "status": "COMPLETED",
  "dueDate": "2026-06-24"
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 602,
    "status": "COMPLETED",
    "completedAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "액션 아이템이 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ACTION_ITEM_NOT_FOUND` | 404 | 액션 아이템을 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

## 8. 프로젝트 인력 API (MEMBER)

### 8.1 프로젝트 멤버 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/members` |
| **설명** | 프로젝트에 배정된 인력 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `role` | string | N | - | 프로젝트 내 역할 필터 |
| `active` | boolean | N | true | 현재 참여 중인 멤버만 조회 |

**Response Body (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": 50,
      "user": {
        "id": 2,
        "name": "김영희",
        "email": "kim@example.com",
        "department": "개발팀",
        "position": "개발자"
      },
      "projectRole": "BACKEND_DEVELOPER",
      "joinDate": "2026-01-01",
      "leaveDate": null,
      "plannedEffortHours": 1200,
      "actualEffortHours": 420,
      "active": true
    },
    {
      "id": 51,
      "user": {
        "id": 3,
        "name": "이철수",
        "email": "lee@example.com",
        "department": "개발팀",
        "position": "개발자"
      },
      "projectRole": "DB_ARCHITECT",
      "joinDate": "2026-01-01",
      "leaveDate": null,
      "plannedEffortHours": 800,
      "actualEffortHours": 280,
      "active": true
    }
  ],
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |

---

### 8.2 프로젝트 멤버 추가

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/members` |
| **설명** | 프로젝트에 새 인력을 배정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Request Body**

```json
{
  "userId": 5,
  "projectRole": "FRONTEND_DEVELOPER",
  "joinDate": "2026-07-01",
  "leaveDate": "2026-12-31",
  "plannedEffortHours": 960
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `userId` | integer | Y | 배정할 사용자 ID |
| `projectRole` | string | Y | 프로젝트 내 역할 |
| `joinDate` | string | Y | 참여 시작일 (yyyy-MM-dd) |
| `leaveDate` | string | N | 참여 종료일 (yyyy-MM-dd) |
| `plannedEffortHours` | number | N | 계획 공수 (시간) |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 55,
    "user": {
      "id": 5,
      "name": "박민수",
      "email": "park@example.com"
    },
    "projectRole": "FRONTEND_DEVELOPER",
    "joinDate": "2026-07-01",
    "leaveDate": "2026-12-31",
    "plannedEffortHours": 960,
    "actualEffortHours": 0,
    "active": true,
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "프로젝트 멤버가 추가되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |
| `MEMBER_ALREADY_EXISTS` | 409 | 이미 참여 중인 멤버 |

---

### 8.3 프로젝트 멤버 정보 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/projects/{projectId}/members/{memberId}` |
| **설명** | 프로젝트 멤버의 역할, 참여 기간, 계획 공수를 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |
| `memberId` | integer | 프로젝트 멤버 ID |

**Request Body**

```json
{
  "projectRole": "TECH_LEAD",
  "leaveDate": "2026-11-30",
  "plannedEffortHours": 1100
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 55,
    "projectRole": "TECH_LEAD",
    "leaveDate": "2026-11-30",
    "plannedEffortHours": 1100,
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "멤버 정보가 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `MEMBER_NOT_FOUND` | 404 | 프로젝트 멤버를 찾을 수 없음 |

---

### 8.4 프로젝트 멤버 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/projects/{projectId}/members/{memberId}` |
| **설명** | 프로젝트 멤버를 삭제(프로젝트에서 제외)합니다. 진행 중인 담당 업무가 있을 경우 경고가 반환됩니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |
| `memberId` | integer | 프로젝트 멤버 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "프로젝트 멤버가 삭제되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `MEMBER_NOT_FOUND` | 404 | 프로젝트 멤버를 찾을 수 없음 |
| `MEMBER_HAS_ACTIVE_TASKS` | 409 | 진행 중인 담당 업무가 있음 |

---

### 8.5 멤버 공수 현황 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/members/{memberId}/effort` |
| **설명** | 특정 프로젝트 멤버의 월별 공수 투입 현황을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`, 본인 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |
| `memberId` | integer | 프로젝트 멤버 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `year` | integer | N | 현재연도 | 조회 연도 |
| `month` | integer | N | - | 조회 월 (없으면 연간 전체) |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "memberId": 55,
    "userName": "박민수",
    "projectId": 10,
    "projectName": "ERP 시스템 구축",
    "year": 2026,
    "summary": {
      "plannedHours": 960,
      "actualHours": 280,
      "utilizationRate": 29.2
    },
    "monthly": [
      {
        "month": 1,
        "plannedHours": 160,
        "actualHours": 0,
        "timeoffHours": 0
      },
      {
        "month": 6,
        "plannedHours": 160,
        "actualHours": 80,
        "timeoffHours": 8
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
| `MEMBER_NOT_FOUND` | 404 | 프로젝트 멤버를 찾을 수 없음 |

---

## 9. 근태/공수 API (TIME-OFF)

### 9.1 휴가/휴직 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/members/{memberId}/timeoffs` |
| **설명** | 특정 멤버의 휴가/휴직 신청 목록을 조회합니다. |
| **권한** | `ADMIN`, `PM`, 본인 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `memberId` | integer | 사용자 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `year` | integer | N | 현재연도 | 조회 연도 |
| `status` | string | N | - | 상태 필터 (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`) |
| `type` | string | N | - | 유형 필터 (`ANNUAL`, `SICK`, `SPECIAL`, `UNPAID`) |

**Response Body (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": 700,
      "type": "ANNUAL",
      "typeName": "연차",
      "status": "APPROVED",
      "startDate": "2026-06-20",
      "endDate": "2026-06-21",
      "days": 2,
      "reason": "개인 사유",
      "approver": { "id": 1, "name": "홍길동" },
      "approvedAt": "2026-06-16T09:00:00Z",
      "createdAt": "2026-06-15T10:00:00Z"
    }
  ],
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

### 9.2 휴가/휴직 신청

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/members/{memberId}/timeoffs` |
| **설명** | 휴가 또는 휴직을 신청합니다. |
| **권한** | 본인, `ADMIN` |

**Request Body**

```json
{
  "type": "ANNUAL",
  "startDate": "2026-07-14",
  "endDate": "2026-07-15",
  "reason": "여름 휴가",
  "approverId": 1
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | string | Y | 휴가 유형 (`ANNUAL`, `SICK`, `SPECIAL`, `UNPAID`) |
| `startDate` | string | Y | 시작일 (yyyy-MM-dd) |
| `endDate` | string | Y | 종료일 (yyyy-MM-dd) |
| `reason` | string | N | 사유 |
| `approverId` | integer | Y | 승인자 ID |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 710,
    "type": "ANNUAL",
    "typeName": "연차",
    "status": "PENDING",
    "startDate": "2026-07-14",
    "endDate": "2026-07-15",
    "days": 2,
    "reason": "여름 휴가",
    "approver": { "id": 1, "name": "홍길동" },
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "휴가가 신청되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `INSUFFICIENT_LEAVE_BALANCE` | 422 | 잔여 연차 부족 |
| `DUPLICATE_TIMEOFF` | 409 | 해당 기간에 이미 신청된 휴가 존재 |
| `INVALID_DATE_RANGE` | 422 | 유효하지 않은 날짜 범위 |

---

### 9.3 휴가 신청 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/timeoffs/{id}` |
| **설명** | PENDING 상태의 휴가 신청을 수정합니다. |
| **권한** | 본인, `ADMIN` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 휴가 신청 ID |

**Request Body**

```json
{
  "startDate": "2026-07-15",
  "endDate": "2026-07-16",
  "reason": "여름 휴가 (일정 변경)"
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 710,
    "startDate": "2026-07-15",
    "endDate": "2026-07-16",
    "days": 2,
    "status": "PENDING",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "휴가 신청이 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TIMEOFF_NOT_FOUND` | 404 | 휴가 신청을 찾을 수 없음 |
| `INVALID_STATUS_FOR_EDIT` | 422 | PENDING 상태가 아니어서 수정 불가 |

---

### 9.4 휴가 신청 취소

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/timeoffs/{id}` |
| **설명** | 휴가 신청을 취소합니다. PENDING 또는 APPROVED 상태에서만 가능합니다. |
| **권한** | 본인, `ADMIN` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 휴가 신청 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "휴가 신청이 취소되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TIMEOFF_NOT_FOUND` | 404 | 휴가 신청을 찾을 수 없음 |
| `CANNOT_CANCEL_PAST_TIMEOFF` | 409 | 이미 사용된 휴가는 취소 불가 |

---

### 9.5 휴가 승인/반려

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/timeoffs/{id}/approve` |
| **설명** | 휴가 신청을 승인하거나 반려합니다. |
| **권한** | 승인자 (`PM`), `ADMIN` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 휴가 신청 ID |

**Request Body**

```json
{
  "action": "APPROVE",
  "comment": "승인합니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `action` | string | Y | `APPROVE` 또는 `REJECT` |
| `comment` | string | N | 승인/반려 사유 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 710,
    "status": "APPROVED",
    "approver": { "id": 1, "name": "홍길동" },
    "approvedAt": "2026-06-15T10:30:00Z"
  },
  "message": "휴가 신청이 승인되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `TIMEOFF_NOT_FOUND` | 404 | 휴가 신청을 찾을 수 없음 |
| `NOT_APPROVER` | 403 | 승인 권한 없음 |
| `INVALID_STATUS_TRANSITION` | 422 | PENDING 상태가 아님 |

---

### 9.6 멤버 공수 요약 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/members/{memberId}/effort-summary` |
| **설명** | 멤버의 공수 투입 현황 및 잔여 연차 요약을 조회합니다. |
| **권한** | `ADMIN`, `PM`, 본인 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `memberId` | integer | 사용자 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `year` | integer | N | 현재연도 | 조회 연도 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "userId": 5,
    "userName": "박민수",
    "year": 2026,
    "leaveBalance": {
      "totalDays": 15,
      "usedDays": 3,
      "pendingDays": 2,
      "remainingDays": 10
    },
    "effortSummary": {
      "totalPlannedHours": 1920,
      "totalActualHours": 680,
      "utilizationRate": 35.4
    },
    "projectBreakdown": [
      {
        "projectId": 10,
        "projectName": "ERP 시스템 구축",
        "plannedHours": 960,
        "actualHours": 420
      },
      {
        "projectId": 11,
        "projectName": "CRM 포털 개편",
        "plannedHours": 960,
        "actualHours": 260
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
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |

---

## 10. 알림 API (NOTIFICATION)

### 10.1 알림 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/notifications` |
| **설명** | 현재 로그인한 사용자의 알림 목록을 조회합니다. |
| **권한** | 인증된 사용자 |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `unreadOnly` | boolean | N | false | 읽지 않은 알림만 조회 |
| `type` | string | N | - | 알림 유형 필터 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "unreadCount": 3,
    "content": [
      {
        "id": 800,
        "type": "TASK_ASSIGNED",
        "title": "업무가 배정되었습니다",
        "message": "[ERP 시스템 구축] 'DB 설계 (ERD)' 업무가 귀하에게 배정되었습니다.",
        "referenceType": "TASK",
        "referenceId": 210,
        "read": false,
        "createdAt": "2026-06-15T10:30:00Z"
      },
      {
        "id": 799,
        "type": "ISSUE_ASSIGNED",
        "title": "이슈가 배정되었습니다",
        "message": "[ERP 시스템 구축] '인증 모듈 세션 만료 버그' 이슈가 귀하에게 배정되었습니다.",
        "referenceType": "ISSUE",
        "referenceId": 410,
        "read": true,
        "createdAt": "2026-06-14T15:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 25,
    "totalPages": 2,
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
| `UNAUTHORIZED` | 401 | 인증 실패 |

---

### 10.2 알림 읽음 처리

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/notifications/{id}/read` |
| **설명** | 특정 알림을 읽음 상태로 변경합니다. |
| **권한** | 본인 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 알림 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 800,
    "read": true,
    "readAt": "2026-06-15T10:35:00Z"
  },
  "message": "알림이 읽음 처리되었습니다.",
  "timestamp": "2026-06-15T10:35:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `NOTIFICATION_NOT_FOUND` | 404 | 알림을 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 본인의 알림이 아님 |

---

### 10.3 전체 알림 읽음 처리

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/notifications/read-all` |
| **설명** | 현재 사용자의 모든 읽지 않은 알림을 읽음 처리합니다. |
| **권한** | 인증된 사용자 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "updatedCount": 3
  },
  "message": "모든 알림이 읽음 처리되었습니다.",
  "timestamp": "2026-06-15T10:35:00Z"
}
```

---

### 10.4 알림 규칙 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/notification-rules` |
| **설명** | 프로젝트의 알림 발송 규칙 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": 900,
      "eventType": "TASK_OVERDUE",
      "eventTypeName": "업무 기한 초과",
      "enabled": true,
      "recipients": ["ASSIGNEE", "PM"],
      "advanceNoticeDays": 3,
      "channels": ["IN_APP", "EMAIL"]
    },
    {
      "id": 901,
      "eventType": "ISSUE_CRITICAL",
      "eventTypeName": "심각도 CRITICAL 이슈 등록",
      "enabled": true,
      "recipients": ["PM", "ADMIN"],
      "advanceNoticeDays": 0,
      "channels": ["IN_APP", "EMAIL"]
    }
  ],
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

---

### 10.5 알림 규칙 생성/수정

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/notification-rules` |
| **설명** | 프로젝트 알림 규칙을 생성하거나 수정합니다. 동일 이벤트 유형이 있으면 업데이트됩니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Request Body**

```json
{
  "eventType": "DELIVERABLE_DUE_SOON",
  "enabled": true,
  "recipients": ["SUBMITTER", "REVIEWER", "PM"],
  "advanceNoticeDays": 5,
  "channels": ["IN_APP", "EMAIL"]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `eventType` | string | Y | 이벤트 유형 |
| `enabled` | boolean | Y | 활성화 여부 |
| `recipients` | array | Y | 수신 대상 목록 |
| `advanceNoticeDays` | integer | N | 사전 알림 일수 (0이면 즉시) |
| `channels` | array | Y | 발송 채널 (`IN_APP`, `EMAIL`) |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 902,
    "eventType": "DELIVERABLE_DUE_SOON",
    "enabled": true,
    "recipients": ["SUBMITTER", "REVIEWER", "PM"],
    "advanceNoticeDays": 5,
    "channels": ["IN_APP", "EMAIL"],
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "알림 규칙이 저장되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `INVALID_EVENT_TYPE` | 422 | 유효하지 않은 이벤트 유형 |

---

## 11. 대시보드 API

### 11.1 PM 대시보드

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/dashboard/pm` |
| **설명** | PM을 위한 특정 프로젝트 종합 현황 대시보드 데이터를 조회합니다. |
| **권한** | `ADMIN`, `PM` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `projectId` | integer | Y | - | 조회할 프로젝트 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "project": {
      "id": 10,
      "name": "ERP 시스템 구축",
      "status": "IN_PROGRESS",
      "progressRate": 35,
      "daysRemaining": 199,
      "scheduleStatus": "ON_TIME"
    },
    "taskOverview": {
      "total": 80,
      "notStarted": 30,
      "inProgress": 35,
      "completed": 15,
      "overdue": 3,
      "dueSoon": 8
    },
    "issueOverview": {
      "open": 5,
      "critical": 1,
      "overdue": 2
    },
    "deliverableOverview": {
      "total": 20,
      "pending": 8,
      "submitted": 5,
      "approved": 7,
      "overdue": 2
    },
    "effortOverview": {
      "totalPlannedHours": 14400,
      "totalActualHours": 4200,
      "utilizationRate": 29.2
    },
    "recentActivities": [
      {
        "type": "TASK_STATUS_CHANGED",
        "description": "'DB 설계 (ERD)' 업무 상태가 IN_PROGRESS로 변경",
        "actor": "이철수",
        "occurredAt": "2026-06-15T09:00:00Z"
      }
    ],
    "overdueTaskList": [
      {
        "id": 205,
        "name": "API 명세서 작성",
        "assignee": "김영희",
        "dueDate": "2026-06-10",
        "daysOverdue": 5
      }
    ],
    "upcomingMilestones": [
      {
        "name": "1차 중간 점검",
        "date": "2026-07-01",
        "daysUntil": 16
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
| `ACCESS_DENIED` | 403 | 해당 프로젝트 접근 권한 없음 |

---

### 11.2 멤버 대시보드

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/dashboard/member` |
| **설명** | 현재 로그인한 멤버 본인의 업무 현황 및 일정 대시보드 데이터를 조회합니다. |
| **권한** | 인증된 사용자 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "myTasks": {
      "total": 12,
      "inProgress": 5,
      "overdue": 1,
      "dueSoon": 3
    },
    "todayTasks": [
      {
        "id": 210,
        "name": "DB 설계 (ERD)",
        "projectName": "ERP 시스템 구축",
        "status": "IN_PROGRESS",
        "progressRate": 50,
        "dueDate": "2026-07-05"
      }
    ],
    "upcomingDeadlines": [
      {
        "id": 215,
        "name": "단위 테스트 작성",
        "projectName": "ERP 시스템 구축",
        "dueDate": "2026-06-20",
        "daysUntil": 5
      }
    ],
    "myIssues": {
      "assigned": 2,
      "reported": 3
    },
    "leaveBalance": {
      "remainingDays": 10,
      "pendingDays": 2
    },
    "monthlyEffort": {
      "month": 6,
      "plannedHours": 160,
      "actualHours": 80
    },
    "recentActivities": [
      {
        "type": "PROGRESS_UPDATED",
        "description": "'DB 설계 (ERD)' 진행률 50% 업데이트",
        "occurredAt": "2026-06-15T10:30:00Z"
      }
    ]
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

---

### 11.3 PMO 대시보드

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/dashboard/pmo` |
| **설명** | PMO(Project Management Office) 전용으로 전체 프로젝트 포트폴리오 현황을 조회합니다. |
| **권한** | `ADMIN` |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "portfolioSummary": {
      "totalProjects": 8,
      "byStatus": {
        "PLANNING": 1,
        "IN_PROGRESS": 5,
        "ON_HOLD": 1,
        "COMPLETED": 1
      },
      "atRisk": 2,
      "onTime": 4,
      "delayed": 2
    },
    "projects": [
      {
        "id": 10,
        "name": "ERP 시스템 구축",
        "status": "IN_PROGRESS",
        "progressRate": 35,
        "scheduleStatus": "ON_TIME",
        "openIssues": 5,
        "criticalIssues": 1,
        "pmName": "홍길동",
        "daysRemaining": 199
      },
      {
        "id": 11,
        "name": "CRM 포털 개편",
        "status": "IN_PROGRESS",
        "progressRate": 20,
        "scheduleStatus": "DELAYED",
        "openIssues": 3,
        "criticalIssues": 0,
        "pmName": "홍길동",
        "daysRemaining": 199
      }
    ],
    "resourceOverview": {
      "totalMembers": 45,
      "activeMembers": 38,
      "averageUtilizationRate": 72.5
    },
    "budgetOverview": {
      "totalBudget": 2500000000,
      "totalUsedBudget": 680000000,
      "overallUsageRate": 27.2
    },
    "criticalIssues": [
      {
        "id": 410,
        "title": "인증 모듈 세션 만료 버그",
        "projectName": "ERP 시스템 구축",
        "severity": "CRITICAL",
        "daysOpen": 5
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
| `ACCESS_DENIED` | 403 | ADMIN 권한 필요 |

---

## 12. 활동 이력 API

### 12.1 프로젝트 활동 이력 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/activities` |
| **설명** | 프로젝트 내 모든 활동 이력을 시간 역순으로 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 30 | 페이지당 항목 수 |
| `type` | string | N | - | 활동 유형 필터 (`TASK`, `ISSUE`, `DELIVERABLE`, `MEMBER`) |
| `actorId` | integer | N | - | 수행자 ID 필터 |
| `from` | string | N | - | 시작 날짜 필터 (yyyy-MM-dd) |
| `to` | string | N | - | 종료 날짜 필터 (yyyy-MM-dd) |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1001,
        "type": "TASK",
        "action": "PROGRESS_UPDATED",
        "description": "'DB 설계 (ERD)' 업무 진행률이 30% → 50%로 변경되었습니다.",
        "actor": {
          "id": 3,
          "name": "이철수"
        },
        "referenceType": "TASK",
        "referenceId": 210,
        "referenceName": "DB 설계 (ERD)",
        "metadata": {
          "oldValue": "30",
          "newValue": "50"
        },
        "occurredAt": "2026-06-15T10:30:00Z"
      },
      {
        "id": 1000,
        "type": "ISSUE",
        "action": "ISSUE_CREATED",
        "description": "'인증 모듈 세션 만료 버그' 이슈가 등록되었습니다. (심각도: HIGH)",
        "actor": {
          "id": 3,
          "name": "이철수"
        },
        "referenceType": "ISSUE",
        "referenceId": 410,
        "referenceName": "인증 모듈 세션 만료 버그",
        "metadata": {
          "severity": "HIGH"
        },
        "occurredAt": "2026-06-10T14:00:00Z"
      }
    ],
    "page": 0,
    "size": 30,
    "totalElements": 215,
    "totalPages": 8,
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
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |

---

### 12.2 Task 활동 이력 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/tasks/{taskId}/activities` |
| **설명** | 특정 Task에 대한 모든 변경 이력 및 활동을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `taskId` | integer | Task ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1001,
        "action": "PROGRESS_UPDATED",
        "description": "진행률이 30% → 50%로 변경되었습니다.",
        "actor": {
          "id": 3,
          "name": "이철수"
        },
        "metadata": {
          "fieldName": "progressRate",
          "oldValue": "30",
          "newValue": "50",
          "comment": "논리 ERD 완료, 물리 모델 진행 중"
        },
        "occurredAt": "2026-06-15T10:30:00Z"
      },
      {
        "id": 999,
        "action": "ASSIGNEE_CHANGED",
        "description": "담당자가 변경되었습니다. (김영희 → 이철수)",
        "actor": {
          "id": 1,
          "name": "홍길동"
        },
        "metadata": {
          "oldAssigneeId": 2,
          "oldAssigneeName": "김영희",
          "newAssigneeId": 3,
          "newAssigneeName": "이철수"
        },
        "occurredAt": "2026-06-16T09:00:00Z"
      },
      {
        "id": 998,
        "action": "TASK_CREATED",
        "description": "업무가 생성되었습니다.",
        "actor": {
          "id": 1,
          "name": "홍길동"
        },
        "metadata": {},
        "occurredAt": "2026-06-15T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 12,
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
| `TASK_NOT_FOUND` | 404 | Task를 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |

---

---

## 13. 공문 API (OFFICIAL-DOC)

### 13.1 프로젝트 공문 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/official-docs` |
| **설명** | 특정 프로젝트의 공문 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `direction` | string | N | - | 수발신 구분 (`INBOUND`, `OUTBOUND`) |
| `approvalStatus` | string | N | - | 결재 상태 필터 |
| `keyword` | string | N | - | 제목 검색어 |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "docNo": "OKE-202603-000312",
        "title": "개발환경 구축 관련 협조 요청",
        "direction": "OUTBOUND",
        "senderOrg": "(주)OKE",
        "receiverOrg": "(주)고객사",
        "sentDate": "2026-03-15",
        "approvalStatus": "APPROVED",
        "reviewStatus": "APPROVED",
        "attachmentCount": 2,
        "drafter": { "id": 1, "name": "홍길동" },
        "createdAt": "2026-03-14T10:00:00Z"
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
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |

---

### 13.2 공문 등록

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/official-docs` |
| **설명** | 프로젝트에 공문을 등록합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Request Body**

```json
{
  "docNo": "OKE-202606-000450",
  "title": "시스템 설계 검토 요청",
  "direction": "OUTBOUND",
  "senderOrg": "(주)OKE",
  "receiverOrg": "(주)고객사",
  "drafterId": 1,
  "draftDept": "개발팀",
  "sentDate": "2026-06-15",
  "content": "시스템 설계서 검토를 요청드립니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `docNo` | string | N | 품의번호 (자동 생성 가능) |
| `title` | string | Y | 공문 제목 |
| `direction` | string | Y | `INBOUND` 또는 `OUTBOUND` |
| `senderOrg` | string | N | 발신 기관 |
| `receiverOrg` | string | N | 수신 기관 |
| `drafterId` | integer | N | 기안자 ID |
| `draftDept` | string | N | 기안부서 |
| `sentDate` | string | N | 발신일 (yyyy-MM-dd) |
| `content` | string | N | 공문 내용 |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "docNo": "OKE-202606-000450",
    "title": "시스템 설계 검토 요청",
    "direction": "OUTBOUND",
    "approvalStatus": "PENDING",
    "reviewStatus": "PENDING",
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "공문이 등록되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `DOC_NO_DUPLICATE` | 409 | 이미 사용 중인 품의번호 |
| `VALIDATION_ERROR` | 422 | 입력값 유효성 오류 |

---

### 13.3 전체 공문 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/official-docs` |
| **설명** | 전체 공문 목록을 조회합니다. 프로젝트, 결재 상태 등으로 필터링 가능합니다. |
| **권한** | `ADMIN`, `PM` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `projectId` | integer | N | - | 프로젝트 필터 |
| `approvalStatus` | string | N | - | 결재 상태 필터 (`PENDING`,`APPROVED`,`REJECTED`,`CANCELLED`) |
| `direction` | string | N | - | 수발신 구분 필터 |
| `keyword` | string | N | - | 제목/품의번호 검색어 |

**Response Body (200)**: 13.1과 동일한 페이징 형식

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 13.4 공문 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/official-docs/{id}` |
| **설명** | 공문 상세 정보를 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 공문 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "project": { "id": 10, "name": "ERP 시스템 구축" },
    "docNo": "OKE-202606-000450",
    "title": "시스템 설계 검토 요청",
    "direction": "OUTBOUND",
    "senderOrg": "(주)OKE",
    "receiverOrg": "(주)고객사",
    "drafter": { "id": 1, "name": "홍길동" },
    "draftDept": "개발팀",
    "sentDate": "2026-06-15",
    "approvalStatus": "APPROVED",
    "reviewStatus": "APPROVED",
    "attachmentCount": 2,
    "content": "시스템 설계서 검토를 요청드립니다.",
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `OFFICIAL_DOC_NOT_FOUND` | 404 | 공문을 찾을 수 없음 |

---

### 13.5 공문 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/official-docs/{id}` |
| **설명** | 공문 정보를 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 공문 ID |

**Request Body**

```json
{
  "title": "시스템 설계 검토 요청 (수정)",
  "approvalStatus": "APPROVED",
  "reviewStatus": "APPROVED",
  "content": "수정된 내용입니다."
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "title": "시스템 설계 검토 요청 (수정)",
    "approvalStatus": "APPROVED",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "message": "공문이 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `OFFICIAL_DOC_NOT_FOUND` | 404 | 공문을 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 13.6 공문 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/official-docs/{id}` |
| **설명** | 공문을 삭제합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 공문 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "공문이 삭제되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `OFFICIAL_DOC_NOT_FOUND` | 404 | 공문을 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

## 14. 회의록 API (MEETING)

### 14.1 프로젝트 회의록 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/projects/{projectId}/meetings` |
| **설명** | 특정 프로젝트의 회의록 목록을 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `projectId` | integer | 프로젝트 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `meetingType` | string | N | - | 회의 유형 필터 |
| `status` | string | N | - | 상태 필터 (`DRAFT`,`COMPLETED`,`CANCELLED`) |
| `keyword` | string | N | - | 제목 검색어 |
| `from` | string | N | - | 회의일 시작 (yyyy-MM-dd) |
| `to` | string | N | - | 회의일 종료 (yyyy-MM-dd) |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "6월 정기 회의",
        "meetingType": "정기 회의",
        "meetingDate": "2026-06-15T14:00:00",
        "location": "회의실 A",
        "status": "COMPLETED",
        "author": { "id": 1, "name": "홍길동" },
        "createdAt": "2026-06-15T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 8,
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
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |

---

### 14.2 회의록 등록

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/v1/projects/{projectId}/meetings` |
| **설명** | 프로젝트에 회의록을 등록합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Request Body**

```json
{
  "title": "7월 킥오프 회의",
  "meetingType": "킥오프",
  "meetingDate": "2026-07-01T10:00:00",
  "location": "대회의실",
  "attendees": "홍길동, 김영희, 이철수",
  "agenda": "1. 프로젝트 개요 설명\n2. 팀 소개\n3. 일정 공유",
  "minutes": "회의록 내용..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | string | Y | 회의 제목 |
| `meetingType` | string | Y | 회의 유형 |
| `meetingDate` | string | Y | 회의 일시 (ISO 8601) |
| `location` | string | N | 회의 장소 |
| `attendees` | string | N | 참석자 목록 |
| `agenda` | string | N | 회의 안건 |
| `minutes` | string | N | 회의록 내용 |

**Response Body (201)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "title": "7월 킥오프 회의",
    "meetingType": "킥오프",
    "meetingDate": "2026-07-01T10:00:00",
    "status": "DRAFT",
    "createdAt": "2026-06-15T10:30:00Z"
  },
  "message": "회의록이 등록되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | 프로젝트를 찾을 수 없음 |
| `VALIDATION_ERROR` | 422 | 입력값 유효성 오류 |

---

### 14.3 전체 회의록 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/meetings` |
| **설명** | 전체 회의록 목록을 조회합니다. 프로젝트, 검색어 등으로 필터링 가능합니다. |
| **권한** | `ADMIN`, `PM` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | N | 0 | 페이지 번호 |
| `size` | integer | N | 20 | 페이지당 항목 수 |
| `projectId` | integer | N | - | 프로젝트 필터 |
| `keyword` | string | N | - | 제목/안건 검색어 |
| `from` | string | N | - | 회의일 시작 |
| `to` | string | N | - | 회의일 종료 |

**Response Body (200)**: 14.1과 동일한 페이징 형식

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 14.4 회의록 상세 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/v1/meetings/{id}` |
| **설명** | 회의록 상세 정보를 조회합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`/`MEMBER` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 회의록 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "project": { "id": 10, "name": "ERP 시스템 구축" },
    "title": "7월 킥오프 회의",
    "meetingType": "킥오프",
    "meetingDate": "2026-07-01T10:00:00",
    "location": "대회의실",
    "attendees": "홍길동, 김영희, 이철수",
    "agenda": "1. 프로젝트 개요 설명\n2. 팀 소개\n3. 일정 공유",
    "minutes": "회의록 내용...",
    "status": "COMPLETED",
    "author": { "id": 1, "name": "홍길동" },
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-07-01T16:00:00Z"
  },
  "message": "조회가 완료되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `MEETING_NOT_FOUND` | 404 | 회의록을 찾을 수 없음 |

---

### 14.5 회의록 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/v1/meetings/{id}` |
| **설명** | 회의록 정보를 수정합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM`, 작성자 |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 회의록 ID |

**Request Body**

```json
{
  "title": "7월 킥오프 회의 (최종)",
  "minutes": "최종 회의록 내용...",
  "status": "COMPLETED"
}
```

**Response Body (200)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "title": "7월 킥오프 회의 (최종)",
    "status": "COMPLETED",
    "updatedAt": "2026-07-01T16:00:00Z"
  },
  "message": "회의록이 수정되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `MEETING_NOT_FOUND` | 404 | 회의록을 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

### 14.6 회의록 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/v1/meetings/{id}` |
| **설명** | 회의록을 삭제합니다. |
| **권한** | `ADMIN`, 해당 프로젝트 `PM` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | integer | 회의록 ID |

**Response Body (200)**

```json
{
  "success": true,
  "data": null,
  "message": "회의록이 삭제되었습니다.",
  "timestamp": "2026-06-15T10:30:00Z"
}
```

**오류 코드**

| 코드 | HTTP | 설명 |
|------|------|------|
| `MEETING_NOT_FOUND` | 404 | 회의록을 찾을 수 없음 |
| `ACCESS_DENIED` | 403 | 권한 없음 |

---

*문서 끝 — PMS REST API 설계 v1.0.0*
