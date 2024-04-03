package org.parser.sppf.node

import org.parser.combinators.BaseParser

sealed interface Node


sealed class NodeWithHashCode : Node {
    abstract fun nodeHashCode(): Int

    override fun hashCode(): Int {
        return nodeHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeWithHashCode) return false
        return nodeHashCode() == other.nodeHashCode()
    }
}


sealed class NonPackedNode<LS, RS, R>(val leftState: LS, val rightState: RS) : NodeWithHashCode() {
    abstract fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2>
}


class PackedNode<LS, MS, RS, R1, R2>(
    val leftNode: NonPackedNode<LS, MS, R1>?,
    val rightNode: NonPackedNode<MS, RS, R2>
) : Node

open class IntermediateNode<LS, RS, R, CR1, CR2>(
    val parser: BaseParser,
    leftState: LS,
    rightState: RS,
    val action: (Pair<CR1, CR2>) -> R
) :
    NonPackedNode<LS, RS, R>(leftState, rightState) {

    val packedNodes: MutableList<PackedNode<LS, *, RS, CR1, CR2>> = ArrayList()


    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2> {
        val res = IntermediateNode(parser, leftState, rightState) { f(action(it)) }
        res.packedNodes.addAll(packedNodes)
        return res
    }

    override fun nodeHashCode(): Int {
        return Triple(parser.hashCode(), leftState.hashCode(), rightState.hashCode()).hashCode()
    }

    override fun toString(): String {
        return "$parser, $leftState, $rightState"
    }
}

class NonTerminalNode<LS, RS, R, CR1, CR2>(
    parser: BaseParser,
    leftState: LS,
    rightState: RS,
    action: (Pair<CR1, CR2>) -> R
) : IntermediateNode<LS, RS, R, CR1, CR2>(parser, leftState, rightState, action) {

    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2> {
        val res = NonTerminalNode(parser, leftState, rightState) { f(action(it)) }
        res.packedNodes.addAll(packedNodes)
        return res
    }

    override fun toString(): String {
        return "NT -> $parser, $leftState, $rightState"
    }
}


open class TerminalNode<LS, RS, R, R2>(
    leftState: LS,
    rightState: RS,
    val result: R,
    val action: (R) -> R2
) : NonPackedNode<LS, RS, R2>(leftState, rightState) {
    override fun <R3> withAction(f: (R2) -> R3): NonPackedNode<LS, RS, R3> {
        return TerminalNode(leftState, rightState, result) { f(action(it)) }
    }

    override fun nodeHashCode(): Int {
        return Pair(leftState.hashCode(), rightState.hashCode()).hashCode()
    }

    override fun toString(): String {
        var resultView = result.toString()
        if (result is String) {
            resultView = "\"$resultView\""
        }
        return "$resultView, $leftState, $rightState"
    }
}


class EpsilonNode<S, R>(state: S, action: (Unit) -> R) : TerminalNode<S, S, Unit, R>(state, state, Unit, action) {
    override fun toString(): String {
        return "ε, $leftState"
    }
    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<S, S, R2> {
        return EpsilonNode(leftState) { f(action(it)) }
    }
}