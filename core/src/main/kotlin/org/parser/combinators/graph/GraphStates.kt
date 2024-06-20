package org.parser.combinators.graph

import org.parser.combinators.Parser

/** Initial state of graph parsing. */
data class StartState<V, E>(val gr: Graph<V, E>) {
    override fun toString(): String {
        return "StartState"
    }
}

data class VertexState<V, E>(val gr: Graph<V, E>, val v: V) {
    override fun toString(): String {
        return "VState($v)"
    }
}

data class EdgeState<V, E>(val gr: Graph<V, E>, val edge: E) {
    override fun toString(): String {
        return "EState($edge)"
    }
}

typealias VVGraphParser<V, E, R> = Parser<VertexState<V, E>, VertexState<V, E>, R>
typealias VEGraphParser<V, E, R> = Parser<VertexState<V, E>, EdgeState<V, E>, R>
typealias EVGraphParser<V, E, R> = Parser<EdgeState<V, E>, VertexState<V, E>, R>
typealias EEGraphParser<V, E, R> = Parser<EdgeState<V, E>, EdgeState<V, E>, R>
