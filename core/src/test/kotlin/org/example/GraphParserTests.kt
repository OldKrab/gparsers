package org.example

import org.example.TestParsers.edge
import org.example.TestParsers.many
import org.example.TestParsers.or
import org.example.TestParsers.outV
import org.example.TestParsers.seq
import org.example.TestParsers.seqr
import org.example.TestParsers.that
import org.example.TestParsers.v
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

data class SimpleValue(val value: String)
data class SimpleEdge(val label: String)

class SimpleGraph : Graph<SimpleValue, SimpleEdge> {
    private val vertexesEdges = HashMap<Vertex<SimpleValue>, MutableList<Edge<SimpleEdge>>>()
    private val edgeVertexes = HashMap<Edge<SimpleEdge>, Pair<Vertex<SimpleValue>, Vertex<SimpleValue>>>()
    private val vertexes = HashSet<Vertex<SimpleValue>>()

    fun addVertex(value: SimpleValue): Vertex<SimpleValue> {
        val v = Vertex(value)
        vertexes.add(v)
        return v
    }

    fun addEdge(u: SimpleValue, label: SimpleEdge, v: SimpleValue) {
        val vU = addVertex(u)
        val vV = addVertex(v)
        val edge = Edge(label)
        vertexesEdges.getOrPut(vU) { ArrayList() }.add(edge)
        edgeVertexes[edge] = Pair(vU, vV)
    }

    override fun getEdges(v: Vertex<SimpleValue>): List<Edge<SimpleEdge>>? = vertexesEdges[v]
    override fun getVertexes(): Set<Vertex<SimpleValue>> = vertexes

    override fun getEdgeVertexes(e: Edge<SimpleEdge>): Pair<Vertex<SimpleValue>, Vertex<SimpleValue>>? =
        edgeVertexes[e]
}


object TestParsers : GraphParsers<SimpleGraph, SimpleValue, SimpleEdge>

class GraphParserTests {


    @Test
    fun simpleNode() {
        val nA = SimpleValue("A")
        val gr = SimpleGraph().apply {
            val eB = SimpleEdge("B")
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" }
        assertEquals(listOf(nA), gr.applyParser(parser).toList())
    }

    @Test
    fun simpleNodeAndEdge() {
        val nA = SimpleValue("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" } seq edge { it.label == "B" }

        assertEquals(listOf(Pair(nA, eB)), gr.applyParser(parser).toList())
    }

    @Test
    fun or() {
        val nA = SimpleValue("A")
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
            val nA = SimpleValue("A")
            val eB = SimpleEdge("B")
            val eC = SimpleEdge("C")
            addEdge(nA, eB, nA)
            addEdge(nA, eC, nA)
        }

        val isA: (SimpleValue) -> Boolean = { it.value == "A" }
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
        val danV = SimpleValue("Dan")
        val friendE = SimpleEdge("friend")
        val lindaV = SimpleValue("Linda")
        val gr = SimpleGraph().apply {
            val loves = SimpleEdge("loves")
            val john = SimpleValue("John")
            val mary = SimpleValue("Mary")
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

}