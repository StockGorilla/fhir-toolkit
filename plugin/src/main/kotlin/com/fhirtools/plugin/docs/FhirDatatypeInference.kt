package com.fhirtools.plugin.docs

import com.intellij.json.psi.JsonObject

object FhirDatatypeInference {

    // Infer which FHIR datatype a JsonObject represents by looking at the set of
    // property names it carries. Heuristic — the same property names can appear in
    // unrelated contexts, but within a FHIR resource these patterns are reliable for
    // the five datatypes we currently document. Order matters: more specific shapes
    // first.
    fun infer(obj: JsonObject): String? {
        val names = obj.propertyList.mapTo(HashSet()) { it.name }
        return when {
            "coding" in names -> "CodeableConcept"
            "reference" in names -> "Reference"
            "value" in names && "unit" in names -> "Quantity"
            "value" in names && "code" in names && "system" in names -> "Quantity"
            "use" in names && "value" in names -> "Identifier"
            "system" in names && "code" in names && "value" !in names -> "Coding"
            "system" in names && "value" in names -> "Identifier"
            else -> null
        }
    }
}
