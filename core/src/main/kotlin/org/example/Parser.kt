package org.example


typealias Continuation<OutS, R> = (OutS, R) -> Unit

@JvmInline
value class ParserResult<OutS, R>(val f: (Continuation<OutS, R>) -> Unit) {
    operator fun invoke(k: Continuation<OutS, R>) {
        f(k)
    }
}

public fun <S1, S2, R1, R2> ParserResult<S1, R1>.map(transform: (S1, R1) -> Pair<S2, R2>): ParserResult<S2, R2> {
    return ParserResult { k ->
        val k2: Continuation<S1, R1> = { s, r ->
            val (s2, r2) = transform(s, r)
            k(s2, r2)
        }
        this(k2)
    }
}

public fun <S1, S2, R1, R2> ParserResult<S1, R1>.flatMap(transform: (S1) -> ParserResult<S2, R2>): ParserResult<S2, Pair<R1, R2>> {
    return ParserResult { k ->
        val resK: Continuation<S1, R1> = { s, r1 ->
            transform(s).invoke { s2, r2 ->
                k(s2, Pair(r1, r2))
            }
        }
        this(resK)
    }
}

public fun <S, R> ParserResult<S, R>.orElse(nextRes: () -> ParserResult<S, R>): ParserResult<S, R> {
    return ParserResult { k ->
        this(k)
        nextRes()(k)
    }
}

fun <S, R> success(s: S, v: R): ParserResult<S, R> = ParserResult { k -> k(s, v) }

fun <S, R> failure(): ParserResult<S, R> = ParserResult { _ -> }

class StopByLimitException: Exception("Stop parsing by limit")

class Parser<E, in InS, OutS, R>(val inner: (E, InS) -> ParserResult<OutS, R>) {
    fun parse(env: E, input: InS): ParserResult<OutS, R> = inner(env, input)
    fun getResults(env: E, input: InS, count: Int = -1): List<Pair<OutS, R>> {
        val res = ArrayList<Pair<OutS, R>>()
        try {
            this.parse(env, input).f { s, r ->
                res.add(Pair(s, r))
                if (res.size == count)
                    throw StopByLimitException()
            }
        } catch (_: StopByLimitException){
        }

        return res
    }
}

interface Parsers<E> {
    infix fun <In, Out1, R1, Out2, R2> Parser<E, In, Out1, R1>.seq(other: Parser<E, Out1, Out2, R2>): Parser<E, In, Out2, Pair<R1, R2>> {
        return Parser { env, input ->
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
        return Parser { env, input ->
            this.parse(env, input).orElse { other.parse(env, input) }
        }
    }

    fun <In, Out, R> lookup(p: Parser<E, In, Out, R>): Parser<E, In, In, R> {
        return Parser { env, input ->
            p.parse(env, input).map { _, r -> Pair(input, r) }
        }
    }

    fun <In, Out, Out2, R, R2> Parser<E, In, Out, R>.that(constraint: Parser<E, Out, Out2, R2>): Parser<E, In, Out, R> {
        return this seql lookup(constraint)
    }

    fun <I, O, R> fix(f: (Parser<E, I, O, R>) -> Parser<E, I, O, R>): Parser<E, I, O, R> {
        fun g(env: E, i: I): ParserResult<O, R> {
            return f(Parser(::g)).parse(env, i)
        }
        return Parser(::g)
    }

    infix fun <In, Out, A, B> Parser<E, In, Out, A>.using(f: (A) -> B): Parser<E, In, Out, B> {
        return Parser { env, input ->
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

    fun <S, R> success(v: R): Parser<E, S, S, R> = Parser { _, s -> ParserResult { k -> k(s, v) } }

    fun <S, R> fail(): Parser<E, S, S, R> = Parser { _, _ -> ParserResult { _ -> } }

    val <S, R> Parser<E, S, S, R>.many: Parser<E, S, S, Sequence<R>>
        get() = fix { manyP ->
            success<S, Sequence<R>>(emptySequence()) or ((this seq manyP) using { head, tail -> sequenceOf(head) + tail })
        }
}
