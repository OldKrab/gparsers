package org.parser.sppf

import org.parser.combinators.Parser
import org.parser.sppf.node.*
import java.nio.file.Files
import java.nio.file.Path


class SPPF {
    @JvmInline
    value class NodeHash(val v: Int)

    private val nodes = HashMap<NodeHash, Node>()
    fun convertAllToDot(path: Path) {
        val visitor = Visualizer()
        visitor.begin()
        for (node in nodes.values)
            visitor.visit(node)
        visitor.end()
        Files.writeString(path, visitor.toString())
    }

    fun <T> getIdAction(): (T) -> T = { it }

    fun <LS, RS, R, R2> withAction(node: NonPackedNode<LS, RS, R>, action: (R) -> R2): NonPackedNode<LS, RS, R2> {
        val newNode = node.withAction(action)
        val key = NodeHash(newNode.nodeHashCode())
        nodes[key] = newNode
        return newNode
    }

    @Suppress("UNCHECKED_CAST")
    fun <LS, RS, R> getTerminalNode(leftState: LS, rightState: RS, result: R): NonPackedNode<LS, RS, R> {
        val newNode = TerminalNode(leftState, rightState, result, getIdAction())
        val key = NodeHash(newNode.nodeHashCode())
        val hashedNode = nodes.computeIfAbsent(key) { newNode }
        return hashedNode as NonPackedNode<LS, RS, R>
    }

    @Suppress("UNCHECKED_CAST")
    fun <S, R> getEpsilonNode(state: S): NonPackedNode<S, S, R> {
        val newNode = EpsilonNode(state, getIdAction())
        val key = NodeHash(newNode.nodeHashCode())
        val res = nodes.computeIfAbsent(key) { newNode }
        return res as NonPackedNode<S, S, R>
    }

    @Suppress("UNCHECKED_CAST")
    fun <LS, MS, RS, R1, R2> getIntermediateNode(
        parser: Parser<LS, RS, Pair<R1, R2>>,
        leftChild: NonPackedNode<LS, MS, R1>,
        rightChild: NonPackedNode<MS, RS, R2>
    ): NonPackedNode<LS, RS, Pair<R1, R2>> {
        val leftState = leftChild.leftState
        val rightState = rightChild.rightState
        val newNode = IntermediateNode<LS, RS, Pair<R1, R2>, R1, R2>(parser, leftState, rightState, getIdAction())
        val key = NodeHash(newNode.nodeHashCode())

        val res = nodes.computeIfAbsent(key) { newNode } as IntermediateNode<LS, RS, Pair<R1, R2>, R1, R2>
        res.packedNodes.add(PackedNode(leftChild, rightChild))
        return res
    }


    @Suppress("UNCHECKED_CAST")
    fun <LS, RS, R> getNonTerminalNode(
        parser: Parser<LS, RS, R>,
        child: NonPackedNode<LS, RS, R>,
    ): NonPackedNode<LS, RS, R> {
        val leftState = child.leftState
        val rightState = child.rightState
        val newNode = NonTerminalNode<LS, RS, R, Unit, R>(parser, leftState, rightState) { it.second }
        val key = NodeHash(newNode.nodeHashCode())

        val res = nodes.computeIfAbsent(key) { newNode } as NonTerminalNode<LS, RS, R, Unit, R>
        res.packedNodes.add(PackedNode(null, child))
        return res
    }
}

