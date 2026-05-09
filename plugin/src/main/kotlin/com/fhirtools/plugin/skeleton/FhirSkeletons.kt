package com.fhirtools.plugin.skeleton

object FhirSkeletons {
    val resourceTypes: List<String> = listOf(
        "Patient",
        "Observation",
        "Encounter",
        "Bundle",
        "Practitioner",
        "Organization",
    )

    fun load(resourceType: String): String {
        val path = "/fhir-skeletons/${resourceType.lowercase()}.json"
        val stream = FhirSkeletons::class.java.getResourceAsStream(path)
            ?: error("Missing FHIR skeleton resource: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
