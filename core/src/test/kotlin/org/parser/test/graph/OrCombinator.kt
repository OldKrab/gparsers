package org.parser.test.graph

import kotlin.test.assertEquals
import org.parser.ParserTests.saveDotsToFolder
import org.parser.combinators.or
import org.parser.combinators.seq
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.v
import kotlin.test.Test

class OrCombinator {
    @Test
    fun run() {
        val nA = TestVertex("A")
        val eB = TestEdge("B")
        val eC = TestEdge("C")
        val gr = TestGraph().apply {
            addEdge(nA, eB, nA)
            addEdge(nA, eC, nA)
        }

        val nodeA = v { it.value == "A" }
        val edgeB = outE { it.label == "B" }
        val edgeC = outE { it.label == "C" }
        val parser = nodeA seq (edgeB or edgeC)

        val results = gr.applyParserForResults(parser)

        assertEquals(setOf(Pair(nA, eB), Pair(nA, eC)), results.toSet())
    }
}