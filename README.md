# AgroSphere Estate Service

Milestone 1A, version 0.2.1. Independent Java 25 / Spring Boot 4.0.9 service using the existing ports/adapters layout. Owns estate, division, plantation block and rubber clone metadata. Geometry, operations, workers, telemetry and predictions remain outside this repository.

## Build and verification

Requires JDK 25 and Docker for integration tests. Maven Wrapper downloads Maven 3.9.12. JUnit remains 5.14.4; tests manage Spring contexts explicitly because the foundation's Spring 7/JUnit 5 combination cannot use SpringExtension.

~~~powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pintegration
python -m pip install -r requirements-test.txt
python scripts/validate_events.py
python scripts/validate_api.py
docker build -t agrosphere-estate-service:0.2.1 .
~~~

On Linux/macOS use ./mvnw. Unit tests require no infrastructure. Integration tests start disposable PostgreSQL 17, RabbitMQ and a local RSA-signed OIDC issuer, exercise real HTTP and JWT decoding, export actual outbox envelopes, and stop their containers. They require Docker and image downloads; failures are not silently skipped. Schema snapshots are pinned copies from contracts 0.2.0, not sibling source dependencies. CI uses JDK 25 and Docker. See docs/MILESTONE_1A_REPORT.md for actual local verification and limitations.

## Local infrastructure and identity

The sibling infrastructure repository already provisions a separate agrosphere_estate database owned by agrosphere_estate, PostgreSQL at localhost:5432, RabbitMQ at localhost:5672 and optional Keycloak at localhost:8180. No central Compose change is needed. From that repository, follow its private .env setup and run:

~~~powershell
docker compose --profile identity up -d postgres rabbitmq keycloak
~~~

Create the agrosphere realm and an estate-service audience in local Keycloak. Provision realm roles PLATFORM_ADMIN, ESTATE_ADMIN, ESTATE_MANAGER and VIEWER. Add an audience mapper and an administrator-managed multivalued estate_ids claim containing authorized estate UUIDs. Never map estate_ids or roles from user-editable profile attributes or request headers. Restrict mapper/client administration to trusted identity administrators. PLATFORM_ADMIN has global access and alone creates estates and rubber clones. ESTATE_ADMIN and ESTATE_MANAGER write assigned estates; all three estate roles, including VIEWER, read assigned estates. Estate lists are filtered in SQL before pagination and counts. Clone catalog reads require a recognized role.

All business routes require a signed JWT with the configured issuer, audience and valid lifetime; Nimbus fetches trusted issuer discovery/JWKS and accepts RS256. No authentication bypass or public business route exists in development. Tokens, passwords and entity payloads are not logged. Only Actuator health is public. Local Keycloak uses its development server and is not a production identity deployment.

Copy .env.example as a reference, then export variables in your shell. Spring does not load .env automatically. Supply private DB_PASSWORD and RABBITMQ_PASSWORD values matching infrastructure, plus DB_URL, DB_USER, RABBITMQ_USER and OIDC_ISSUER_URI. OIDC_AUDIENCE defaults to estate-service. Start:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

For example, after configuring local Keycloak, these PowerShell assignments identify the existing service-owned resources (replace passwords privately):

~~~powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/agrosphere_estate"
$env:DB_USER = "agrosphere_estate"
$env:DB_PASSWORD = "REPLACE_LOCALLY"
$env:RABBITMQ_HOST = "localhost"
$env:RABBITMQ_USER = "agrosphere"
$env:RABBITMQ_PASSWORD = "REPLACE_LOCALLY"
$env:OIDC_ISSUER_URI = "http://localhost:8180/realms/agrosphere"
$env:OIDC_AUDIENCE = "estate-service"
$env:OUTBOX_PUBLICATION_ENABLED = "false"
.\mvnw.cmd spring-boot:run
~~~

