package org.parser.combinators

import java.util.*
import kotlin.collections.ArrayDeque

object Trampoline {
     val calls: Queue<() -> Unit> = LinkedList()

    fun call(f: () -> Unit) {
        calls.add(f)
    }

    fun runLoop() {
        while (calls.isNotEmpty()) {
            val call = calls.remove()
            call()
        }
    }
}