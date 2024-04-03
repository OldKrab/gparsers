package org.graphparser

import org.graphparser.TestCombinators.edge
import org.graphparser.TestCombinators.edgeEps
import org.graphparser.TestCombinators.eps
import org.graphparser.TestCombinators.fail
import org.graphparser.TestCombinators.fix
import org.graphparser.TestCombinators.many
import org.graphparser.TestCombinators.or
import org.graphparser.TestCombinators.outV
import org.graphparser.TestCombinators.rule
import org.graphparser.TestCombinators.seq
import org.graphparser.TestCombinators.seqr
import org.graphparser.TestCombinators.success
import org.graphparser.TestCombinators.using
import org.graphparser.TestCombinators.v
import org.graphparser.TestCombinators.vertexEps
import org.junit.jupiter.api.Test
import org.parser.combinators.applyParser
import org.parser.combinators.graph.*
import org.parser.sppf.Visualizer
import java.nio.file.Path
import kotlin.io.path.createDirectories

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
        vertexesEdges.getOrPut(v) { ArrayList() }.add(e)
        edgeVertexes[e] = Pair(u, v)
    }

    override fun getEdges(v: SimpleVertex): List<SimpleEdge>? = vertexesEdges[v]
    override fun getVertexes(): Set<SimpleVertex> = vertexes

    override fun getEdgeVertexes(e: SimpleEdge): Pair<SimpleVertex, SimpleVertex>? =
        edgeVertexes[e]
}


private object TestCombinators : GraphCombinators<SimpleGraph, SimpleVertex, SimpleEdge>

class GraphParserTests {


    @Test
    fun simpleNode() {
        val nA = SimpleVertex("A")
        val gr = SimpleGraph().apply {
            val eB = SimpleEdge("B")
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" }
        val results = gr.applyParser(parser)
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("simpleNode").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
    }

    @Test
    fun simpleNodeAndEdge() {
        val nA = SimpleVertex("A")
        val eB = SimpleEdge("B")
        val gr = SimpleGraph().apply {
            addEdge(nA, eB, nA)
        }

        val parser = v { it.value == "A" } seq edge { it.label == "B" }

        val results = gr.applyParser(parser)
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("simpleNodeAndEdge").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
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

        val results = gr.applyParser(parser)
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("or").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
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
        val results = gr.applyParser(p)
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("many").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
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

        val vA = outV { it.value == "A" }
        vA.name = "vA"
        val edgeB = edge { it.label == "B" }
        edgeB.name = "eB"
        val x =
            fix("x") { x ->

                rule(
                    (edgeB seq vA seq x) using { _ -> },
                    vertexEps()
                )
            }
        val results = applyParser(gr, x, VertexState(SimpleVertex("A")))
        results.forEach { println(it) }
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("loop").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
    }

    @Test
    fun loopWithMany() {
        val gr = SimpleGraph().apply {
            val nA = SimpleVertex("A")
            val eB = SimpleEdge("B")
            addEdge(nA, eB, nA)
        }

        val vA = outV { it.value == "A" }
        vA.name = "vA"
        val edgeB = edge { it.label == "B" }
        edgeB.name = "eB"
        val x = (edgeB seq vA).many using { _ -> }
        val results = applyParser(gr, x, VertexState(SimpleVertex("A")))
        results.forEach { println(it) }
        val dir = Path.of(System.getProperty("java.io.tmpdir")).resolve("loop").createDirectories()
        for (i in results.indices) {
            Visualizer().toDotFile(results[i], dir.resolve("$i.dot"))
        }
        println("Look images in '$dir'")
    }

}