# Milestone 1A completion report

Date: October 23, 2026 (America/Chicago). Estate Service and coordinated contract/documentation version: 0.2.0.

## Outcome

Estate Service is implemented, independently buildable and container-tested. All requested acceptance checks completed locally: Java 21 build/tests, PostgreSQL startup, fresh and upgrade Flyway migrations, REST APIs, JWT authorization, atomic outbox creation, real RabbitMQ publication, Docker image and reproducible instructions. No other microservice was implemented, no central infrastructure configuration changed, no remote/commit/push/cloud deployment occurred, and Milestone 1B was not started.

All existing non-placeholder foundation behavior was preserved, including the four security policy checks and the V1 migration. The entry point was renamed from Application to EstateServiceApplication because Windows class loading confused Application.class with the application package during Hibernate startup. Dependency versions remain the M0 baseline; only the BOM-managed validation starter was added. Python contract verification uses the existing contracts baseline versions.

## Implemented APIs

All routes use /api/v1, bearer JWTs and JSON. Collections support status, page and size, stable ID ordering and SQL-scoped pagination. Creation returns 201; resource commands support validation and Problem Details. No physical-delete API exists.

| Resource | Operations |
|---|---|
| Estates | POST/GET /estates; GET/PUT /estates/{id}; PATCH /estates/{id}/status |
| Divisions | POST/GET /estates/{estateId}/divisions; GET/PUT /divisions/{id} |
| Blocks | POST /divisions/{divisionId}/blocks; GET /estates/{estateId}/blocks; GET/PUT /blocks/{id}; PATCH /blocks/{id}/status |
| Clones | POST/GET /rubber-clones |

X-Correlation-ID accepts a UUID, generates a replacement when missing/invalid, propagates into errors/events/logging and returns in response headers. /v3/api-docs is generated and authenticated. The pinned OpenAPI contract and runtime document validate and contain the same sixteen operations.

## Database and domain rules

V1 is unchanged. V2 creates estate, division, plantation_block and rubber_clone, and extends outbox_event with delivery status and retry scheduling. The existing processed_event idempotency table is retained. Published V1 outbox rows remain published after upgrade. All tables/queries are Estate Service-owned; there are no cross-service foreign keys, entities, databases or source dependencies.

Estate/clone codes are globally unique; division/block codes are unique within an estate. Codes normalize to uppercase. Domain checks plus composite foreign keys enforce existing parents and matching estate relationships. Active blocks require positive area; inactive blocks permit zero. All areas are nonnegative with at most four decimal places. Counts are nonnegative and tappable never exceeds planted. IDs and creation timestamps stay stable; updates refresh timestamps and use JPA @Version plus request versions. Concurrent writes and database uniqueness constraints are tested. Future crop extensibility is isolated in CropType without adding crop features.

Documented decisions: divisions/blocks cannot change estate, blocks may change division within that estate; parent inactivation does not cascade; historical inactive clones remain referenceable; future planting dates are permitted for planning. Inactive blocks return false for operational eligibility; downstream enforcement is documented without introducing operational transactions. Clone catalog entries have only the requested fields and no update/delete endpoint.

## Event contracts and outbox

EstateCreated, EstateUpdated, PlantationBlockCreated and PlantationBlockUpdated use self-contained v2 schemas and full entity snapshots. Envelopes include eventId, eventType, eventVersion=2, occurredAt, correlationId, sourceService, estateId, entityId, blockId for blocks, dataProvenance and payload with committed revision/audit fields.

Every v1 schema/example remains unchanged. V1 EstateCreated's mandatory companyId is outside the requested model; v1 block update vocabulary cannot express status/tree counts. V2 resolves that incompatibility explicitly. Consumers must opt into agrosphere.<EventType>.v2; no fabricated company identity or implicit dual publishing occurs. Snapshots are copied into service test resources for independent contract validation.

