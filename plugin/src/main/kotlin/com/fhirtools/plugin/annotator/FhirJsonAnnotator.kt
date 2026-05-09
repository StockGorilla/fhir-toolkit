package com.fhirtools.plugin.annotator

import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.validation.ResultSeverityEnum
import ca.uhn.fhir.validation.SingleValidationMessage
import com.fhirtools.plugin.fhir.FhirContextHolder
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class FhirJsonAnnotator : ExternalAnnotator<FhirJsonAnnotator.Input, FhirJsonAnnotator.Output>() {

    data class Input(val text: String)

    data class Output(
        val parseError: String?,
        val messages: List<SingleValidationMessage>,
    )

    override fun collectInformation(file: PsiFile): Input? {
        if (file !is JsonFile) return null
        val root = file.topLevelValue as? JsonObject ?: return null
        val typeProp = root.findProperty("resourceType") ?: return null
        val typeLiteral = typeProp.value as? JsonStringLiteral ?: return null
        if (typeLiteral.value !in FhirContextHolder.resourceTypes) return null
        return Input(file.text)
    }

    override fun doAnnotate(input: Input): Output = FhirContextHolder.withPluginClassLoader {
        try {
            val resource = FhirContextHolder.parser().parseResource(input.text)
            val result = FhirContextHolder.validator.validateWithResult(resource)
            Output(null, result.messages)
        } catch (e: DataFormatException) {
            Output(e.message ?: "Failed to parse FHIR resource", emptyList())
        }
    }

    override fun apply(file: PsiFile, output: Output, holder: AnnotationHolder) {
        if (file !is JsonFile) return
        val root = file.topLevelValue as? JsonObject ?: return
        val resourceTypeRange = root.findProperty("resourceType")?.textRange ?: return

        if (output.parseError != null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "FHIR parse error: ${output.parseError}")
                .range(resourceTypeRange)
                .create()
            return
        }

        for (msg in output.messages) {
            if (isOfflineTerminologyNoise(msg)) continue
            val severity = when (msg.severity) {
                ResultSeverityEnum.FATAL, ResultSeverityEnum.ERROR -> HighlightSeverity.ERROR
                ResultSeverityEnum.WARNING -> HighlightSeverity.WARNING
                else -> HighlightSeverity.WEAK_WARNING
            }
            val range = resolveFhirPath(root, msg.locationString.orEmpty()) ?: resourceTypeRange
            val location = msg.locationString?.takeIf { it.isNotBlank() }
            val text = if (location != null) "FHIR: ${msg.message} [$location]" else "FHIR: ${msg.message}"
            holder.newAnnotation(severity, text).range(range).create()
        }
    }

    // HAPI's offline validator chain doesn't ship LOINC, SNOMED, RxNorm, ICD-10, etc.
    // When a resource references one of those, HAPI emits a warning that's not
    // actionable for the user — there's no resource bug, just a capability gap on
    // our end. Suppress these so the annotator only surfaces real validation issues.
    // Errors (e.g., bad code in a *known* code system like AdministrativeGender)
    // still come through unchanged.
    private fun isOfflineTerminologyNoise(msg: SingleValidationMessage): Boolean {
        if (msg.severity != ResultSeverityEnum.WARNING && msg.severity != ResultSeverityEnum.INFORMATION) {
            return false
        }
        val text = msg.message ?: return false
        return text.startsWith("CodeSystem is unknown") ||
            text.startsWith("Unknown code system") ||
            (text.startsWith("Could not confirm") && "not known to the validator" in text) ||
            text.startsWith("Can't expand the value set") ||
            (text.startsWith("ValueSet ") && "not found" in text) ||
            (text.startsWith("The code cannot be checked because codeSystem") && "is not supported" in text)
    }

    private fun resolveFhirPath(root: JsonObject, path: String): TextRange? {
        if (path.isBlank()) return null
        val segments = path.split(".")
        // First segment is the resource type — root IS that resource. A path with
        // only the resource name (e.g. "Patient", "Observation") means HAPI couldn't
        // pin the error to a specific field — fall through to the caller's fallback.
        if (segments.size <= 1) return null

        var currentValue: JsonValue = root
        var lastResolved: TextRange? = null
        for (i in 1 until segments.size) {
            val raw = segments[i]
            // FHIRPath function calls like ofType(Quantity), where(...) — too rare
            // and complex to walk in v0. Stop at the deepest plain-segment match.
            if ('(' in raw || ')' in raw) return lastResolved
            val (name, index) = parseSegment(raw) ?: return lastResolved
            val obj = currentValue as? JsonObject ?: return lastResolved
            val prop = obj.findProperty(name) ?: return lastResolved
            val value = prop.value ?: return lastResolved
            if (index != null) {
                val array = value as? JsonArray ?: return lastResolved
                val element = array.valueList.getOrNull(index) ?: return lastResolved
                currentValue = element
                lastResolved = element.textRange
            } else {
                currentValue = value
                lastResolved = prop.textRange
            }
        }
        return lastResolved
    }

    private fun parseSegment(seg: String): Pair<String, Int?>? {
        val open = seg.indexOf('[')
        if (open < 0) return seg to null
        val close = seg.indexOf(']', open)
        if (close < 0) return null
        val name = seg.substring(0, open)
        val index = seg.substring(open + 1, close).toIntOrNull() ?: return null
        return name to index
    }
}
