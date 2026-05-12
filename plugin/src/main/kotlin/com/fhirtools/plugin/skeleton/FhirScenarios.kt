package com.fhirtools.plugin.skeleton

object FhirScenarios {

    data class Scenario(val displayName: String, val resourcePath: String) {
        override fun toString(): String = displayName
    }

    val all: List<Scenario> = listOf(
        Scenario("Adult — Type 2 Diabetes management", "/fhir-scenarios/adult-diabetes.json"),
        Scenario("Pediatric — Asthma ED visit", "/fhir-scenarios/pediatric-asthma.json"),
        Scenario("Adult — Hypertension + Hyperlipidemia panel", "/fhir-scenarios/hypertension-lipid.json"),
        Scenario("Adult — Acute MI (NSTEMI)", "/fhir-scenarios/acute-mi.json"),
    )

    fun load(scenario: Scenario): String {
        val stream = FhirScenarios::class.java.getResourceAsStream(scenario.resourcePath)
            ?: error("Missing FHIR scenario resource: ${scenario.resourcePath}")
        return stream.bufferedReader().use { it.readText() }
    }
}
