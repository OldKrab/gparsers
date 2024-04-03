package org.parser.sppf

import org.parser.combinators.Parser
import org.parser.sppf.node.*


class SPPF<E> {
    @JvmInline
    value class NodeHash(val v: Int)

    private val nodes = HashMap<NodeHash, NodeWithHashCode>()

    @Suppress("UNCHECKED_CAST")
    fun <LS, RS, R> getTerminalNode(leftState: LS, rightState: RS, result: R): NonPackedNode<LS, RS, R> {
        val key = NodeHash(Triple(leftState.hashCode(), rightState.hashCode(), result.hashCode()).hashCode())
        val res = nodes.computeIfAbsent(key) { TerminalNode(leftState, rightState, result) {it} }
        return res as TerminalNode<LS, RS, R, R>
    }

    @Suppress("UNCHECKED_CAST")
    fun <S, R> getEpsilonNode(state: S): NonPackedNode<S, S, R> {
        val key = NodeHash(state.hashCode())
        val res = nodes.computeIfAbsent(key) { EpsilonNode(state) {} }
        return res as EpsilonNode<S, R>
    }

    @Suppress("UNCHECKED_CAST")
    fun <LS, MS, RS, R1, R2> getIntermediateNode(
        parser: Parser<E, LS, RS, Pair<R1, R2>>,
        leftChild: NonPackedNode<LS, MS, R1>,
        rightChild: NonPackedNode<MS, RS, R2>
    ): IntermediateNode<LS, RS, Pair<R1, R2>, R1, R2> {
        val leftState = leftChild.leftState
        val rightState = rightChild.rightState
        val key = NodeHash(Triple(parser.hashCode(), leftState.hashCode(), rightState.hashCode()).hashCode())

        val res = nodes.computeIfAbsent(key) {
            IntermediateNode<LS, RS, Pair<R1, R2>, R1, R2>(parser, leftState, rightState) { it }
        } as IntermediateNode<LS, RS, Pair<R1, R2>, R1, R2>
        res.packedNodes.add(PackedNode(leftChild, rightChild))
        return res
    }


    @Suppress("UNCHECKED_CAST")
    fun <LS, RS, R> getNonTerminalNode(
        parser: Parser<E, LS, RS, R>,
        child: NonPackedNode<LS, RS, R>,
    ): NonTerminalNode<LS, RS, R, Nothing, R> {
        val leftState = child.leftState
        val rightState = child.rightState
        val key = NodeHash(Triple(parser.hashCode(), leftState.hashCode(), rightState.hashCode()).hashCode())

        val res = nodes.computeIfAbsent(key) {
            NonTerminalNode<LS, RS, R, Nothing, R>(parser, leftState, rightState) { it.second }
        } as NonTerminalNode<LS, RS, R, Nothing, R>
        res.packedNodes.add(PackedNode(null, child))
        return res
    }
}