Service defaults to port 8081. Health: /actuator/health, /actuator/health/liveness, /actuator/health/readiness. Readiness includes database connectivity; RabbitMQ health is enabled when publication is enabled. /v3/api-docs and /swagger-ui/index.html require authentication (supply the bearer header through an authenticated local proxy/browser); the static OpenAPI contract can also be imported into an API client. Security is never disabled to expose documentation.

For Docker use --env-file with a private local file and publish 127.0.0.1:8081:8081. DB_URL, RABBITMQ_HOST and OIDC_ISSUER_URI must be reachable from the container (host.docker.internal on Docker Desktop, or service DNS on a shared network). The token issuer must exactly match OIDC_ISSUER_URI; configure Keycloak's advertised hostname accordingly. The image uses a nonroot Java 25 runtime. Example: docker run --rm --env-file .env -p 127.0.0.1:8081:8081 agrosphere-estate-service:0.2.1. The Docker build uses the builder image's pinned Maven 3.9.12; wrapper builds remain the default outside that image.

## API

All routes begin /api/v1. Estates: POST/GET /estates, GET/PUT /estates/{id}, PATCH /estates/{id}/status. Divisions: POST/GET /estates/{estateId}/divisions, GET/PUT /divisions/{id}. Blocks: POST /divisions/{divisionId}/blocks, GET /estates/{estateId}/blocks, GET/PUT /blocks/{id}, PATCH /blocks/{id}/status. Rubber clones: POST/GET /rubber-clones. No DELETE routes exist.

Collection parameters: page (zero-based, default 0), size (1–100, default 20), optional status=ACTIVE|INACTIVE. Stable id ordering. Responses contain content, page, size and totalElements. POST returns 201; estate/division/block creation also returns a resource Location. PUT is full replacement of mutable fields; send the latest version. Status PATCH accepts {"status":"INACTIVE","version":0}. Stale versions and missing PUT versions return 409; missing PATCH versions fail request validation with 400. Invalid input is 400, absent records 404, scope denial 403, duplicate constraints 409. Errors use application/problem+json and correlationId. Send a UUID X-Correlation-ID; invalid/missing IDs are replaced and returned in the response.

See examples/estate.http for the five requested example requests and full field names. Replace token and resource UUIDs after each create; the example records are explicitly synthetic, so use EVENT_DATA_PROVENANCE=SIMULATED for demos.

## Domain decisions and persistence

Codes are trimmed/uppercased, restricted to A–Z, digits, underscore and hyphen, and limited to 64 characters. Estate codes and clone codes are globally unique; division/block codes are unique within an estate. ISO alpha-2 country codes and Java IANA timezones are checked. Areas use numeric(14,4); active blocks require positive area, inactive blocks permit zero but never negative area. Counts must be nonnegative and tappable <= planted. Planting date is optional; future planting dates are permitted for planning. A referenced clone must exist, including historical inactive clones. Estate/division/block status is independently managed; parent inactivation does not cascade. Downstream integrations must check block status/eligibility before new operations. This milestone introduces no operational endpoint.

Divisions and blocks cannot move between estates. Blocks may move to another division in the same estate. Domain checks and composite database foreign keys enforce consistent parents. IDs/creation timestamps are preserved, update timestamps are refreshed on mutation, and JPA @Version plus request versions reject lost updates. Database constraints handle concurrent duplicate-code races. RubberClone is an immutable catalog entry in this milestone; its requested model has no version/audit fields and no update endpoint.

Flyway V1 is preserved unchanged. V2 creates estate, division, plantation_block and rubber_clone, extends outbox delivery metadata, and preserves already-published V1 history. processed_event remains the consumer idempotency foundation. Hibernate only validates the schema. There are no cross-service database reads, entities, dependencies or foreign keys.

## Events and recovery

EstateCreated, EstateUpdated, PlantationBlockCreated and PlantationBlockUpdated are emitted as v2 full snapshots with stable eventId, eventVersion=2, occurredAt, correlationId, sourceService=estate-service, estateId, entityId, blockId for blocks, dataProvenance and payload. Payload includes the committed entity revision/audit metadata. EVENT_DATA_PROVENANCE defaults REAL; set SIMULATED for synthetic demo inputs. Division/clone changes have no events in this milestone.

