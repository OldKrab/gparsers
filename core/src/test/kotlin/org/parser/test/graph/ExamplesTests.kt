package org.parser.test.graph

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.parser.ParserTests.saveDotsToFolder
import org.parser.combinators.*
import org.parser.combinators.graph.*
import org.parser.test.graph.TestCombinators.outE
import org.parser.test.graph.TestCombinators.outV


class ExamplesTests  {

    @Test
    fun example1() {
        val xV = TestVertex("x")
        val yV = TestVertex("y")
        val gr = TestGraph().apply {
            addEdge(xV, "A", yV)
            addEdge(yV, "B", yV)
        }

        val x by outV { it.value == "x" }
        val y by outV { it.value == "y" }
        val A by outE { it.label == "A" }
        val B by outE { it.label == "B" }
        val p by A seqr y
        val s by p or (p seqr B seqr y)

        val results = s.parseStateForResults(VertexState(gr, xV))

        assertEquals(setOf(yV), results.toSet())
    }
}