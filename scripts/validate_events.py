"""Validate actual outbox envelopes against the pinned contracts 0.2.0 snapshot."""
import json
from decimal import Decimal
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker
ROOT = Path(__file__).resolve().parents[1]
files = sorted((ROOT / "target/event-contract-samples").glob("*.json"))
assert files, "Run ./mvnw verify -Pintegration first; actual outbox samples are required"
seen = set()
for file in files:
    event = json.loads(file.read_text(), parse_float=Decimal)
    schema = json.loads((ROOT / "src/test/resources/contracts/events/v2" / (event["eventType"] + ".schema.json")).read_text(), parse_float=Decimal)
    Draft202012Validator.check_schema(schema)
    Draft202012Validator(schema, format_checker=FormatChecker()).validate(event)
    assert event["entityId"] == event["payload"]["id"]
    assert event["estateId"] == (event["payload"].get("estateId") or event["payload"]["id"])
    seen.add(event["eventType"])
assert seen == {"EstateCreated", "EstateUpdated", "PlantationBlockCreated", "PlantationBlockUpdated"}, seen
print(f"PASS: {len(files)} actual outbox envelopes; all four v2 event contracts")
