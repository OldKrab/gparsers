package org.example

typealias StringParser<R> = Parser<String, StringPos, StringPos, R>

data class StringPos(val pos: Int) {
    fun move(d: Int) = StringPos(pos + d)

}

val String.l
    get() = StringParser { str, i ->
        if (str.startsWith(this, i.pos))
            sequenceOf(ParserResult(i.move(this.length), this))
        else
            emptySequence()
    }

object StringParsers: Parsers<String>
