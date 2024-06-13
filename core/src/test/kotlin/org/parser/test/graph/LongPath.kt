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

class LongPath {
    @Test
    fun run() {
        val startV = TestVertex("0")
        val pathLen = 500
        val gr = TestGraph().apply {
            var prev = startV
            for (i in 1..pathLen) {
                val cur = TestVertex(i.toString())
                val e = TestEdge("e$i")
                addEdge(prev, e, cur)
                prev = cur
            }
        }

        val s =
            fix { s ->
                rule(
                    vertexEps() using { _ -> emptyList() },
                    (outE { true } seq outV() seq s) using { e, v, rest: List<Pair<TestEdge, TestVertex>> ->
                        listOf(Pair(e, v)) + rest
                    },
                )
            }

        val maxPath = s.parseStateForResults(VertexState(gr, startV)).toList().maxBy { it.size }
        assertEquals(pathLen, maxPath.size)

    }
}