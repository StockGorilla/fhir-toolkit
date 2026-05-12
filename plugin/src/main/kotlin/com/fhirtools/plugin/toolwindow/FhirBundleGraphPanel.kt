package com.fhirtools.plugin.toolwindow

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

class FhirBundleGraphPanel(
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
                val data = node.userObject as? GraphNodeData ?: return
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
            if (rt != "Bundle") return@runReadAction setEmpty("Active file is not a Bundle")

            val bundleType = (root.findProperty("type")?.value as? JsonStringLiteral)?.value ?: "unknown"
            val entryArray = root.findProperty("entry")?.value as? JsonArray
            val entryObjects = entryArray?.valueList?.filterIsInstance<JsonObject>().orEmpty()

            val rootLabel = "Bundle ($bundleType, ${entryObjects.size} entries)"
            val rootNode = DefaultMutableTreeNode(GraphNodeData(rootLabel, root))

            for (entry in entryObjects) {
                val entryNode = buildEntryNode(root, entry)
                rootNode.add(entryNode)
            }

            treeModel.setRoot(rootNode)
            tree.expandPath(TreePath(rootNode.path))
        }
    }

    private fun setEmpty(message: String) {
        treeModel.setRoot(DefaultMutableTreeNode(message))
    }

    private fun buildEntryNode(bundleRoot: JsonObject, entry: JsonObject): DefaultMutableTreeNode {
        val resource = entry.findProperty("resource")?.value as? JsonObject
        val resourceType = (resource?.findProperty("resourceType")?.value as? JsonStringLiteral)?.value
        val resourceId = (resource?.findProperty("id")?.value as? JsonStringLiteral)?.value
        val fullUrl = (entry.findProperty("fullUrl")?.value as? JsonStringLiteral)?.value

        val entryLabel = when {
            resourceType != null && resourceId != null -> "$resourceType/$resourceId"
            fullUrl != null -> fullUrl
            resourceType != null -> "$resourceType (no id)"
            else -> "(unrecognized entry)"
        }

        val entryTarget: PsiElement = resource ?: entry
        val entryNode = DefaultMutableTreeNode(GraphNodeData(entryLabel, entryTarget))

        if (resource != null) {
            val refs = mutableListOf<RefInfo>()
            collectReferences(resource, parentChain = emptyList(), out = refs)
            for (refInfo in refs) {
                val resolved = resolveReference(bundleRoot, refInfo.refValue)
                val marker = if (resolved != null) "✓" else "✗"
                val parentName = refInfo.parentChain.lastOrNull() ?: "reference"
                val refLabel = "$parentName → ${refInfo.refValue} $marker"
                val refTarget: PsiElement = resolved ?: refInfo.literalElement
                entryNode.add(DefaultMutableTreeNode(GraphNodeData(refLabel, refTarget)))
            }
            if (refs.isEmpty()) {
                entryNode.add(DefaultMutableTreeNode("(no outgoing references)"))
            }
        }

        return entryNode
    }

    private fun collectReferences(
        value: JsonValue,
        parentChain: List<String>,
        out: MutableList<RefInfo>,
    ) {
        when (value) {
            is JsonObject -> {
                for (prop in value.propertyList) {
                    val propVal = prop.value ?: continue
                    if (prop.name == "reference" && propVal is JsonStringLiteral) {
                        val refValue = propVal.value
                        if (refValue.isNotBlank() && !refValue.startsWith("#")) {
                            out.add(RefInfo(parentChain, refValue, propVal))
                        }
                    } else {
                        collectReferences(propVal, parentChain + prop.name, out)
                    }
                }
            }
            is JsonArray -> {
                for (item in value.valueList) {
                    collectReferences(item, parentChain, out)
                }
            }
            else -> {}
        }
    }

    // Mirrors the resolution rules in BundleReferenceLineMarkerProvider:
    // 1. fullUrl exact match, 2. relative Type/id match, 3. absolute URL whose suffix matches.
    private fun resolveReference(bundleRoot: JsonObject, ref: String): PsiElement? {
        val entryArray = bundleRoot.findProperty("entry")?.value as? JsonArray ?: return null
        for (entry in entryArray.valueList) {
            val entryObj = entry as? JsonObject ?: continue

            val fullUrl = (entryObj.findProperty("fullUrl")?.value as? JsonStringLiteral)?.value
            if (fullUrl != null && fullUrl == ref) return targetOf(entryObj)

            val resource = entryObj.findProperty("resource")?.value as? JsonObject ?: continue
            val resourceType = (resource.findProperty("resourceType")?.value as? JsonStringLiteral)?.value ?: continue
            val resourceId = (resource.findProperty("id")?.value as? JsonStringLiteral)?.value

            if (resourceId != null && ref == "$resourceType/$resourceId") return targetOf(entryObj)
            if (resourceId != null && ref.endsWith("/$resourceType/$resourceId")) return targetOf(entryObj)
        }
        return null
    }

    private fun targetOf(entry: JsonObject): PsiElement {
        return entry.findProperty("resource")?.value ?: entry
    }

    private fun navigateTo(element: PsiElement) {
        val containingFile = element.containingFile?.virtualFile ?: return
        OpenFileDescriptor(project, containingFile, element.textOffset).navigate(true)
    }

    private data class RefInfo(
        val parentChain: List<String>,
        val refValue: String,
        val literalElement: JsonStringLiteral,
    )

    private data class GraphNodeData(val label: String, val element: PsiElement) {
        override fun toString(): String = label
    }

    private companion object {
        const val EMPTY_MESSAGE = "Open a FHIR Bundle to see its reference graph"
    }
}
