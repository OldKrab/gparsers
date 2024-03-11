package org.example


typealias Continuation<OutS, R> = (OutS, R) -> Unit

@JvmInline
value class ParserResult<OutS, R>(private val f: (Continuation<OutS, R>) -> Unit) {
    fun apply(k: Continuation<OutS, R>) {
        f(k)
    }
}

fun <S1, S2, R1, R2> ParserResult<S1, R1>.map(transform: (S1, R1) -> Pair<S2, R2>): ParserResult<S2, R2> {
    return ParserResult { k ->
        val k2: Continuation<S1, R1> = { s, r ->
            val (s2, r2) = transform(s, r)
            k(s2, r2)
        }
        this.apply(k2)
    }
}

fun <S1, S2, R1, R2> ParserResult<S1, R1>.flatMap(transform: (S1) -> ParserResult<S2, R2>): ParserResult<S2, Pair<R1, R2>> {
    return ParserResult { k ->
        val resK: Continuation<S1, R1> = { s, r1 ->
            transform(s).apply { s2, r2 ->
                k(s2, Pair(r1, r2))
            }
        }
        this.apply(resK)
    }
}

fun <S, R> ParserResult<S, R>.orElse(nextRes: () -> ParserResult<S, R>): ParserResult<S, R> {
    return ParserResult { k ->
        this.apply(k)
        nextRes().apply(k)
    }
}

fun <S, R> success(s: S, v: R): ParserResult<S, R> = ParserResult { k -> k(s, v) }

fun <S, R> failure(): ParserResult<S, R> = ParserResult { _ -> }

class StopByLimitException : Exception("Stop parsing by limit")

class Parser<E, in InS, OutS, R> private constructor(
    private val inner: (E, InS) -> ParserResult<OutS, R>,
    private val name: String
) {
    fun parse(env: E, input: InS): ParserResult<OutS, R> = inner(env, input)
    fun getResults(env: E, input: InS, count: Int = -1): List<Pair<OutS, R>> {
        val res = ArrayList<Pair<OutS, R>>()
        try {
            this.parse(env, input).apply { s, r ->
                res.add(Pair(s, r))
                if (res.size == count)
                    throw StopByLimitException()
            }
        } catch (_: StopByLimitException) {
        }

        return res
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun <E, InS, OutS, R> make(name: String, inner: (E, InS) -> ParserResult<OutS, R>): Parser<E, InS, OutS, R> {
            return memo(Parser(inner, name))
        }

        fun <E, InS, OutS, R> memo(parser: Parser<E, InS, OutS, R>): Parser<E, InS, OutS, R> {
            val table = HashMap<Pair<E, InS>, ParserResult<OutS, R>>()
            return Parser(
                { env, inS -> table.getOrPut(Pair(env, inS)) { memoResult { parser.parse(env, inS) } } },
                parser.name
            )
        }
    }
}

fun <S, R> memoResult(res: () -> ParserResult<S, R>): ParserResult<S, R> {
    val results = ArrayList<Pair<S, R>>()
    val continuations = ArrayList<Continuation<S, R>>()
    return ParserResult { k ->
        val isFirstCall = continuations.isEmpty()
        continuations.add(k)
        if (isFirstCall) {
            res().apply { s, r ->
                val newState = Pair(s, r)
                if (!results.contains(newState)) {
                    results.add(newState)
                    for (continuation in continuations) {
                        continuation(s, r)
                    }
                }
            }
        } else {
            for ((s, r) in results) {
                k(s, r)
            }
        }
    }
}


interface Parsers<E> {
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
