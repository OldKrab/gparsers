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
    return this seq other using { (l, r) -> l }
}

infix fun <In, Out1, R1, Out2, R2> Parser<In, Out1, R1>.seqr(other: Parser<Out1, Out2, R2>): Parser<In, Out2, R2> {
    return this seq other using { (l, r) -> r }
}

infix fun <In, Out, R> Parser<In, Out, R>.or(other: Parser<In, Out, R>): Parser<In, Out, R> {
    return Parser { input ->
        this.parse(input) + other.parse(input)
    }
}

fun <In, Out, R> sub(p: Parser<In, Out, R>): Parser<In, In, R> {
    return Parser { input ->
        p.parse(input).map { r -> ParserResult(input, r.res) }
    }
}

fun <In, Out, Out2, R, R2> Parser<In, Out, R>.that(constraint: Parser<Out, Out2, R2>): Parser<In, Out, R> {
    return this seql sub(constraint)
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

typealias StringParser<R> = Parser<StringPos, StringPos, R>

data class StringPos(val s: String, val pos: Int) {
    constructor(s: String) : this(s, 0)

    fun move(d: Int) = StringPos(s, pos + d)

    fun startsWith(sub: String) = s.startsWith(sub, pos)
}

fun <S, R> success(v: R): Parser<S, S, R> = Parser { s -> sequenceOf(ParserResult(s, v)) }
fun <S, R> fail(): Parser<S, S, R> = Parser { emptySequence() }

val <S, R> Parser<S, S, R>.many: Parser<S, S, Sequence<R>>
    get() = fix { manyP ->
        success<S, Sequence<R>>(emptySequence()) or
                ((this seq manyP) using { head, tail -> sequenceOf(head) + tail })
    }


val String.l
    get() = StringParser { i ->
        if (i.startsWith(this))
            sequenceOf(ParserResult(i.move(this.length), this))
        else
            emptySequence()
    }

data class SimpleNode(val value: String)
data class SimpleEdge(val label: String)

class Graph<N, E> {
    val nodesEdges = HashMap<N, MutableList<Pair<E, N>>>()
    val nodes = HashSet<N>()

    fun addNode(node: N) {
        nodes.add(node)
    }

    fun addEdge(u: N, edge: E, v: N) {
        addNode(u)
        addNode(v)
        nodesEdges.getOrPut(u) { ArrayList() }.add(Pair(edge, v))
    }

    fun <Out, R> applyParser(parser: Parser<StartState<N, E>, Out, R>): Sequence<R> {
        return parser.parse(StartState(this)).map { it.res }
    }
}

data class StartState<N, E>(val graph: Graph<N, E>)
data class NodeState<N, E>(val graph: Graph<N, E>, val node: N)
data class EdgeState<N, E>(val graph: Graph<N, E>, val edge: E, val outNode: N)


fun <N, E> outV(p: (N) -> Boolean): Parser<EdgeState<N, E>, NodeState<N, E>, N> {
    return Parser { (gr, _, outV) ->
        if (!p(outV)) return@Parser emptySequence()
        sequenceOf(ParserResult(NodeState(gr, outV), outV))
    }
}

fun <N, E> outV(): Parser<EdgeState<N, E>, NodeState<N, E>, N> = outV { true }

fun <N, E> v(p: (N) -> Boolean): Parser<StartState<N, E>, NodeState<N, E>, N> {
    return Parser { (gr) ->
        gr.nodes.asSequence().filter(p).map { ParserResult(NodeState(gr, it), it) }
    }
}

fun <N, E> v(): Parser<StartState<N, E>, NodeState<N, E>, N> = v { true }

fun <N, E> edge(p: (E) -> Boolean): Parser<NodeState<N, E>, EdgeState<N, E>, E> {
    return Parser { (gr, node) ->
        val edges = gr.nodesEdges[node] ?: return@Parser emptySequence()
        edges.asSequence().filter { p(it.first) }.map { (edge, outV) -> ParserResult(EdgeState(gr, edge, outV), edge) }
    }
}


typealias GraphNodeParser<R> = Parser<NodeState<SimpleNode, SimpleEdge>, EdgeState<SimpleNode, SimpleEdge>, R>
typealias GraphEdgeParser<R> = Parser<NodeState<SimpleNode, SimpleEdge>, EdgeState<SimpleNode, SimpleEdge>, R>
typealias NodeS = NodeState<SimpleNode, SimpleEdge>
typealias EdgeS = EdgeState<SimpleNode, SimpleEdge>


fun main() {
    //graph()
    graphFilter()
    //ambigious()
    //leftRec()
}

fun graph() {
    val gr = Graph<SimpleNode, SimpleEdge>().apply {
        val nA = SimpleNode("A")
        val eB = SimpleEdge("B")
        val eC = SimpleEdge("C")
        addEdge(nA, eB, nA)
        addEdge(nA, eC, nA)
    }

    val nodeA = v<SimpleNode, SimpleEdge> { it.value == "A" }
    val outNodeA = outV<SimpleNode, SimpleEdge> { it.value == "A" }
    val edgeB = edge<SimpleNode, SimpleEdge> { it.label == "B" }
    val edgeC = edge<SimpleNode, SimpleEdge> { it.label == "C" }

    val p = nodeA seq ((edgeB or edgeC) seq outNodeA).many

    gr.applyParser(p)
        .take(20)
        .forEach { (first, seq) ->
            print("(${first.value})")
            println(seq.joinToString("") { (e, n) -> "-${e.label}->(${n.value})" })
        }
}

fun graphFilter() {
    val gr = Graph<SimpleNode, SimpleEdge>().apply {
        val friend = SimpleEdge("friend")
        val loves = SimpleEdge("loves")
        val dan = SimpleNode("Dan")
        val john = SimpleNode("John")
        val mary = SimpleNode("Mary")
        val linda = SimpleNode("Linda")
        addEdge(dan, friend, john)
        addEdge(dan, loves, mary)
        addEdge(john, friend, linda)
    }

    val person = v<SimpleNode, SimpleEdge>()
    val mary = outV<SimpleNode, SimpleEdge> { it.value == "Mary" }
    val loves = edge<SimpleNode, SimpleEdge> { it.label == "loves" }
    val friend = edge<SimpleNode, SimpleEdge> { it.label == "friend" }
    val maryLover = person.that(loves seq mary)
    val parser = maryLover seq friend seq outV()

    gr.applyParser(parser)
        .forEach { println(it) }
}

fun test1() {
    val p = v<SimpleNode, SimpleEdge> { it.value == "Dan" } seqr edge { it.label == "loves" } seqr outV()
}

fun ambigious() {
    val a = "a".l
    val ambigious: StringParser<String> = fix { S ->
        (a seqr S seql a).many using { s -> s.joinToString(separator = "") { "[a${it}a]" } }
    }

    val str2 = "aaaaaa"
    ambigious.parse(StringPos(str2)).forEach { println(it) }

}

fun brackets() {
    val brackets: StringParser<String> = fix { brackets ->
        ("[".l seqr brackets seql "]".l seq brackets) using { s1, s2 -> "[$s1]$s2" } or
                "".l
    }

    val str = "[][[][]]"
    brackets.parse(StringPos(str)).forEach { println(it) }
}

fun leftRec() {
    val a = "a".l
    val p: StringParser<String> = fix { S ->
        (S seql a) or "".l
    }

    val str2 = "aaaaaa"
    p.parse(StringPos(str2)).forEach { println(it) }
}