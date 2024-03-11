package org.example

typealias StringParser<R> = Parser<String, StringPos, StringPos, R>

data class StringPos(val pos: Int) {
    fun move(d: Int) = StringPos(pos + d)

}

fun <R> String.applyParser(parser: StringParser<R>, count: Int = -1): List<Pair<StringPos, R>> {
    return parser.getResults(this, StringPos(0), count).map { it }
}

val String.p: StringParser<String>
    get() = StringParser.make("literal") { str, i ->
        if (str.startsWith(this, i.pos))
            success(i.move(this.length), this)
        else
            failure()
    }

object StringParsers: Parsers<String>
