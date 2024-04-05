package org.graphparser

import org.graphparser.TestCombinators.edge
import org.graphparser.TestCombinators.outV

import org.graphparser.TestCombinators.v
import org.graphparser.TestCombinators.vertexEps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.parser.combinators.graph.*
import org.parser.combinators.*

private data class SimpleVertex(val value: String) {
    override fun toString(): String {
        return "V(\"$value\")"
    }
}

private data class SimpleEdge(val label: String) {
    override fun toString(): String {
        return "E(\"$label\")"
    }
}

private class SimpleGraph : Graph<SimpleVertex, SimpleEdge> {
    private val vertexesEdges = HashMap<SimpleVertex, MutableList<SimpleEdge>>()
    private val edgeVertexes = HashMap<SimpleEdge, Pair<SimpleVertex, SimpleVertex>>()
    private val vertexes = HashSet<SimpleVertex>()

    fun addVertex(value: SimpleVertex): SimpleVertex {
        vertexes.add(value)
        return value
    }

    fun addEdge(u: SimpleVertex, e: SimpleEdge, v: SimpleVertex) {
        addVertex(u)
        addVertex(v)
        vertexesEdges.getOrPut(v) { ArrayList() }.add(e)
        edgeVertexes[e] = Pair(u, v)
    }

    override fun getEdges(v: SimpleVertex): List<SimpleEdge>? = vertexesEdges[v]
    override fun getVertexes(): Set<SimpleVertex> = vertexes
    override fun getEdgeVertexes(e: SimpleEdge): Pair<SimpleVertex, SimpleVertex>? = edgeVertexes[e]
}

private object TestCombinators : GraphCombinators<SimpleVertex, SimpleEdge>

class GraphParserTests : ParserTests() {
    @Test
    fun simpleNode() {
        val nA = SimpleVertex("A")
        val nB = SimpleVertex("B")
        val eC = SimpleEdge("C")
        val gr = SimpleGraph().apply {
            addVertex(nA)
            addVertex(nB)
            addEdge(nA, eC, nB)
        }

        val parser = v { it.value == "A" }
        val nodes = gr.applyParser(parser)
        saveDotsToFolder(nodes, "simpleNode")

        assertEquals(1, nodes.size)
        val results = nodes[0].getResults()
        assertEquals(setOf(nA), results.toSet())
    }

    @Test
    fun simpleNodeAndEdge() {
        val nA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" } seq edge { it.label == "B" }

        val nodes = gr.applyParser(parser)
        saveDotsToFolder(nodes, "simpleNodeAndEdge")

        assertEquals(1, nodes.size)
        val results = nodes[0].getResults()
        assertEquals(setOf(Pair(nA, eB)), results.toSet())
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

        val nodes = gr.applyParser(parser)
        saveDotsToFolder(nodes, "or")

        val results = nodes.map { it.getResults().toList() }.onEach { assertEquals(1, it.size) }.map { it[0] }

        assertEquals(setOf(Pair(nA, eB), Pair(nA, eC)), results.toSet())
    }


    @Test
    fun many() {
        val vA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val eC = SimpleEdge("C")
        val gr = SimpleGraph().apply {
            addEdge(vA, eB, vA)
            addEdge(vA, eC, vA)
        }

        val isA: (SimpleVertex) -> Boolean = { it.value == "A" }
        val edgeB = edge { it.label == "B" }
        val edgeC = edge { it.label == "C" }

        val p = v(isA) seq ((edgeB or edgeC) seq outV(isA)).many
        val nodes = gr.applyParser(p)
        saveDotsToFolder(nodes, "many")

        assertEquals(1, nodes.size)
        val results = nodes[0].getResults().take(7).toSet()
        val rests = results.onEach {
            assertEquals(vA, it.first)
        }.map {
            it.second
        }
        assertEquals(
            setOf(
                listOf(),
                listOf(Pair(eB, vA)),
                listOf(Pair(eC, vA)),
                listOf(Pair(eB, vA), Pair(eB, vA)),
                listOf(Pair(eB, vA), Pair(eC, vA)),
                listOf(Pair(eC, vA), Pair(eB, vA)),
                listOf(Pair(eC, vA), Pair(eC, vA)),
            ), rests.toSet()
        )
    }

