package org.parser.combinators



interface Combinators<E> {
    infix fun <In, Out1, R1, Out2, R2> Parser<E, In, Out1, R1>.seq(other: Parser<E, Out1, Out2, R2>): Parser<E, In, Out2, Pair<R1, R2>> {
        return Parser.make("seq") { env, input ->
            this.parse(env, input).flatMap { s -> other.parse(env, s) }
        }
    }

    infix fun <In, Out1, R1, Out2, R2> Parser<E, In, Out1, R1>.seql(other: Parser<E, Out1, Out2, R2>): Parser<E, In, Out2, R1> {
        return this seq other using { (l, _) -> l }
    }

    infix fun <In, Out1, R1, Out2, R2> Parser<E, In, Out1, R1>.seqr(other: Parser<E, Out1, Out2, R2>): Parser<E, In, Out2, R2> {
        return this seq other using { (_, r) -> r }
    }

    infix fun <In, Out, R> Parser<E, In, Out, R>.or(other: Parser<E, In, Out, R>): Parser<E, In, Out, R> {
        return Parser.make("or") { env, input ->
            this.parse(env, input).orElse { other.parse(env, input) }
        }
    }

    fun <In, Out, R> rule(p: Parser<E, In, Out, R>, vararg parsers: Parser<E, In, Out, R>): Parser<E, In, Out, R> {
        return Parser.make("rule") { env, input ->
            parsers.fold(p.parse(env, input)) { acc, cur ->
                acc.orElse { cur.parse(env, input) }
            }
        }
    }

    fun <In, Out, R> lookup(p: Parser<E, In, Out, R>): Parser<E, In, In, R> {
        return Parser.make("lookup") { env, input ->
            p.parse(env, input).map { _, r -> Pair(input, r) }
        }
    }

    fun <In, Out, Out2, R, R2> Parser<E, In, Out, R>.that(constraint: Parser<E, Out, Out2, R2>): Parser<E, In, Out, R> {
        return this seql lookup(constraint)
    }

    fun <I, O, R> fix(f: (Parser<E, I, O, R>) -> Parser<E, I, O, R>): Parser<E, I, O, R> {
        lateinit var p: Parser<E, I, O, R>
        p = Parser.make("fix") { env, s -> f(p).parse(env, s) }
        return p
    }

    infix fun <In, Out, A, B> Parser<E, In, Out, A>.using(f: (A) -> B): Parser<E, In, Out, B> {
        return Parser.make("using") { env, input ->
            this.parse(env, input).map { p, r -> Pair(p, f(r)) }
        }
    }

    infix fun <In, Out, A1, A2, B> Parser<E, In, Out, Pair<A1, A2>>.using(f: (A1, A2) -> B): Parser<E, In, Out, B> {
        return this using { r -> f(r.first, r.second) }
    }

    infix fun <In, Out, A1, A2, A3, B> Parser<E, In, Out, Pair<Pair<A1, A2>, A3>>.using(f: (A1, A2, A3) -> B): Parser<E, In, Out, B> {
        return this using { (r, a3) -> f(r.first, r.second, a3) }
    }

    infix fun <In, Out, A1, A2, A3, A4, B> Parser<E, In, Out, Pair<Pair<Pair<A1, A2>, A3>, A4>>.using(f: (A1, A2, A3, A4) -> B): Parser<E, In, Out, B> {
        return this using { (r, a4) -> f(r.first.first, r.first.second, r.second, a4) }
    }

    fun <S, R> success(v: R): Parser<E, S, S, R> = Parser.make("success") { _, s -> ParserResult { k -> k(s, v) } }

    fun <S, R> fail(): Parser<E, S, S, R> = Parser.make("fail") { _, _ -> ParserResult { _ -> } }

    val <S, R> Parser<E, S, S, R>.many: Parser<E, S, S, List<R>>
        get() = fix { manyP ->
            success<S, List<R>>(emptyList()) or ((this seq manyP) using { head, tail -> listOf(head) + tail })
        }
}