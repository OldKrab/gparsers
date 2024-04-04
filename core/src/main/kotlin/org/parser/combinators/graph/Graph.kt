package org.parser.combinators.graph

import org.parser.combinators.Parser
import org.parser.combinators.applyParser
import org.parser.sppf.NonPackedNode


interface Graph<V, E> {
    fun getEdges(v: V): List<E>?
    fun getVertexes(): Set<V>
    fun getEdgeVertexes(e: E): Pair<V, V>?
}

fun <V, E, G : Graph<V, E>, O, R> G.applyParser(parser: Parser<StartState<V, E>, O, R>): List<NonPackedNode<StartState<V, E>, O, R>> {
    return applyParser(parser, StartState(this))
}



data class StartState<V, E>(val gr: Graph<V, E>) {
    override fun toString(): String {
        return "StartState"
    }
}
data class VertexState<V, E>(val gr: Graph<V, E>, val v: V){
    override fun toString(): String {
        return "VState($v)"
    }
}
data class EdgeState<V, E>(val gr: Graph<V, E>, val edge: E) {

    override fun toString(): String {
        return "EState($edge)"
    }
}