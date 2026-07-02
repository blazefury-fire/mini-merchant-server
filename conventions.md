# Feature Conventions

The authoritative pattern for adding a new entity + its database schema to Mini Merchant Server. Written to be followed mechanically — by a person or a skill — so every entity looks identical to the existing `merchants` entity. When in doubt, open `merchants` and copy it.

**Placeholders:** `{Entity}` = PascalCase singular (`Order`) · `{Entities}` = pluralized PascalCase as used in code (`Merchants`) · `{entities}` = lowercase plural for tables/packages (`orders`).

**Scope:** this doc covers **entity + migration only**. The full CRUD stack (repository, service, controller, DTOs, tests) is added *only* when a plan explicitly asks for it — then follow the `merchants` domain as the reference implementation.

> If this doc and the code ever disagree, **the code wins** — update this doc.

---

## 0. Execution scope — STAY INSIDE THE PLAN (CRITICAL)

**Read this first. It overrides any temptation to "improve" surrounding code.**

When implementing an approved plan, change **only** what the plan explicitly says to change. The plan is a whitelist, not a starting point.

- **Only touch files the plan names.** If a file is not listed in the plan, do not create, modify, delete, rename, or move it — full stop. If the plan literally cannot compile without touching an unlisted file, **stop and report it**; do not silently expand scope.
- **Do not change any existing public contract** unless the plan says so, verbatim: no changing a method's name, parameters, or return type; no changing a DTO's fields; no adding/removing/renaming repository or service methods; no altering an interface.
- **Make minimal, surgical edits.** Even when a plan shows a full file body, change only the specific lines the plan describes. Do not reformat, reorder, "clean up", re-architect, or regenerate unrelated parts of a file you are editing.
- **Never invent new classes, DTOs, packages, or abstractions** that the plan did not ask for (e.g. do not create a parallel `XxxDetailModel` when `XxxResModel` already exists and is used).
- **Do not weaken, rewrite, or delete existing tests** to make a build pass. Existing tests are behavior guarantees. If your change legitimately requires a test edit, the plan must call it out explicitly and it must *tighten*, not loosen, coverage. A green suite achieved by rewriting the tests to match new code is a **failure**, not a pass.
- **A passing test run is necessary but not sufficient.** If passing required editing files outside the plan's list, the task has failed its scope contract — revert and re-plan rather than shipping the expanded diff.

> Rationale: a full-file automated executor tends to regenerate whole files and their collaborators, which silently regresses unrelated behavior (e.g. dropping soft-delete filtering, changing id types). Scope discipline is what keeps an approved plan safe to ship.

---

## 1. Core rules (always apply)

These hold for **every** change, entity-only or full CRUD slice.

1. **Package structure follows `README.md` (CRITICAL).** The *Project Structure* layout is authoritative — `common/` (shared DTOs like `ApiResponse`), `entity/` (flat, shared JPA entities), `repository/{entities}/`, `domain/{entities}/{controller,service,dto}/`. Never invent new top-level packages or place a class outside this structure.
2. **Program to interfaces across layers.** The chain `controller → service → repository` is wired through interfaces, never concrete impls:
   - Controller injects `I{Entity}Service`.
   - Service injects `I{Entity}Repository`.
   - Only the `{Entity}Repository` impl knows `I{Entity}JpaRepository`.
   - Inject the interface and keep the `i`-prefixed field name, e.g. `private final IMerchantRepository iMerchantRepository;`.
3. **Small functions — no method over 25–30 lines.** If a method is longer or does more than one thing, extract private helpers. Keep controllers thin; push logic into the service; factor mapping/validation into their own methods.
4. **Validate input at the boundary.** Request DTOs use `jakarta.validation` constraints (`@NotBlank`, `@NotNull`, `@Positive`, …) with human-readable messages; controllers use `@Valid @RequestBody`. Clients never send `id`, timestamps, `isDeleted`, or server-generated secrets.
5. **Handle errors explicitly.** Never swallow exceptions or return `null` on failure. Not-found lookups throw `RuntimeException("{Entity} not found: " + id)` (until centralized `@RestControllerAdvice` handling lands). Every controller response is wrapped in `ApiResponse<T>`.
6. **Money is always `BigDecimal` — never `double`/`float`.** Amount columns are `NUMERIC(19, 2)` in SQL and `java.math.BigDecimal` in Java. Currency is an ISO-4217 `VARCHAR(3)` code.
7. **Fix lint before finishing.** No unused imports, correct import grouping (§4), clean formatting. A lint error means the task is not done.
8. **Test after every change.** Confirm green before considering the task complete: `./mvnw compile` for entity/migration-only changes, `./mvnw test` when service logic is touched. New service logic ships with unit tests (JUnit 5 + Mockito + AssertJ).

---

## 2. Entity

- Location: `entity/{Entity}s.java` — flat `entity` package, **not** under `domain/`. Class and table are pluralized: `Merchants` → table `merchants`.
- Annotations: `@Entity`, `@Table(name = "{entities}")`, Lombok `@Getter` + `@Setter` (no `@Builder` — entities are built with a no-arg constructor + setters in the service).
- `@Id` is a `UUID`, assigned by the service (no `@GeneratedValue`).
- Every `@Column` names its column explicitly (`name = "snake_case"`) and mirrors the migration's `nullable` / `unique`.
- **Standard audit + soft-delete columns on every entity:**

```java
@Column(name = "created_at", nullable = false)  private LocalDateTime createdAt;
@Column(name = "created_by", nullable = false)  private String createdBy;
@Column(name = "updated_at")                    private LocalDateTime updatedAt;
@Column(name = "updated_by")                    private String updatedBy;
@Column(name = "is_deleted", nullable = false)  private Boolean isDeleted;
```

Template:

```java
package com.mini_merchant.pay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "{entities}")
@Getter
@Setter
public class {Entity}s {

    @Id
    @Column(name = "id")
    private UUID id;

    // ... domain columns ...

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;
}
```

---

## 3. Flyway migration

- **Schema is only ever changed by a Flyway migration.** Hibernate is `ddl-auto: validate`; any mismatch fails startup.
- Location: `src/main/resources/db/migration/V{n}__{snake_description}.sql`, next sequential `{n}`.
- Column types/constraints must match the entity exactly. Audit columns get DB defaults.
- Migrations are immutable once committed — never edit an applied migration; add a new one.

```sql
CREATE TABLE {entities} (
    id UUID PRIMARY KEY,
    -- domain columns ...
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

## 4. Import ordering

Group imports and **separate every group with a blank line**, in this order:

1. `java.*` / `javax.*`
2. `org.springframework.*` (and other third-party `org.*`)
3. `com.mini_merchant.pay.*` (project)
4. `jakarta.*`
5. `lombok.*`

Static imports (tests) go in their own group at the top. A missing blank line between groups is a lint error.

> The existing `Merchants.java` orders imports `jakarta → lombok → java`, contradicting the above. The code is authoritative — when copying an existing file, match the file.

---

## 5. Checklist (entity + migration)

- [ ] `entity/{Entity}s.java` — domain columns + standard audit/soft-delete columns.
- [ ] `db/migration/V{n}__create_table_{entities}.sql` — matches the entity exactly.
- [ ] Imports grouped with blank lines; lint clean.
- [ ] `./mvnw compile` green.
- [ ] **Scope check (§0):** only the files named in the plan changed; no unlisted file, signature, DTO, interface, or existing test was touched.

> CRUD layers (repository / service / controller / DTOs / tests) are added only when a plan explicitly requests them.
