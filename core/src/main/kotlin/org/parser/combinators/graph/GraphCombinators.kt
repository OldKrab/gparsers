package org.parser.combinators.graph

import org.parser.combinators.*
import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage

/**
 * Contains parser combinators for graphs.
 *
 * You should instantiate this interface with your vertex and edge types.
 * @sample org.parser.samples.SimpleCombinators
 */
interface GraphCombinators<V, E> {
    /** Returns epsilon parser that accept [EdgeState]. */
    fun edgeEps(): BaseParser<EdgeState<V, E>, EdgeState<V, E>, Unit> {
        return eps()
    }

    /** Returns epsilon parser that accept [VertexState]. */
    fun vertexEps(): BaseParser<VertexState<V, E>, VertexState<V, E>, Unit> {
        return eps()
    }

    /** Returns vertex parser that parses outgoing vertex of [EdgeState]'s edge if vertex match [p]. */
    fun outV(p: (V) -> Boolean = { true }): BaseParser<EdgeState<V, E>, VertexState<V, E>, V> {
        return Parser.new("outV") { sppf, edgeState ->
            val gr = edgeState.gr
            val outV = gr.getEndEdgeVertex(edgeState.edge) ?: return@new ParserResult.failure()
            if (!p(outV)) return@new ParserResult.failure()
            ParserResult.success(sppf.getTerminalNode(edgeState, VertexState(gr, outV), outV))
        }
    }

    /** Returns vertex parser that parses incoming vertex of [EdgeState]'s edge if vertex match [p]. */
    fun inV(p: (V) -> Boolean = { true }): BaseParser<EdgeState<V, E>, VertexState<V, E>, V> {
        return Parser.new("inV") { sppf, edgeState ->
            val gr = edgeState.gr
            val inV = gr.getStartEdgeVertex(edgeState.edge) ?: return@new ParserResult.failure()
            if (!p(inV)) return@new ParserResult.failure()
            ParserResult.success(sppf.getTerminalNode(edgeState, VertexState(gr, inV), inV))
        }
    }

    /** Returns vertex parser that parses all vertexes of graph that match [p]. Parser accept [StartState]. */
    fun v(p: (V) -> Boolean = { true }): BaseParser<StartState<V, E>, VertexState<V, E>, V> {
        return Parser.new("v") { sppf, startState ->
            val gr = startState.gr
            gr.getVertexes()
                .filter { p(it) }
                .map {
                    ParserResult.success(sppf.getTerminalNode(startState, VertexState(gr, it), it))
                }
                .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
        }
    }

    /** Returns edge parser that parses all edges of graph that match [p]. Parser accept [StartState]. */
    fun edge(p: (E) -> Boolean = { true }): BaseParser<StartState<V, E>, EdgeState<V, E>, E> {
        return Parser.new("v") { sppf, startState ->
            val gr = startState.gr
            gr.getEdges()
                .filter { p(it) }
                .map {
                    ParserResult.success(sppf.getTerminalNode(startState, EdgeState(gr, it), it))
                }
                .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
        }
    }

    private fun edgesParserResult(
        edges: List<E>,
        sppf: SPPFStorage,
        vState: VertexState<V, E>,
        p: (E) -> Boolean
    ): ParserResult<NonPackedNode<VertexState<V, E>, EdgeState<V, E>, E>> {
        val gr = vState.gr
        return edges
            .filter { e -> p(e) }
            .map { e -> ParserResult.success(sppf.getTerminalNode(vState, EdgeState(gr, e), e)) }
            .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
    }

    /** Returns edge parser that parses all outgoing edges from a vertex in the [VertexState] that match [p]. */
    fun outE(p: (E) -> Boolean = { true }): BaseParser<VertexState<V, E>, EdgeState<V, E>, E> {
        return Parser.new("outE") { sppf, vState ->
            val gr = vState.gr
            val edges = gr.getOutgoingEdges(vState.v) ?: return@new ParserResult.failure()
            edgesParserResult(edges, sppf, vState, p)
        }
    }

    /** Returns edge parser that parses all incoming edges to a vertex in the [VertexState] that match [p]. */
    fun inE(p: (E) -> Boolean = { true }): BaseParser<VertexState<V, E>, EdgeState<V, E>, E> {
        return Parser.new("inE") { sppf, vState ->
            val gr = vState.gr
            val edges = gr.getIncomingEdges(vState.v) ?: return@new ParserResult.failure()
            edgesParserResult(edges, sppf, vState, p)
        }
    }
}


