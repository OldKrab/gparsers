package org.example

data class ParserResult<out OutS, out R>(val outState: OutS, val res: R)

class Parser<in InS, out OutS, R>(val inner: (InS) -> Sequence<ParserResult<OutS, R>>) {
    fun parse(i: InS) = inner(i)
}

infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seq(other: Parser<Out1, Out2, R2>): Parser<In, Out2, Pair<R1, R2>> {
    return Parser { input ->
        val sequences = this.parse(input).map { (pos1, r1) ->
            sequence {
                yieldAll(other.parse(pos1).map { (pos2, r2) -> ParserResult(pos2, Pair(r1, r2)) })
            }
        }

        sequence {
            val iterators = ArrayList<Iterator<ParserResult<Out2, Pair<R1, R2>>>>()
            for (it in sequences.map { it.iterator() }) {
                iterators.add(it)
                if (it.hasNext())
                    yield(it.next())
            }
            while (true) {
                var isAllIteratorsEnd = true
                for (it in iterators) {
                    if (it.hasNext()) {
                        isAllIteratorsEnd = false
                        yield(it.next())
                    }
                }
                if (isAllIteratorsEnd)
                    break
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

infix fun <In, Out, R> Parser<In, Out, R>.or(other: Parser<In, Out, R>): Parser<In, Out, R> {
    return Parser { input ->
        this.parse(input) + other.parse(input)
    }
}

fun <In, Out, R> lookup(p: Parser<In, Out, R>): Parser<In, In, R> {
    return Parser { input ->
        p.parse(input).map { r -> ParserResult(input, r.res) }
    }
}

fun <In, Out, Out2, R, R2> Parser<In, Out, R>.that(constraint: Parser<Out, Out2, R2>): Parser<In, Out, R> {
    return this seql lookup(constraint)
}

fun <I, O, R> fix(f: (Parser<I, O, R>) -> Parser<I, O, R>): Parser<I, O, R> {
    fun g(i: I): Sequence<ParserResult<O, R>> {
        return f(Parser(::g)).parse(i)
    }
    return Parser(::g)
}

infix fun <In, Out, A, B> Parser<In, Out, A>.using(f: (A) -> B): Parser<In, Out, B> {
    return Parser { input ->
        this.parse(input).map { (p, r) -> ParserResult(p, f(r)) }
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


fun <S, R> success(v: R): Parser<S, S, R> = Parser { s -> sequenceOf(ParserResult(s, v)) }
fun <S, R> fail(): Parser<S, S, R> = Parser { emptySequence() }

val <S, R> Parser<S, S, R>.many: Parser<S, S, Sequence<R>>
    get() = fix { manyP ->
        success<S, Sequence<R>>(emptySequence()) or
                ((this seq manyP) using { head, tail -> sequenceOf(head) + tail })
    }
