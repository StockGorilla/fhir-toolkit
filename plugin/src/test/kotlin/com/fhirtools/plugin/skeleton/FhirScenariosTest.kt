package com.fhirtools.plugin.skeleton

import ca.uhn.fhir.validation.ResultSeverityEnum
import com.fhirtools.plugin.fhir.FhirContextHolder
import org.junit.Assert.assertTrue
import org.junit.Test

class FhirScenariosTest {

    @Test
    fun allScenariosLoadFromClasspath() {
        for (scenario in FhirScenarios.all) {
            val text = FhirScenarios.load(scenario)
            assertTrue("scenario ${scenario.displayName} was empty", text.isNotBlank())
            assertTrue(
                "scenario ${scenario.displayName} is not a Bundle",
                text.contains("\"resourceType\": \"Bundle\"")
                    || text.contains("\"resourceType\":\"Bundle\""),
            )
        }
    }

    @Test
    fun allScenariosValidateWithZeroErrors() {
        for (scenario in FhirScenarios.all) {
            val text = FhirScenarios.load(scenario)
            val resource = FhirContextHolder.parser().parseResource(text)
            val result = FhirContextHolder.validator.validateWithResult(resource)
            val errors = result.messages.filter {
                it.severity == ResultSeverityEnum.ERROR || it.severity == ResultSeverityEnum.FATAL
            }
            assertTrue(
                "scenario ${scenario.displayName} should validate without errors; got: ${errors.map { it.message }}",
                errors.isEmpty(),
            )
        }
    }
}
