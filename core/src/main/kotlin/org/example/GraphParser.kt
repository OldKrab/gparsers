package org.example

abstract class Graph<V, E> {
    abstract fun getEdges(v: V): List<Pair<E, V>>?
    abstract fun getVertexes(): Set<V>

    fun <Out, R> applyParser(parser: Parser<StartState<V, E>, Out, R>): Sequence<R> {
        return parser.parse(StartState(this)).map { it.res }
    }
}

data class StartState<V, E>(val graph: Graph<V, E>)
data class VertexState<V, E>(val graph: Graph<V, E>, val v: V)
data class EdgeState<V, E>(val graph: Graph<V, E>, val edge: E, val outV: V)


fun <V, E> outV(p: (V) -> Boolean): Parser<EdgeState<V, E>, VertexState<V, E>, V> {
    return Parser { (gr, _, outV) ->
        if (!p(outV)) return@Parser emptySequence()
        sequenceOf(ParserResult(VertexState(gr, outV), outV))
    }
}

fun <V, E> outV(): Parser<EdgeState<V, E>, VertexState<V, E>, V> = outV { true }

fun <V, E> v(p: (V) -> Boolean): Parser<StartState<V, E>, VertexState<V, E>, V> {
    return Parser { (gr) ->
        gr.getVertexes().asSequence().filter(p).map { ParserResult(VertexState(gr, it), it) }
    }
}

fun <V, E> v(): Parser<StartState<V, E>, VertexState<V, E>, V> = v { true }

fun <V, E> edge(p: (E) -> Boolean): Parser<VertexState<V, E>, EdgeState<V, E>, E> {
    return Parser { (gr, v) ->
        val edges = gr.getEdges(v) ?: return@Parser emptySequence()
        edges.asSequence().filter { p(it.first) }.map { (edge, outV) -> ParserResult(EdgeState(gr, edge, outV), edge) }
    }
}


