package com.fhirtools.plugin.docs

object FhirDatatypeDocs {

    private val docs: Map<String, Map<String, String>> = mapOf(
        "CodeableConcept" to mapOf(
            "coding" to "Array of <b>Coding</b> entries — code/system pairs identifying the concept. Multiple codings can represent the same concept across different code systems (e.g., LOINC + SNOMED).",
            "text" to "Plain-text representation of the concept. Used when no coding fits, or alongside codings as a human-readable fallback.",
        ),
        "Coding" to mapOf(
            "system" to "URI identifying the code system this code belongs to (e.g., <code>http://loinc.org</code>, <code>http://snomed.info/sct</code>).",
            "code" to "The literal coded value within the system above (e.g., <code>29463-7</code> for body weight in LOINC).",
            "display" to "Human-readable label for the code, useful for UI display.",
            "version" to "Version of the code system this code is drawn from, when relevant.",
            "userSelected" to "<code>true</code> if the user explicitly chose this coding (vs. it being derived/inferred).",
        ),
        "Reference" to mapOf(
            "reference" to "Literal reference to another resource. Common shapes: <code>Patient/123</code> (relative), <code>http://server/Patient/123</code> (absolute), or <code>urn:uuid:…</code> for transient transactions.",
            "type" to "Resource type the reference points to (e.g., <code>Patient</code>), useful when only a logical identifier is provided.",
            "identifier" to "Logical reference using a business identifier when no literal URL is available.",
            "display" to "Human-readable description of the referenced resource.",
        ),
        "Quantity" to mapOf(
            "value" to "Numeric magnitude of the quantity (e.g., <code>72.5</code>).",
            "comparator" to "Comparator (<code>&lt;</code>, <code>&lt;=</code>, <code>&gt;=</code>, <code>&gt;</code>) for inexact measurements such as \"&lt; 5 mg/L\".",
            "unit" to "Human-readable unit text (e.g., <code>kg</code>, <code>mg/dL</code>).",
            "system" to "URI of the code system for the unit — typically UCUM (<code>http://unitsofmeasure.org</code>).",
            "code" to "Coded form of the unit (e.g., <code>kg</code> for kilogram in UCUM).",
        ),
        "Identifier" to mapOf(
            "use" to "Purpose of the identifier: <code>usual</code>, <code>official</code>, <code>temp</code>, <code>secondary</code>, or <code>old</code>.",
            "type" to "Coded description of the identifier kind (e.g., MRN, NIIP, SSN).",
            "system" to "URI namespace for the identifier — the assigning authority.",
            "value" to "The identifier value itself (e.g., the MRN or SSN number).",
            "period" to "Time period during which the identifier is/was valid.",
            "assigner" to "<b>Reference</b> to the organization that issued the identifier.",
        ),
    )

    val datatypes: Set<String> get() = docs.keys

    fun lookup(datatype: String, property: String): String? = docs[datatype]?.get(property)
}
