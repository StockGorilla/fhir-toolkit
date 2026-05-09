package com.fhirtools.plugin.skeleton

import ca.uhn.fhir.validation.ResultSeverityEnum
import com.fhirtools.plugin.fhir.FhirContextHolder
import org.junit.Assert.assertTrue
import org.junit.Test

class FhirSkeletonsTest {

    @Test
    fun allSkeletonsLoadFromClasspath() {
        for (type in FhirSkeletons.resourceTypes) {
            val text = FhirSkeletons.load(type)
            assertTrue("skeleton for $type was empty", text.isNotBlank())
            assertTrue(
                "skeleton for $type does not declare resourceType=$type",
                text.contains("\"resourceType\": \"$type\""),
            )
        }
    }

    @Test
    fun allSkeletonsValidateWithZeroErrors() {
        for (type in FhirSkeletons.resourceTypes) {
            val text = FhirSkeletons.load(type)
            val resource = FhirContextHolder.parser().parseResource(text)
            val result = FhirContextHolder.validator.validateWithResult(resource)
            val errors = result.messages.filter {
                it.severity == ResultSeverityEnum.ERROR || it.severity == ResultSeverityEnum.FATAL
            }
            assertTrue(
                "skeleton for $type should validate without errors; got: ${errors.map { it.message }}",
                errors.isEmpty(),
            )
        }
    }
}
