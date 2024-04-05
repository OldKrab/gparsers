package org.parser.sppf

import org.parser.combinators.Parser
import java.util.*
import kotlin.collections.ArrayList

sealed interface Node


sealed interface NodeWithResults<out R>: Node {
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


class PackedNode<LS, MS, RS, out R1, out R2>(
    val leftNode: NonPackedNode<LS, MS, R1>?,
    val rightNode: NonPackedNode<MS, RS, R2>
) : NodeWithResults<Pair<R1?, R2>> {
    override fun getResults(): Sequence<Pair<R1?, R2>> {
        val rightResults = rightNode.getResults()
        val leftResults = leftNode?.getResults() ?: return rightResults.map { r -> Pair(null, r) }
        return rightResults.flatMap { r -> leftResults.map { l -> Pair(l, r) } }
    }
}


sealed class NonPackedNode<LS, RS, out R>(val leftState: LS, val rightState: RS) : NodeWithHashCode(), NodeWithResults<R> {
    /** Returns new node where results are mapped with [f] function. */
    abstract fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2>
}


open class IntermediateNode<LS, RS, R, CR1, CR2>(
    val parser: Parser<LS, RS, *>,
    leftState: LS,
    rightState: RS,
    val action: (Pair<CR1?, CR2>) -> R
) : NonPackedNode<LS, RS, R>(leftState, rightState) {

    val packedNodes: MutableList<PackedNode<LS, *, RS, CR1?, CR2>> = ArrayList()

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
        return packedNodes.asSequence().flatMap { it.getResults() }.map { action(it) }
    }
}


class NonTerminalNode<LS, RS, R, CR>(
    val nt: String,
    val parser: Parser<LS, RS, CR>,
    leftState: LS,
    rightState: RS,
    val action: (CR) -> R
) : NonPackedNode<LS, RS, R>(leftState, rightState) {

    val packedNodes: MutableList<PackedNode<LS, *, RS, Nothing, CR>> = ArrayList()

    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<LS, RS, R2> {
        val res = NonTerminalNode(nt, parser, leftState, rightState) { f(action(it)) }
        res.packedNodes.addAll(packedNodes)
        return res
    }

    override fun nodeHashCode(): Int {
        return Objects.hash(parser, leftState, rightState)
    }

    override fun toString(): String {
        return "$nt, $leftState, $rightState"
    }

    override fun getResults(): Sequence<R> {
        return packedNodes.asSequence().flatMap { it.getResults() }.map { action(it.second) }
    }
}


open class TerminalNode<LS, RS, R, R2>(
    leftState: LS,
    rightState: RS,
    private val result: R,
    val action: (R) -> R2
) : NonPackedNode<LS, RS, R2>(leftState, rightState) {
    override fun <R3> withAction(f: (R2) -> R3): NonPackedNode<LS, RS, R3> {
        return TerminalNode(leftState, rightState, result) { f(action(it)) }
    }

    override fun getResults(): Sequence<R2> {
        return sequenceOf(action(result))
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
}


class EpsilonNode<S, R>(state: S, val action: (Unit) -> R) : NonPackedNode<S, S, R>(state, state) {

    override fun getResults(): Sequence<R> {
        return sequenceOf(action(Unit))
    }

    override fun <R2> withAction(f: (R) -> R2): NonPackedNode<S, S, R2> {
        return EpsilonNode(leftState) { f(action(it)) }
    }

    override fun nodeHashCode(): Int {
        return Objects.hash(leftState, action)
    }

    override fun toString(): String {
        return "ε, $leftState"
    }
}