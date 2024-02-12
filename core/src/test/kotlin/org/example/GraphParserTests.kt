package org.example

import org.junit.jupiter.api.Test

class GraphParserTests {

    data class SimpleVertex(val value: String)
    data class SimpleEdge(val label: String)
    class SimpleGraph : Graph<SimpleVertex, SimpleEdge>() {
        private val vertexesEdges = HashMap<SimpleVertex, MutableList<Pair<SimpleEdge, SimpleVertex>>>()
        private val vertexes = HashSet<SimpleVertex>()

        fun addVertex(v: SimpleVertex) {
            vertexes.add(v)
        }

        fun addEdge(u: SimpleVertex, edge: SimpleEdge, v: SimpleVertex) {
            addVertex(u)
            addVertex(v)
            vertexesEdges.getOrPut(u) { ArrayList() }.add(Pair(edge, v))
        }

        override fun getEdges(v: SimpleVertex): MutableList<Pair<SimpleEdge, SimpleVertex>>? = vertexesEdges[v]
        override fun getVertexes(): Set<SimpleVertex> = vertexes
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

        val nodeA = v<SimpleVertex, SimpleEdge> { it.value == "A" }
        val outNodeA = outV<SimpleVertex, SimpleEdge> { it.value == "A" }
        val edgeB = edge<SimpleVertex, SimpleEdge> { it.label == "B" }
        val edgeC = edge<SimpleVertex, SimpleEdge> { it.label == "C" }

        val p = nodeA seq ((edgeB or edgeC) seq outNodeA).many

        gr.applyParser(p)
            .take(20)
            .forEach { (first, seq) ->
                print("(${first.value})")
                println(seq.joinToString("") { (e, n) -> "-${e.label}->(${n.value})" })
            }
    }

    @Test
    fun withThat() {
        val gr = SimpleGraph().apply {
            val friend = SimpleEdge("friend")
            val loves = SimpleEdge("loves")
            val dan = SimpleVertex("Dan")
            val john = SimpleVertex("John")
            val mary = SimpleVertex("Mary")
            val linda = SimpleVertex("Linda")
            addEdge(dan, friend, john)
            addEdge(dan, loves, mary)
            addEdge(john, friend, linda)
        }

        val person = v<SimpleVertex, SimpleEdge>()
        val mary = outV<SimpleVertex, SimpleEdge> { it.value == "Mary" }
        val loves = edge<SimpleVertex, SimpleEdge> { it.label == "loves" }
        val friend = edge<SimpleVertex, SimpleEdge> { it.label == "friend" }
        val maryLover = person.that(loves seq mary)
        val parser = maryLover seq friend seq outV()

        gr.applyParser(parser)
            .forEach { println(it) }
    }

    @Test
    fun test1() {
        val p = v<SimpleVertex, SimpleEdge> { it.value == "Dan" } seqr edge { it.label == "loves" } seqr outV()
    }

}