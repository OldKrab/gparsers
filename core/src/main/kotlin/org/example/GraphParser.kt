package org.example

interface Graph<V, E> {
    fun getEdges(v: Vertex<V>): List<Edge<E>>?
    fun getVertexes(): Set<Vertex<V>>
    fun getEdgeVertexes(e: Edge<E>): Pair<Vertex<V>, Vertex<V>>?
}

fun <V, E, G : Graph<V, E>, Out, R> G.applyParser(parser: Parser<G, StartState, Out, R>): Sequence<R> {
    return parser.parse(this, StartState()).map { it.res }
}

data class Vertex<V>(val value: V)
data class Edge<E>(val label: E)


class StartState
data class VertexState<V>(val v: Vertex<V>)
data class EdgeState<E>(val edge: Edge<E>)

interface GraphParsers<G : Graph<V, E>, V, E>: Parsers<G> {

    fun outV(p: (V) -> Boolean): Parser<G, EdgeState<E>, VertexState<V>, V> {
        return Parser { gr, (edge) ->
            val (_, outV) = gr.getEdgeVertexes(edge) ?: return@Parser emptySequence()
            if (!p(outV.value)) return@Parser emptySequence()
            sequenceOf(ParserResult(VertexState(outV), outV.value))
        }
    }

    fun outV(): Parser<G, EdgeState<E>, VertexState<V>, V> = outV { true }

    fun v(p: (V) -> Boolean): Parser<G, StartState, VertexState<V>, V> {
        return Parser { gr, _ ->
            gr.getVertexes().asSequence().filter { p(it.value) }.map { ParserResult(VertexState(it), it.value) }
        }
    }

    fun v(): Parser<G, StartState, VertexState<V>, V> = v { true }

    fun edge(p: (E) -> Boolean): Parser<G, VertexState<V>, EdgeState<E>, E> {
        return Parser { gr, (v) ->
            val edges = gr.getEdges(v) ?: return@Parser emptySequence()
            edges.asSequence().filter { e -> p(e.label) }.map { e -> ParserResult(EdgeState(e), e.label) }
        }
    }

}


