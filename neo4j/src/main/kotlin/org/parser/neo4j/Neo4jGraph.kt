package org.parser.neo4j

import org.neo4j.graphdb.*
import org.parser.combinators.graph.Graph

open class Neo4jNode(val id: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Neo4jNode) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

open class Neo4jEdge(val id: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Neo4jEdge) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

interface Neo4jGraphFactory<N : Neo4jNode, E : Neo4jEdge> {
    fun createNode(neo4jNode: Node): N
    fun createEdge(neo4jRelationship: Relationship): E
}

open class Neo4jGraph<N : Neo4jNode, E : Neo4jEdge>(
    db: GraphDatabaseService,
    private val graphFactory: Neo4jGraphFactory<N, E>
) : Graph<N, E>, AutoCloseable {

    private val tx: Transaction = db.beginTx()

    override fun getOutgoingEdges(v: N): List<E>? {
        return try {
            tx.getNodeById(v.id).getRelationships(Direction.OUTGOING).map { graphFactory.createEdge(it) }
        } catch (_: NotFoundException) {
            null
        }

    }

    override fun getIncomingEdges(v: N): List<E>? {
        return try {
            tx.getNodeById(v.id).getRelationships(Direction.INCOMING).map { graphFactory.createEdge(it) }
        } catch (_: NotFoundException) {
            null
        }
    }

    override fun getVertexes(): Iterable<N> {
        return tx.allNodes.asSequence().map { graphFactory.createNode(it) }.asIterable()
    }

    override fun getEdgeVertexes(e: E): Pair<N, N>? {
        return try {
            val nodes = tx.getRelationshipById(e.id).nodes
            val first = graphFactory.createNode(nodes[0])
            val last = graphFactory.createNode(nodes[1])
            return Pair(first, last)
        } catch (_: NotFoundException) {
            null
        }
    }

    override fun close() {
        tx.close()
    }
}



