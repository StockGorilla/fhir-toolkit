# FHIR Toolkit

Productivity tooling for healthcare developers working with FHIR R4 in JetBrains IDEs.

Open any FHIR resource JSON and get inline structural validation against the R4 spec. Generate minimum-valid resource skeletons from the editor. Hover on FHIR datatypes for inline documentation. Navigate Bundle references with a click.

## Features

- **Inline validation.** HAPI FHIR R4 validator surfaces issues as you type, with FHIRPath-resolved error locations.
- **Sample resource generator.** "Insert FHIR Resource…" action in the Tools menu and editor right-click menu — chooser popup inserts minimum-valid skeletons for Patient, Observation, Encounter, Bundle, Practitioner, and Organization.
- **Datatype hovers.** Contextual documentation for CodeableConcept, Coding, Reference, Quantity, and Identifier properties.
- **Bundle navigation.** Gutter icons next to `"reference"` values jump to the matching entry within the same Bundle.
- **Code lookup.** Curated LOINC, SNOMED CT, ICD-10-CM, and UCUM codes with hover-readable labels.

Designed for engineers building EHR integrations, healthtech apps, and payer systems. Open source, no telemetry, no network calls.

## Installation

From inside any compatible JetBrains IDE (IntelliJ IDEA, PyCharm, WebStorm, GoLand, etc.):

1. Open **Settings → Plugins → Marketplace**
2. Search for **"FHIR Toolkit"**
3. Click **Install** and restart the IDE

## Usage

### Validation

Open any `.json` file containing a FHIR R4 resource (recognized by a top-level `resourceType` field). Errors and warnings appear inline. Hover over a squiggle for details.

### Inserting a resource skeleton

In any JSON file, place the cursor where you want the resource and either:

- **Tools → Insert FHIR Resource…**, or
- Right-click in the editor → **Insert FHIR Resource…**

Pick a resource type from the popup. The inserted skeleton is minimum-valid — it parses and validates without errors.

### Bundle navigation

Inside a Bundle resource, click the gutter icon next to any `"reference"` value to jump to the matching entry's resource.

### Hover documentation

Hover (or press F1 on Mac, Ctrl+Q on Windows/Linux) on:

- A FHIR datatype property key (e.g., `coding`, `system`, `reference`) for a contextual description.
- A `code` *value* inside a Coding or Quantity for the human-readable display when the system is LOINC, SNOMED CT, ICD-10-CM, or UCUM.

## Compatibility

- **IDE:** IntelliJ Platform 2025.2.6.2 and later
- **JDK:** Plugin runs on the IDE's bundled JVM (Java 21+)
- **FHIR version:** R4 (4.0.1)

## Roadmap

This is the v0 free release. Planned for v1 (paid):

- US Core profile validation
- AI-assisted CSV → FHIR resource translation
- Bulk validation across a project
- Custom profile authoring

## Issues / Feedback

Report bugs and feature requests via GitHub Issues (link to be added with public repo).

## License

Apache License 2.0. See [LICENSE](LICENSE).
