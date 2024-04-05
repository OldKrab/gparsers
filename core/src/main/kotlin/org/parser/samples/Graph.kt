package org.parser.samples

import org.parser.combinators.*
import org.parser.combinators.graph.GraphCombinators
import org.parser.combinators.string.p

class SimpleVertex
class SimpleEdge
object SimpleCombinators : GraphCombinators<SimpleVertex, SimpleEdge>

fun fixExample() {
    val s = fix { s -> ("a".p seql s).many }
}