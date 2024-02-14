package org.example

data class ParserResult<out OutS, out R>(val outState: OutS, val res: R)


class Parser<E, in InS, out OutS, R>(val inner: (E, InS) -> Sequence<ParserResult<OutS, R>>) {
    fun parse(env: E, input: InS) = inner(env, input)
}

interface Parsers<E> {
    infix fun <In, Out1, R1, Out2, R2> Parser<E, In, Out1, R1>.seq(other: Parser<E, Out1, Out2, R2>): Parser<E, In, Out2, Pair<R1, R2>> {
        return Parser { env, input ->
            val sequences = this.parse(env, input).map { (pos1, r1) ->
                sequence {
                    yieldAll(other.parse(env, pos1).map { (pos2, r2) -> ParserResult(pos2, Pair(r1, r2)) })
                }
            }

            sequence {
                val iterators = ArrayList<Iterator<ParserResult<Out2, Pair<R1, R2>>>>()
                for (it in sequences.map { it.iterator() }) {
                    iterators.add(it)
                    if (it.hasNext()) yield(it.next())
                }

                while (true) {
                    var isAllIteratorsEnd = true
                    for (it in iterators) {
                        if (it.hasNext()) {
                            isAllIteratorsEnd = false
                            yield(it.next())
                        }
                    }
                    if (isAllIteratorsEnd) break
                }

            }

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
            this.parse(env, input) + other.parse(env, input)
        }
    }

    fun <In, Out, R> lookup(p: Parser<E, In, Out, R>): Parser<E, In, In, R> {
        return Parser { env, input ->
            p.parse(env, input).map { r -> ParserResult(input, r.res) }
        }
    }

    fun <In, Out, Out2, R, R2> Parser<E, In, Out, R>.that(constraint: Parser<E, Out, Out2, R2>): Parser<E, In, Out, R> {
        return this seql lookup(constraint)
    }

    fun <I, O, R> fix(f: (Parser<E, I, O, R>) -> Parser<E, I, O, R>): Parser<E, I, O, R> {
        fun g(env: E, i: I): Sequence<ParserResult<O, R>> {
            return f(Parser(::g)).parse(env, i)
        }
        return Parser(::g)
    }

    infix fun <In, Out, A, B> Parser<E, In, Out, A>.using(f: (A) -> B): Parser<E, In, Out, B> {
        return Parser { env, input ->
            this.parse(env, input).map { (p, r) -> ParserResult(p, f(r)) }
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

    fun <S, R> success(v: R): Parser<E, S, S, R> = Parser { _, s -> sequenceOf(ParserResult(s, v)) }

    fun <S, R> fail(): Parser<E, S, S, R> = Parser { _, _ -> emptySequence() }

    val <S, R> Parser<E, S, S, R>.many: Parser<E, S, S, Sequence<R>>
        get() = fix { manyP ->
            success<S, Sequence<R>>(emptySequence()) or ((this seq manyP) using { head, tail -> sequenceOf(head) + tail })
        }
}
