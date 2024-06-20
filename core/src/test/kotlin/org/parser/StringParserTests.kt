package org.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.parser.combinators.string.StringParser

import org.parser.combinators.string.applyParser
import org.parser.combinators.string.p
import org.junit.jupiter.api.Test
import org.parser.ParserTests.saveDotsToFolder
import org.parser.combinators.*
import org.parser.combinators.string.StringPos


class StringParserTests {
    @Test
    fun simple() {
        val s = "a".p
        val str = "a"
        val results = str.applyParser(s).filter { it.rightState.pos == str.length }
        assertEquals(1, results.size)
        assertEquals(setOf("a"), results[0].getResults().toSet())
    }

    @Test
    fun simpleAlternation() {
        val s1 = "A".p seq "B".p
        val s2 = "A".p seq "C".p
        val s = s1 or s2
        val str = "AB"
        val results = str.applyParser(s).filter { it.rightState.pos == str.length }
        assertEquals(1, results.size)
        assertEquals(setOf(Pair("A", "B")), results[0].getResults().toSet())
    }

    @Test
    fun sameTerminalParserWithDifferentAction() {
        val s = "a".p using { _ -> 42 } or ("a".p using { _ -> 24 })
        val str = "a"
        val nodes = str.applyParser(s)
        assertEquals(1, nodes.size)
        assertEquals(setOf(42, 24), nodes[0].getResults().toSet())
    }

    @Test
    fun sameEpsilonParserWithDifferentAction() {
        val eps = eps<StringPos>()
        val s = eps using { _ -> 42 } or (eps using { _ -> 24 })
        val str = "a"
        val nodes = str.applyParser(s)
        val results = nodes.flatMap { it.getResults() }
        assertEquals(setOf(42, 24), results.toSet())
    }

    @Test
    fun simpleAmbiguous() {
        val str = "aa"

        val s: StringParser<String> = rule(
            ("a".p seq "a".p) using { _, _ -> "[a][a]" },
            "aa".p using { "[aa]" }
        )
        val nodes = str.applyParser(s)
        saveDotsToFolder(nodes, "simpleAmbiguous")

        val results = nodes.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(setOf("[aa]", "[a][a]"), results[0].toSet())

        val expr = LateInitParser<StringPos, StringPos, Int>()
        expr.init(rule(
            (expr seql "+".p seq expr) using { x, y -> x + y },
            (expr seql "−".p seq expr) using { x, y -> x - y },
            "(".p seqr expr seql ")".p,
            "[0−9]*".toRegex().p using { it.toInt() }))
    }

    @Test
    fun ambiguousWithFix() {
        val str = "aaaaaa"
        val a = "a".p
        val s by fix("s") { s ->
            (a seqr s seql a).many using { res -> res.joinToString("") { "[a${it}a]" } }
        }

        val nodes = str.applyParser(s)
        saveDotsToFolder(nodes, "ambiguous")

        val results = nodes.filter { it.rightState.pos == str.length }.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(
            setOf("[aa][aa][aa]", "[aa][a[aa]a]", "[a[aa]a][aa]", "[a[aa][aa]a]", "[a[a[aa]a]a]"),
            results[0].toSet()
        )
    }

    @Test
    fun ambiguousWithRule() {
        val s by LateInitParser<StringPos, StringPos, String>()
        s.init(rule(
            ("a".p seqr s seql "a".p seq s) using { s1, s2 -> "[a${s1}a]$s2" },
            "".p
        ))

        val str = "aaaa"
        val nodes = str.applyParser(s)
        saveDotsToFolder(nodes, "ambiguous2")
        val results = nodes.filter { it.rightState.pos == str.length }.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(setOf("[aa][aa]", "[a[aa]a]"), results[0].toSet())
    }

    @Test
    fun brackets() {
        val s: StringParser<String> = fix("S") { s ->
            rule(
                ("[".p seqr s seql "]".p seq s) using { s1, s2 -> "[$s1]$s2" },
                "".p
            )
        }

        val str = "[][[]][[][]]"
        val nodes = str.applyParser(s)
        saveDotsToFolder(nodes, "brackets")
        val results = nodes.filter { it.rightState.pos == str.length }.map { it.getResults() }
        assertEquals(1, results.size)
        assertEquals(setOf("[][[]][[][]]"), results[0].toSet())
    }

    @Test
    fun leftRec() {
        val a = "a".p
        val s = fix("S") { s ->
            rule(
                s seq a using { a, b -> "$a$b" },
                a
            )
        }

        val str = "aaaa"
        val nodes = str.applyParser(s)

        saveDotsToFolder(nodes, "leftRec")
        val results = nodes.map { it.getResults().toList() }.onEach { assertEquals(1, it.size) }.map { it[0] }
        assertEquals(setOf("a", "aa", "aaa", "aaaa"), results.toSet())
    }

    @Test
    fun infinite() {
        val p = ("".p).many

        val str = "aaaa"
        val nodes = str.applyParser(p)
        saveDotsToFolder(nodes, "infinite")
    }

    @Test
    fun expressions() {
        val expr = LateInitParser<StringPos, StringPos, Int>()
        expr.init(rule(
            expr seql "+".p seq expr using { x: Int, y: Int -> x + y },
            expr seql "-".p seq expr using { x: Int, y: Int -> x - y },
            "(".p seqr expr seql ")".p,
            "[0-9]*".toRegex().p using { it.toInt() }
        ))
        val str = "10+(20-5)"
        val nodes = str.applyParser(expr).filter { it.rightState.pos == str.length }
        assertEquals(1, nodes.size)
        assertEquals(listOf(25), nodes[0].getResults().toList())
    }
}