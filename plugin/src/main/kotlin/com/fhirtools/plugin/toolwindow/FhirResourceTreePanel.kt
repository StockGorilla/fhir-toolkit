package com.fhirtools.plugin.toolwindow

import com.fhirtools.plugin.fhir.FhirContextHolder
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class FhirResourceTreePanel(
    private val project: Project,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()) {

    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode(EMPTY_MESSAGE))
    private val tree = Tree(treeModel)

    init {
        tree.isRootVisible = true
        tree.showsRootHandles = true

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val path: TreePath = tree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val data = node.userObject as? TreeNodeData ?: return
                navigateTo(data.element)
            }
        })

        add(JBScrollPane(tree), BorderLayout.CENTER)

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
        if (file == null) return setEmpty(EMPTY_MESSAGE)
        if (!file.name.endsWith(".json", ignoreCase = true)) return setEmpty("Active file is not JSON")

        ApplicationManager.getApplication().runReadAction {
            val psi = PsiManager.getInstance(project).findFile(file) as? JsonFile
                ?: return@runReadAction setEmpty("Active file is not a JSON file")
            val root = psi.topLevelValue as? JsonObject
                ?: return@runReadAction setEmpty("Active file has no JSON object at the top level")
            val rt = (root.findProperty("resourceType")?.value as? JsonStringLiteral)?.value
            if (rt == null || rt !in FhirContextHolder.resourceTypes) {
                return@runReadAction setEmpty("Active file is not a FHIR R4 resource")
            }
            val rootNode = DefaultMutableTreeNode(TreeNodeData(rt, root))
            buildChildren(rootNode, root)
            treeModel.setRoot(rootNode)
            tree.expandPath(TreePath(rootNode.path))
        }
    }

    private fun setEmpty(message: String) {
        treeModel.setRoot(DefaultMutableTreeNode(message))
    }

    private fun buildChildren(parent: DefaultMutableTreeNode, value: JsonValue) {
        when (value) {
            is JsonObject -> {
                for (prop in value.propertyList) {
                    val propVal = prop.value
                    val node = DefaultMutableTreeNode(TreeNodeData(labelFor(prop, propVal), prop))
                    parent.add(node)
                    if (propVal != null) buildChildren(node, propVal)
                }
            }
            is JsonArray -> {
                value.valueList.forEachIndexed { i, item ->
                    val node = DefaultMutableTreeNode(TreeNodeData("[$i]${summaryFor(item)}", item))
                    parent.add(node)
                    buildChildren(node, item)
                }
            }
            else -> {} // primitive — leaf, no children
        }
    }

    private fun labelFor(prop: JsonProperty, value: JsonValue?): String {
        val key = prop.name
        return when (value) {
            null -> "$key: null"
            is JsonObject -> {
                val rt = (value.findProperty("resourceType")?.value as? JsonStringLiteral)?.value
                if (rt != null) "$key: $rt" else "$key {${value.propertyList.size}}"
            }
            is JsonArray -> "$key [${value.valueList.size}]"
            is JsonStringLiteral -> "$key: \"${value.value.take(80)}\""
            else -> "$key: ${value.text.take(80)}"
        }
    }

    private fun summaryFor(value: JsonValue): String {
        return when (value) {
            is JsonObject -> {
                val rt = (value.findProperty("resourceType")?.value as? JsonStringLiteral)?.value
                if (rt != null) " $rt" else " {${value.propertyList.size}}"
            }
            is JsonArray -> " [${value.valueList.size}]"
            is JsonStringLiteral -> " \"${value.value.take(60)}\""
            else -> " ${value.text.take(60)}"
        }
    }

    private fun navigateTo(element: PsiElement) {
        val containingFile = element.containingFile?.virtualFile ?: return
        OpenFileDescriptor(project, containingFile, element.textOffset).navigate(true)
    }

    private data class TreeNodeData(val label: String, val element: PsiElement) {
        override fun toString(): String = label
    }

    private companion object {
        const val EMPTY_MESSAGE = "Open a FHIR resource to see its structure"
    }
}
