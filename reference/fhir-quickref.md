# FHIR R4 Quick Reference

A compressed cheat sheet so you can navigate FHIR without re-reading the spec every time.

## What FHIR is

- **FHIR (Fast Healthcare Interoperability Resources)** — HL7's modern healthcare data standard
- Replaces older formats (HL7 v2 messages, CDA documents) for new development
- Versions: DSTU2 (legacy), STU3 (legacy), R4 (current US standard, ONC Cures Act mandate), R5 (next gen, slow adoption)
- **Use R4** — that's where the market is in 2026

## Core concept: Resources

A **Resource** is a discrete unit of healthcare data — Patient, Observation, Encounter, etc. Each has a defined schema and can be stored, retrieved, updated, deleted via REST API.

Every resource has:
- `resourceType` (the type name, e.g., "Patient")
- `id` (server-assigned unique identifier)
- `meta` (versioning, profiles, security tags)

Example skeleton:
```json
{
  "resourceType": "Patient",
  "id": "example",
  "meta": {
    "profile": ["http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient"]
  },
  ...
}
```

## The 80/20 resources you'll see most

| Resource | What it represents | Typical fields |
|---|---|---|
| Patient | A person receiving care | name, gender, birthDate, address |
| Practitioner | A clinician/provider | name, qualification, telecom |
| Organization | A hospital/clinic/payer | name, type, address |
| Encounter | A clinical visit/episode | status, class, subject, period |
| Observation | A measurement/finding | code, value, subject, effectiveDateTime |
| Condition | A diagnosis | code, subject, clinicalStatus |
| MedicationRequest | A prescription | medicationCodeableConcept, subject, dosageInstruction |
| DiagnosticReport | A lab/imaging report | code, subject, result, conclusion |
| Composition | A clinical document | type, subject, section |
| Bundle | A collection (search results, transactions, documents) | type, entry[] |
| OperationOutcome | An error/warning response | issue[] |

## Key datatypes (where validation gets tricky)

### CodeableConcept

A coded value with optional human text. Almost every clinical field uses this.

```json
{
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "85354-9",
      "display": "Blood pressure panel"
    }],
    "text": "Blood pressure"
  }
}
```

The `system` is a URI identifying the code system (LOINC, SNOMED, ICD-10, etc.). The `code` is the actual code. `display` is human-readable. `text` is free-text fallback.

### Reference

A pointer to another resource. **This is the #1 source of bugs.**

```json
{
  "subject": {
    "reference": "Patient/123",
    "display": "John Doe"
  }
}
```

References can be:
- **Relative** — `"Patient/123"` (resolved against base URL)
- **Absolute** — `"http://example.org/fhir/Patient/123"`
- **Internal (within Bundle)** — `"urn:uuid:abc-123"` matching a Bundle entry's `fullUrl`

A common bug: `Reference` doesn't validate that the target resource type matches the field's allowed types. Your plugin can flag this.

### Quantity

A measurement with units.

```json
{
  "valueQuantity": {
    "value": 120,
    "unit": "mmHg",
    "system": "http://unitsofmeasure.org",
    "code": "mm[Hg]"
  }
}
```

`system` should be UCUM (`http://unitsofmeasure.org`). `code` is the UCUM code (machine-readable); `unit` is human-readable.

### Identifier

A business identifier (MRN, NPI, SSN, etc.).

```json
{
  "identifier": [{
    "system": "http://hospital.example.org/mrn",
    "value": "12345"
  }]
}
```

### Period

A time range.

```json
{
  "period": {
    "start": "2024-01-01T08:00:00Z",
    "end": "2024-01-01T09:30:00Z"
  }
}
```

## Bundles — the wrapper format

Bundles wrap collections of resources. Three common types:

### `searchset` — search results

```json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 42,
  "entry": [
    { "resource": { "resourceType": "Patient", ... } },
    ...
  ]
}
```

### `transaction` — atomic batch operation

