package org.parser.combinators

import org.parser.sppf.SPPF
import org.parser.sppf.node.NonPackedNode

fun <S> eps(): Parser<S, S, Unit> {
    return Parser.memo("eps") { sppf, i ->
        ParserResult.success(sppf.getEpsilonNode(i))
    }
}

infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seq(p2: Parser<Out1, Out2, R2>): Parser<In, Out2, Pair<R1, R2>> {
    val p1 = this
    val name = "${p1.name} ${p2.name}"
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

infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seql(other: Parser<Out1, Out2, R2>): Parser<In, Out2, R1> {
    return this seq other using { (l, _) -> l }
}

infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seqr(other: Parser<Out1, Out2, R2>): Parser<In, Out2, R2> {
    return this seq other using { (_, r) -> r }
}

private fun <In, Out, R> Parser<In, Out, R>.rule1(head: Parser<In, Out, R>): Parser<In, Out, R> {
    val p = this
    return Parser.memo(name) { sppf, inS ->
        p.parse(sppf, inS).map { t -> sppf.getNonTerminalNode(head, t) }
    }
}

infix fun <In, Out, R> Parser<In, Out, R>.or(other: Parser<In, Out, R>): Parser<In, Out, R> {
    val name = "${this.name} | ${other.name}"
    return fix(name) { q ->
        Parser.memo(name) { sppf, input ->
            this.rule1(q).parse(sppf, input).orElse { other.rule1(q).parse(sppf, input) }
        }
    }
}

fun <In, Out, R> rule(
    p: Parser<In, Out, R>,
    vararg parsers: Parser<In, Out, R>
): Parser<In, Out, R> {
    val name = "${p.name} ${parsers.joinToString { " | ${it.name}" }}"
    return fix(name) { q ->
        Parser.memo(name) { sppf, input ->
            val firstRes = p.rule1(q).parse(sppf, input)
            parsers.fold(firstRes) { acc, cur ->
                acc.orElse { cur.rule1(q).parse(sppf, input) }
            }
        }
    }

}

//    fun <In, Out, R> lookup(p: Parser<E, In, Out, R>): Parser<E, In, In, R> {
//        return Parser.make("lookup") { env, sppf, input ->
//            p.parse(env, sppf, input).map { t -> t }
//        }
//    }

//    fun <In, Out, Out2, R, R2> Parser<E, In, Out, R>.that(constraint: Parser<E, Out, Out2, R2>): Parser<E, In, Out, R> {
//        return this seql lookup(constraint)
//    }

fun <I, O, R> fix(name: String, f: (Parser<I, O, R>) -> Parser<I, O, R>): Parser<I, O, R> {
    lateinit var p: Parser<I, O, R>
    p = Parser.memo(name) { sppf, s -> f(p).parse(sppf, s) }
    return p
}

infix fun <In, Out, A, B> Parser<In, Out, A>.using(f: (A) -> B): Parser<In, Out, B> {
    return Parser.memo(this.name) { sppf, input ->
        this.parse(sppf, input).map { t -> sppf.withAction(t, f) }
    }
}

infix fun <In, Out, A1, A2, B> Parser<In, Out, Pair<A1, A2>>.using(f: (A1, A2) -> B): Parser<In, Out, B> {
    return this using { r -> f(r.first, r.second) }
}

infix fun <In, Out, A1, A2, A3, B> Parser<In, Out, Pair<Pair<A1, A2>, A3>>.using(f: (A1, A2, A3) -> B): Parser<In, Out, B> {
    return this using { (r, a3) -> f(r.first, r.second, a3) }
}

infix fun <In, Out, A1, A2, A3, A4, B> Parser<In, Out, Pair<Pair<Pair<A1, A2>, A3>, A4>>.using(f: (A1, A2, A3, A4) -> B): Parser<In, Out, B> {
    return this using { (r, a4) -> f(r.first.first, r.first.second, r.second, a4) }
}

fun <S, R> success(v: R): Parser<S, S, R> =
    Parser.memo("success") { sppf, s -> ParserResult { k -> k(sppf.getTerminalNode(s, s, v)) } }

fun <S, R> fail(): Parser<S, S, R> = Parser.memo("fail") { _, _ -> ParserResult { _ -> } }

val <S, R> Parser<S, S, R>.many: Parser<S, S, List<R>>
    get() {
        val name = "(${this.name})*"
        return fix(name) { manyP ->
            val res =
                success<S, List<R>>(emptyList()) or ((this seq manyP) using { head, tail -> listOf(head) + tail })
            res.name = name
            res
        }
    }

