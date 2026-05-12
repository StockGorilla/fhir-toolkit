package com.fhirtools.plugin.server

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MockFhirServerService : Disposable {

    private val server = MockFhirServer()

    val isRunning: Boolean get() = server.isRunning
    val baseUrl: String? get() = if (server.isRunning) server.baseUrl else null

    fun start(): String = server.start()
    fun stop() = server.stop()

    override fun dispose() {
        server.stop()
    }

    companion object {
        fun getInstance(project: Project): MockFhirServerService = project.service()
    }
}
