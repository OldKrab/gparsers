package org.parser.test.graph

import org.parser.combinators.graph.*
import java.util.*
import kotlin.collections.ArrayList


class TestVertex(val value: String) {
    override fun toString(): String {
        return "V(\"$value\")"
    }
}

class TestEdge(val label: String) {
    override fun toString(): String {
        return "E(\"$label\")"
    }
}

class TestGraph : Graph<TestVertex, TestEdge> {
    private val vertexesOutEdges = IdentityHashMap<TestVertex, MutableList<TestEdge>>()
    private val vertexesInEdges = IdentityHashMap<TestVertex, MutableList<TestEdge>>()
    private val edgeVertexes = IdentityHashMap<TestEdge, Pair<TestVertex, TestVertex>>()
    private val vertexes: MutableSet<TestVertex> = Collections.newSetFromMap(IdentityHashMap())

    fun addVertex(value: TestVertex): TestVertex {
        vertexes.add(value)
        return value
    }

    fun addEdge(u: TestVertex, e: TestEdge, v: TestVertex) {
        addVertex(u)
        addVertex(v)
        vertexesOutEdges.getOrPut(u) { ArrayList() }.add(e)
        vertexesInEdges.getOrPut(v) { ArrayList() }.add(e)
        edgeVertexes[e] = Pair(u, v)
    }

    fun addEdge(u: TestVertex, e: String, v: TestVertex) {
        addEdge(u, TestEdge(e), v)
    }

    override fun getOutgoingEdges(v: TestVertex): List<TestEdge>? = vertexesOutEdges[v]
    override fun getIncomingEdges(v: TestVertex): List<TestEdge>? = vertexesInEdges[v]

    override fun getVertexes(): Set<TestVertex> = vertexes
    override fun getEdges(): Iterable<TestEdge> = edgeVertexes.keys
    override fun getEndEdgeVertex(e: TestEdge): TestVertex? = edgeVertexes[e]?.second
    override fun getStartEdgeVertex(e: TestEdge): TestVertex? = edgeVertexes[e]?.first
}


object TestCombinators : GraphCombinators<TestVertex, TestEdge>
typealias TestVertexState = VertexState<TestVertex, TestEdge>
typealias TestEdgeState = EdgeState<TestVertex, TestEdge>
typealias TestStartState = StartState<TestVertex, TestEdge>
