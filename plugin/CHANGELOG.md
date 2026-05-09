<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# FHIR Toolkit — Changelog

## [Unreleased]

## [0.1.0]

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
