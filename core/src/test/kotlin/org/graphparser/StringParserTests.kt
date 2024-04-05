package org.graphparser

import org.junit.jupiter.api.Assertions.assertEquals
import org.parser.combinators.string.StringParser

import org.parser.combinators.string.applyParser
import org.parser.combinators.string.p
import org.junit.jupiter.api.Test
import org.parser.combinators.*
import org.parser.combinators.string.StringPos


class StringParserTests :ParserTests() {

    @Test
    fun simple() {
        val s = "a".p
        val str = "a"
        val results = str.applyParser(s).filter { it.rightState.pos == str.length }
        assertEquals(1, results.size)
        assertEquals(setOf("a"), results[0].getResults().toSet())
    }

    @Test
    fun sameTerminalParserWithDifferentAction() {
        val s = "a".p using { _ -> 42 } or ("a".p using { _ -> 24})
        val str = "a"
        val nodes = str.applyParser(s)
        assertEquals(1, nodes.size)
        assertEquals(setOf(42, 24), nodes[0].getResults().toSet())
    }

    @Test
    fun sameEpsilonParserWithDifferentAction() {
        val s = eps<StringPos>() using { _ -> 42 } or (eps<StringPos>() using { _ -> 24})
        val str = "a"
        val nodes = str.applyParser(s)
        assertEquals(1, nodes.size)
        assertEquals(setOf(42, 24), nodes[0].getResults().toSet())
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
        val s: StringParser<String> = fix("S") { s ->
            rule(
                ("a".p seqr s seql "a".p seq s) using { s1, s2 -> "[a${s1}a]$s2" },
                "".p
            )
        }

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
}