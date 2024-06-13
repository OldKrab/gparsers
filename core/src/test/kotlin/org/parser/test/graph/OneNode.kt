package org.parser.test.graph

import kotlin.test.assertEquals
import org.parser.test.graph.TestCombinators.v
import kotlin.test.Test

class OneNode {
    @Test
    fun run() {
        val nA = TestVertex("A")

        val gr = TestGraph().apply {
            addVertex(nA)
            addEdge(nA, TestEdge("C"), TestVertex("B"))
        }

        val parser = v { it.value == "A" }

        val results = gr.applyParserForResults(parser)

        assertEquals(setOf(nA), results.toSet())
    }
}