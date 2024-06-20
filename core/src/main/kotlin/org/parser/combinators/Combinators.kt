package org.parser.combinators

import org.parser.sppf.NonPackedNode
import org.parser.sppf.SPPFStorage


/** Returns parser that, for all output states of current parser, runs the [p2] parser.
 * Parser returns all possible combinations of results from the current and [p2] parsers. */
infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seq(p2: Parser<Out1, Out2, R2>): Parser<In, Out2, Pair<R1, R2>> {
    return MemoParser(SeqParser(this, p2))
}

/** Same as [seq] but returns parser that returns results only from the current parser. */
infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seql(p2: Parser<Out1, Out2, R2>): Parser<In, Out2, R1> {
    return this seq p2 using { (l, _) -> l }
}

/** Same as [seq] but returns parser that returns results only from the [p2] parser. */
infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seqr(p2: Parser<Out1, Out2, R2>): Parser<In, Out2, R2> {
    return this seq p2 using { (_, r) -> r }
}

private fun <In, Out, R> Parser<In, Out, R>.rule1(head: Parser<In, Out, R>): Parser<In, Out, R> {
    val p = this
    return Rule1Parser(head, p)
}

/** Returns parser that combines results of the current and [p2] parser. */
infix fun <In, Out, R> Parser<In, Out, R>.or(p2: Parser<In, Out, R>): Parser<In, Out, R> {
    return MemoParser(OrParser(this, p2))
}

/** Returns parser that combines results of all provided parsers. */
fun <In, Out, R> rule(
    first: Parser<In, Out, R>,
    vararg rest: Parser<In, Out, R>
): Parser<In, Out, R> {
    return MemoParser(RuleParser(first, *rest))
}

fun <In, Out, R> lookup(p: Parser<In, Out, R>): Parser<In, In, Unit> {
    return object : Parser<In, In, Unit>(p.view) {
        override fun parse(sppf: SPPFStorage, inS: In): ParserResult<NonPackedNode<In, In, Unit>> {
            val sppf = SPPFStorage()
            return p.parse(sppf, inS).map { sppf.getEpsilonNode(inS) }
        }
    }
}

fun <In, Out, Out2, R, R2> Parser<In, Out, R>.that(constraint: Parser<Out, Out2, R2>): Parser<In, Out, R> {
    return this seql lookup(constraint)
}

/** Returns parser that [f] returns. Same parser will be passed as argument of [f]. You can use it to define parser that uses itself. */
fun <I, O, R> fix(name: String = "fix", f: (Parser<I, O, R>) -> Parser<I, O, R>): Parser<I, O, R> {
    val p = LateInitParser<I, O, R>()
    val res = f(p)
    res.view = name
    p.init(res)
    return p
}

/** Returns same parser where result of this parser will be replaced with [f]. */
infix fun <In, Out, A, B> Parser<In, Out, A>.using(f: (A) -> B): Parser<In, Out, B> {
    return UsingParser(this, f)
}

//TODO we should generate the next `using` functions
infix fun <In, Out, A1, A2, B> Parser<In, Out, Pair<A1, A2>>.using(f: (A1, A2) -> B): Parser<In, Out, B> {
    return this using { r -> f(r.first, r.second) }
}

infix fun <In, Out, A1, A2, A3, B> Parser<In, Out, Pair<Pair<A1, A2>, A3>>.using(f: (A1, A2, A3) -> B): Parser<In, Out, B> {
    return this using { (r, a3) -> f(r.first, r.second, a3) }
}

infix fun <In, Out, A1, A2, A3, A4, B> Parser<In, Out, Pair<Pair<Pair<A1, A2>, A3>, A4>>.using(f: (A1, A2, A3, A4) -> B): Parser<In, Out, B> {
    return this using { (r, a4) -> f(r.first.first, r.first.second, r.second, a4) }
}

/** Returns parser that not change state and returns Unit */
fun <S> eps(): Parser<S, S, Unit> {
    return object : Parser<S, S, Unit>("eps") {
        override fun parse(sppf: SPPFStorage, inS: S): ParserResult<NonPackedNode<S, S, Unit>> {
            return ParserResult.success(sppf.getEpsilonNode(inS))
        }
    }
}

/** Returns parser that not change state and returns [v]. */
fun <S, R> success(v: R): Parser<S, S, R> {
    return object : Parser<S, S, R>("eps") {
        override fun parse(sppf: SPPFStorage, inS: S): ParserResult<NonPackedNode<S, S, R>> {
            return ParserResult.success(sppf.getTerminalNode(inS, inS, v))
        }
    }
}

/** Returns parser that not change state and returns nothing. */
fun <S, R> fail(): Parser<S, S, R> = object : Parser<S, S, R>("fail") {
    override fun parse(sppf: SPPFStorage, inS: S): ParserResult<NonPackedNode<S, S, R>> {
        return ParserResult.failure()
    }
}


/** Returns parser that applies this parser zero or more times. Parser returns [List] of results. */
val <S, R> Parser<S, S, R>.many: Parser<S, S, List<R>>
    get() {
        val name = "(${this.view})*"
        return fix(name) { manyP ->
            manyP.view = name
            val res =
                success<S, List<R>>(emptyList()) or ((this seq manyP) using { head, tail -> listOf(head) + tail })
            res.view = name
            res
        }
    }

/** Returns parser that applies this parser one or more times. Parser returns [List] of results. */
val <S, R> Parser<S, S, R>.some: Parser<S, S, List<R>>
    get() {
        return (this seq this.many) using { x, lst -> listOf(x) + lst }
    }

