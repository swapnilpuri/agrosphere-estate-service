# Repository instructions

Work only within this independent Git repository. No parent repository, submodules, shared persistence models, cross-service SQL or multi-service Maven parent.

Current scope: Milestone 1A Estate plus requested Milestone 1C integration patches only; preserve all existing business rules, contracts and persistence ownership.

Ownership: Companies, estates, divisions, plantation blocks, crop/clone and planting metadata; canonical estateId and blockId. No geometry.

Use ports/adapters and service-owned migrations. APIs and events are designed in agrosphere-contracts; coordinate breaking changes through a versioned contract release, never a source path dependency. UUID estateId/blockId remain stable across clients. Mark data REAL, SIMULATED, ESTIMATED or PREDICTED. GeoJSON is WGS84 longitude/latitude (RFC 7946). Procedural trees are illustrative.

Never commit credentials, local environment files, generated datasets or large geospatial assets. Use environment examples containing placeholders only. No remote creation, push or cloud deployment is authorized.

Run ./mvnw verify, ./mvnw verify -Pintegration (Docker required), python scripts/validate_events.py and python scripts/validate_api.py after integration, and docker build as documented in README.md. Retain JUnit 5: manage Spring contexts explicitly; do not introduce SpringExtension. Run repository checks documented in README.md. Report actual checks separately from unavailable checks. Version independently using SemVer and CHANGELOG.md; changes to 0.x contracts can still break consumers and require review.
