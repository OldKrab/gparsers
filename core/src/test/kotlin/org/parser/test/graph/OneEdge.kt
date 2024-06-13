package org.parser.test.graph

import org.parser.test.graph.TestCombinators.edge
import org.parser.test.graph.TestCombinators.v
import kotlin.test.Test
import kotlin.test.assertEquals

class OneEdge {
    @Test
    fun run() {
        val nA = TestVertex("A")
        val eC = TestEdge("C")

        val gr = TestGraph().apply {
            addVertex(nA)
            addEdge(nA, eC, TestVertex("B"))
            addEdge(nA, TestEdge("D"), TestVertex("B"))
        }

        val parser = edge { it.label == "C" }

        val results = gr.applyParserForResults(parser)

        assertEquals(setOf(eC), results.toSet())
    }
}