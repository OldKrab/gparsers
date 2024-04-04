package org.parser.sppf

import org.parser.sppf.node.*

interface SPPFVisitor {
    fun <LS, MS, RS, R1, R2> visit(n: PackedNode<LS, MS, RS, R1, R2>)
    fun <LS, RS, R, CR1, CR2> visit(n: IntermediateNode<LS, RS, R, CR1, CR2>)
    fun <LS, RS, R, R2> visit(n: TerminalNode<LS, RS, R, R2>)
    fun <S, R> visit(n: EpsilonNode<S, R>)
    fun <LS, RS, R, CR1, CR2> visit(n: NonTerminalNode<LS, RS, R, CR1, CR2>)



}