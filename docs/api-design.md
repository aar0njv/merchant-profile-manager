# Merchant Profile Service — API Design (v1)

## 1. Overview & Design Principles

The **Merchant Profile Service** is the single source of truth for merchant identity and lifecycle status (`PENDING`, `ACTIVE`, `SUSPENDED`, `CLOSED`). This document defines the v1 REST contract, headers, status codes, request/response bodies, error handling, pagination, and database indexing strategy.

* **JSON Naming Style:** `camelCase` across all request and response payloads to match Java DTO/record conventions consistently, instead of `snake_case` or mixed casing.
* **Identifiers:** Server-generated `UUID` (`id`) on creation; clients never supply or modify `id`, `createdAt`, or `updatedAt`.

---

## 2. Endpoint Table

**Base Path:** `/api/v1`

| # | Operation | Method | URL | Request Body | Success Code & Headers | Success Body | Failure Codes |
|---|-----------|--------|-----|--------------|------------------------|--------------|---------------|
| 1 | **List all merchants** | `GET` | `/api/v1/merchants?status={status}&page={page}&size={size}` | None | `200 OK`<br>`Content-Type: application/json` | `PageResponse<MerchantResponse>` | `400 Bad Request`, `500 Internal Server Error` |
| 2 | **Get a specific merchant** | `GET` | `/api/v1/merchants/{id}` | None | `200 OK`<br>`Content-Type: application/json` | `MerchantResponse` | `400 Bad Request`, `404 Not Found`, `500 Internal Server Error` |
| 3 | **Create a merchant** | `POST` | `/api/v1/merchants` | `CreateMerchantRequest` | `201 Created`<br>`Content-Type: application/json`<br>`Location: /api/v1/merchants/{id}` | `MerchantResponse` | `400 Bad Request`, `409 Conflict`, `500 Internal Server Error` |
| 4 | **Update a merchant** | `PUT` | `/api/v1/merchants/{id}` | `UpdateMerchantRequest` | `200 OK`<br>`Content-Type: application/json` | `MerchantResponse` | `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error` |
| 5 | **Delete a merchant (soft)** | `DELETE` | `/api/v1/merchants/{id}` | None | `204 No Content` | Empty (no body) | `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error` |

---

## 3. API Versioning Strategy

* **Choice:** URI Path Versioning with major version only (`/api/v1/merchants`) because the version is immediately visible in browser URLs, server logs, and test scripts with simple routing
* **Enum Evolution Policy:** Adding a new value to `MerchantStatus` or `BusinessType` in responses is treated as a breaking change for strict clients (e.g., exhaustive `switch` statements), so new enum values require a new major version (`v2`).

---

## 4. Request & Response Headers

### Request Headers
| Header | Required On | Value | Purpose |
|--------|-------------|-------|---------|
| `Content-Type` | `POST`, `PUT` | `application/json` | Declares JSON request payload; unsupported/missing media types fail before reaching business logic. |
| `Accept` | Optional (all) | `application/json`, `application/problem+json` | Indicates expected response/error representation. |

### Response Headers
| Header | Returned On | Example Value | Purpose |
|--------|-------------|---------------|---------|
| `Content-Type` | All responses with a body (`200`, `201`) | `application/json` | Identifies standard JSON DTO responses. |
| `Content-Type` | All error responses (`4xx`, `5xx`) | `application/problem+json` | Identifies RFC 9457 Problem Details error payloads so clients parse all errors uniformly. |
| `Location` | `201 Created` (`POST /api/v1/merchants`) | `/api/v1/merchants/7f3c9a10-4b2e-4f8a-9c1d-2a3b4c5d6e7f` | Points the client directly to the newly created resource URI. |
| `Deprecation` / `Sunset` | Future deprecated versions (e.g., retiring `v1`) | `Deprecation: @1798761600`<br>`Sunset: Wed, 01 Jul 2027 00:00:00 GMT` | RFC 9745 / RFC 8594 headers added during version retirement before returning `410 Gone`. |

