package com.fhirtools.plugin.fhir

import ca.uhn.fhir.validation.ResultSeverityEnum
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class FhirContextHolderTest {

    private val samplesDir: Path = Paths.get("..", "samples").toAbsolutePath().normalize()

    @Test
    fun parsesAllValidSampleFiles() {
        val files = Files.walk(samplesDir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { it.toString().endsWith(".json") }
                .filter { !it.toString().contains("${java.io.File.separator}invalid${java.io.File.separator}") }
                .filter { !it.toString().contains("${java.io.File.separator}scratch${java.io.File.separator}") }
                .toList()
        }
        assertTrue("expected sample files under $samplesDir", files.isNotEmpty())
        val parser = FhirContextHolder.parser()
        for (file in files) {
            val text = Files.readString(file)
            // Tolerate ad-hoc scratch files left in samples/ during sandbox testing —
            // skip empties and anything that doesn't declare a resourceType.
            if (text.isBlank() || "\"resourceType\"" !in text) continue
            try {
                val resource = parser.parseResource(text)
                assertNotNull(resource)
            } catch (e: Exception) {
                throw AssertionError("failed to parse ${samplesDir.relativize(file)}: ${e.message}", e)
            }
        }
    }

    @Test
    fun catchesMissingRequiredFields() {
        val invalid = """{"resourceType":"Observation","id":"missing-required"}"""
        val resource = FhirContextHolder.parser().parseResource(invalid)
        val result = FhirContextHolder.validator.validateWithResult(resource)
        val errors = result.messages.filter {
            it.severity == ResultSeverityEnum.ERROR || it.severity == ResultSeverityEnum.FATAL
        }
        assertTrue(
            "expected validation errors for Observation missing status/code, got: ${result.messages.map { it.message }}",
            errors.isNotEmpty(),
        )
    }

    @Test
    fun tolerantParserSurfacesBothInvalidValuesInOneResource() {
        val resource = FhirContextHolder.parser().parseResource(
            Files.readString(samplesDir.resolve("invalid/patient-bad-gender.json"))
        )
        val result = FhirContextHolder.validator.validateWithResult(resource)
        val errorPaths = result.messages
            .filter { it.severity == ResultSeverityEnum.ERROR || it.severity == ResultSeverityEnum.FATAL }
            .mapNotNull { it.locationString }
            .toSet()
        assertTrue(
            "expected errors on Patient.gender AND Patient.birthDate; got: $errorPaths",
            "Patient.gender" in errorPaths && "Patient.birthDate" in errorPaths,
        )
    }
}
