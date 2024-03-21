package org.graphparser

import org.graphparser.TestParsers.edge
import org.graphparser.TestParsers.many
import org.graphparser.TestParsers.or
import org.graphparser.TestParsers.outV
import org.graphparser.TestParsers.seq
import org.graphparser.TestParsers.seqr
import org.graphparser.TestParsers.that
import org.graphparser.TestParsers.v
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private data class SimpleVertex(val value: String)
private data class SimpleEdge(val label: String)

private class SimpleGraph : Graph<SimpleVertex, SimpleEdge> {
    private val vertexesEdges = HashMap<Vertex<SimpleVertex>, MutableList<Edge<SimpleEdge>>>()
    private val edgeVertexes = HashMap<Edge<SimpleEdge>, Pair<Vertex<SimpleVertex>, Vertex<SimpleVertex>>>()
    private val vertexes = HashSet<Vertex<SimpleVertex>>()

    fun addVertex(value: SimpleVertex): Vertex<SimpleVertex> {
        val v = Vertex(value)
        vertexes.add(v)
        return v
    }

    fun addEdge(u: SimpleVertex, label: SimpleEdge, v: SimpleVertex) {
        val vU = addVertex(u)
        val vV = addVertex(v)
        val edge = Edge(label)
        vertexesEdges.getOrPut(vU) { ArrayList() }.add(edge)
        edgeVertexes[edge] = Pair(vU, vV)
    }

    override fun getEdges(v: Vertex<SimpleVertex>): List<Edge<SimpleEdge>>? = vertexesEdges[v]
    override fun getVertexes(): Set<Vertex<SimpleVertex>> = vertexes

    override fun getEdgeVertexes(e: Edge<SimpleEdge>): Pair<Vertex<SimpleVertex>, Vertex<SimpleVertex>>? =
        edgeVertexes[e]
}


private object TestParsers : GraphParsers<SimpleGraph, SimpleVertex, SimpleEdge>

class GraphParserTests {


    @Test
    fun simpleNode() {
        val nA = SimpleVertex("A")
        val gr = SimpleGraph().apply {
            val eB = SimpleEdge("B")
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" }
        assertEquals(listOf(nA), gr.applyParser(parser).toList())
    }

    @Test
    fun simpleNodeAndEdge() {
        val nA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" } seq edge { it.label == "B" }

        assertEquals(listOf(Pair(nA, eB)), gr.applyParser(parser).toList())
    }

    @Test
    fun or() {
        val nA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val eC = SimpleEdge("C")
        val gr = SimpleGraph().apply {
            addEdge(nA, eB, nA)
            addEdge(nA, eC, nA)
        }

        val nodeA = v { it.value == "A" }
        val edgeB = edge { it.label == "B" }
        val edgeC = edge { it.label == "C" }

        val parser = nodeA seq (edgeB or edgeC)

        assertEquals(setOf(Pair(nA, eB), Pair(nA, eC)), gr.applyParser(parser).toSet())
    }

    @Test
    fun many() {
        val gr = SimpleGraph().apply {
            val nA = SimpleVertex("A")
            val eB = SimpleEdge("B")
            val eC = SimpleEdge("C")
            addEdge(nA, eB, nA)
            addEdge(nA, eC, nA)
        }

        val isA: (SimpleVertex) -> Boolean = { it.value == "A" }
        val edgeB = edge { it.label == "B" }
        val edgeC = edge { it.label == "C" }

        val p = v(isA) seq ((edgeB or edgeC) seq outV(isA)).many
        val res = gr.applyParser(p, 20).toList()
        res.forEach { (first, seq) ->
            print("(${first.value})")
            println(seq.joinToString("") { (e, n) -> "-${e.label}->(${n.value})" })
        }
        assertEquals(20, res.size)
    }

    @Test
    fun withThat() {
        val danV = SimpleVertex("Dan")
        val friendE = SimpleEdge("friend")
        val lindaV = SimpleVertex("Linda")
        val gr = SimpleGraph().apply {
            val loves = SimpleEdge("loves")
            val john = SimpleVertex("John")
            val mary = SimpleVertex("Mary")
            addEdge(danV, friendE, john)
            addEdge(danV, loves, mary)
            addEdge(john, friendE, lindaV)
        }

        val person = v()
        val mary = outV { it.value == "Mary" }
        val loves = edge { it.label == "loves" }
        val friend = edge { it.label == "friend" }
        val maryLover = person.that(loves seq mary)
        val parser = maryLover seq friend seq outV()
        val res = gr.applyParser(parser).toList()
        assertEquals(listOf(Pair(Pair(danV, friendE), lindaV)), res)
    }

    @Test
    fun test1() {
        val p = v { it.value == "Dan" } seqr edge { it.label == "loves" } seqr outV()
    }

    @Test
    fun loop() {
        val gr = SimpleGraph().apply {
            val nA = SimpleVertex("A")
            val eB = SimpleEdge("B")
            addEdge(nA, eB, nA)
        }

        val isA: (SimpleVertex) -> Boolean = { it.value == "A" }
        val edgeB = edge { it.label == "B" }

        val p = v(isA) seq (edgeB seq outV(isA)).many
        val res = gr.applyParser(p)
        res.forEach { (first, seq) ->
            print("(${first.value})")
            println(seq.joinToString("") { (e, n) -> "-${e.label}->(${n.value})" })
        }
        assertEquals(20, res.size)
    }

}