package com.fhirtools.plugin.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FhirCodeSystemsTest {

    @Test
    fun loincLookups() {
        val entry = FhirCodeSystems.lookup("http://loinc.org", "29463-7")
        assertNotNull(entry)
        assertEquals("LOINC", entry!!.systemLabel)
        assertEquals("Body weight", entry.display)
    }

    @Test
    fun snomedLookups() {
        val entry = FhirCodeSystems.lookup("http://snomed.info/sct", "38341003")
        assertNotNull(entry)
        assertEquals("SNOMED CT", entry!!.systemLabel)
        assertEquals("Hypertensive disorder, systemic arterial", entry.display)
    }

    @Test
    fun icd10cmLookups() {
        val entry = FhirCodeSystems.lookup("http://hl7.org/fhir/sid/icd-10-cm", "I10")
        assertNotNull(entry)
        assertEquals("ICD-10-CM", entry!!.systemLabel)
        assertEquals("Essential (primary) hypertension", entry.display)
    }

    @Test
    fun ucumLookups() {
        val entry = FhirCodeSystems.lookup("http://unitsofmeasure.org", "kg")
        assertNotNull(entry)
        assertEquals("UCUM", entry!!.systemLabel)
        assertEquals("kilogram", entry.display)
    }

    @Test
    fun unknownSystemReturnsNull() {
        assertNull(FhirCodeSystems.lookup("http://example.com/unknown", "anything"))
    }

    @Test
    fun unknownCodeWithinKnownSystemReturnsNull() {
        assertNull(FhirCodeSystems.lookup("http://loinc.org", "0000-0"))
    }
}
