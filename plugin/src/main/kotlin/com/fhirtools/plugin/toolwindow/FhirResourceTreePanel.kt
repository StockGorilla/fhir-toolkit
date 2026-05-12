package com.fhirtools.plugin.toolwindow

import com.fhirtools.plugin.fhir.FhirContextHolder
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonBooleanLiteral
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonNumberLiteral
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class FhirResourceTreePanel(
    private val project: Project,
    parentDisposable: Disposable,
) : JPanel(BorderLayout()) {

    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode(EMPTY_MESSAGE))
    private val tree = Tree(treeModel)

    // Reverse map: PsiElement -> tree node, used for cursor-in-editor -> tree-highlight sync.
    private val elementToNode = mutableMapOf<PsiElement, DefaultMutableTreeNode>()

    // The file the tree currently mirrors. Used to filter caret events from other editors.
    private var currentFile: VirtualFile? = null

    // Suppress mouseClicked-driven navigation while we programmatically select via cursor sync,
    // so cursor-in-editor doesn't bounce back to navigate the editor again.
    private var syncingSelection = false

    init {
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.cellRenderer = FhirNodeRenderer()

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (syncingSelection) return
                val path: TreePath = tree.getPathForLocation(e.x, e.y) ?: return
                val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val data = node.userObject as? TreeNodeData ?: return
                navigateTo(data.element)
            }
        })

        project.messageBus.connect(parentDisposable).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    refresh(event.newFile)
                }
            },
        )

        EditorFactory.getInstance().eventMulticaster.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) {
                    syncTreeFromCaret(event)
                }
            },
            parentDisposable,
        )

        refresh(FileEditorManager.getInstance(project).selectedFiles.firstOrNull())
    }

    private fun refresh(file: VirtualFile?) {
        currentFile = null
        elementToNode.clear()

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

            currentFile = file
            val rootNode = DefaultMutableTreeNode(TreeNodeData(rt, root, NodeKind.RESOURCE_ROOT))
            elementToNode[root] = rootNode
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
                    val kind = kindOf(prop.name, propVal)
                    val node = DefaultMutableTreeNode(TreeNodeData(labelFor(prop, propVal), prop, kind))
                    parent.add(node)
                    elementToNode[prop] = node
                    if (propVal != null) {
                        elementToNode.putIfAbsent(propVal, node)
                        buildChildren(node, propVal)
                    }
                }
            }
            is JsonArray -> {
                value.valueList.forEachIndexed { i, item ->
                    val kind = kindOf(null, item)
                    val node = DefaultMutableTreeNode(TreeNodeData("[$i]${summaryFor(item)}", item, kind))
                    parent.add(node)
                    elementToNode[item] = node
                    buildChildren(node, item)
                }
            }
            else -> {} // primitive — leaf, no children
        }
    }

    private fun kindOf(propertyName: String?, value: JsonValue?): NodeKind {
        if (propertyName == "reference" && value is JsonStringLiteral) return NodeKind.REFERENCE
        return when (value) {
            is JsonObject -> NodeKind.OBJECT
            is JsonArray -> NodeKind.ARRAY
            is JsonStringLiteral -> NodeKind.STRING
            is JsonNumberLiteral -> NodeKind.NUMBER
            is JsonBooleanLiteral -> NodeKind.BOOLEAN
            else -> NodeKind.OTHER
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

    private fun syncTreeFromCaret(event: CaretEvent) {
        val editor = event.editor
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        if (file != currentFile) return
        val offset = editor.caretModel.offset

        ApplicationManager.getApplication().runReadAction {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: return@runReadAction
            val element = psiFile.findElementAt(offset) ?: return@runReadAction
            val node = findClosestMappedNode(element) ?: return@runReadAction

            val path = TreePath(node.path)
            syncingSelection = true
            try {
                tree.selectionPath = path
                tree.scrollPathToVisible(path)
            } finally {
                syncingSelection = false
            }
        }
    }

    private fun findClosestMappedNode(start: PsiElement): DefaultMutableTreeNode? {
        // Walk up the PSI tree, stopping at JSON structural nodes that are in our index.
        var cursor: PsiElement? = start
        while (cursor != null) {
            elementToNode[cursor]?.let { return it }
            cursor = PsiTreeUtil.getParentOfType(
                cursor,
                JsonProperty::class.java,
                JsonObject::class.java,
                JsonArray::class.java,
                JsonStringLiteral::class.java,
            )
            // Move strictly upward to avoid infinite loop when getParentOfType returns the same node.
            if (cursor === start) cursor = cursor.parent
        }
        return null
    }

    private inner class FhirNodeRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any?,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
            val node = value as? DefaultMutableTreeNode
            val data = node?.userObject as? TreeNodeData
            icon = iconFor(data?.kind)
            return this
        }

        private fun iconFor(kind: NodeKind?): javax.swing.Icon = when (kind) {
            NodeKind.RESOURCE_ROOT -> AllIcons.Nodes.Class
            NodeKind.OBJECT -> AllIcons.Json.Object
            NodeKind.ARRAY -> AllIcons.Json.Array
            NodeKind.REFERENCE -> AllIcons.Gutter.ImplementedMethod
            NodeKind.STRING -> AllIcons.Nodes.Property
            NodeKind.NUMBER -> AllIcons.Nodes.Property
            NodeKind.BOOLEAN -> AllIcons.Nodes.Property
            NodeKind.OTHER, null -> AllIcons.Nodes.Tag
        }
    }

    private enum class NodeKind { RESOURCE_ROOT, OBJECT, ARRAY, REFERENCE, STRING, NUMBER, BOOLEAN, OTHER }

    private data class TreeNodeData(val label: String, val element: PsiElement, val kind: NodeKind) {
        override fun toString(): String = label
    }

    private companion object {
        const val EMPTY_MESSAGE = "Open a FHIR resource to see its structure"
    }
}
