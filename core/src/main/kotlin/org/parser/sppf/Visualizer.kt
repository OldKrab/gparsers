package org.parser.sppf

import guru.nidi.graphviz.model.Factory.mutGraph
import org.parser.sppf.node.*
import java.nio.file.Path

class Visualizer {
    private val sb = StringBuilder()

    private val visitedNodes = HashSet<Node>()
    fun toDot(node: Node): String {
        sb.append("digraph {\nordering=\"out\"\n")
        visit(node)
        sb.append("}\n")
        return sb.toString()
    }

    fun toDotFile(node: Node, path: Path) {
        val dot = toDot(node)
        path.toFile().writeText(dot)
    }

    private fun getNodeId(node: Node): String {
        var nodeStr = node.toString()
        nodeStr = nodeStr.replace("\"", "\\\"")
        return "\"$nodeStr\""
    }
    private fun addNode(id: String, label: String, shape: String, height: Double=0.5, width: Double=0.75, style: String="\"\"") {
        sb.append("$id [label=$label, shape=$shape, style=$style, width=$width, height=$height]")
        sb.append("\n")
    }
    private fun addNode(node: Node) {
        when (node) {
            is NonTerminalNode<*, *, *, *, *> -> addNode(getNodeId(node), getNodeId(node), "box", style="\"rounded,bold\"")
            is IntermediateNode<*, *, *, *, *> -> addNode(getNodeId(node), getNodeId(node), "box", style="rounded")
            is TerminalNode<*, *, *, *> -> addNode(getNodeId(node), getNodeId(node), "box")
            is PackedNode<*, *, *, *, *> -> addNode(getNodeId(node), "\"\"", "box", width = 0.2, height = 0.2)
        }
    }

    private fun addEdge(x: Node, y: Node) {
        sb.append("${getNodeId(x)} -> ${getNodeId(y)}\n")
    }

    private fun visit(node: Node) {
        if(node in visitedNodes) return
        visitedNodes.add(node)
        val g = mutGraph("example1").setDirected(true)
        when (node) {
            is IntermediateNode<*, *, *, *, *> -> {
                addNode(node)
                for (c in node.packedNodes) visit(c)
                for (c in node.packedNodes) addEdge(node, c)
            }
            is TerminalNode<*, *, *, *> -> {
                addNode(node)
            }

            is PackedNode<*, *, *, *, *> -> {
                addNode(node)
                if (node.leftNode != null) {
                    visit(node.leftNode)
                    addEdge(node, node.leftNode)
                }
                visit(node.rightNode)
                addEdge(node, node.rightNode)
            }
        }
    }
}