> **`204 No Content` Note:** `DELETE /api/v1/merchants/{id}` returns no body and therefore omits `Content-Type` for a payload.

---

## 5. HTTP Status Codes Reference

### Success Codes (`2xx`)
| Code | Meaning | Used By | Semantics |
|------|---------|---------|-----------|
| `200 OK` | Request succeeded and response body contains the resource or page. | `GET /merchants`, `GET /merchants/{id}`, `PUT /merchants/{id}` | `GET` is safe & idempotent; `PUT` is idempotent (with matching `version`). |
| `201 Created` | New merchant persisted with server-generated `id` and `PENDING` status. | `POST /merchants` | Not idempotent; includes `Location` header and created `MerchantResponse`. |
| `204 No Content` | Merchant soft-deleted (`status` set to `CLOSED`); nothing to return. | `DELETE /merchants/{id}` | No response body. |

### Error Codes (`4xx` / `5xx`)
| Code | Meaning | Triggers in This API |
|------|---------|----------------------|
| `400 Bad Request` | Syntactically invalid input or constraint failure. | Malformed JSON, invalid UUID path parameter `{id}`, unknown enum value in `status` query param, `page < 0`, `size < 1` or `size > max-size`, or Bean Validation failures on request fields. |
| `404 Not Found` | Target merchant does not exist. | `GET`, `PUT`, or `DELETE` with a valid UUID `{id}` that has no matching row in `merchants`. |
| `409 Conflict` | Request is valid in syntax, but conflicts with current server/DB state. | Duplicate `email` on `POST` or `PUT` (enforced by service check + DB unique constraint `uq_merchants_email`), stale `version` on `PUT` (optimistic locking conflict), or attempting to `DELETE` a merchant that is already `CLOSED`. |
| `500 Internal Server Error` | Unexpected server/database failure. | Unhandled exceptions, DB connection loss. **Never exposes stack traces or SQL details.** |

> **400 vs 409 Distinction:** `400` means the request *by itself* is invalid regardless of database state (e.g., blank `legalName`, malformed `email`, `mcc` not 4 digits). `409` means the request is well-formed, but clashes with *existing state* (e.g., `email` already belongs to another merchant, or `version` is stale). I chose `409 Conflict` instead of `422 Unprocessable Content` so state conflicts and uniqueness violations share one clear status code.

---

## 6. Request & Response Body Contracts

### 6.1 Enums
* **`BusinessType`:** `SOLE_PROPRIETOR`, `PARTNERSHIP`, `PRIVATE_LIMITED`, `PUBLIC_LIMITED`
* **`MerchantStatus`:** `PENDING`, `ACTIVE`, `SUSPENDED`, `CLOSED`

### 6.2 `CreateMerchantRequest` (`POST /api/v1/merchants`)
Clients omit `id`, `status`, `createdAt`, `updatedAt`, and `version`. The server generates `id` (`UUID`), sets `status = PENDING`, initializes `version = 0`, and sets `createdAt` / `updatedAt`.

| Field | Type | Required | Constraints / Rules |
|-------|------|----------|---------------------|
| `legalName` | `String` | Yes | Not blank, max 200 chars |
| `displayName` | `String` | Yes | Not blank, max 100 chars |
| `businessType` | `BusinessType` | Yes | One of the 4 allowed enum values |
| `mcc` | `String` | Yes | Exactly 4 digits (`^\d{4}$`), e.g. `"5411"` |
| `email` | `String` | Yes | Valid email format, max 254 chars, unique across all merchants |
| `phone` | `String` | Yes | Not blank, max 20 chars |
| `addressLine1` | `String` | Yes | Not blank, max 200 chars |
| `addressLine2` | `String` | No | Optional (`null` allowed), max 200 chars |
| `city` | `String` | Yes | Not blank, max 100 chars |
| `state` | `String` | Yes | Not blank, max 100 chars |
| `postalCode` | `String` | Yes | Not blank, max 10 chars |
| `country` | `String` | Yes | ISO 3166-1 alpha-2 code (`^[A-Z]{2}$`), e.g. `"IN"` |

