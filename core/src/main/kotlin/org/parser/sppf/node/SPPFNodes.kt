package org.parser.sppf.node

import org.parser.combinators.BaseParser
import org.parser.sppf.SPPF
import java.util.*
import kotlin.collections.ArrayList

sealed interface Node

sealed interface NodeWithResults<R> {
    fun getResults(): Sequence<R>
}

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

sealed class NonPackedNode<LS, RS, R>(val leftState: LS, val rightState: RS) : NodeWithHashCode(), NodeWithResults<R> {
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
) : NonPackedNode<LS, RS, R>(leftState, rightState) {

    val packedNodes: MutableList<PackedNode<LS, *, RS, CR1, CR2>> = ArrayList()


    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2> {
        val res = IntermediateNode(parser, leftState, rightState) { f(action(it)) }
        res.packedNodes.addAll(packedNodes)
        return res
    }

    override fun nodeHashCode(): Int {
        return Objects.hash(parser, leftState, rightState)
    }

    override fun toString(): String {
        return "$parser, $leftState, $rightState"
    }

    override fun getResults(): Sequence<R> {
        return packedNodes.asSequence().flatMap { pn ->
            val rightResults = pn.rightNode.getResults()
            val leftResults = pn.leftNode!!.getResults()
            rightResults.flatMap { r -> leftResults.map { l -> action(Pair(l, r)) } }
        }
    }
}

class NonTerminalNode<LS, RS, R, CR1, CR2>(
    val parser: BaseParser,
    leftState: LS,
    rightState: RS,
    val action: (Pair<CR1?, CR2>) -> R
) : NonPackedNode<LS, RS, R>(leftState, rightState) {

    val packedNodes: MutableList<PackedNode<LS, *, RS, CR1?, CR2>> = ArrayList()

    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2> {
        val res = NonTerminalNode<LS, RS, R2, CR1, CR2>(parser, leftState, rightState) { f(action(it)) }
        res.packedNodes.addAll(packedNodes)
        return res
    }

    override fun nodeHashCode(): Int {
        return Objects.hash(parser, leftState, rightState)
    }

    override fun toString(): String {
        return "NT -> $parser, $leftState, $rightState"
    }

    override fun getResults(): Sequence<R> {
        return packedNodes.asSequence().flatMap { pn ->
            val rightResults = pn.rightNode.getResults()
            val leftResults = pn.leftNode?.getResults()
            if (leftResults == null)
                rightResults.map { action(Pair(null, it)) }
            else
                rightResults.flatMap { r -> leftResults.map { l -> action(Pair(l, r)) } }
        }
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
        return Objects.hash(result, leftState, rightState, action)
    }

    override fun toString(): String {
        var resultView = result.toString()
        if (result is String) {
            resultView = "\"$resultView\""
        }
        return "$resultView, $leftState, $rightState"
    }

    override fun getResults(): Sequence<R2> {
        return sequenceOf(action(result))
    }
}


class EpsilonNode<S, R>(state: S, val action: (Unit) -> R) : NonPackedNode<S, S, R>(state, state) {
    override fun toString(): String {
        return "ε, $leftState"
    }

    override fun nodeHashCode(): Int {
        return Objects.hash(leftState, action)
    }

    override fun getResults(): Sequence<R> {
        return sequenceOf(action(Unit))
    }

    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<S, S, R2> {
        return EpsilonNode(leftState) { f(action(it)) }
    }
}