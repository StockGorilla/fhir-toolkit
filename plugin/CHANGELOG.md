<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# FHIR Toolkit — Changelog

## [Unreleased]

## [0.2.0] - 2026-05-14

Six new features focused on giving FHIR developers more in-IDE workflows: visual exploration of resources and bundles, interactive FHIRPath evaluation, a local mock server for client testing, multi-resource clinical scenarios, and a 4× expansion of the curated code library.

### Added

- **FHIR Resource tree tool window.** A right-anchored "FHIR Resource" panel renders the active FHIR JSON as a collapsible hierarchy (resource type → properties → array entries → primitives). Click any node to navigate to that JSON element in the editor. Auto-detects the active file via `FhirContextHolder.resourceTypes`.
- **Bundle Graph tool window.** A right-anchored "Bundle Graph" panel (stacks as a tab next to FHIR Resource) shows each Bundle entry as a node with its outgoing references nested as children, marked ✓ (resolved within the Bundle) or ✗ (unresolved). Click an entry → navigate to its resource. Click a reference → navigate to the resolved target. Reuses the same resolution rules as the inline gutter-icon navigation: relative `Type/id`, exact `fullUrl`, absolute URLs whose suffix matches `Type/id`.
- **FHIRPath playground tool window.** A right-anchored "FHIRPath" panel with a text input + Evaluate button. Runs FHIRPath expressions against the currently-open FHIR resource using HAPI's modern `IFhirPath` engine. Results render as raw values for primitives and pretty-printed JSON for resources/elements. Useful for exploring real-world FHIR data shape interactively.
- **Mock FHIR server.** New `Tools → Run Mock FHIR Server` action toggles an in-memory R4 server on an auto-allocated localhost port. Endpoints: `GET /fhir/metadata`, `GET/POST /fhir/{Type}`, `GET/PUT/DELETE /fhir/{Type}/{id}`. Bound to `127.0.0.1` only; nothing accepts external connections. Echo-mode storage (server returns exactly what was POSTed); resource is given a UUID id when one isn't provided. Project-scoped service auto-stops on close.
- **Insert FHIR Scenario.** New `Tools → Insert FHIR Scenario…` action with a chooser popup of 4 hand-crafted multi-resource clinical Bundles: Adult Type 2 Diabetes management, Pediatric asthma ED visit, Hypertension + Hyperlipidemia panel, Acute MI (NSTEMI). Each Bundle uses absolute fullUrls so internal references resolve cleanly under the Bundle Graph and gutter-icon navigation. Locked in by integration tests that assert zero validation errors per scenario.
- **Curated code-system tables expanded ~4×.** LOINC 25 → 88 codes (added CBC, CMP, thyroid, coag, cardiac troponin, BNP/NT-proBNP, inflammatory markers, urinalysis, COVID, mental-health screens). SNOMED CT 20 → 93 codes (more cardiovascular, GI, neuro, MSK, derm, infectious, oncology, pregnancy). ICD-10-CM 21 → 104 codes (matched to SNOMED additions plus more E/F/G/J/K/L/M/N/R series). UCUM 30 → 97 codes (mass/volume/length scaling, dose units, lab/cell counts, pressure, energy, IU/mIU). Total 96 → 382 curated codes available for hover lookup.

### Notes

- Privacy stance unchanged in spirit, slightly clarified: **no outbound network calls, no telemetry**. The optional Mock FHIR Server binds to `127.0.0.1` (loopback) only — nothing on your network or the internet can reach it.
- Plugin zip size ~91 MB (no new external dependencies — all six features use the existing HAPI bundle, JDK built-ins, and IntelliJ Platform APIs).

## [0.1.0] - 2026-05-09

Initial release.

### Added

- **Inline FHIR R4 JSON validation.** Detects FHIR resources by their top-level `resourceType` and validates them with HAPI FHIR's `FhirInstanceValidator`. Errors and warnings appear inline with locations resolved through the FHIRPath in each validation message rather than HAPI's reserialized line/column numbers.
- **Sample resource generator.** "Insert FHIR Resource…" action in the Tools menu and editor right-click menu opens a chooser popup and inserts a minimum-valid skeleton for Patient, Observation, Encounter, Bundle, Practitioner, or Organization. Each skeleton is locked in by an integration test that asserts zero validation errors.
- **Bundle reference navigation.** Gutter icons next to `"reference"` values inside a Bundle jump to the matching entry's resource. Resolves relative `Type/id` references, exact `fullUrl` matches, and absolute URLs whose suffix matches a contained resource's `Type/id`.
- **Datatype hover documentation.** Hovering on property keys inside a recognized FHIR datatype (CodeableConcept, Coding, Reference, Quantity, Identifier) shows a contextual description of the property's role.
- **Code-system hover lookup.** Hovering on the `code` *value* inside a Coding or Quantity returns the human-readable display when the system is one of LOINC, SNOMED CT, ICD-10-CM, or UCUM. Ships with a curated starter set focused on common vital signs, chronic conditions, and units.
- **Noise filtering.** Suppresses HAPI's `dom-6` "should have narrative" best-practice nag and "CodeSystem unknown / cannot be expanded" warnings on terminologies the offline validator doesn't ship (LOINC, SNOMED, etc.) so inline annotations focus on real, actionable issues.

### Notes

- Runs entirely in the IDE. No telemetry. No network calls. Validates against the bundled R4 StructureDefinitions only.
- Plugin size is ~96 MB driven by HAPI FHIR's R4 + cross-version core JARs (locked in by HAPI's internal `VersionCanonicalizer`). A `shadowJar` minimization pass is on the roadmap.
