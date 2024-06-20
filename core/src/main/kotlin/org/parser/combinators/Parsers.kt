package org.parser.combinators

import org.parser.combinators.Parser
import org.parser.combinators.ParserResult
import org.parser.combinators.orElse
import org.parser.sppf.SPPFStorage
import org.parser.sppf.NonPackedNode


internal class SeqParser<I, M, O, out R1, out R2>(
    val p1: Parser<I, M, R1>,
    val p2: Parser<M, O, R2>,
    view: String = "${p1.view} ${p2.view}"
) : Parser<I, O, Pair<R1, R2>>(view) {
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, Pair<R1, R2>>> {
        return p1.parse(sppf, inS)
            .flatMap { t1 ->
                p2.parse(sppf, t1.rightState)
                    .map { t2 ->
                        sppf.getIntermediateNode(this, t1, t2)
                    }
            }
    }
}

/** Returns parser which results will be memoized for any input state. */
internal class MemoParser<I, O, R> internal constructor(
    val p: Parser<I, O, R>,
) : Parser<I, O, R>(p.view) {
    private val table = HashMap<I, ParserResult<NonPackedNode<I, O, R>>>()

    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        return table.computeIfAbsent(inS) { ParserResult.memoResult { p.parse(sppf, inS) } }
    }
}

internal class Rule1Parser<I, O, R>(
    private val head: Parser<I, O, R>,
    val p: Parser<I, O, R>,
    view: String = p.view
) : Parser<I, O, R>(view) {
    private val table = HashMap<I, ParserResult<NonPackedNode<I, O, R>>>()

    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        return p.parse(sppf, inS).map { t -> sppf.getIntermediateNode(head, t) }
    }
}

internal class OrParser<I, O, R>(
    left: Parser<I, O, R>,
    right: Parser<I, O, R>,
    view: String = "${left.view} | ${right.view}"
) : Parser<I, O, R>(view) {
    private val p1 = Rule1Parser(this, left)
    private val p2 = Rule1Parser(this, right)
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        return p1.parse(sppf, inS).orElse { p2.parse(sppf, inS) }
    }
}

internal class RuleParser<I, O, R>(
    first: Parser<I, O, R>,
    vararg rest: Parser<I, O, R>,
    view: String = "${first.view} ${rest.joinToString { " | ${it.view}" }}"
) : Parser<I, O, R>(view) {
    private val first = Rule1Parser(this, first)
    private val rest = rest.map { Rule1Parser(this, it) }
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        val firstRes = first.parse(sppf, inS)
        return rest.fold(firstRes) { acc, cur ->
            acc.orElse { cur.parse(sppf, inS) }
        }
    }
}

internal class UsingParser<I, O, R, R2>(
    private val p: Parser<I, O, R>,
    private val f: (R) -> R2
) : Parser<I, O, R2>(p.view) {
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R2>> {
        return p.parse(sppf, inS).map { t -> sppf.withAction(t, f) }
    }
}





