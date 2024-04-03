package org.parser.combinators.graph

import org.parser.combinators.Parser
import org.parser.combinators.applyParser
import org.parser.sppf.node.NonPackedNode


interface Graph<V, E> {
    fun getEdges(v: V): List<E>?
    fun getVertexes(): Set<V>
    fun getEdgeVertexes(e: E): Pair<V, V>?
}

fun <V, E, G : Graph<V, E>, Out, R> G.applyParser(parser: Parser<G, StartState, Out, R>, count: Int = -1): List<NonPackedNode<StartState, Out, R>> {
    return applyParser(this, parser, StartState(), count)
}



class StartState {
    override fun toString(): String {
        return "StartState"
    }
}
data class VertexState<V>(val v: V){
    override fun toString(): String {
        return "VState($v)"
    }
}
data class EdgeState<E>(val edge: E) {
    override fun toString(): String {
        return "EState($edge)"
    }
}