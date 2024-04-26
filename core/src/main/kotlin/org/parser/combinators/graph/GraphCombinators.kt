package org.parser.combinators.graph

import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.combinators.eps

/**
 * Contains parser combinators for graphs.
 *
 * You should instantiate this interface with your vertex and edge types. 
 * @sample org.parser.samples.SimpleCombinators
 */
interface GraphCombinators<V, E> {

    /** Returns epsilon parser that accept [EdgeState]. */
    fun edgeEps(): Parser<EdgeState<V, E>, EdgeState<V, E>, Unit> {
        return eps()
    }

    /** Returns epsilon parser that accept [VertexState]. */
    fun vertexEps(): Parser<VertexState<V, E>, VertexState<V, E>, Unit> {
        return eps()
    }

    /** Returns vertex parser that parses outgoing vertex of [EdgeState]'s edge if vertex match [p]. */
    fun outV(p: (V) -> Boolean = { true }): Parser<EdgeState<V, E>, VertexState<V, E>, V> {
        return Parser.memo("outV") { sppf, edgeState ->
            val gr = edgeState.gr
            val (_, outV) = gr.getEdgeVertexes(edgeState.edge) ?: return@memo ParserResult.failure()
            if (!p(outV)) return@memo ParserResult.failure()
            ParserResult.success(sppf.getTerminalNode(edgeState, VertexState(gr, outV,), outV))
        }
    }

    /** Returns vertex parser that parses all vertexes of graph that match [p]. Parser accept [StartState]. */
    fun v(p: (V) -> Boolean = { true }): Parser<StartState<V, E>, VertexState<V, E>, V> {
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


    /** Returns edge parser that parses all outgoing edges from a vertex in the [VertexState] that match [p]. */
    fun edge(p: (E) -> Boolean): Parser<VertexState<V, E>, EdgeState<V, E>, E> {
        return Parser.memo("edge") { sppf, vState ->
            val gr = vState.gr
            val edges = gr.getEdges(vState.v) ?: return@memo ParserResult.failure()
            edges
                .filter { e -> p(e) }
                .map { e -> ParserResult.success(sppf.getTerminalNode(vState, EdgeState(gr, e), e)) }
                .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
        }
    }
}


