package com.fhirtools.plugin.actions

import com.fhirtools.plugin.server.MockFhirServerService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class RunMockFhirServerAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
        val running = project?.let { MockFhirServerService.getInstance(it).isRunning } == true
        e.presentation.text = if (running) "Stop Mock FHIR Server" else "Run Mock FHIR Server"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = MockFhirServerService.getInstance(project)
        if (service.isRunning) {
            service.stop()
            notify(project, "Mock FHIR server stopped")
        } else {
            val url = service.start()
            notify(project, "Mock FHIR server running at $url")
        }
    }

    private fun notify(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("FHIR Toolkit")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }
}
