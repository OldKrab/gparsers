package org.example


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

interface GraphParsers<G : Graph<V, E>, V, E> : Parsers<G> {

    fun outV(p: (V) -> Boolean): Parser<G, EdgeState<E>, VertexState<V>, V> {
        return Parser { gr, (edge) ->
            val (_, outV) = gr.getEdgeVertexes(edge) ?: return@Parser failure()
            if (!p(outV.value)) return@Parser failure()
            success(VertexState(outV), outV.value)
        }
    }

    fun outV(): Parser<G, EdgeState<E>, VertexState<V>, V> = outV { true }

    fun v(p: (V) -> Boolean): Parser<G, StartState, VertexState<V>, V> {
        return Parser { gr, _ ->
            gr.getVertexes()
                .filter { p(it.value) }
                .map {
                    success(VertexState(it), it.value)
                }
                .reduce { acc, cur -> acc.orElse { cur } }
        }
    }

    fun v(): Parser<G, StartState, VertexState<V>, V> = v { true }

    fun edge(p: (E) -> Boolean): Parser<G, VertexState<V>, EdgeState<E>, E> {
        return Parser { gr, (v) ->
            val edges = gr.getEdges(v) ?: return@Parser failure()
            edges
                .filter { e -> p(e.label) }
                .map { e -> success(EdgeState(e), e.label) }
                .fold(failure()) { acc, cur -> acc.orElse { cur } }
        }
    }

}


