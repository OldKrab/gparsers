package org.parser.combinators.graph

import org.parser.combinators.*
import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage

/**
 * Contains parser combinators for graphs.
 *
 * You should instantiate this interface with your vertex and edge types.
 */
interface GraphCombinators<V, E> {
    /** Creates predicate from function. Useful when you don't want to write type label of [V]  */
    fun vPred(f: (V) -> Boolean): (V) -> Boolean = f

    /** Creates predicate from function. Useful when you don't want to write type label of [E]  */
    fun ePred(f: (E) -> Boolean): (E) -> Boolean = f

    /** Returns epsilon parser that accept [EdgeState]. */
    fun edgeEps(): BaseParser<EdgeState<V, E>, EdgeState<V, E>, Unit> {
        return eps()
    }

    /** Returns epsilon parser that accept [VertexState]. */
    fun vertexEps(): BaseParser<VertexState<V, E>, VertexState<V, E>, Unit> {
        return eps()
    }

    /** Returns vertex parser that parses outgoing vertex of [EdgeState]'s edge if vertex match [p]. */
    fun outV(p: (V) -> Boolean = { true }): EVGraphParser<V, E, V> {
        return object : EVGraphParser<V, E, V>("outV") {
            override fun parse(sppf: SPPFStorage, inS: EdgeState<V, E>)
                    : ParserResult<NonPackedNode<EdgeState<V, E>, VertexState<V, E>, V>> {
                val gr = inS.gr
                val outV = gr.getEndEdgeVertex(inS.edge) ?: return ParserResult.failure()
                if (!p(outV)) return ParserResult.failure()
                return ParserResult.success(sppf.getTerminalNode(inS, VertexState(gr, outV), outV))
            }
        }
    }

    /** Returns vertex parser that parses incoming vertex of [EdgeState]'s edge if vertex match [p]. */
    fun inV(p: (V) -> Boolean = { true }): BaseParser<EdgeState<V, E>, VertexState<V, E>, V> {
        return object : EVGraphParser<V, E, V>("inV") {
            override fun parse(sppf: SPPFStorage, inS: EdgeState<V, E>)
                    : ParserResult<NonPackedNode<EdgeState<V, E>, VertexState<V, E>, V>> {
                val gr = inS.gr
                val inV = gr.getStartEdgeVertex(inS.edge) ?: return ParserResult.failure()
                if (!p(inV)) return ParserResult.failure()
                return ParserResult.success(sppf.getTerminalNode(inS, VertexState(gr, inV), inV))
            }
        }
    }

    /** Returns vertex parser that parses all vertexes of graph that match [p]. Parser accept [StartState]. */
    fun v(p: (V) -> Boolean = { true }): BaseParser<StartState<V, E>, VertexState<V, E>, V> {
        return object : BaseParser<StartState<V, E>, VertexState<V, E>, V>("v") {
            override fun parse(sppf: SPPFStorage, inS: StartState<V, E>)
                    : ParserResult<NonPackedNode<StartState<V, E>, VertexState<V, E>, V>> {
                val gr = inS.gr
                return gr.getVertexes().filter { p(it) }.map {
                    ParserResult.success(sppf.getTerminalNode(inS, VertexState(gr, it), it))
                }.fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
            }
        }
    }

    /** Returns edge parser that parses all edges of graph that match [p]. Parser accept [StartState]. */
    fun edge(p: (E) -> Boolean = { true }): BaseParser<StartState<V, E>, EdgeState<V, E>, E> {
        return object : BaseParser<StartState<V, E>, EdgeState<V, E>, E>("edge") {
            override fun parse(sppf: SPPFStorage, inS: StartState<V, E>)
                    : ParserResult<NonPackedNode<StartState<V, E>, EdgeState<V, E>, E>> {
                val gr = inS.gr
                return gr.getEdges().filter { p(it) }.map {
                    ParserResult.success(sppf.getTerminalNode(inS, EdgeState(gr, it), it))
                }.fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
            }
        }
    }

    private fun edgesParserResult(
        edges: List<E>, sppf: SPPFStorage, vState: VertexState<V, E>, p: (E) -> Boolean
    ): ParserResult<NonPackedNode<VertexState<V, E>, EdgeState<V, E>, E>> {
        val gr = vState.gr
        return edges.filter { e -> p(e) }
            .map { e -> ParserResult.success(sppf.getTerminalNode(vState, EdgeState(gr, e), e)) }
            .fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
    }

    /** Returns edge parser that parses all outgoing edges from a vertex in the [VertexState] that match [p]. */
    fun outE(p: (E) -> Boolean = { true }): BaseParser<VertexState<V, E>, EdgeState<V, E>, E> {
        return object : VEGraphParser<V, E, E>("inV") {
            override fun parse(sppf: SPPFStorage, inS: VertexState<V, E>)
                    : ParserResult<NonPackedNode<VertexState<V, E>, EdgeState<V, E>, E>> {
                val gr = inS.gr
                val edges = gr.getOutgoingEdges(inS.v) ?: return ParserResult.failure()
                return edgesParserResult(edges, sppf, inS, p)
            }
        }
    }

    /** Returns edge parser that parses all incoming edges to a vertex in the [VertexState] that match [p]. */
    fun inE(p: (E) -> Boolean = { true }): BaseParser<VertexState<V, E>, EdgeState<V, E>, E> {
        return object : VEGraphParser<V, E, E>("inV") {
            override fun parse(sppf: SPPFStorage, inS: VertexState<V, E>)
                    : ParserResult<NonPackedNode<VertexState<V, E>, EdgeState<V, E>, E>> {
                val gr = inS.gr
                val edges = gr.getIncomingEdges(inS.v) ?: return ParserResult.failure()
                return edgesParserResult(edges, sppf, inS, p)
            }
        }
    }

    fun throughOutE(p: (E) -> Boolean = { true }): BaseParser<VertexState<V, E>, VertexState<V, E>, E> {
        return object : VVGraphParser<V, E, E>("inV") {
            override fun parse(sppf: SPPFStorage, inS: VertexState<V, E>)
                    : ParserResult<NonPackedNode<VertexState<V, E>, VertexState<V, E>, E>> {
                val gr = inS.gr
                val edges = gr.getOutgoingEdges(inS.v) ?: return ParserResult.failure()
                return edges.filter { e -> p(e) }.map { e ->
                    val outV = gr.getEndEdgeVertex(e)!!
                    ParserResult.success(sppf.getTerminalNode(inS, VertexState(gr, outV), e))
                }.fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
            }
        }
    }

    fun throughInE(p: (E) -> Boolean = { true }): BaseParser<VertexState<V, E>, VertexState<V, E>, E> {
        return object : VVGraphParser<V, E, E>("inV") {
            override fun parse(sppf: SPPFStorage, inS: VertexState<V, E>)
                    : ParserResult<NonPackedNode<VertexState<V, E>, VertexState<V, E>, E>> {
                val gr = inS.gr
                val edges = gr.getIncomingEdges(inS.v) ?: return ParserResult.failure()
                return edges.filter { e -> p(e) }.map { e ->
                    val inV = gr.getStartEdgeVertex(e)!!
                    ParserResult.success(sppf.getTerminalNode(inS, VertexState(gr, inV), e))
                }.fold(ParserResult.failure()) { acc, cur -> acc.orElse { cur } }
            }
        }
    }


}


