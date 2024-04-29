package org.parser.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.NotFoundException
import org.neo4j.graphdb.Relationship
import org.parser.combinators.graph.Graph

open class Neo4jNode(val elementId: String)

open class Neo4jEdge(val elementId: String)

interface Neo4jGraphFactory<N : Neo4jNode, E : Neo4jEdge> {
    fun createNode(neo4jNode: Node): N
    fun createEdge(neo4jRelationship: Relationship): E
}

class Neo4jGraph<N : Neo4jNode, E : Neo4jEdge>(
    private val db: GraphDatabaseService,
    private val graphFactory: Neo4jGraphFactory<N, E>
) : Graph<N, E> {

    override fun getEdges(v: N): List<E>? {
        return db.beginTx().use { tx ->
            try {
                tx.getNodeByElementId(v.elementId).relationships.map { graphFactory.createEdge(it) }
            } catch (_: NotFoundException) {
                null
            }
        }
    }

    override fun getVertexes(): Set<N> {
        return db.beginTx().use { tx ->
            tx.allNodes.map { graphFactory.createNode(it) }.toSet()
        }
    }

    override fun getEdgeVertexes(e: E): Pair<N, N>? {
        return db.beginTx().use { tx ->
            try {
                val nodes = tx.getRelationshipByElementId(e.elementId).nodes
                val first = graphFactory.createNode(nodes[0])
                val last = graphFactory.createNode(nodes[1])
                return Pair(first, last)
            } catch (_: NotFoundException) {
                null
            }
        }
    }
}



