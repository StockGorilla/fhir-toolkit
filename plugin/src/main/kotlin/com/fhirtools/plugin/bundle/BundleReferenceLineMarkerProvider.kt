package com.fhirtools.plugin.bundle

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement

class BundleReferenceLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName(): String = "FHIR Bundle reference"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Convention: only emit for the smallest leaf element to avoid duplicate markers.
        // JsonStringLiteral nodes can be either composite (with quote-token children) or
        // a single leaf token, so accept both shapes.
        val literal = when {
            element is JsonStringLiteral -> if (element.firstChild == null) element else return null
            element.parent is JsonStringLiteral && element === element.parent.firstChild ->
                element.parent as JsonStringLiteral
            else -> return null
        }

        val property = literal.parent as? JsonProperty ?: return null
        if (property.name != "reference") return null
        if (property.value !== literal) return null

        val refValue = literal.value
        if (refValue.isBlank() || refValue.startsWith("#")) return null // contained refs out of scope for v0

        val bundleRoot = findBundleRoot(literal) ?: return null
        val target = resolveReference(bundleRoot, refValue) ?: return null

        return LineMarkerInfo(
            element,
            literal.textRange,
            AllIcons.Gutter.ImplementedMethod,
            { "Go to Bundle entry: $refValue" },
            { _, _ -> (target as? NavigatablePsiElement)?.navigate(true) },
            GutterIconRenderer.Alignment.LEFT,
            { "Go to Bundle entry $refValue" },
        )
    }

    private fun findBundleRoot(element: PsiElement): JsonObject? {
        val file = element.containingFile as? JsonFile ?: return null
        val root = file.topLevelValue as? JsonObject ?: return null
        val rt = (root.findProperty("resourceType")?.value as? JsonStringLiteral)?.value
        return if (rt == "Bundle") root else null
    }

    private fun resolveReference(bundleRoot: JsonObject, ref: String): PsiElement? {
        val entryArray = bundleRoot.findProperty("entry")?.value as? JsonArray ?: return null
        for (entry in entryArray.valueList) {
            val entryObj = entry as? JsonObject ?: continue

            // 1. fullUrl exact match (covers absolute URLs and urn:uuid:...)
            val fullUrl = (entryObj.findProperty("fullUrl")?.value as? JsonStringLiteral)?.value
            if (fullUrl != null && fullUrl == ref) return targetOf(entryObj)

            val resource = entryObj.findProperty("resource")?.value as? JsonObject ?: continue
            val resourceType = (resource.findProperty("resourceType")?.value as? JsonStringLiteral)?.value ?: continue
            val resourceId = (resource.findProperty("id")?.value as? JsonStringLiteral)?.value

            // 2. Relative reference Type/id matches entry's resource Type+id
            if (resourceId != null && ref == "$resourceType/$resourceId") return targetOf(entryObj)

            // 3. Absolute reference whose tail matches entry's Type/id (some servers emit absolute
            //    URLs that don't appear as fullUrl but do match the contained resource by suffix)
            if (resourceId != null && ref.endsWith("/$resourceType/$resourceId")) return targetOf(entryObj)
        }
        return null
    }

    // Prefer landing on the entry's resource value (where the data starts) if present;
    // otherwise the entry object itself.
    private fun targetOf(entry: JsonObject): PsiElement {
        val resourceValue = entry.findProperty("resource")?.value
        return resourceValue ?: entry
    }
}
