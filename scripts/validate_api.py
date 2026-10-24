"""Validate pinned and generated OpenAPI, including operation coverage."""
import json
from pathlib import Path
from openapi_spec_validator import validate
ROOT = Path(__file__).resolve().parents[1]
expected = json.loads((ROOT / "src/test/resources/contracts/estate-service.openapi.json").read_text())
actual = json.loads((ROOT / "target/openapi-runtime.json").read_text())
validate(expected)
validate(actual)
verbs = {"get", "post", "put", "patch", "delete", "head", "options"}
def operations(api):
    return {(path, method) for path, item in api["paths"].items() for method in item if method in verbs}
assert operations(expected) == operations(actual), (operations(expected) - operations(actual), operations(actual) - operations(expected))
assert len(operations(actual)) == 16
print("PASS: pinned and runtime OpenAPI; all 16 operations match")
