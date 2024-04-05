package org.parser.combinators.graph

/** Initial state of graph parsing. */
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