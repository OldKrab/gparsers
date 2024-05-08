package org.parser.combinators.graph

import org.parser.combinators.Parser
import org.parser.sppf.NonPackedNode

/**
 * Base interface for graphs that can be parsed.
 */
interface Graph<V, E> {
    /** Returns outgoing edges of vertex [v] if vertex present. */
    fun getEdges(v: V): List<E>?

    /** Returns all vertexes of graph. */
    fun getVertexes(): Iterable<V>

    /** Returns start and end vertexes of edge [e] if edge present. */
    fun getEdgeVertexes(e: E): Pair<V, V>?

    /**
     * Applies parser to graph from every vertex. Parser should accept state [StartState].
     * @return list of [NonPackedNode]. */
    fun <O, R> applyParser(parser: Parser<StartState<V, E>, O, R>): List<NonPackedNode<StartState<V, E>, O, R>> {
        return parser.parseState(StartState(this))
    }
}
