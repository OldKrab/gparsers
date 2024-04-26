package org.parser

import org.parser.TestCombinators.edge
import org.parser.TestCombinators.outV

import org.parser.TestCombinators.v
import org.parser.TestCombinators.vertexEps
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
        vertexesEdges.getOrPut(u) { ArrayList() }.add(e)
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

        val nodeA by v { it.value == "A" }
        val edgeB by edge { it.label == "B" }
        val edgeC by edge { it.label == "C" }

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
        val edgeB by edge { it.label == "B" }
        val edgeC by edge { it.label == "C" }
        val vertexA by outV(isA)
        val startVertexA by v(isA)

        val p by startVertexA seq ((edgeB or edgeC) seq vertexA).many
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
        val dan = SimpleVertex("Dan")
        val mary = SimpleVertex("Mary")
        val john = SimpleVertex("John")
        val gr = SimpleGraph().apply {
            addEdge(dan, SimpleEdge("loves"), mary)
            addEdge(dan, SimpleEdge("friend"), john)
        }
        val p = v { it.value == "Dan" } seqr edge { it.label == "loves" } seqr outV()
        val nodes = gr.applyParser(p)
        saveDotsToFolder(nodes, "example1")
        assertEquals(1, nodes.size)
        assertEquals(setOf(mary), nodes[0].getResults().toSet())
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
    fun testStackOverflow() {
        val gr = SimpleGraph().apply {
            var prev = SimpleVertex("0")
            for (i in 1..300) {
                val cur = SimpleVertex(i.toString())
                val e = SimpleEdge("e$i")
                addEdge(prev, e, cur)
                prev = cur
            }
        }

        val s =
            fix("x") { s ->
                rule(
                    vertexEps() using { _ -> emptyList() },
                    (edge { true } seq outV() seq s) using { e, v, rest: List<Pair<SimpleEdge, SimpleVertex>> ->
                        listOf(Pair(e, v)) + rest
                    },
                )
            }
        val nodes = s.parseState(VertexState(gr, SimpleVertex("0")))
//        saveDotsToFolder(nodes, "testStackOverflow")
//
//        val result = nodes[0].getResults().drop(10).first()

    }

    @Test
    fun loopLeftRec() {
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
            fix("s") { s ->
                rule(
                    (s seq edgeB seq vertexA) using { prefix: List<Pair<SimpleEdge, SimpleVertex>>, e, v ->
                        prefix + Pair(e, v)
                    },
                    vertexEps() using { _ -> emptyList() },
                )
            }
        val nodes = s.parseState(VertexState(gr, SimpleVertex("A")))
        saveDotsToFolder(nodes, "loopLeftRec")

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
    fun loopLeftRec2() {
        val vA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val eC = SimpleEdge("C")
        val gr = SimpleGraph().apply {
            addEdge(vA, eB, vA)
            addEdge(vA, eC, vA)
        }

        val vertexA by outV { it.value == "A" }
        val edgeB by edge { it.label == "B" }
        val edgeC by edge { it.label == "C" }
        val s1: VVGraphParser<SimpleVertex, SimpleEdge, List<Pair<SimpleEdge, SimpleVertex>>>
                by fix("s1") { s1 ->
                    rule(
                        (s1 seq edgeB seq vertexA) using { prefix, e, v -> prefix + Pair(e, v) },
                        vertexEps() using { _ -> emptyList() }
                    )
                }
        val s2: VVGraphParser<SimpleVertex, SimpleEdge, List<Pair<SimpleEdge, SimpleVertex>>>
                by fix("s2") { s2 ->
                    rule(
                        (s2 seq edgeC seq vertexA) using { prefix, e, v -> prefix + Pair(e, v) },
                        vertexEps() using { _ -> emptyList() }
                    )
                }
        val s by s1 or s2

        val nodes = s.parseState(VertexState(gr, vA))
        saveDotsToFolder(nodes, "loopLeftRec2")

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

        val vertexA by outV { it.value == "A" }
        val edgeB by edge { it.label == "B" }
        vertexA.view = "vA"
        edgeB.view = "eB"
        val s by (edgeB seq vertexA).many

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