### 6.3 `UpdateMerchantRequest` (`PUT /api/v1/merchants/{id}`)
* **Choice:** Full resource replacement via `PUT` rather than partial `PATCH` because replacing all editable profile fields makes updates idempotent and keeps validation rules unambiguous, instead of `PATCH` where distinguishing omitted fields from explicit `null` values complicates Java DTOs.
* **Fields:** Same editable profile fields as `CreateMerchantRequest`, **plus** `version` (`long`, required, `>= 0`) for optimistic locking. Clients still cannot change `id`, `status`, `createdAt`, or `updatedAt`.

### 6.4 `MerchantResponse`
Returned by `POST` (`201`), `GET /{id}` (`200`), `PUT /{id}` (`200`), and inside `PageResponse.items` (`200`). Never returns the JPA `MerchantEntity` directly.

```json
{
  "id": "7f3c9a10-4b2e-4f8a-9c1d-2a3b4c5d6e7f",
  "legalName": "Sunrise Grocers Private Limited",
  "displayName": "Sunrise Grocers",
  "businessType": "PRIVATE_LIMITED",
  "mcc": "5411",
  "email": "finance@sunrisegrocers.in",
  "phone": "+919876543210",
  "addressLine1": "42 MG Road",
  "addressLine2": "Floor 2, Indiranagar",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postalCode": "560038",
  "country": "IN",
  "status": "PENDING",
  "createdAt": "2026-11-24T10:15:30Z",
  "updatedAt": "2026-11-24T10:15:30Z",
  "version": 0
}
```