Domain writes and mandatory outbox inserts share one JPA-managed database transaction. The dispatcher locks due rows with SKIP LOCKED and sends persistent mandatory messages to durable agrosphere.domain using publisher confirms. Returns, nacks and exceptions retry with configurable exponential backoff; exhausted events remain FAILED for operator-controlled replay. Publication defaults disabled while preserving outbox records. Confirmation/crash ambiguity permits duplicates with the same eventId. Consumers must deduplicate consumer_name/eventId transactionally, ACK after commit and reject stale revisions. No exactly-once delivery is claimed. The implementation follows the [Spring AMQP confirm/return API](https://docs.spring.io/spring-amqp/reference/amqp/template.html).

## Security

Business routes are authenticated by default. Nimbus validates RS256 signatures, configured issuer, lifetime and estate-service audience. Trusted Keycloak-style realm_access.roles supplies PLATFORM_ADMIN, ESTATE_ADMIN, ESTATE_MANAGER and VIEWER. Signed, administrator-managed estate_ids scopes reads/writes. Client headers or user-editable attributes are not treated as grants. PLATFORM_ADMIN alone creates estates/clones and has global scope; estate admins/managers write assigned estates; viewers read assigned estates. Lists apply scope in SQL before counting/pagination. Only health routes are public. No production credentials are hardcoded. See [Spring Security JWT validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) and the service runbook.

## Tests executed and results

Final complete suite ran inside maven:3.9.12-eclipse-temurin-21 on Java 21.0.10, with PostgreSQL 17.11 and RabbitMQ 4.3.6 disposable containers. The host has Java 27; earlier host Maven runs compiled release21 successfully, but Java 21 runtime evidence comes from the container run.

| Suite/check | Result |
|---|---|
| ApplicationServiceTest | 9 passed |
| DomainRulesTest | 10 passed |
| EstateAuthorizationTest | 8 passed |
| OutboxDispatcherTest | 7 passed |
| Existing SecurityPolicyTest | 4 passed |
| EstateServiceIT | 9 passed |
| Total Java tests | 47 passed, 0 failures/errors/skips |
| Actual serialized outbox envelopes against all four v2 schemas | Passed, exact decimal arithmetic |
| Pinned and generated runtime OpenAPI, all 16 operations | Passed |
| Contracts validator, unchanged v1 plus v2 examples/negative cases | Passed |
| Architecture/workspace audit | Passed: 8 ADRs, 12 independent Git roots, YAML/workflow/ignore/heuristic secret checks |
| Docker image build | Passed: agrosphere-estate-service:0.2.0 |

Integration covers all sixteen REST operations, duplicate estate/division/block/clone codes, request errors, estate filtering, viewer write denial, wrong estate/audience/issuer, expired/forged JWTs, actual JPA optimistic conflicts, parent mismatch/composite FK enforcement, fresh migrations and V1 upgrade, atomic domain/outbox rollback, real confirmed RabbitMQ delivery, same-eventId redelivery, health/readiness/liveness and generated OpenAPI. Unit messaging tests include retry caps, failure retention, nacks and unroutable acknowledged messages.

Image: sha256:ce0b84e0432f0ef190033c35388330bd86eeb202eed09dbe50f26170af2f261f; runtime user 10001:10001. The builder uses its pinned Maven 3.9.12 because the stock wrapper switches ZIP downloads to TAR when unzip is absent, invalidating the ZIP checksum in that minimal image. Wrapper files/checksums remain intact. Disposable test containers were stopped by the test lifecycle; Docker Desktop and a reusable Maven cache remain available locally. Logs are ignored local artifacts: java21-verification.log, milestone-verification.log and docker-build.log.

## Commands to build and run

From agrosphere-estate-service with JDK 21 and Docker:

~~~powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pintegration
python -m pip install -r requirements-test.txt
python scripts/validate_events.py
python scripts/validate_api.py
docker build -t agrosphere-estate-service:0.2.0 .
~~~

To start local infrastructure, follow its private environment setup and run docker compose --profile identity up -d postgres rabbitmq keycloak in agrosphere-infrastructure. Provision the realm/clients/issuer-managed roles and estate scopes as described in README.md. Export DB_URL, DB_USER, DB_PASSWORD, RABBITMQ_HOST, RABBITMQ_USER, RABBITMQ_PASSWORD, OIDC_ISSUER_URI and OIDC_AUDIENCE using .env.example as a reference, then run:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

Set OUTBOX_PUBLICATION_ENABLED=true after durable consumer queues/bindings exist. With false, the service retains events without needing RabbitMQ publication. For container execution use a private --env-file and container-reachable DB/broker/issuer URLs. README.md contains exact configuration and Docker invocation; examples/estate.http contains the five requested synthetic create/list examples.

## Known issues and limits

No acceptance blocker remains in the local build/container-test scope. A real Keycloak realm was not provisioned or exercised: JWT integration used a local OIDC discovery/JWKS server with real RSA signatures and Keycloak-style claims. Live identity mapper configuration and production credentials remain operator setup. No production deployment or dependency vulnerability audit is claimed. Existing v1 consumers require an explicit v2 migration. Retention and automated replay administration remain deferred; failed/published rows are retained. No spatial/operational consumer is included. License remains the foundation's rights-reserved decision.

## Files created or modified

The repository had uncommitted foundation files before this task. The following inventory records edits against the initial filesystem inventory; no initial commits were created.

- Modified: `agrosphere-architecture/CHANGELOG.md`
- Modified: `agrosphere-architecture/README.md`
- Modified: `agrosphere-architecture/VERSION`
- Created: `agrosphere-architecture/docs/adr/0008-estate-management.md`
- Modified: `agrosphere-architecture/docs/architecture/DOMAIN_MODEL.puml`
- Modified: `agrosphere-architecture/docs/architecture/EVENT_FLOWS.puml`
- Modified: `agrosphere-architecture/docs/architecture/INTEGRATION_PATTERNS.md`
- Modified: `agrosphere-architecture/docs/roadmap/MILESTONES.md`
- Created: `agrosphere-architecture/docs/roadmap/MILESTONE_1A.md`
- Modified: `agrosphere-architecture/docs/roadmap/NEXT_CODEX_PROMPT.md`
- Modified: `agrosphere-architecture/scripts/validate.py`
- Modified: `agrosphere-contracts/AGENTS.md`
- Modified: `agrosphere-contracts/CHANGELOG.md`
- Modified: `agrosphere-contracts/README.md`
- Modified: `agrosphere-contracts/VERSION`
- Created: `agrosphere-contracts/events/v2/EstateCreated.schema.json`
- Created: `agrosphere-contracts/events/v2/EstateUpdated.schema.json`
- Created: `agrosphere-contracts/events/v2/PlantationBlockCreated.schema.json`
- Created: `agrosphere-contracts/events/v2/PlantationBlockUpdated.schema.json`
- Created: `agrosphere-contracts/examples/v2/EstateCreated.json`
- Created: `agrosphere-contracts/examples/v2/EstateUpdated.json`
- Created: `agrosphere-contracts/examples/v2/PlantationBlockCreated.json`
- Created: `agrosphere-contracts/examples/v2/PlantationBlockUpdated.json`
- Modified: `agrosphere-contracts/openapi/estate-service.yaml`
- Modified: `agrosphere-contracts/scripts/validate.py`
- Modified: `agrosphere-estate-service/.env.example`
- Modified: `agrosphere-estate-service/.github/workflows/ci.yml`
- Modified: `agrosphere-estate-service/AGENTS.md`
- Modified: `agrosphere-estate-service/CHANGELOG.md`
- Modified: `agrosphere-estate-service/Dockerfile`
- Modified: `agrosphere-estate-service/README.md`
- Modified: `agrosphere-estate-service/VERSION`
- Created: `agrosphere-estate-service/examples/estate.http`
- Modified: `agrosphere-estate-service/pom.xml`
- Created: `agrosphere-estate-service/requirements-test.txt`
- Created: `agrosphere-estate-service/scripts/validate_api.py`
- Created: `agrosphere-estate-service/scripts/validate_events.py`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/EstateServiceApplication.java`
- Modified: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/SecurityConfiguration.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/in/web/ApiErrors.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/in/web/CorrelationFilter.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/in/web/EstateController.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/in/web/OpenApiConfiguration.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/in/web/SecurityProblem.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/messaging/JdbcEventOutbox.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/messaging/MessagingConfiguration.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/messaging/OutboxDispatcher.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/persistence/DivisionEntity.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/persistence/EstateEntity.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/persistence/JpaEstateStore.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/persistence/PlantationBlockEntity.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/persistence/RubberCloneEntity.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/security/JwtEstateAccess.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/adapter/out/security/JwtValidationConfiguration.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/CorrelationContext.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/EstateApplicationService.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/PageResult.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/Requests.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/Responses.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/port/out/EstateAccess.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/port/out/EstateStore.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/application/port/out/EventOutbox.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/CropType.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/Division.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/DomainException.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/Estate.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/PlantationBlock.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/RubberClone.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/Rules.java`
- Created: `agrosphere-estate-service/src/main/java/com/agrosphere/estate/domain/Status.java`
- Modified: `agrosphere-estate-service/src/main/resources/application.yaml`
- Created: `agrosphere-estate-service/src/main/resources/db/migration/V2__estate_management.sql`
- Created: `agrosphere-estate-service/src/test/java/com/agrosphere/estate/ApplicationServiceTest.java`
- Created: `agrosphere-estate-service/src/test/java/com/agrosphere/estate/DomainRulesTest.java`
- Created: `agrosphere-estate-service/src/test/java/com/agrosphere/estate/EstateAuthorizationTest.java`
- Created: `agrosphere-estate-service/src/test/java/com/agrosphere/estate/EstateServiceIT.java`
- Created: `agrosphere-estate-service/src/test/java/com/agrosphere/estate/OutboxDispatcherTest.java`
- Modified: `agrosphere-estate-service/src/test/java/com/agrosphere/estate/SecurityPolicyTest.java`
- Created: `agrosphere-estate-service/src/test/resources/contracts/README.md`
- Created: `agrosphere-estate-service/src/test/resources/contracts/estate-service.openapi.json`
- Created: `agrosphere-estate-service/src/test/resources/contracts/events/v2/EstateCreated.schema.json`
- Created: `agrosphere-estate-service/src/test/resources/contracts/events/v2/EstateUpdated.schema.json`
- Created: `agrosphere-estate-service/src/test/resources/contracts/events/v2/PlantationBlockCreated.schema.json`
- Created: `agrosphere-estate-service/src/test/resources/contracts/events/v2/PlantationBlockUpdated.schema.json`
- Created: agrosphere-estate-service/docs/MILESTONE_1A_REPORT.md (this report)
- Removed/renamed: agrosphere-estate-service/src/main/java/com/agrosphere/estate/Application.java -> EstateServiceApplication.java

## Recommended Milestone 1B prompt

Implement AgroSphere Milestone 1B Spatial Service and estate identity projection only. Read affected AGENTS.md files, architecture ADR-0008, the completed Estate Service report, infrastructure configuration and contracts 0.2.0 first. Preserve independent Git roots and service-owned databases. Consume the Estate Service v2 snapshots with transactional consumer_name/eventId deduplication, revision-aware updates, retry/DLQ/replay and reconciliation. Implement spatial-owned PostGIS geometry and RFC 7946 WGS84 API contracts, using canonical estate/block UUIDs without cross-database access. Reject new operational use of inactive blocks and label synthetic fixtures SIMULATED. Add contract/security/PostGIS integration and duplicate/out-of-order event tests. Validate builds, migrations, permissions and Docker, document actual results and blockers, and stop after Milestone 1B. Do not add operations, telemetry, AI, web/immersive functionality or deploy to Azure.

