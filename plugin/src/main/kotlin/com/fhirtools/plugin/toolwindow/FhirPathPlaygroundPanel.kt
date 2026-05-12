package com.fhirtools.plugin.toolwindow

import ca.uhn.fhir.parser.IParser
import com.fhirtools.plugin.fhir.FhirContextHolder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

class FhirPathPlaygroundPanel(
    private val project: Project,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()) {

    private val expressionField = JBTextField().apply {
        toolTipText = "e.g. Patient.name.given  |  Observation.code.coding[0].code  |  Bundle.entry.resource.where(resourceType='Patient')"
    }
    private val resultArea = JTextArea().apply {
        isEditable = false
        lineWrap = false
        font = font.deriveFont(font.size2D)
    }
    private val statusLabel = JBLabel(STATUS_NO_FILE).apply {
        border = BorderFactory.createEmptyBorder(4, 6, 4, 6)
    }

    @Volatile private var currentResourceJson: String? = null

    init {
        val top = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
            add(expressionField, BorderLayout.CENTER)
            add(JButton(EvalAction()), BorderLayout.EAST)
        }
        expressionField.addActionListener { evaluate() }

        add(top, BorderLayout.NORTH)
        add(JBScrollPane(resultArea), BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)

        project.messageBus.connect(parentDisposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    refresh(event.newFile)
                }
            },
        )
        refresh(FileEditorManager.getInstance(project).selectedFiles.firstOrNull())
    }

    private fun refresh(file: VirtualFile?) {
        if (file == null || !file.name.endsWith(".json", ignoreCase = true)) {
            currentResourceJson = null
            statusLabel.text = STATUS_NO_FILE
            return
        }
        ApplicationManager.getApplication().runReadAction {
            val text = FileDocumentManager.getInstance().getDocument(file)?.text
                ?: file.contentsToByteArray().toString(Charsets.UTF_8)
            currentResourceJson = text
            statusLabel.text = "Active: ${file.name}"
        }
    }

    private fun evaluate() {
        val expr = expressionField.text?.trim().orEmpty()
        if (expr.isEmpty()) {
            resultArea.text = ""
            statusLabel.text = "Enter a FHIRPath expression"
            return
        }
        val json = currentResourceJson
        if (json.isNullOrBlank()) {
            resultArea.text = ""
            statusLabel.text = "No FHIR resource active"
            return
        }
        try {
            FhirContextHolder.withPluginClassLoader {
                val parser = FhirContextHolder.parser()
                val resource = parser.parseResource(json)
                val results = FhirContextHolder.context.newFhirPath()
                    .evaluate(resource, expr, IBase::class.java)
                renderResults(results, parser)
            }
        } catch (t: Throwable) {
            resultArea.text = ""
            statusLabel.text = "Error: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    private fun renderResults(results: List<IBase>, parser: IParser) {
        if (results.isEmpty()) {
            resultArea.text = "(no matches)"
            statusLabel.text = "0 results"
            return
        }
        val pretty = parser.setPrettyPrint(true)
        resultArea.text = results.joinToString("\n---\n") { encodeForDisplay(it, pretty) }
        resultArea.caretPosition = 0
        statusLabel.text = "${results.size} result${if (results.size == 1) "" else "s"}"
    }

    private fun encodeForDisplay(base: IBase, prettyParser: IParser): String {
        return when (base) {
            is IPrimitiveType<*> -> base.valueAsString ?: "(empty)"
            is IBaseResource -> prettyParser.encodeResourceToString(base)
            else -> try {
                prettyParser.encodeToString(base)
            } catch (e: Exception) {
                base.toString()
            }
        }
    }

    private inner class EvalAction : AbstractAction("Evaluate") {
        override fun actionPerformed(e: ActionEvent?) = evaluate()
    }

    private companion object {
        const val STATUS_NO_FILE = "Open a FHIR resource, then enter a FHIRPath expression"
    }
}