### 6.5 `PageResponse<MerchantResponse>` (`GET /api/v1/merchants`)
```json
{
  "items": [
    {
      "id": "7f3c9a10-4b2e-4f8a-9c1d-2a3b4c5d6e7f",
      "legalName": "Sunrise Grocers Private Limited",
      "displayName": "Sunrise Grocers",
      "businessType": "PRIVATE_LIMITED",
      "mcc": "5411",
      "email": "finance@sunrisegrocers.in",
      "phone": "+919876543210",
      "addressLine1": "42 MG Road",
      "addressLine2": null,
      "city": "Bengaluru",
      "state": "Karnataka",
      "postalCode": "560038",
      "country": "IN",
      "status": "PENDING",
      "createdAt": "2026-11-24T10:15:30Z",
      "updatedAt": "2026-11-24T10:15:30Z",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 7. Soft Delete Behaviour

* **Choice:** Soft delete (`DELETE /api/v1/merchants/{id}` sets `status = CLOSED` and updates `updatedAt`).
* **Reasoning:** I chose a soft delete because a payment company must retain merchant records for financial audits, chargeback disputes, and regulatory compliance, instead of a hard delete which permanently erases historical records.
* **Read Behaviour After Delete:** `GET /api/v1/merchants/{id}` still returns `200 OK` with `"status": "CLOSED"` so payment and verification services can verify why the merchant cannot transact. Calling `DELETE` again on an already `CLOSED` merchant returns `409 Conflict`.

---

## 8. Pagination & Filtering Design

`GET /api/v1/merchants?status={status}&page={page}&size={size}`

* **Query Parameters:**
  * `status` (optional): Filter by `PENDING`, `ACTIVE`, `SUSPENDED`, or `CLOSED`.
  * `page` (optional, default `0`): Zero-based page index (`page >= 0`).
  * `size` (optional, default `20`, max `100`): Number of items per page (`1 <= size <= maxPageSize`).
* **Configuration (`application.properties`):**
  ```properties
  merchant.pagination.default-size=20
  merchant.pagination.max-size=100
  ```
  Injected via `@ConfigProperty(name = "merchant.pagination.default-size")` and `@ConfigProperty(name = "merchant.pagination.max-size")` (or `@ConfigMapping`).
* **Default Sort Order:** `created_at DESC, id ASC` (newest merchants first, with `id` as a deterministic tie-breaker).
* **Reasoning:** I chose bounded query-parameter pagination with a deterministic `(created_at DESC, id ASC)` sort because it prevents unbounded client requests from exhausting memory and guarantees stable page boundaries across 50,000+ rows, instead of returning unpaginated lists or sorting on `created_at` alone.

---

## 9. Error Handling Contract (RFC 9457 Problem Details)

Every `4xx` and `5xx` error returns `Content-Type: application/problem+json` using the unified `ErrorResponse` structure:

```java
public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    List<ValidationError> errors
) {
    public record ValidationError(
        String field,
        String message
    ) {}
}
```

* **Choice:** RFC 9457 Problem Details with an `errors` list for field-level validation errors.
* **Reasoning:** I chose RFC 9457 because every endpoint returns one predictable, standard error shape for clients to parse centrally, instead of custom ad-hoc error payloads or single-string validation messages.

### Error Payload Examples

**1. `400 Bad Request` — Validation Failure (all invalid fields returned together)**
```json
{
  "type": "https://api.merchant-profile.internal/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more request fields failed validation",
  "instance": "/api/v1/merchants",
  "errors": [
    {
      "field": "email",
      "message": "Must be a valid email address"
    },
    {
      "field": "mcc",
      "message": "MCC must be exactly 4 digits"
    }
  ]
}
```

**2. `400 Bad Request` — Invalid Parameter / Malformed JSON (`errors` is `[]` or omitted)**
```json
{
  "type": "https://api.merchant-profile.internal/problems/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid UUID format for merchant id: 'abc'",
  "instance": "/api/v1/merchants/abc",
  "errors": []
}
```

**3. `404 Not Found` — Unknown Merchant**
```json
{
  "type": "https://api.merchant-profile.internal/problems/merchant-not-found",
  "title": "Merchant Not Found",
  "status": 404,
  "detail": "No merchant exists with id 7f3c9a10-4b2e-4f8a-9c1d-2a3b4c5d6e7f",
  "instance": "/api/v1/merchants/7f3c9a10-4b2e-4f8a-9c1d-2a3b4c5d6e7f",
  "errors": []
}
```

**4. `409 Conflict` — Duplicate Email or Optimistic Lock Failure**
```json
{
  "type": "https://api.merchant-profile.internal/problems/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "A merchant with email finance@sunrisegrocers.in already exists",
  "instance": "/api/v1/merchants",
  "errors": []
}
```
> **Concurrency Guard:** Even if two concurrent `POST` or `PUT` requests pass the application-level `existsByEmail` check, PostgreSQL's `uq_merchants_email` constraint rejects the second write; the exception mapper catches the database constraint violation and maps it to `409 Conflict`. Similarly, an `OptimisticLockException` from `@Version` mismatch maps to `409 Conflict`.

**5. `500 Internal Server Error` — Unexpected Failure (No Stack Trace)**
```json
{
  "type": "https://api.merchant-profile.internal/problems/internal-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please try again later.",
  "instance": "/api/v1/merchants",
  "errors": []
}
```

---

## 10. Database Indexes (50,000+ Merchants)

To keep filtered and unfiltered paginated queries fast at 50,000+ rows without full-table sorts:

1. **`uq_merchants_email` (`UNIQUE (email)`):**
   * *Reasoning:* I chose a database unique constraint on `email` because it enforces uniqueness atomically under concurrent writes, instead of relying only on a race-prone application `SELECT`.
2. **`idx_merchants_status_created_id` (`(status, created_at DESC, id ASC)`):**
   * *Reasoning:* I chose a composite index covering `status` followed by the sort columns `(created_at DESC, id ASC)` because `GET /api/v1/merchants?status=...` can read the requested page in order directly from the index, instead of indexing `status` alone and sorting thousands of matching rows in memory.
3. **`idx_merchants_created_id` (`(created_at DESC, id ASC)`):**
   * *Reasoning:* I chose a composite index on `(created_at DESC, id ASC)` because unfiltered `GET /api/v1/merchants` queries still need a fast ordered index scan across 50,000 rows, instead of performing a sequential scan and full sort on every page request.
