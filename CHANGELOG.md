# Changelog

## 0.2.2 — 2026-10-24

Allow operational workers/supervisors/auditors to read authorized estate/block metadata for authenticated tapping validation; preserve master-data write rules.

## 0.2.1 — 2026-10-24

Additive V3 durable outbox W3C transport metadata, observed Rabbit publication, message correlation IDs and bounded JSON ingress. Existing API/event/domain semantics and V1/V2 migrations preserved.

Optional container-reachable JWKS URL while preserving exact public issuer/audience/signature/lifetime checks; independent image version and local integration documentation. No business, migration or contract changes.

## 0.2.0 — Milestone 1A

Estate/division/block/clone domain and REST API, V2 business migration, scoped JWT authorization, transactional outbox relay with confirmed RabbitMQ publishing and bounded retry, OpenAPI and v2 event snapshots, JUnit5/Testcontainers coverage, Docker and run instructions. Renamed Application to EstateServiceApplication to avoid the application package collision on Windows. Existing V1 migration and v1 contracts are preserved.



## 0.1.0 — 2026-10-23

Initial Milestone 0 foundation. No production business capability is delivered.
