data class ParserResult<OutS, R>(val outState: OutS, val res: R)

class Parser<InS, OutS, R>(val inner: (InS) -> Sequence<ParserResult<OutS, R>>) {
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

fun <In, Out, R> exists(p: Parser<In, Out, R>, input: In): Boolean {
    return p.parse(input).iterator().hasNext()
}

fun <In, Out, R> Parser<In, Out, R>.filter(p: (In) -> Boolean): Parser<In, Out, R> {
    return Parser { input ->
        if (p(input))
            this.parse(input)
        else
            emptySequence()
    }
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
    val matrix = HashMap<N, MutableList<Pair<E, N>>>()
    val nodes: MutableSet<N> = HashSet()
    val edges: MutableSet<E> = HashSet()

    fun addNode(node: N) {
        nodes.add(node)
    }

    fun addEdge(u: N, edge: E, v: N) {
        edges.add(edge)
        addNode(u)
        addNode(v)
        matrix.getOrPut(u) { ArrayList() }.add(Pair(edge, v))
    }

    fun getEdges(v: N): MutableList<Pair<E, N>>? = matrix[v]

    fun <Out, R> applyParser(parser: Parser<NodeState<N, E>, Out, R>): Sequence<R> {
        val gr = this
        return sequence {
            for (node in nodes) {
                yieldAll(parser.parse(NodeState(gr, node)).map { it.res })
            }
        }
    }
}

data class NodeState<N, E>(val graph: Graph<N, E>, val node: N)
data class EdgeState<N, E>(val graph: Graph<N, E>, val edge: E, val endNode: N)


fun <N, E> vThat(p: (N) -> Boolean): Parser<NodeState<N, E>, EdgeState<N, E>, N> {
    return Parser { (gr, node) ->
        val edges = gr.getEdges(node)
        val edgeStates = if (p(node) && edges != null)
            edges.map { (edge, endNode) -> EdgeState(gr, edge, endNode) }.asSequence()
        else
            emptySequence()
        edgeStates.map { ParserResult(it, node) }
    }
}

fun <N, E> v(): Parser<NodeState<N, E>, EdgeState<N, E>, N> = vThat { true }

fun <N, E> edgeThat(p: (E) -> Boolean): Parser<EdgeState<N, E>, NodeState<N, E>, E> {
    return Parser { (gr, edge, endNode) ->
        val nodeStates = if (p(edge))
            sequenceOf(NodeState(gr, endNode))
        else
            emptySequence()
        nodeStates.map { ParserResult(it, edge) }
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


    val nodeA = vThat<SimpleNode, SimpleEdge> { it.value == "A" }
    val edgeB = edgeThat<SimpleNode, SimpleEdge> { it.label == "B" }
    val edgeC = edgeThat<SimpleNode, SimpleEdge> { it.label == "C" }

    val p = ((nodeA seq edgeB) or (nodeA seq edgeC)).many

    gr.applyParser(p)
        .take(20)
        .forEach { println(it.joinToString("") { (n, e) -> "(${n.value})-${e.label}->" }) }
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


    val p = v<SimpleNode, SimpleEdge>()
    val mary = vThat<SimpleNode, SimpleEdge> { it.value == "Mary" }
    val loves = edgeThat<SimpleNode, SimpleEdge> { it.label == "loves" }
    val friend = edgeThat<SimpleNode, SimpleEdge> { it.label == "friend" }
    val parser = p seq friend seq p

    gr.applyParser(parser)
        .forEach { println(it) }
}

fun test1() {
    val p = vThat<SimpleNode, SimpleEdge> { it.value == "Dan" } seqr edgeThat { it.label == "LOVES" } seqr v()
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