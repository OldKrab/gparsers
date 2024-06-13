package org.parser.test.graph

import kotlin.test.assertEquals
import org.parser.combinators.seq
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.v
import kotlin.test.Test

class AmbiguousEdge {
    @Test
    fun run() {
        val nA = TestVertex("A")
        val eB = TestEdge("B")
        val eC = TestEdge("C")
        val gr = TestGraph().apply {
            addEdge(nA, eB, nA)
            addEdge(nA, eC, nA)
        }

        val parser = v { it.value == "A" } seq outE()

        val results = gr.applyParserForResults(parser)
        assertEquals(setOf(Pair(nA, eB), Pair(nA, eC)), results.toSet())
    }
}