# Curated Links

## FHIR specification

- [FHIR R4 home](https://hl7.org/fhir/R4/) — primary spec
- [Patient resource](https://hl7.org/fhir/R4/patient.html) — most-used resource
- [Bundle resource](https://hl7.org/fhir/R4/bundle.html) — wrapper for collections, transactions, search results
- [Observation resource](https://hl7.org/fhir/R4/observation.html) — vitals, labs
- [Datatypes](https://hl7.org/fhir/R4/datatypes.html) — CodeableConcept, Reference, Quantity, Identifier
- [Resource list](https://hl7.org/fhir/R4/resourcelist.html) — every resource, alphabetical
- [JSON format](https://hl7.org/fhir/R4/json.html) — how FHIR maps to JSON
- [Search](https://hl7.org/fhir/R4/search.html) — query parameters

## Implementation guides (for v1 paid features)

- [US Core](https://www.hl7.org/fhir/us/core/) — most important US implementation guide; required for ONC certification
- [Carin Blue Button](https://hl7.org/fhir/us/carin-bb/) — payer-side data
- [Davinci](https://hl7.org/fhir/us/davinci-pdex/) — payer-provider exchange

## Libraries

- [HAPI FHIR (Java)](https://hapifhir.io/) — the gold-standard FHIR library; use for validation, parsing, generation
- [HAPI FHIR GitHub](https://github.com/hapifhir/hapi-fhir)
- [pyfhirsdk / fhir.resources (Python)](https://pypi.org/project/fhir.resources/) — for any Python tooling
- [fhir-kit-client (TypeScript)](https://github.com/Vermonster/fhir-kit-client) — client lib

## Test servers (free public sandboxes)

- [HAPI FHIR test server](http://hapi.fhir.org/baseR4) — most popular, very forgiving
- [SMART Health IT sandbox](https://launch.smarthealthit.org/) — has SMART-on-FHIR launch flow
- [Synthea](https://synthea.mitre.org/) — synthetic patient generator (great for bulk realistic test data)

## JetBrains Plugin development

- [Plugin SDK overview](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Plugin DevKit setup](https://plugins.jetbrains.com/docs/intellij/setting-up-environment.html)
- [intellij-platform-plugin-template (GitHub)](https://github.com/JetBrains/intellij-platform-plugin-template) — recommended starter
- [Annotator API](https://plugins.jetbrains.com/docs/intellij/annotator.html) — for inline validation errors
- [DocumentationProvider](https://plugins.jetbrains.com/docs/intellij/documentation.html) — for hover tooltips
- [IntentionAction](https://plugins.jetbrains.com/docs/intellij/code-intentions.html) — for "Insert sample resource" actions
- [Plugin distribution](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-version.html)

## JetBrains Marketplace

- [Marketplace home](https://plugins.jetbrains.com/) — browse competition here
- [Plugin monetization docs](https://plugins.jetbrains.com/docs/marketplace/plugin-monetization.html)
- [Revenue sharing](https://plugins.jetbrains.com/docs/marketplace/revenue-sharing-and-fees.html)
- [Sales reports](https://plugins.jetbrains.com/docs/marketplace/sales-report.html)

## FHIR communities (where to launch)

- [r/FHIR](https://www.reddit.com/r/FHIR/) — small but engaged
- [r/healthIT](https://www.reddit.com/r/healthIT/) — broader audience
- [HL7 Confluence / chat.fhir.org](https://chat.fhir.org/) — official FHIR community chat (Zulip)
- [FHIR DevDays](https://www.devdays.com/) — the major FHIR conference
- [Hacker News](https://news.ycombinator.com/) — "Show HN" works well for dev tools

## Reference data sources (for v0 code lookup)

- [LOINC](https://loinc.org/) — lab tests, observations. Free download with registration.
- [SNOMED CT](https://www.snomed.org/) — clinical terminology. Licensed (free for some uses in US via NLM).
- [ICD-10-CM](https://www.cdc.gov/nchs/icd/icd-10-cm.htm) — diagnosis codes. Free download.
- [RxNorm](https://www.nlm.nih.gov/research/umls/rxnorm/) — medications. Free.
- [ValueSet Authority](https://vsac.nlm.nih.gov/) — curated value sets

For v0, just embed a curated subset (~200 most-common codes per system) as JSON in plugin resources. Don't ship full datasets — too big and licensing-tricky.

## Domain learning

- [FHIR for Software Engineers (Bert Loedeman, free PDF)](https://fhirforsoftwareengineers.com/)
- [FHIR fundamentals tutorial](https://hl7.org/fhir/R4/fhir-fundamentals.html)
- [FHIR Shorthand (FSH) primer](https://hl7.org/fhir/uv/shorthand/) — for IG authoring; useful background
