package org.parser.combinators.graph

import org.parser.combinators.Combinators
import org.parser.combinators.Parser
import org.parser.combinators.ParserResult


interface GraphCombinators<G : Graph<V, E>, V, E> : Combinators<G> {

    fun outV(p: (V) -> Boolean): Parser<G, EdgeState<E>, VertexState<V>, V> {
        return Parser.make("outV") { gr, (edge) ->
            val (_, outV) = gr.getEdgeVertexes(edge) ?: return@make ParserResult.failure()
            if (!p(outV.value)) return@make ParserResult.failure()
            ParserResult.success(VertexState(outV), outV.value)
        }
    }

    fun outV(): Parser<G, EdgeState<E>, VertexState<V>, V> = outV { true }

    fun v(p: (V) -> Boolean): Parser<G, StartState, VertexState<V>, V> {
        return Parser.make("v") { gr, _ ->
            gr.getVertexes()
                .filter { p(it.value) }
                .map {
                    ParserResult.success(VertexState(it), it.value)
                }
                .reduce { acc, cur -> acc.orElse { cur } }
        }
    }

    fun v(): Parser<G, StartState, VertexState<V>, V> = v { true }

    fun edge(p: (E) -> Boolean): Parser<G, VertexState<V>, EdgeState<E>, E> {
        return Parser.make("edge") { gr, (v) ->
            val edges = gr.getEdges(v) ?: return@make ParserResult.failure()
            edges
                .filter { e -> p(e.label) }
                .map { e -> ParserResult.success(EdgeState(e), e.label) }
                .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
        }
    }
}


