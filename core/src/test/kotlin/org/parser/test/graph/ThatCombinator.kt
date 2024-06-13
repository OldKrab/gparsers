package org.parser.test.graph

import org.parser.combinators.*
import org.parser.test.graph.TestCombinators.ePred
import org.parser.test.graph.TestCombinators.inE
import org.parser.test.graph.TestCombinators.inV
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.outV
import org.parser.test.graph.TestCombinators.v
import org.parser.test.graph.TestCombinators.vPred
import kotlin.test.Test
import kotlin.test.assertEquals

class ThatCombinator {
    @Test
    fun run() {
        val danV = TestVertex("Dan")
        val johnV = TestVertex("John")
        val friendE = TestEdge("friend")
        val gr = TestGraph().apply {
            val lindaV = TestVertex("Linda")
            val maryV = TestVertex("Mary")
            addEdge(danV, friendE, johnV)
            addEdge(danV, "loves", maryV)
            addEdge(maryV, "loves", danV)
            addEdge(johnV, "loves", maryV)
            addEdge(johnV, "friend", lindaV)
        }

        val person = v()
        val isMary = vPred { v -> v.value == "Mary" }
        val isLoves = ePred { e -> e.label == "loves" }
        val marysLover = person
            .that(outE(isLoves) seq outV(isMary))
            .that(inE(isLoves) seq inV(isMary))
        val friend = outE { it.label == "friend" }
        val marysLoversFriend = marysLover seq friend seq outV()

        val res = gr.applyParserForResults(marysLoversFriend).toList()

        assertEquals(listOf(Pair(Pair(danV, friendE), johnV)), res)
    }
}