```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "fullUrl": "urn:uuid:abc",
      "resource": { "resourceType": "Patient", ... },
      "request": { "method": "POST", "url": "Patient" }
    },
    ...
  ]
}
```

References between entries use `urn:uuid:` URIs that match `fullUrl` values.

### `message` — async messaging

```json
{
  "resourceType": "Bundle",
  "type": "message",
  "entry": [
    { "resource": { "resourceType": "MessageHeader", ... } },
    ...
  ]
}
```

First entry is always `MessageHeader`.

## Common code systems (for v0 lookup)

| System | URI | Use for |
|---|---|---|
| LOINC | `http://loinc.org` | Lab tests, observations, vital signs |
| SNOMED CT | `http://snomed.info/sct` | Clinical findings, procedures, conditions |
| ICD-10-CM | `http://hl7.org/fhir/sid/icd-10-cm` | Diagnoses (US) |
| RxNorm | `http://www.nlm.nih.gov/research/umls/rxnorm` | Medications |
| CPT | `http://www.ama-assn.org/go/cpt` | Procedures (US) |
| UCUM | `http://unitsofmeasure.org` | Units of measure |
| HL7 v3 ActCode | `http://terminology.hl7.org/CodeSystem/v3-ActCode` | Encounter classes, etc. |

## Profiles (US Core, etc.)

Base FHIR is permissive. Profiles tighten requirements for specific use cases.

**US Core** is the most important profile in the US — required by ONC Cures Act for certified EHR data export. Every healthtech app supporting US Core needs to validate against it.

A profile says things like: "for a US Core Patient, `name`, `gender`, and `birthDate` MUST be present" (base FHIR makes them optional).

Validating against a profile is a separate step from validating against base FHIR — and is one of the most-requested paid features.

## Useful gotchas to surface in your plugin

1. **Reference target type mismatch** — `Encounter.subject` should reference Patient or Group, not Practitioner. Base FHIR validation doesn't catch this consistently.
2. **Missing required `system` in CodeableConcept.coding** — common mistake; just `code` without `system` is invalid.
3. **UCUM vs human-readable units** — `valueQuantity.unit: "mmHg"` works for display but `code` should be the UCUM `"mm[Hg]"` for interop.
4. **Bundle internal references not resolving** — `urn:uuid:abc` in a `reference` field with no matching `fullUrl` in the same Bundle.
5. **Profile compliance failures** — a Patient passes base FHIR but fails US Core because `birthDate` is missing.
6. **Date format mistakes** — FHIR allows `"2024"`, `"2024-01"`, `"2024-01-15"`, or full ISO 8601. Mixing formats per resource is suspicious.

## Plugin feature mapping

| v0 feature | FHIR concept | HAPI FHIR API |
|---|---|---|
| Inline JSON validation | base FHIR schema | `FhirContext.forR4().newJsonParser().parseResource()` + `.validateWithResult()` |
| Sample generator | resource templates | hand-rolled JSON skeletons (don't generate from schema; minimum-valid is hand-curated) |
| Datatype hover | CodeableConcept, Reference, Quantity, Identifier | static descriptions in plugin resources |
| Bundle navigation | Bundle.entry[].fullUrl + Reference.reference | walk JSON tree, match by fullUrl |
| Code lookup | LOINC/SNOMED/ICD-10 | curated subset bundled as JSON in plugin resources |

## Testing approach

Use the files in `samples/` as your fixtures. Specifically:
- `samples/patient/patient-example.json` — minimal valid Patient (good baseline)
- `samples/patient/patient-glossy-example.json` — complex Patient (good stress test)
- `samples/bundle/bundle-transaction.json` — Bundle with internal references
- `samples/observation/observation-example-bloodpressure.json` — complex Observation with components
- `samples/operationoutcome/operationoutcome-example.json` — error response shape

Add deliberately broken versions (delete required fields, mismatch reference types) to your test corpus as you build validators.
