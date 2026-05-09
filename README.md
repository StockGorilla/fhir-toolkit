# FHIR Toolkit — Workspace

JetBrains IDE plugin for FHIR R4 healthcare data tooling. Open source under Apache 2.0.

The actual plugin source lives in [`plugin/`](plugin/). See [`plugin/README.md`](plugin/README.md) for installation, features, and usage.

## Folder layout

```
fhir-intellij-plugin/
├── plugin/              ← IntelliJ plugin source (the shipped artifact)
├── samples/             ← real FHIR R4 sample resources for testing
│   ├── patient/
│   ├── observation/
│   ├── encounter/
│   ├── bundle/
│   ├── practitioner/
│   ├── organization/
│   ├── condition/
│   ├── medicationrequest/
│   ├── composition/
│   ├── diagnosticreport/
│   ├── operationoutcome/
│   ├── invalid/         ← deliberately-broken fixtures for validator stress tests
│   └── scratch/         ← scratch zone for sandbox experiments
└── reference/
    ├── fhir-quickref.md ← FHIR R4 cheat sheet
    └── links.md         ← curated specs, libraries, and community links
```

## Sample data sources

- **Canonical examples** from [hl7.org/fhir/R4](https://hl7.org/fhir/R4) — official, stable, named
- **HAPI test server bundles** from [hapi.fhir.org/baseR4](http://hapi.fhir.org/baseR4) — real-world, varied, good for stress testing

Both are public and free to use for testing.
