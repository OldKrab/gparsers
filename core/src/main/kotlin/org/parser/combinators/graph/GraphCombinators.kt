package org.parser.combinators.graph

import org.parser.combinators.Combinators
import org.parser.combinators.Parser
import org.parser.combinators.ParserResult


interface GraphCombinators<G : Graph<V, E>, V, E> : Combinators<G> {

    fun edgeEps(): Parser<G, EdgeState<E>, EdgeState<E>, Unit> {
        return eps()
    }

    fun vertexEps(): Parser<G, VertexState<V>, VertexState<V>, Unit> {
        return eps()
    }

    fun outV(p: (V) -> Boolean): Parser<G, EdgeState<E>, VertexState<V>, V> {
        return Parser.memo("outV") { gr, sppf, edgeState ->
            val (_, outV) = gr.getEdgeVertexes(edgeState.edge) ?: return@memo ParserResult.failure()
            if (!p(outV)) return@memo ParserResult.failure()
            ParserResult.success(sppf.getTerminalNode(edgeState, VertexState(outV), outV))
        }
    }

    fun outV(): Parser<G, EdgeState<E>, VertexState<V>, V> = outV { true }

    fun v(p: (V) -> Boolean): Parser<G, StartState, VertexState<V>, V> {
        return Parser.memo("v") { gr, sppf, startState ->
            gr.getVertexes()
                .filter { p(it) }
                .map {
                    ParserResult.success(sppf.getTerminalNode(startState, VertexState(it), it))
                }
                .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
        }
    }

    fun v(): Parser<G, StartState, VertexState<V>, V> = v { true }

    fun edge(p: (E) -> Boolean): Parser<G, VertexState<V>, EdgeState<E>, E> {
        return Parser.memo("edge") { gr, sppf, vState ->
            val edges = gr.getEdges(vState.v) ?: return@memo ParserResult.failure()
            edges
                .filter { e -> p(e) }
                .map { e -> ParserResult.success(sppf.getTerminalNode(vState, EdgeState(e), e)) }
                .fold(ParserResult.failure())  { acc, cur -> acc.orElse { cur } }
        }
    }
}


