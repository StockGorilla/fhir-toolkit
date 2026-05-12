package com.fhirtools.plugin.actions

import com.fhirtools.plugin.skeleton.FhirScenarios
import com.intellij.json.JsonFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory

class InsertFhirScenarioAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            editor != null && file != null && file.fileType == JsonFileType.INSTANCE
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(FhirScenarios.all)
            .setTitle("Insert FHIR Scenario")
            .setItemChosenCallback { scenario -> insertScenario(project, editor, scenario) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun insertScenario(project: Project, editor: Editor, scenario: FhirScenarios.Scenario) {
        val bundle = FhirScenarios.load(scenario).trimEnd()
        WriteCommandAction.runWriteCommandAction(
            project,
            "Insert FHIR Scenario: ${scenario.displayName}",
            null,
            {
                val offset = editor.caretModel.offset
                editor.document.insertString(offset, bundle)
                editor.caretModel.moveToOffset(offset + bundle.length)
            },
        )
    }
}
