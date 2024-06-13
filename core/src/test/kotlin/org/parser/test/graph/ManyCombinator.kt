package org.parser.test.graph

import kotlin.test.assertEquals
import org.parser.combinators.many
import org.parser.combinators.or
import org.parser.combinators.seq
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.outV
import org.parser.test.graph.TestCombinators.v
import kotlin.test.Test

class ManyCombinator {
    @Test
    fun run() {
        val vA = TestVertex("A")
        val eB = TestEdge("B")
        val eC = TestEdge("C")
        val gr = TestGraph().apply {
            addEdge(vA, eB, vA)
            addEdge(vA, eC, vA)
        }

        val isA: (TestVertex) -> Boolean = { it.value == "A" }
        val edgeB = outE { it.label == "B" }
        val edgeC = outE { it.label == "C" }

        val p = v(isA) seq ((edgeB or edgeC) seq outV(isA)).many

        assertEquals(1234, gr.applyParserForResults(p).take(1234).count())

        val results = gr.applyParserForResults(p).take(7)
        val rests = results.onEach { assertEquals(vA, it.first) }.map { it.second }
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
}