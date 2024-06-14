package org.parser.combinators

import org.parser.sppf.SPPFStorage
import org.parser.sppf.NonPackedNode


class SeqParser<I, M, O, out R1, out R2>(
    val p1: BaseParser<I, M, R1>,
    val p2: BaseParser<M, O, R2>,
    view: String = "${p1.view} ${p2.view}"
) : BaseParser<I, O, Pair<R1, R2>>(view) {
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
class MemoParser<I, O, R>(
    val p: BaseParser<I, O, R>,
    view: String = "memo"
) : BaseParser<I, O, R>(view) {
    private val table = HashMap<I, ParserResult<NonPackedNode<I, O, R>>>()

    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        return table.computeIfAbsent(inS) { ParserResult.memoResult { p.parse(sppf, inS) } }
    }
}

class Rule1Parser<I, O, R>(
    private val head: BaseParser<I, O, R>,
    val p: BaseParser<I, O, R>,
    view: String = p.view
) : BaseParser<I, O, R>(view) {
    private val table = HashMap<I, ParserResult<NonPackedNode<I, O, R>>>()

    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        return p.parse(sppf, inS).map { t -> sppf.getIntermediateNode(head, t) }
    }
}

class OrParser<I, O, R>(
    left: BaseParser<I, O, R>,
    right: BaseParser<I, O, R>,
    view: String = "${left.view} | ${right.view}"
) : BaseParser<I, O, R>(view) {
    private val p1 = Rule1Parser(this, left)
    private val p2 = Rule1Parser(this, right)
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        return p1.parse(sppf, inS).orElse { p2.parse(sppf, inS) }
    }
}

class RuleParser<I, O, R>(
    first: BaseParser<I, O, R>,
    vararg rest: BaseParser<I, O, R>,
    view: String = "${first.view} ${rest.joinToString { " | ${it.view}" }}"
) : BaseParser<I, O, R>(view) {
    val first_ = Rule1Parser(this, first)
    val rest_ = rest.map { Rule1Parser(this, it) }
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R>> {
        val firstRes = first_.parse(sppf, inS)
        return rest_.fold(firstRes) { acc, cur ->
            acc.orElse { cur.parse(sppf, inS) }
        }
    }
}

class UsingParser<I, O, R, R2>(
    private val p: BaseParser<I, O, R>,
    private val f: (R) -> R2
) : BaseParser<I, O, R2>(p.view) {
    override fun parse(sppf: SPPFStorage, inS: I): ParserResult<NonPackedNode<I, O, R2>> {
        return p.parse(sppf, inS).map { t -> sppf.withAction(t, f) }
    }
}





