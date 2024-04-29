package org.parser.combinators

import org.parser.sppf.SPPFStorage


/** Returns parser that, for all output states of current parser, runs the [p2] parser.
 * Parser returns all possible combinations of results from the current and [p2] parsers. */
infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seq(p2: Parser<Out1, Out2, R2>): Parser<In, Out2, Pair<R1, R2>> {
    val p1 = this
    val name = "${p1.view} ${p2.view}"
    return fix(name) { q ->
        Parser.memo(name) { sppf, i ->
            p1.parse(sppf, i)
                .flatMap { t1 ->
                    p2.parse(sppf, t1.rightState)
                        .map { t2 ->
                            sppf.getIntermediateNode(q, t1, t2)
                        }
                }
        }
    }
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
    return Parser.memo(view) { sppf, inS ->
        p.parse(sppf, inS).map { t -> sppf.getIntermediateNode(head, t) }
    }
}

/** Returns parser that combines results of the current and [p2] parser. */
infix fun <In, Out, R> Parser<In, Out, R>.or(p2: Parser<In, Out, R>): Parser<In, Out, R> {
    val name = "${this.view} | ${p2.view}"
    return fix(name) { q ->
        Parser.memo(name) { sppf, input ->
            this.rule1(q).parse(sppf, input).orElse { p2.rule1(q).parse(sppf, input) }
        }
    }
}

/** Returns parser that combines results of all provided parsers. */
fun <In, Out, R> rule(
    p: Parser<In, Out, R>,
    vararg parsers: Parser<In, Out, R>
): Parser<In, Out, R> {
    val name = "${p.view} ${parsers.joinToString { " | ${it.view}" }}"
    return fix(name) { q ->
        Parser.memo(name) { sppf, input ->
            val firstRes = p.rule1(q).parse(sppf, input)
            parsers.fold(firstRes) { acc, cur ->
                acc.orElse { cur.rule1(q).parse(sppf, input) }
            }
        }
    }

}

fun <In, Out, R> lookup(p: Parser<In, Out, R>): Parser<In, In, R> {
    return Parser.memo("lookup") { _, input ->
        val sppf = SPPFStorage()
        p.parse(sppf, input).map { sppf.getEpsilonNode(input) }
    }
}

fun <In, Out, Out2, R, R2> Parser<In, Out, R>.that(constraint: Parser<Out, Out2, R2>): Parser<In, Out, R> {
    return this seql lookup(constraint)
}

/** Returns parser that [f] returns. Same parser will be passed as argument of [f]. You can use it to define parser that uses itself.
 * @sample org.parser.samples.fixExample */
fun <I, O, R> fix(name: String = "fix", f: (Parser<I, O, R>) -> Parser<I, O, R>): Parser<I, O, R> {
    //TODO if q get name after fix, then f will use previous name of q
    lateinit var p: Parser<I, O, R>
    val q: Parser<I, O, R> = Parser.memo(name) { sppf, s -> p.parse(sppf, s) }
    p = f(q)
    return q
}

/** Returns same parser where result of this parser will be replaced with [f]. */
infix fun <In, Out, A, B> Parser<In, Out, A>.using(f: (A) -> B): Parser<In, Out, B> {
    return Parser.memo(this.view) { sppf, input ->
        this.parse(sppf, input).map { t -> sppf.withAction(t, f) }
    }
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
    return Parser.memo("eps") { sppf, i ->
        ParserResult.success(sppf.getEpsilonNode(i))
    }
}

/** Returns parser that not change state and returns [v]. */
fun <S, R> success(v: R): Parser<S, S, R> {
    return Parser.memo("success($v)") { sppf, s ->
        ParserResult.success(sppf.getTerminalNode(s, s, v))
    }
}

/** Returns parser that not change state and returns nothing. */
fun <S, R> fail(): Parser<S, S, R> = Parser.memo("fail") { _, _ -> ParserResult.failure() }

/** Returns parser that applies this parser zero or more times. Parser returns [List] of results. */
val <S, R> Parser<S, S, R>.many: Parser<S, S, List<R>>
    get() {
        val name = "(${this.view})*"
        return fix(name) { manyP ->
            val res =
                success<S, List<R>>(emptyList()) or ((this seq manyP) using { head, tail -> listOf(head) + tail })
            res.view = name
            res
        }
    }

