package org.parser.test.graph

import org.parser.ParserTests.saveDotsToFolder
import org.parser.combinators.*
import org.parser.combinators.graph.VertexState
import org.parser.test.graph.LazyParser
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.outV
import org.parser.test.graph.TestCombinators.v
import org.parser.test.graph.TestCombinators.vertexEps
import kotlin.test.Test
import kotlin.test.assertEquals

class FixCombinator {
    @Test
    fun run() {
        val vA = TestVertex("A")
        val eB = TestEdge("B")
        val gr = TestGraph().apply {
            addEdge(vA, eB, vA)
        }

        val vertexA = outV { it.value == "A" }
        val edgeB = outE { it.label == "B" }
        val s =
            fix("x") { s ->
                rule(
                    vertexEps() using { _ -> emptyList() },
                    (edgeB seq vertexA seq s) using { e, v, rest: List<Pair<TestEdge, TestVertex>> ->
                        listOf(Pair(e, v)) + rest
                    },
                )
            }

        val results = s.parseStateForResults(VertexState(gr, vA)).take(3)

        assertEquals(
            setOf(
                listOf(),
                listOf(Pair(eB, vA)),
                listOf(Pair(eB, vA), Pair(eB, vA)),
            ), results.toSet()
        )
    }
}