V1 contracts remain unchanged. V1 EstateCreated requires companyId and v1 block updates cannot represent status/count changes. V2 explicitly resolves these incompatibilities without fabricating company identity. Existing v1 consumers must opt into v2 with a deliberate projection migration; no automatic v1 dual publishing occurs. See contracts events/v2 and architecture ADR-0008.

Domain mutation, flush and outbox insert use one JPA-managed database transaction; JDBC participates in that same datasource transaction. Insertion requires an existing transaction. Publication defaults disabled (OUTBOX_PUBLICATION_ENABLED=false); outbox records remain durable while RabbitMQ is unavailable. Enable with true when the broker and consumer queues are provisioned. The relay declares durable topic exchange agrosphere.domain. Consumers own durable queues/DLQs and bind agrosphere.<EventType>.v2. Mandatory persistent publishing requires publisher confirms and treats unroutable returns as failures.

The relay locks due batches using FOR UPDATE SKIP LOCKED and marks PUBLISHED only after confirmation. A crash between confirmation and commit can redeliver the same eventId: delivery is at least once. Defaults: 20 records/batch, 1-second polling, 5-second confirm timeout, 10 attempts, exponential backoff from 5 seconds capped at one hour. Configure OUTBOX_BATCH_SIZE, OUTBOX_POLL_MS, OUTBOX_CONFIRM_TIMEOUT_SECONDS, OUTBOX_MAX_ATTEMPTS and OUTBOX_RETRY_SECONDS. Configure transaction/DB idle timeouts above batch-size times confirm-timeout; locks remain held during bounded broker waits. Ordering is not guaranteed across concurrent relays; projections must compare payload.version to reject stale snapshots.

FAILED records are retained indefinitely for inspection. After repairing the cause, an authorized database operator can replay specific records in the service-owned database:

~~~sql
UPDATE outbox_event SET delivery_status='PENDING', attempts=0, next_attempt_at=now()
WHERE event_id = '<reviewed-event-uuid>' AND delivery_status='FAILED';
~~~

Consumers must insert (consumer_name,event_id) into their own processed_event table with ON CONFLICT DO NOTHING in the same transaction as projection changes; duplicates have no effect and ACK follows commit. Outbox records and idempotency markers are not automatically purged: define retention only after consumer/replay requirements are known. OUTBOX publication being disabled does not disable JWT security or persistence. Optional OTLP metrics export defaults off; enable OTEL_METRICS_ENABLED only with a configured collector. Logging includes correlation IDs without event payloads or credentials.

No Azure deployment, remote push or work beyond Milestone 1A is included.

## Milestone 1C integration patch 0.2.1

The central infrastructure apps profile now runs this service with its own database credentials, real Keycloak and RabbitMQ. Optional OIDC_JWK_SET_URI selects an internal JWKS address while preserving exact public issuer, signature, audience and lifetime validation. When absent, existing issuer discovery is unchanged. No API/event contracts, business rules or persistence ownership changed. Existing V1/V2 migrations are preserved; additive V3 stores outbox traceparent/tracestate. Prefer the central compose.yaml --profile apps for shared-platform execution; older standalone instructions describe host development. The infrastructure completion report records end-to-end identity, projection, geometry, tracing and outage recovery.

Milestone 1C additionally enables Rabbit producer/consumer observations and persists W3C transport context with atomic outbox rows, restoring it before confirmed publication. Event JSON is unchanged; old rows with null context remain publishable. The central acceptance suite asserts HTTP-to-relay-to-consumer trace continuity in Jaeger. JSON bodies are bounded to 2 MiB (including streamed bodies); infrastructure injects a distinct service field into structured logs. The full PKCE/two-division/ten-block canonical GeoJSON test and safe no-reset procedures are in infrastructure docs/INTEGRATION_TESTING.md and LOCAL_DEVELOPMENT.md.