    @Test
    fun loopWithManyWithStartState() {
        val vA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(vA, eB, vA)
        }

        val vertexA = outV { it.value == "A" }
        val edgeB = edge { it.label == "B" }
        vertexA.view = "vA" // for debug
        edgeB.view = "eB"

        val x = v { it.value == "A" } seq (edgeB seq vertexA).many
        val nodes = gr.applyParser(x)
        saveDotsToFolder(nodes, "loopWithManyWithStartState")

        assertEquals(1, nodes.size)
        val results = nodes[0].getResults().take(3).toSet()
        val rests = results.onEach {
            assertEquals(vA, it.first)
        }.map { it.second }
        assertEquals(
            setOf(
                listOf(),
                listOf(Pair(eB, vA)),
                listOf(Pair(eB, vA), Pair(eB, vA)),
            ), rests.toSet()
        )
    }

    @Test
    fun example1() {
        v { it.value == "Dan" } seqr edge { it.label == "loves" } seqr outV()
    }

    @Test
    fun loopWithFix() {
        val vA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(vA, eB, vA)
        }

        val vertexA = outV { it.value == "A" }
        val edgeB = edge { it.label == "B" }
        vertexA.view = "vA" // for debug
        edgeB.view = "eB"
        val s =
            fix("x") { s ->
                rule(
                    vertexEps() using { _ -> emptyList() },
                    (edgeB seq vertexA seq s) using { e, v, rest: List<Pair<SimpleEdge, SimpleVertex>> ->
                        listOf(Pair(e, v)) + rest
                    },
                )
            }
        val nodes = s.parseState(VertexState(gr, SimpleVertex("A")))
        saveDotsToFolder(nodes, "loopWithFix")

        assertEquals(1, nodes.size)
        val results = nodes[0].getResults().take(3).toSet()
        assertEquals(
            setOf(
                listOf(),
                listOf(Pair(eB, vA)),
                listOf(Pair(eB, vA), Pair(eB, vA)),
            ), results.toSet()
        )
    }

    @Test
    fun loopWithMany() {
        val vA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(vA, eB, vA)
        }

        val vertexA = outV { it.value == "A" }
        val edgeB = edge { it.label == "B" }
        vertexA.view = "vA"
        edgeB.view = "eB"
        val s = (edgeB seq vertexA).many

        val nodes = s.parseState(VertexState(gr, SimpleVertex("A")))
        saveDotsToFolder(nodes, "loopWithMany")

        assertEquals(1, nodes.size)
        val results = nodes[0].getResults().take(3).toSet()
        assertEquals(
            setOf(
                listOf(),
                listOf(Pair(eB, vA)),
                listOf(Pair(eB, vA), Pair(eB, vA)),
            ), results.toSet()
        )
    }


//    @Test
//    fun withThat() {
//        val danV = SimpleVertex("Dan")
//        val friendE = SimpleEdge("friend")
//        val lindaV = SimpleVertex("Linda")
//        val gr = SimpleGraph().apply {
//            val loves = SimpleEdge("loves")
//            val john = SimpleVertex("John")
//            val mary = SimpleVertex("Mary")
//            addEdge(danV, friendE, john)
//            addEdge(danV, loves, mary)
//            addEdge(john, friendE, lindaV)
//        }
//
//        val person = v()
//        val mary = outV { it.value == "Mary" }
//        val loves = edge { it.label == "loves" }
//        val friend = edge { it.label == "friend" }
//        val maryLover = person.that(loves seq mary)
//        val parser = maryLover seq friend seq outV()
//        val res = gr.applyParser(parser).toList()
//        assertEquals(listOf(Pair(Pair(danV, friendE), lindaV)), res)
//    }

}