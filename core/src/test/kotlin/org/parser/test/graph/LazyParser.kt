package org.parser.test.graph

import org.parser.ParserTests.saveDotsToFolder
import org.parser.combinators.*
import org.parser.combinators.graph.VertexState
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.outV
import org.parser.test.graph.TestCombinators.vertexEps
import kotlin.test.Test
import kotlin.test.assertEquals


class LazyParser {

    @Test
    fun run() {
        val vX = TestVertex("x")
        val eA = TestEdge("A")
        val gr = TestGraph().apply {
            addEdge(vX, eA, vX)
        }
        val x by outV { it.value == "x" }
        val a by outE { it.label == "A" }
        val S by LazyParser {
            rule(
                (this seq a seq x) using { prefix: List<Pair<TestEdge, TestVertex>>, e, v ->
                    prefix + Pair(e, v)
                },
                vertexEps() using { _ -> emptyList() },
            )
        }


        val results = S.parseStateForResults(VertexState(gr, vX)).take(3)
        assertEquals(
            setOf(
                listOf(),
                listOf(Pair(eA, vX)),
                listOf(Pair(eA, vX), Pair(eA, vX)),
            ), results.toSet()
        )

    }
}