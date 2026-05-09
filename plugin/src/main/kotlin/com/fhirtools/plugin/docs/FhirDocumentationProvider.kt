package com.fhirtools.plugin.docs

import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class FhirDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? =
        renderDoc(element, originalElement)

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? =
        renderDoc(element, originalElement)

    // IntelliJ's documentation system only claims "identifier-like" elements by default
    // — JSON property keys qualify, but string-literal values do not, so hovering on a
    // code value never reaches generateDoc unless we claim it explicitly here. Return
    // the JsonStringLiteral when we know we have a code-system lookup for it.
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        val ctx = contextElement ?: return null
        val literal = PsiTreeUtil.getParentOfType(ctx, JsonStringLiteral::class.java, false) ?: return null
        val property = literal.parent as? JsonProperty ?: return null
        if (property.value !== literal) return null
        if (property.name != "code") return null
        val container = property.parent as? JsonObject ?: return null
        val datatype = FhirDatatypeInference.infer(container) ?: return null
        if (datatype != "Coding" && datatype != "Quantity") return null
        val systemValue = (container.findProperty("system")?.value as? JsonStringLiteral)?.value ?: return null
        if (FhirCodeSystems.lookup(systemValue, literal.value) == null) return null
        return literal
    }

    private fun renderDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val origin = originalElement ?: element ?: return null
        val property = PsiTreeUtil.getParentOfType(origin, JsonProperty::class.java) ?: return null
        val container = property.parent as? JsonObject ?: return null
        val datatype = FhirDatatypeInference.infer(container) ?: return null

        val onKey = PsiTreeUtil.isAncestor(property.nameElement, origin, false)
        return if (onKey) datatypeKeyDoc(datatype, property.name)
        else codeValueDoc(datatype, property, container)
    }

    private fun datatypeKeyDoc(datatype: String, propertyName: String): String? {
        val description = FhirDatatypeDocs.lookup(datatype, propertyName) ?: return null
        return formatDatatypeHtml(datatype, propertyName, description)
    }

    private fun codeValueDoc(datatype: String, property: JsonProperty, container: JsonObject): String? {
        // Only resolve on the value of a "code" property within a Coding or Quantity —
        // those are the contexts where (system, code) pairs identify a real terminology
        // entry we can look up.
        if (datatype != "Coding" && datatype != "Quantity") return null
        if (property.name != "code") return null
        val codeValue = (property.value as? JsonStringLiteral)?.value ?: return null
        val systemValue = (container.findProperty("system")?.value as? JsonStringLiteral)?.value ?: return null
        val entry = FhirCodeSystems.lookup(systemValue, codeValue) ?: return null
        return formatCodeHtml(entry.systemLabel, codeValue, entry.display)
    }

    private fun formatDatatypeHtml(datatype: String, prop: String, desc: String): String =
        """
        <div class='definition'><b>$datatype</b>.<b>$prop</b></div>
        <div class='content'>$desc</div>
        """.trimIndent()

    private fun formatCodeHtml(systemLabel: String, code: String, display: String): String =
        """
        <div class='definition'><b>$systemLabel</b> $code</div>
        <div class='content'>$display</div>
        """.trimIndent()
}
