package org.parser.combinators.graph

import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.combinators.eps


interface GraphCombinators<V, E> {

    fun edgeEps(): Parser<EdgeState<V, E>, EdgeState<V, E>, Unit> {
        return eps()
    }

    fun vertexEps(): Parser<VertexState<V, E>, VertexState<V, E>, Unit> {
        return eps()
    }

    fun outV(p: (V) -> Boolean): Parser<EdgeState<V, E>, VertexState<V, E>, V> {
        return Parser.memo("outV") { sppf, edgeState ->
            val gr = edgeState.gr
            val (_, outV) = gr.getEdgeVertexes(edgeState.edge) ?: return@memo ParserResult.failure()
            if (!p(outV)) return@memo ParserResult.failure()
            ParserResult.success(sppf.getTerminalNode(edgeState, VertexState(gr, outV), outV))
        }
    }

    fun outV(): Parser<EdgeState<V, E>, VertexState<V, E>, V> = outV { true }

    fun v(p: (V) -> Boolean): Parser<StartState<V, E>, VertexState<V, E>, V> {
        return Parser.memo("v") { sppf, startState ->
            val gr = startState.gr
            gr.getVertexes()
                .filter { p(it) }
                .map {
                    ParserResult.success(sppf.getTerminalNode(startState, VertexState(gr, it), it))
                }
                .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
        }
    }

    fun v(): Parser<StartState<V, E>, VertexState<V, E>, V> = v { true }

    fun edge(p: (E) -> Boolean): Parser<VertexState<V, E>, EdgeState<V, E>, E> {
        return Parser.memo("edge") { sppf, vState ->
            val gr = vState.gr
            val edges = gr.getEdges(vState.v) ?: return@memo ParserResult.failure()
            edges
                .filter { e -> p(e) }
                .map { e -> ParserResult.success(sppf.getTerminalNode(vState, EdgeState(gr, e), e)) }
                .fold(ParserResult.failure())  { acc, cur -> acc.orElse { cur } }
        }
    }
}


