package org.parser.combinators.graph

import org.parser.combinators.Parser


interface Graph<V, E> {
    fun getEdges(v: Vertex<V>): List<Edge<E>>?
    fun getVertexes(): Set<Vertex<V>>
    fun getEdgeVertexes(e: Edge<E>): Pair<Vertex<V>, Vertex<V>>?
}

fun <V, E, G : Graph<V, E>, Out, R> G.applyParser(parser: Parser<G, StartState, Out, R>, count: Int = -1): List<R> {
    return parser.getResults(this, StartState(), count).map { it.second }
}

data class Vertex<V>(val value: V)
data class Edge<E>(val label: E)


class StartState
data class VertexState<V>(val v: Vertex<V>)
data class EdgeState<E>(val edge: Edge<E>)