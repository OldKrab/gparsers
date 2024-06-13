package org.parser.test.graph


import kotlin.test.assertEquals
import org.parser.ParserTests.saveDotsToFolder
import org.parser.combinators.seq
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.v
import kotlin.test.Test

class NodeAndEdge {
    @Test
    fun run() {
        val nA = TestVertex("A")
        val eB = TestEdge("B")
        val gr = TestGraph().apply {
            addEdge(nA, eB, nA)
            addEdge(nA, TestEdge("C"), nA)
        }

        val parser = v { it.value == "A" } seq outE { it.label == "B" }

        val results = gr.applyParserForResults(parser)
        assertEquals(setOf(Pair(nA, eB)), results.toSet())
    }
}