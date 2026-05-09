package com.fhirtools.plugin.docs

import com.fasterxml.jackson.databind.ObjectMapper

object FhirCodeSystems {

    data class Entry(val systemLabel: String, val display: String)

    private val mapper by lazy { ObjectMapper() }

    private val systems: Map<String, Pair<String, Map<String, String>>> by lazy {
        mapOf(
            "http://loinc.org" to ("LOINC" to load("loinc.json")),
            "http://snomed.info/sct" to ("SNOMED CT" to load("snomed.json")),
            "http://hl7.org/fhir/sid/icd-10-cm" to ("ICD-10-CM" to load("icd10cm.json")),
            "http://hl7.org/fhir/sid/icd-10" to ("ICD-10" to load("icd10cm.json")),
            "http://unitsofmeasure.org" to ("UCUM" to load("ucum.json")),
        )
    }

    fun lookup(system: String, code: String): Entry? {
        val (label, table) = systems[system] ?: return null
        val display = table[code] ?: return null
        return Entry(label, display)
    }

    private fun load(filename: String): Map<String, String> {
        val path = "/code-systems/$filename"
        val stream = FhirCodeSystems::class.java.getResourceAsStream(path)
            ?: error("Missing code-system resource: $path")
        return stream.bufferedReader().use { reader ->
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(reader, Map::class.java) as Map<String, String>
        }
    }
}
