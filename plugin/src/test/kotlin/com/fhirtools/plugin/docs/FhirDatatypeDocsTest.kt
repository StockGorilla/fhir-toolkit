package com.fhirtools.plugin.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FhirDatatypeDocsTest {

    @Test
    fun coversAllFiveRegisteredDatatypes() {
        assertEquals(
            setOf("CodeableConcept", "Coding", "Reference", "Quantity", "Identifier"),
            FhirDatatypeDocs.datatypes,
        )
    }

    @Test
    fun knownLookupsResolveWithNonEmptyDocs() {
        val knownPairs = listOf(
            "CodeableConcept" to "coding",
            "CodeableConcept" to "text",
            "Coding" to "system",
            "Coding" to "code",
            "Coding" to "display",
            "Reference" to "reference",
            "Reference" to "display",
            "Quantity" to "value",
            "Quantity" to "unit",
            "Quantity" to "system",
            "Identifier" to "use",
            "Identifier" to "value",
            "Identifier" to "system",
            "Identifier" to "assigner",
        )
        for ((dt, prop) in knownPairs) {
            val doc = FhirDatatypeDocs.lookup(dt, prop)
            assertNotNull("$dt.$prop should resolve", doc)
            assertNotNull("$dt.$prop should not be blank", doc?.takeIf { it.isNotBlank() })
        }
    }

    @Test
    fun unknownLookupsReturnNull() {
        assertNull(FhirDatatypeDocs.lookup("Patient", "name"))
        assertNull(FhirDatatypeDocs.lookup("Coding", "bogus"))
        assertNull(FhirDatatypeDocs.lookup("", ""))
    }
}
