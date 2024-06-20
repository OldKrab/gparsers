package org.parser.test.graph

import org.parser.combinators.*
import org.parser.combinators.graph.VVGraphParser
import org.parser.combinators.graph.VertexState
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.outV
import org.parser.test.graph.TestCombinators.vertexEps
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoLeftRec {
    @Test
    fun run() {
        val vA = TestVertex("A")
        val eB = TestEdge("B")
        val eC = TestEdge("C")
        val gr = TestGraph().apply {
            addEdge(vA, eB, vA)
            addEdge(vA, eC, vA)
        }

        val vertexA = outV { it.value == "A" }
        val edgeB = outE { it.label == "B" }
        val edgeC = outE { it.label == "C" }
        val s1: VVGraphParser<TestVertex, TestEdge, List<Pair<TestEdge, TestVertex>>> = fix("s1") { s1 ->
            rule(
                (s1 seq edgeB seq vertexA) using { prefix, e, v -> prefix + Pair(e, v) },
                vertexEps() using { _ -> emptyList() }
            )
        }
        val s2: VVGraphParser<TestVertex, TestEdge, List<Pair<TestEdge, TestVertex>>> = fix("s2") { s2 ->
            rule(
                (s2 seq edgeC seq vertexA) using { prefix, e, v -> prefix + Pair(e, v) },
                vertexEps() using { _ -> emptyList() }
            )
        }
        val s = s1 or s2

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