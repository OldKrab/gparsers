package org.parser.combinators.string

import org.parser.combinators.Combinators
import org.parser.combinators.Parser
import org.parser.combinators.ParserResult

typealias StringParser<R> = Parser<String, StringPos, StringPos, R>

data class StringPos(val pos: Int) {
    fun move(d: Int) = StringPos(pos + d)

}

fun <R> String.applyParser(parser: StringParser<R>, count: Int = -1): List<Pair<StringPos, R>> {
    return parser.getResults(this, StringPos(0), count).map { it }
}

val String.p: StringParser<String>
    get() = Parser.make("literal") { str, i ->
        if (str.startsWith(this, i.pos))
            ParserResult.success(i.move(this.length), this)
        else
            ParserResult.failure()
    }

object StringCombinators: Combinators<String>
