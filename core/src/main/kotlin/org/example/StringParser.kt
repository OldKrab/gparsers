package org.example

typealias StringParser<R> = Parser<StringPos, StringPos, R>

data class StringPos(val s: String, val pos: Int) {
    constructor(s: String) : this(s, 0)

    fun move(d: Int) = StringPos(s, pos + d)

    fun startsWith(sub: String) = s.startsWith(sub, pos)
}

val String.l
    get() = StringParser { i ->
        if (i.startsWith(this))
            sequenceOf(ParserResult(i.move(this.length), this))
        else
            emptySequence()
    }
