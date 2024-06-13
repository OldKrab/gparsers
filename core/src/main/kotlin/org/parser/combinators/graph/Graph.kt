package org.parser.combinators.graph

import org.parser.combinators.BaseParser
import org.parser.combinators.Parser
import org.parser.sppf.NonPackedNode

/**
 * Base interface for graphs that can be parsed.
 */
interface Graph<V, E> {
    /** Returns outgoing edges of vertex [v] if vertex present. */
    fun getOutgoingEdges(v: V): List<E>?

    /** Returns incoming edges of vertex [v] if vertex present. */
    fun getIncomingEdges(v: V): List<E>?

    /** Returns all vertexes of graph. */
    fun getVertexes(): Iterable<V>

    /** Returns all edges of graph. */
    fun getEdges(): Iterable<E>

    /** Returns start vertex of edge [e] if edge present. */
    fun getStartEdgeVertex(e: E): V?

    /** Returns end vertex of edge [e] if edge present. */
    fun getEndEdgeVertex(e: E): V?

    /**
     * Applies parser to graph from every vertex. Parser should accept state [StartState].
     * @return list of [NonPackedNode]. */
    fun <O, R> applyParser(parser: BaseParser<StartState<V, E>, O, R>): List<NonPackedNode<StartState<V, E>, O, R>> {
        return parser.parseState(StartState(this))
    }

    /**
     * Applies parser to graph from every vertex. Parser should accept state [StartState].
     * @return list of [NonPackedNode]. */
    fun <O, R> applyParserForResults(parser: BaseParser<StartState<V, E>, O, R>): Sequence<R> {
        val nodes =  parser.parseState(StartState(this))
        return nodes.asSequence().flatMap { it.getResults() }
    }
}
